package com.redcore.gameengine.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.redcore.gameengine.data.model.CrosshairDesign

/**
 * Draws the crosshair in a square canvas. Shared by the studio preview and the
 * real overlay window, so what you design is exactly what you get on screen.
 */
@Composable
fun CrosshairView(
    design: CrosshairDesign,
    color: Color,
    sizeDp: Int,
    opacity: Int,
    modifier: Modifier = Modifier,
    withShadow: Boolean = true
) {
    Canvas(modifier = modifier) {
        val alpha = (opacity / 100f).coerceIn(0.1f, 1f)
        if (withShadow) {
            drawShadowCrosshair(design, size.minDimension, alpha)
        }
        drawCrosshair(design, color.copy(alpha = alpha), size.minDimension)
    }
}

fun DrawScope.drawCrosshair(design: CrosshairDesign, color: Color, box: Float) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val unit = box / 100f
    val stroke = (unit * 4.2f).coerceAtLeast(1.5f)
    val r = box / 2f

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float = stroke, col: Color = color) {
        drawLine(
            color = col,
            start = Offset(c.x + x1, c.y + y1),
            end = Offset(c.x + x2, c.y + y2),
            strokeWidth = width,
            cap = StrokeCap.Round
        )
    }

    when (design) {
        CrosshairDesign.DOT -> {
            drawCircle(color, radius = unit * 7f, center = c)
            drawCircle(color.copy(alpha = 0.35f), radius = unit * 12f, center = c, style = Stroke(width = stroke * 0.6f))
        }
        CrosshairDesign.CROSS -> {
            val gap = r * 0.18f
            val len = r * 0.52f
            line(0f, -gap, 0f, -gap - len)
            line(0f, gap, 0f, gap + len)
            line(-gap, 0f, -gap - len, 0f)
            line(gap, 0f, gap + len, 0f)
        }
        CrosshairDesign.RING -> {
            drawCircle(color, radius = r * 0.62f, center = c, style = Stroke(width = stroke))
        }
        CrosshairDesign.RING_DOT -> {
            drawCircle(color, radius = r * 0.58f, center = c, style = Stroke(width = stroke))
            drawCircle(color, radius = unit * 6.5f, center = c)
        }
        CrosshairDesign.T_CROSS -> {
            val gap = r * 0.16f
            line(-r * 0.55f, 0f, -gap, 0f)
            line(gap, 0f, r * 0.55f, 0f)
            line(0f, gap, 0f, r * 0.62f)
            drawCircle(color, radius = unit * 4f, center = c)
        }
        CrosshairDesign.CHEVRON -> {
            line(-r * 0.42f, -r * 0.30f, 0f, -r * 0.02f)
            line(r * 0.42f, -r * 0.30f, 0f, -r * 0.02f)
            line(0f, r * 0.20f, 0f, r * 0.55f)
        }
        CrosshairDesign.X_MARK -> {
            val len = r * 0.45f
            line(-len, -len, len, len)
            line(-len, len, len, -len)
        }
    }
}

private fun DrawScope.drawShadowCrosshair(design: CrosshairDesign, box: Float, alpha: Float) {
    // A dark halo keeps the crosshair readable on bright game scenes.
    drawCrosshair(design, Color.Black.copy(alpha = 0.55f * alpha), box * 1.06f)
}
