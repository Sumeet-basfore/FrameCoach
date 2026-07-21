package com.framecoach.app.rules

import com.framecoach.app.detection.BoundingBox

/**
 * Composition rules engine — pure functions with no Android dependencies.
 *
 * Three camera modes:
 * - `"general"` — rule-of-thirds positioning, multi-peak fill ratio
 * - `"portrait"` — same as general (face bounding boxes drive the same rules)
 * - `"product"` — centered positioning (dead-center, not thirds), higher fill-ratio target
 */
object CompositionRules {

    // -------------------------------------------------------------------------
    // Tunable thresholds
    // -------------------------------------------------------------------------

    /** Tolerance squared for quadratic distance to the nearest peak. */
    const val PEAK_TOLERANCE_SQ = 0.0025f

    /** Minimum change to allow flipping to the opposite direction (anti-oscillation). */
    const val MIN_IMPROVEMENT_THRESHOLD = 0.05f

    /** Cap on repositioning offsets. */
    const val MAX_REPOSITION_CAP = 0.30f

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private const val LEFT_THIRD  = 1f / 3f
    private const val RIGHT_THIRD = 2f / 3f

    private const val PHI_LOW  = 0.382f
    private const val PHI_HIGH = 0.618f

    /** Center position band for product mode — subject should be dead center. */
    private const val CENTER_LOW  = 0.35f
    private const val CENTER_HIGH = 0.65f

    /** Fill-ratio peaks: general uses three peaks, product uses a tighter single center peak. */
    private val GENERAL_PEAKS = listOf(0.10f, 0.56f, 0.82f)
    private val PRODUCT_PEAKS = listOf(0.35f, 0.62f)

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    fun analyse(
        box: BoundingBox,
        style: String = "rule_of_thirds",
        mode: String = "general",
        previousSuggestion: CompositionSuggestion? = null,
        sensitivity: Float = 0.42f
    ): CompositionSuggestion {
        val fillRatio = box.area
        val isProduct = mode == "product"

        // 1. Size (fill ratio) check
        val peaks = if (isProduct) PRODUCT_PEAKS else GENERAL_PEAKS
        val nearestPeak = peaks.minByOrNull { (fillRatio - it) * (fillRatio - it) } ?: peaks.first()
        val distanceSq = (fillRatio - nearestPeak) * (fillRatio - nearestPeak)
        val toleranceSq = 0.0025f * ((1.0f - sensitivity) / 0.58f).coerceIn(0f, 5f)
        val isSizeGood = distanceSq <= toleranceSq

        var sizeDirection = if (isSizeGood) {
            Direction.NONE
        } else {
            if (fillRatio < nearestPeak) Direction.CLOSER else Direction.AWAY
        }

        // Anti-oscillation on size direction
        if (previousSuggestion != null && isFlip(sizeDirection, previousSuggestion.direction)) {
            val deltaFill = Math.abs(fillRatio - previousSuggestion.fillRatio)
            if (deltaFill < MIN_IMPROVEMENT_THRESHOLD) {
                sizeDirection = previousSuggestion.direction
            }
        }

        if (sizeDirection != Direction.NONE) {
            return CompositionSuggestion.move(sizeDirection, fillRatio = fillRatio)
        }

        // 2. Position check
        val cx = box.centerX
        val cy = box.centerY

        val (lowBound, highBound) = when {
            isProduct || style == "center_grid" -> CENTER_LOW to CENTER_HIGH
            style == "golden_ratio" -> PHI_LOW to PHI_HIGH
            else -> LEFT_THIRD to RIGHT_THIRD
        }

        val rawOffsetX = when {
            cx < lowBound -> cx - lowBound
            cx > highBound -> cx - highBound
            else -> 0f
        }
        val rawOffsetY = when {
            cy < lowBound -> cy - lowBound
            cy > highBound -> cy - highBound
            else -> 0f
        }

        val offsetX = rawOffsetX.coerceIn(-MAX_REPOSITION_CAP, MAX_REPOSITION_CAP)
        val offsetY = rawOffsetY.coerceIn(-MAX_REPOSITION_CAP, MAX_REPOSITION_CAP)

        var hSuggestion = when {
            offsetX < 0f -> Direction.RIGHT
            offsetX > 0f -> Direction.LEFT
            else -> Direction.NONE
        }

        var vSuggestion = when {
            offsetY < 0f -> Direction.DOWN
            offsetY > 0f -> Direction.UP
            else -> Direction.NONE
        }

        // Anti-oscillation on position direction flips
        if (previousSuggestion != null) {
            if (isFlip(hSuggestion, previousSuggestion.direction)) {
                val deltaX = Math.abs(offsetX - previousSuggestion.offsetX)
                if (deltaX < MIN_IMPROVEMENT_THRESHOLD) {
                    hSuggestion = previousSuggestion.direction
                }
            }
            if (isFlip(vSuggestion, previousSuggestion.direction)) {
                val deltaY = Math.abs(offsetY - previousSuggestion.offsetY)
                if (deltaY < MIN_IMPROVEMENT_THRESHOLD) {
                    vSuggestion = previousSuggestion.direction
                }
            }
        }

        return when {
            hSuggestion != Direction.NONE -> CompositionSuggestion.move(hSuggestion, offsetX, offsetY, fillRatio)
            vSuggestion != Direction.NONE -> CompositionSuggestion.move(vSuggestion, offsetX, offsetY, fillRatio)
            else -> CompositionSuggestion(Direction.NONE, isGood = true, offsetX, offsetY, fillRatio)
        }
    }

    private fun isFlip(dir1: Direction, dir2: Direction): Boolean {
        return (dir1 == Direction.LEFT && dir2 == Direction.RIGHT) ||
               (dir1 == Direction.RIGHT && dir2 == Direction.LEFT) ||
               (dir1 == Direction.UP && dir2 == Direction.DOWN) ||
               (dir1 == Direction.DOWN && dir2 == Direction.UP) ||
               (dir1 == Direction.CLOSER && dir2 == Direction.AWAY) ||
               (dir1 == Direction.AWAY && dir2 == Direction.CLOSER)
    }
}