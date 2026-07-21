package com.framecoach.app.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.framecoach.app.ui.overlay.CompositionState
import com.framecoach.app.rules.CompositionSuggestion
import com.framecoach.app.rules.Direction
import com.framecoach.app.ui.theme.SuccessIce
import com.framecoach.app.ui.theme.SurfaceMedium
import com.framecoach.app.ui.theme.AccentSage
import com.framecoach.app.ui.theme.ErrorPlum

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
    val gridColor = if (isGood) SuccessIce else SurfaceMedium.copy(alpha = 0.4f)
    val strokeWidth = 2.dp.toPx()
    val lowRatio = when (compositionStyle) {
        "golden_ratio" -> 0.382f
        "center_grid" -> 0.35f
        else -> 1f / 3f
    }
    val highRatio = when (compositionStyle) {
        "golden_ratio" -> 0.618f
        "center_grid" -> 0.65f
        else -> 2f / 3f
    }

    gridLines(
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        color = gridColor,
        strokeWidth = strokeWidth,
        lowRatio = lowRatio,
        highRatio = highRatio,
    )

    // Crosshair intersection markers — small circles at the four grid-line crossings.
    val dotRadius = 3.dp.toPx()
    val dotColor = if (isGood) SuccessIce else SurfaceMedium.copy(alpha = 0.5f)
    val xs = listOf(canvasWidth * lowRatio, canvasWidth * highRatio)
    val ys = listOf(canvasHeight * lowRatio, canvasHeight * highRatio)
    for (x in xs) {
        for (y in ys) {
            drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
            // Outer ring for subtle emphasis
            drawCircle(
                color = dotColor.copy(alpha = 0.3f),
                radius = dotRadius * 2.5f,
                center = Offset(x, y),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

/**
 * Draw vertical and horizontal lines partitioning the screen.
 */
private fun DrawScope.gridLines(
    canvasWidth: Float,
    canvasHeight: Float,
    color: Color,
    strokeWidth: Float,
    lowRatio: Float,
    highRatio: Float,
) {
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
 * Draw directional indicator based on composition suggestion — a filled arrow
 * triangle pointing the way the camera should move.
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

    val arrowSize = 28.dp.toPx()
    val offset = 60.dp.toPx() // Distance of the arrow center from the screen centre

    val tip = when (direction) {
        Direction.UP -> Offset(centerX, centerY - offset - arrowSize * 0.5f)
        Direction.DOWN -> Offset(centerX, centerY + offset + arrowSize * 0.5f)
        Direction.LEFT -> Offset(centerX - offset - arrowSize * 0.5f, centerY)
        Direction.RIGHT -> Offset(centerX + offset + arrowSize * 0.5f, centerY)
        Direction.CLOSER -> Offset(centerX, centerY - offset - arrowSize * 0.5f)
        Direction.AWAY -> Offset(centerX, centerY + offset + arrowSize * 0.5f)
        else -> return
    }

    val color = when (direction) {
        Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT, Direction.CLOSER -> AccentSage
        Direction.AWAY -> ErrorPlum
        else -> AccentSage
    }

    // Build an arrow path: tip + two base corners = isosceles triangle.
    val arrowPath = Path().apply {
        when (direction) {
            Direction.UP, Direction.CLOSER -> {
                moveTo(tip.x, tip.y)
                lineTo(tip.x - arrowSize * 0.5f, tip.y + arrowSize)
                lineTo(tip.x + arrowSize * 0.5f, tip.y + arrowSize)
            }
            Direction.DOWN, Direction.AWAY -> {
                moveTo(tip.x, tip.y)
                lineTo(tip.x - arrowSize * 0.5f, tip.y - arrowSize)
                lineTo(tip.x + arrowSize * 0.5f, tip.y - arrowSize)
            }
            Direction.LEFT -> {
                moveTo(tip.x, tip.y)
                lineTo(tip.x + arrowSize, tip.y - arrowSize * 0.5f)
                lineTo(tip.x + arrowSize, tip.y + arrowSize * 0.5f)
            }
            Direction.RIGHT -> {
                moveTo(tip.x, tip.y)
                lineTo(tip.x - arrowSize, tip.y - arrowSize * 0.5f)
                lineTo(tip.x - arrowSize, tip.y + arrowSize * 0.5f)
            }
            else -> {}
        }
        close()
    }

    // Draw the arrow with a semi-transparent fill and a solid stroke.
    drawPath(
        path = arrowPath,
        color = color.copy(alpha = 0.7f),
    )
    drawPath(
        path = arrowPath,
        color = color,
        style = Stroke(width = 2.dp.toPx()),
    )
}