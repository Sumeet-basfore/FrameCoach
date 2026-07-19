package com.framecoach.app.rules

/**
 * Result of composition analysis: a directional suggestion paired with
 * a "good zone" flag.
 *
 * [direction] — which way to move the camera, or [Direction.NONE] when the
 *                 subject is already well-composed.
 * [isGood]      — true when the subject is in a good zone (no movement needed).
 *
 * Convenience extensions are provided for common callers (T5 overlay, T8 haptics).
 */
data class CompositionSuggestion(
    val direction: Direction,
    val isGood: Boolean,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val fillRatio: Float = 0f,
) {
    companion object {
        /** Subject fills the frame nicely and sits within the rule-of-thirds band. */
        val GOOD = CompositionSuggestion(Direction.NONE, isGood = true)

        /** Factory for directional suggestions. */
        fun move(
            direction: Direction,
            offsetX: Float = 0f,
            offsetY: Float = 0f,
            fillRatio: Float = 0f
        ) = CompositionSuggestion(
            direction = direction,
            isGood = false,
            offsetX = offsetX,
            offsetY = offsetY,
            fillRatio = fillRatio
        )
    }
}

/**
 * Cardinal / depth direction for camera movement.
 *
 * When [isGood] is false in [CompositionSuggestion], the UI should display
 * [displayText] and optionally show an arrow pointing in this direction.
 */
enum class Direction(val displayText: String) {
    LEFT   ("← move left"),
    RIGHT  ("move right →"),
    UP     ("↑ move up"),
    DOWN   ("move down ↓"),
    CLOSER ("get closer ↑"),
    AWAY   ("step back ↓"),
    NONE   ("good"),
    ;
}