package com.framecoach.app.detection

import java.nio.ByteBuffer

/**
 * Luminance-based exposure analysis for T12.
 *
 * Samples the Y (luminance) plane of a camera frame on a regular grid and
 * classifies the scene as well-exposed, overexposed, or underexposed.
 *
 * ## Algorithm
 *
 * A fixed grid of pixels is sampled from the Y plane.  The mean luminance
 * is compared against two thresholds:
 *   - Below [UNDEREXPOSED_THRESHOLD] → underexposed (too dark)
 *   - Above [OVEREXPOSED_THRESHOLD] → overexposed (too bright)
 *
 * ## Performance
 *
 * With a 32×32 grid (1024 samples) the scan completes in well under 1ms on
 * any modern device — negligible compared to the ~50–300ms detection latency.
 *
 * Pure Kotlin — no Android framework dependencies.  Testable via JUnit.
 */
class ExposureAnalyzer {

    companion object {
        /** Luminance below this value is considered underexposed. */
        const val UNDEREXPOSED_THRESHOLD = 0.25f

        /** Luminance above this value is considered overexposed. */
        const val OVEREXPOSED_THRESHOLD = 0.85f

        /** Grid dimension for Y-plane sampling (gridSize × gridSize total samples). */
        const val GRID_SIZE = 32

        /** Luminance range is [0, 255] for an 8-bit Y plane. */
        private const val MAX_8BIT = 255f
    }

    /**
     * Analyse a camera frame's luminance via its Y plane.
     *
     * @param yBuffer  Direct access to the Y (luminance) plane of the frame.
     *                 Position and limit should be at the start and end of
     *                 the visible Y data (width × height bytes).
     * @param width    Width of the Y plane in pixels.
     * @param height   Height of the Y plane in pixels.
     * @return         [ExposureResult] with the mean luminance and a warning flag.
     */
    fun analyse(yBuffer: ByteBuffer, width: Int, height: Int): ExposureResult {
        val xStep = maxOf(1, width / GRID_SIZE)
        val yStep = maxOf(1, height / GRID_SIZE)

        var totalLuma = 0.0
        var sampleCount = 0

        // Duplicate buffer so we don't disturb the caller's position.
        val buf = yBuffer.duplicate()
        buf.rewind()

        for (y in 0 until height step yStep) {
            for (x in 0 until width step xStep) {
                val index = y * width + x
                if (index >= buf.capacity()) continue
                buf.position(index)
                val pixel = buf.get().toInt() and 0xFF
                totalLuma += pixel / MAX_8BIT
                sampleCount++
            }
        }

        val meanLuminance = if (sampleCount > 0) (totalLuma / sampleCount).toFloat() else 0.5f

        return ExposureResult(
            meanLuminance = meanLuminance,
            isUnderexposed = meanLuminance < UNDEREXPOSED_THRESHOLD,
            isOverexposed = meanLuminance > OVEREXPOSED_THRESHOLD,
        )
    }
}

/**
 * Result of a single exposure analysis pass.
 *
 * @property meanLuminance  Average normalised luminance [0..1]
 *                          0 = pure black, 1 = pure white.
 * @property isUnderexposed True when [meanLuminance] < [ExposureAnalyzer.UNDEREXPOSED_THRESHOLD].
 * @property isOverexposed  True when [meanLuminance] > [ExposureAnalyzer.OVEREXPOSED_THRESHOLD].
 */
data class ExposureResult(
    val meanLuminance: Float,
    val isUnderexposed: Boolean,
    val isOverexposed: Boolean,
) {
    /** True when the scene is significantly over- or underexposed. */
    val isWarning: Boolean get() = isUnderexposed || isOverexposed
}
