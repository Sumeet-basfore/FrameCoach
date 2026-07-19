package com.framecoach.app.detection

/**
 * Adaptive frame-skip gate for the detection pipeline (T7).
 *
 * Tracks a rolling average of recent per-frame detection latency and decides
 * whether each incoming frame should be processed or dropped, so that the
 * camera preview never has to wait for an in-flight inference call.
 *
 * ## Algorithm
 *
 * We maintain an exponential moving average (EMA) of measured processing times:
 *   ema = α * latestSampleMs + (1 - α) * ema
 *
 * The skip interval [currentSkipInterval] starts at 1 (process every frame) and
 * is clamped between [MIN_SKIP_INTERVAL] and [MAX_SKIP_INTERVAL]:
 *
 * - If ema > [SLOW_THRESHOLD_MS]  → increase skip interval by 1 (drop more frames)
 * - If ema < [FAST_THRESHOLD_MS]  → decrease skip interval by 1 (process more frames)
 *
 * An internal frame counter advances on every [shouldProcess] call; when the
 * counter mod [currentSkipInterval] == 0, the frame is processed.
 *
 * ## Thread safety
 *
 * All mutable state is `@Volatile` so reads/writes are atomic.  The gate is
 * designed to be called from a single background dispatcher (Dispatchers.Default
 * in [FrameProcessor]) and mutated from the same thread, so no locking is needed.
 *
 * @param slowThresholdMs   EMA latency above which the skip interval increases.
 *                          Default 80 ms → roughly the budget for ≤12 fps detection
 *                          while the viewfinder runs at 30 fps.
 * @param fastThresholdMs   EMA latency below which the skip interval decreases
 *                          (recovering throughput after a thermal event).
 *                          Default 40 ms.
 * @param emaAlpha          Smoothing factor for the EMA (0 < α ≤ 1).
 *                          Smaller values react more slowly to spikes.
 */
class AdaptiveFrameGate(
    internal val slowThresholdMs: Long = SLOW_THRESHOLD_MS,
    internal val fastThresholdMs: Long = FAST_THRESHOLD_MS,
    private val emaAlpha: Double = EMA_ALPHA,
) {
    companion object {
        /** Skip interval never goes below this — process at least every frame. */
        const val MIN_SKIP_INTERVAL = 1

        /** Skip interval cap — never skip more than 7 of every 8 frames (≈3-4 fps detection). */
        const val MAX_SKIP_INTERVAL = 8

        /** EMA latency above which we slow down. */
        const val SLOW_THRESHOLD_MS = 80L

        /** EMA latency below which we speed back up. */
        const val FAST_THRESHOLD_MS = 40L

        /** Smoothing factor: react gradually to latency trends, not individual spikes. */
        const val EMA_ALPHA = 0.2
    }

    /** Counts every call to [shouldProcess], including skipped frames. */
    @Volatile
    private var frameCounter: Long = 0L

    /** Current skip interval in number of frames (1 = process all, 2 = process every other…). */
    @Volatile
    var currentSkipInterval: Int = MIN_SKIP_INTERVAL
        private set

    /** Exponential moving average of detection latency in milliseconds. */
    @Volatile
    var emaMs: Double = 0.0
        private set

    /** Current thermal status (0 = none, 1-2 = light/moderate, 3+ = severe). */
    @Volatile
    var thermalStatus: Int = 0
        set(value) {
            field = value
            applyThermalThrottling()
        }

    /**
     * Decide whether this frame should be sent to the detector.
     *
     * Must be called once per incoming [ImageProxy], regardless of whether
     * detection runs.  The internal counter advances on every call.
     *
     * @return true if detection should run; false if the frame should be dropped
     *         (caller must still close the [ImageProxy]).
     */
    fun shouldProcess(): Boolean {
        val count = frameCounter++
        return (count % currentSkipInterval) == 0L
    }

    /**
     * Record the wall-clock time it took to run a single detection pass and
     * adjust the skip interval accordingly.
     *
     * Should be called only when [shouldProcess] returned true and detection
     * actually ran.
     *
     * @param elapsedMs Wall-clock milliseconds for the completed detection call.
     */
    fun recordLatency(elapsedMs: Long) {
        // Update EMA.
        emaMs = if (emaMs == 0.0) {
            elapsedMs.toDouble() // Seed with first real sample.
        } else {
            emaAlpha * elapsedMs + (1.0 - emaAlpha) * emaMs
        }

        // Adapt skip interval based on the smoothed latency trend.
        var newInterval = when {
            emaMs > slowThresholdMs && currentSkipInterval < MAX_SKIP_INTERVAL ->
                currentSkipInterval + 1
            emaMs < fastThresholdMs && currentSkipInterval > MIN_SKIP_INTERVAL ->
                currentSkipInterval - 1
            else -> currentSkipInterval
        }

        // Apply thermal throttling overrides
        if (thermalStatus >= 3) {
            newInterval = MAX_SKIP_INTERVAL
        } else if (thermalStatus >= 1) {
            newInterval = Math.max(newInterval, 3)
        }

        currentSkipInterval = newInterval.coerceIn(MIN_SKIP_INTERVAL, MAX_SKIP_INTERVAL)
    }

    private fun applyThermalThrottling() {
        if (thermalStatus >= 3) {
            currentSkipInterval = MAX_SKIP_INTERVAL
        } else if (thermalStatus >= 1) {
            currentSkipInterval = Math.max(currentSkipInterval, 3)
        }
    }

    /**
     * Reset all state — useful for testing and when the camera is re-bound.
     */
    fun reset() {
        frameCounter = 0L
        currentSkipInterval = MIN_SKIP_INTERVAL
        emaMs = 0.0
        thermalStatus = 0
    }
}
