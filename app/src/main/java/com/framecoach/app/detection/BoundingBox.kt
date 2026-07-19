package com.framecoach.app.detection

/**
 * Normalised bounding box for a detected object.
 *
 * All coordinates are in [0..1] range relative to the frame width/height,
 * so that callers (the overlay renderer, the rules engine) don't need to
 * worry about the analysis frame's resolution.
 *
 * @property label   Object category label, e.g. "person", "dog", "cell phone".
 * @property confidence  Detection confidence score [0..1].
 * @property left    Normalised left edge (0 = left of frame, 1 = right).
 * @property top     Normalised top edge (0 = top of frame, 1 = bottom).
 * @property right   Normalised right edge.
 * @property bottom  Normalised bottom edge.
 */
data class BoundingBox(
    val label: String,
    val confidence: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val area: Float get() = width * height
}
