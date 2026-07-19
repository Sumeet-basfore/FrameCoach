package com.framecoach.app.rules

import com.framecoach.app.detection.BoundingBox

/**
 * Composition rules engine — pure functions with no Android dependencies.
 *
 * Ported from T2's `analyze_box()` Python prototype.
 *
 * Rules:
 *  1. Size check (fill ratio): subject must occupy 5–50% of the frame.
 *     - < 5 %  → "move closer"
 *     - > 50%  → "step back"
 *  2. Position check (rule of thirds): subject center must fall within the
 *     middle third band (x in [1/3, 2/3] AND y in [1/3, 2/3]).
 *     - Left of the band   → "move right"
 *     - Right of the band  → "move left"
 *     - Above the band      → "move down"
 *     - Below the band      → "move up"
 *  3. If both checks pass → "good" (isGood = true).
 *
 * Coordinate system: all inputs are already normalised by [BoundingBox]
 * (coordinates in [0..1] relative to frame width / height).  The rules
 * operate on normalised values directly, so they work at any resolution.
 */
object CompositionRules {

    // -------------------------------------------------------------------------
    // Tunable thresholds
    // -------------------------------------------------------------------------

    /**
     * Tolerance squared for quadratic distance to the nearest peak.
     * With a linear tolerance of 0.05 (5% of the frame), the squared tolerance is 0.0025.
     */
    const val PEAK_TOLERANCE_SQ = 0.0025f

    /**
     * Minimum change in position or size coordinate required to allow
     * flipping to the opposite direction (prevents jitter/oscillation).
     */
    const val MIN_IMPROVEMENT_THRESHOLD = 0.05f

    /**
     * Cap on how large a suggested repositioning offset can be.
     */
    const val MAX_REPOSITION_CAP = 0.30f

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Analyse a single detected object's [BoundingBox] against the frame
     * dimensions and return a [CompositionSuggestion].
     *
     * @param box                The detected object's bounding box.
     * @param style              Active composition coaching style ("rule_of_thirds" or "golden_ratio")
     * @param previousSuggestion The previous frame's suggestion, used for the anti-oscillation guard.
     * @return                   A [CompositionSuggestion].
     */
    fun analyse(
        box: BoundingBox,
        style: String = "rule_of_thirds",
        previousSuggestion: CompositionSuggestion? = null
    ): CompositionSuggestion {
        val fillRatio = box.area

        // 1. Size (fill ratio) check using the three-peak approach: ~10% (0.10), ~56% (0.56), ~82% (0.82)
        val peaks = listOf(0.10f, 0.56f, 0.82f)
        val nearestPeak = peaks.minByOrNull { (fillRatio - it) * (fillRatio - it) } ?: 0.10f
        val distanceSq = (fillRatio - nearestPeak) * (fillRatio - nearestPeak)
        val isSizeGood = distanceSq <= PEAK_TOLERANCE_SQ

        var sizeDirection = if (isSizeGood) {
            Direction.NONE
        } else {
            if (fillRatio < nearestPeak) Direction.CLOSER else Direction.AWAY
        }

        // Apply anti-oscillation guard on size direction flip
        if (previousSuggestion != null && isFlip(sizeDirection, previousSuggestion.direction)) {
            val deltaFill = Math.abs(fillRatio - previousSuggestion.fillRatio)
            if (deltaFill < MIN_IMPROVEMENT_THRESHOLD) {
                sizeDirection = previousSuggestion.direction
            }
        }

        if (sizeDirection != Direction.NONE) {
            return CompositionSuggestion.move(sizeDirection, fillRatio = fillRatio)
        }

        // 2. Position check (Rule of Thirds vs. Golden Ratio)
        val cx = box.centerX
        val cy = box.centerY

        val lowBound = if (style == "golden_ratio") PHI_LOW else LEFT_THIRD
        val highBound = if (style == "golden_ratio") PHI_HIGH else RIGHT_THIRD

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

        // Cap on how large a suggested repositioning can be
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

        // Apply anti-oscillation guard on position direction flips
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

    /**
     * Check if dir1 and dir2 are opposite directions.
     */
    private fun isFlip(dir1: Direction, dir2: Direction): Boolean {
        return (dir1 == Direction.LEFT && dir2 == Direction.RIGHT) ||
               (dir1 == Direction.RIGHT && dir2 == Direction.LEFT) ||
               (dir1 == Direction.UP && dir2 == Direction.DOWN) ||
               (dir1 == Direction.DOWN && dir2 == Direction.UP) ||
               (dir1 == Direction.CLOSER && dir2 == Direction.AWAY) ||
               (dir1 == Direction.AWAY && dir2 == Direction.CLOSER)
    }

    // -------------------------------------------------------------------------
    // Private constants
    // -------------------------------------------------------------------------

    private const val LEFT_THIRD  = 1f / 3f  // ≈ 0.333
    private const val RIGHT_THIRD = 2f / 3f  // ≈ 0.667

    // Golden Ratio (Phi) partitions:
    // Left/Top line = 1 - 1/phi = 1 - 0.618 = 0.382
    // Right/Bottom line = 1/phi = 0.618
    private const val PHI_LOW = 0.382f
    private const val PHI_HIGH = 0.618f
}