package com.framecoach.app.sensors

import kotlin.math.abs

/**
 * Pure-Kotlin horizon-level calculator for T9.
 *
 * Maps a raw roll angle (in degrees, sourced from the accelerometer) to a
 * [HorizonLevelState] that the overlay and any future consumers can use
 * without touching Android APIs.
 *
 * **Coordinate convention**: positive roll = device tilted clockwise (right
 * side down); negative roll = device tilted counter-clockwise (left side down).
 * At exactly 0°, the device is perfectly level.
 *
 * **Threshold hysteresis** is deliberately *not* included here — the overlay's
 * alpha / animation is a better place to smooth visual transitions.  The
 * calculator stays simple: in-band → level, out-of-band → not level.
 */
object HorizonLevelCalculator {

    /**
     * Roll deviation (degrees) within which the device is considered level.
     * ±2° is a common professional-camera guideline and is imperceptible in a
     * finished photo.
     */
    const val LEVEL_THRESHOLD_DEG = 2.0f

    /**
     * Compute the horizon-level state from a raw roll angle.
     *
     * @param rollDeg   Roll angle in degrees, as produced by [HorizonSensor].
     * @param threshold Threshold in degrees; defaults to [LEVEL_THRESHOLD_DEG].
     *                  Exposed as a parameter so tests can probe boundary behaviour
     *                  without hardcoding the production constant.
     * @return          [HorizonLevelState] describing whether the device is level
     *                  and the normalised tilt fraction for the overlay.
     */
    fun compute(
        rollDeg: Float,
        rollOffset: Float = 0.0f,
        threshold: Float = LEVEL_THRESHOLD_DEG,
    ): HorizonLevelState {
        val calibratedRoll = rollDeg - rollOffset
        val isLevel = abs(calibratedRoll) <= threshold
        // Normalise roll to [-1, 1] clamped at ±45° so the overlay line doesn't
        // go off-screen on extreme tilts.
        val clampedRoll = calibratedRoll.coerceIn(-45f, 45f)
        val normalisedTilt = clampedRoll / 45f
        return HorizonLevelState(
            rollDeg = calibratedRoll,
            isLevel = isLevel,
            normalisedTilt = normalisedTilt,
        )
    }
}

/**
 * Snapshot of the horizon-level computation for a single sensor reading.
 *
 * @property rollDeg        Raw roll angle in degrees from the accelerometer.
 * @property isLevel        True when |rollDeg| ≤ threshold.
 * @property normalisedTilt Roll clamped to ±45° and scaled to [-1, 1].
 *                          Used by the overlay to position the indicator line.
 */
data class HorizonLevelState(
    val rollDeg: Float,
    val isLevel: Boolean,
    val normalisedTilt: Float,
)
