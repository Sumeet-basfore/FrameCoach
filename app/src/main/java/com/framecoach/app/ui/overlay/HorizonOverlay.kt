package com.framecoach.app.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.framecoach.app.sensors.HorizonLevelCalculator
import com.framecoach.app.sensors.HorizonSensor
import com.framecoach.app.ui.theme.MochaGreen
import com.framecoach.app.ui.theme.MochaYellow

/**
 * Canvas overlay that draws a horizon level indicator (T9).
 *
 * **Visible only when the device is NOT level** (|roll| > [HorizonLevelCalculator.LEVEL_THRESHOLD_DEG]).
 * When the device is level the composable draws nothing, so it imposes zero
 * visual overhead during well-composed shots.
 *
 * ## Visual design (04_Frontend_Specification_Document.md §3)
 * - A horizontal line spans ~60 % of the canvas width, centred on screen,
 *   rotated by the current roll angle.  The line is [MochaYellow] while
 *   off-level; a brief green flash would require animation state that is
 *   outside T9's scope.
 * - A small circle (bubble) sits at the centre of the line to suggest a
 *   spirit level — it migrates left/right as the device tilts.
 * - Tick marks at the left and right ends give a clear reference point.
 *
 * @param horizonSensor Live sensor providing roll angles.
 * @param modifier      Forwarded to the [Canvas] — caller should pass
 *                      [Modifier.fillMaxSize] so the overlay covers the preview.
 */
@Composable
fun HorizonOverlay(
    horizonSensor: HorizonSensor,
    modifier: Modifier = Modifier,
) {
    val rollDeg by horizonSensor.rollDeg.collectAsState()
    val state = HorizonLevelCalculator.compute(rollDeg)

    // Draw nothing when level — zero visual noise during a good shot.
    if (state.isLevel) return

    Canvas(modifier = modifier.fillMaxSize()) {
        drawHorizonIndicator(
            rollDeg = state.rollDeg,
            normalisedTilt = state.normalisedTilt,
        )
    }
}

// ---------------------------------------------------------------------------
// Private drawing helpers
// ---------------------------------------------------------------------------

/**
 * Draw the horizon level indicator centred on the canvas, rotated by [rollDeg].
 */
private fun DrawScope.drawHorizonIndicator(
    rollDeg: Float,
    normalisedTilt: Float,
) {
    val cx = size.width / 2f
    val cy = size.height / 2f

    // Line spans 60 % of the narrower canvas dimension so it stays on screen.
    val halfLineLen = minOf(size.width, size.height) * 0.30f
    val strokeWidth = 2.dp.toPx()
    val tickHeight = 10.dp.toPx()
    val bubbleRadius = 6.dp.toPx()

    // Colour: yellow while off-level (matches spec §1 "Warning" colour).
    val lineColor: Color = MochaYellow

    // Rotate the entire drawing around the canvas centre by the roll angle.
    rotate(degrees = rollDeg, pivot = Offset(cx, cy)) {
        // Horizontal reference line.
        drawLine(
            color = lineColor,
            start = Offset(cx - halfLineLen, cy),
            end = Offset(cx + halfLineLen, cy),
            strokeWidth = strokeWidth,
        )

        // Left tick.
        drawLine(
            color = lineColor,
            start = Offset(cx - halfLineLen, cy - tickHeight / 2f),
            end = Offset(cx - halfLineLen, cy + tickHeight / 2f),
            strokeWidth = strokeWidth,
        )

        // Right tick.
        drawLine(
            color = lineColor,
            start = Offset(cx + halfLineLen, cy - tickHeight / 2f),
            end = Offset(cx + halfLineLen, cy + tickHeight / 2f),
            strokeWidth = strokeWidth,
        )

        // Centre reference tick (where the bubble should sit when level).
        drawLine(
            color = lineColor.copy(alpha = 0.5f),
            start = Offset(cx, cy - tickHeight),
            end = Offset(cx, cy + tickHeight),
            strokeWidth = strokeWidth * 0.75f,
        )
    }

    // Bubble: drawn AFTER the rotate block so it stays upright and migrates
    // horizontally, giving an intuitive spirit-level metaphor.
    // Bubble position: offset by normalisedTilt * halfLineLen from centre.
    val bubbleX = cx + normalisedTilt * halfLineLen
    drawCircle(
        color = lineColor,
        center = Offset(bubbleX, cy),
        radius = bubbleRadius,
    )
    // Hollow centre so it reads as a bubble, not a dot.
    drawCircle(
        color = Color.Black.copy(alpha = 0.6f),
        center = Offset(bubbleX, cy),
        radius = bubbleRadius * 0.55f,
    )
}
