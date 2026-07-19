package com.framecoach.app.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framecoach.app.ui.overlay.CompositionState
import com.framecoach.app.rules.CompositionSuggestion
import com.framecoach.app.rules.Direction
import com.framecoach.app.ui.theme.MochaGreen
import com.framecoach.app.ui.theme.MochaPeach
import com.framecoach.app.ui.theme.MochaRed
import com.framecoach.app.ui.theme.MochaOverlay1

/**
 * Overlay that draws the composition guide: 3x3 grid and directional indicator.
 *
 * The grid color changes based on composition quality:
 *   - Green: good composition (subject in rule-of-thirds area with proper size)
 *   - Red: poor configuration (needs adjustment)
 *
 * Directional indicator shows suggested camera movement direction.
 *
 * @param showGrid When false the rule-of-thirds grid is hidden (user preference
 *                 from Settings / T10). The directional indicator still renders.
 */
@Composable
fun CompositionOverlay(
    showGrid: Boolean = true,
    compositionStyle: String = "rule_of_thirds",
    modifier: Modifier = Modifier,
) {
    val suggestion: State<CompositionSuggestion> = CompositionState.suggestion.collectAsState()

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw grid only when the setting is enabled.
        if (showGrid) {
            drawGrid(canvasWidth, canvasHeight, suggestion.value.isGood, compositionStyle)
        }

        // Draw directional indicator if needed (always shown — useful even without the grid).
        if (!suggestion.value.isGood && suggestion.value.direction != Direction.NONE) {
            drawDirectionIndicator(canvasWidth, canvasHeight, suggestion.value.direction)
        }
    }
}

/**
 * Draw the composition grid overlay.
 *
 * @param canvasWidth      Width of the canvas in pixels
 * @param canvasHeight     Height of the canvas in pixels
 * @param isGood           If true, draw green grid; otherwise neutral overlay color
 * @param compositionStyle Active composition coaching style ("rule_of_thirds" or "golden_ratio")
 */
private fun DrawScope.drawGrid(
    canvasWidth: Float,
    canvasHeight: Float,
    isGood: Boolean,
    compositionStyle: String
) {
    val gridColor = if (isGood) MochaGreen else MochaOverlay1.copy(alpha = 0.4f)
    val strokeWidth = 2.dp.toPx()
    gridLines(
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        color = gridColor,
        strokeWidth = strokeWidth,
        style = compositionStyle
    )
}

/**
 * Draw vertical and horizontal lines partitioning the screen.
 */
private fun DrawScope.gridLines(
    canvasWidth: Float,
    canvasHeight: Float,
    color: Color,
    strokeWidth: Float,
    style: String
) {
    val lowRatio = if (style == "golden_ratio") 0.382f else 1f / 3f
    val highRatio = if (style == "golden_ratio") 0.618f else 2f / 3f

    // Vertical lines
    val x1 = canvasWidth * lowRatio
    val x2 = canvasWidth * highRatio
    drawLine(
        start = Offset(x1, 0f),
        end = Offset(x1, canvasHeight),
        color = color,
        strokeWidth = strokeWidth
    )
    drawLine(
        start = Offset(x2, 0f),
        end = Offset(x2, canvasHeight),
        color = color,
        strokeWidth = strokeWidth
    )

    // Horizontal lines
    val y1 = canvasHeight * lowRatio
    val y2 = canvasHeight * highRatio
    drawLine(
        start = Offset(0f, y1),
        end = Offset(canvasWidth, y1),
        color = color,
        strokeWidth = strokeWidth
    )
    drawLine(
        start = Offset(0f, y2),
        end = Offset(canvasWidth, y2),
        color = color,
        strokeWidth = strokeWidth
    )
}

/**
 * Draw directional indicator based on composition suggestion.
 *
 * @param canvasWidth  Width of the canvas in pixels
 * @param canvasHeight Height of the canvas in pixels
 * @param direction    Direction to move the camera
 */
private fun DrawScope.drawDirectionIndicator(
    canvasWidth: Float,
    canvasHeight: Float,
    direction: Direction
) {
    // Center of the canvas
    val centerX = canvasWidth / 2
    val centerY = canvasHeight / 2
    
    // Simple visual indicator - draw a colored circle in the direction needed
    val indicatorRadius = 20.dp.toPx()
    val offset = 60.dp.toPx() // Distance from center
    
    val (x, y) = when (direction) {
        Direction.UP -> Pair(centerX, centerY - offset)
        Direction.DOWN -> Pair(centerX, centerY + offset)
        Direction.LEFT -> Pair(centerX - offset, centerY)
        Direction.RIGHT -> Pair(centerX + offset, centerY)
        Direction.CLOSER -> Pair(centerX, centerY - offset) // Up
        Direction.AWAY -> Pair(centerX, centerY + offset) // Down
        else -> Pair(centerX, centerY) // Center (shouldn't happen for non-None)
    }
    
    // Choose color based on direction type
    val color = when (direction) {
        Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT, Direction.CLOSER -> MochaPeach
        Direction.AWAY -> MochaRed
        else -> Color.White
    }
    
    // Draw the indicator circle
    drawCircle(
        color = color,
        center = Offset(x, y),
        radius = indicatorRadius
    )
}