package com.framecoach.app.ui.overlay

/**
 * Detects the rising edge of the "good zone" state (T8).
 *
 * Returns `true` **only** on the transition from not-good → good, i.e. the
 * first frame where [isGood] is true after one or more frames where it was
 * false.  Subsequent calls with [isGood] = true return `false` until the
 * state has toggled back to false and then to true again.
 *
 * This provides the debounce required by the T8 acceptance criteria:
 *   "Single distinct pulse on entering a good zone; no repeat pulse until the
 *    frame leaves and re-enters that state."
 *
 * Pure Kotlin — no Android framework dependencies, so it can be tested directly
 * with JUnit without a device or Robolectric.
 */
class GoodZoneEdgeDetector {

    /**
     * Tracks the composition state from the *previous* call.
     * Starts as `true` so that the very first frame, if already good, does NOT
     * fire a spurious pulse — the user hasn't done anything yet.
     */
    private var previouslyGood: Boolean = true

    /**
     * Feed the latest [isGood] value from [CompositionState].
     *
     * @param isGood Current "good zone" flag from [CompositionSuggestion.isGood].
     * @return `true` if this is a rising edge (false → true transition); `false` otherwise.
     */
    fun onUpdate(isGood: Boolean): Boolean {
        val risingEdge = isGood && !previouslyGood
        previouslyGood = isGood
        return risingEdge
    }

    /**
     * Reset internal state — call when the camera is re-bound or the screen is
     * re-entered so that a stale "previously good" value doesn't suppress the
     * first real pulse.
     */
    fun reset() {
        previouslyGood = true
    }
}
