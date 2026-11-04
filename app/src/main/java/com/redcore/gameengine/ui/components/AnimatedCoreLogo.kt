package com.redcore.gameengine.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.redcore.gameengine.ui.theme.PrimaryRed
import kotlin.math.cos
import kotlin.math.sin

/**
 * The REDCORE mark: a rotating segmented ring around a faceted core.
 * Pure Canvas — no bitmaps, so it stays crisp at every density.
 */
@Composable
fun AnimatedCoreLogo(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    accent: Color = PrimaryRed,
    animated: Boolean = true,
    pulse: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "logo")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "spin"
    )
    val glow by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "glow"
    )
    val rotation = if (animated) spin else 0f
    val glowAlpha = if (pulse) glow else 0.4f

    Canvas(modifier = modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = Offset(this.size.width / 2f, this.size.height / 2f)

        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.25f * glowAlpha), Color.Transparent),
                center = c,
                radius = r
            ),
            radius = r,
            center = c
        )

        // Segmented rotating ring
        val ringRadius = r * 0.82f
        val strokeWidth = r * 0.085f
        drawArc(
            brush = Brush.sweepGradient(
                0f to Color.Transparent,
                0.25f to accent.copy(alpha = 0.9f),
                0.5f to Color.Transparent,
                0.75f to accent.copy(alpha = 0.45f),
                1f to Color.Transparent
            ),
            startAngle = rotation,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(c.x - ringRadius, c.y - ringRadius),
            size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Tick marks
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30 + rotation * 0.4).toDouble())
            val inner = r * 0.63f
            val outer = r * 0.70f
            drawLine(
                color = accent.copy(alpha = if (i % 3 == 0) 0.75f else 0.22f),
                start = Offset(c.x + (cos(angle) * inner).toFloat(), c.y + (sin(angle) * inner).toFloat()),
                end = Offset(c.x + (cos(angle) * outer).toFloat(), c.y + (sin(angle) * outer).toFloat()),
                strokeWidth = if (i % 3 == 0) 2.4f else 1.4f,
                cap = StrokeCap.Round
            )
        }

        // Core diamond
        val d = r * 0.40f
        val core = Path().apply {
            moveTo(c.x, c.y - d)
            lineTo(c.x + d, c.y)
            lineTo(c.x, c.y + d)
            lineTo(c.x - d, c.y)
            close()
        }
        drawPath(
            path = core,
            brush = Brush.linearGradient(
                colors = listOf(accent, Color(0xFF8F101D)),
                start = Offset(c.x - d, c.y - d),
                end = Offset(c.x + d, c.y + d)
            )
        )
        val inner = d * 0.46f
        val innerPath = Path().apply {
            moveTo(c.x, c.y - inner)
            lineTo(c.x + inner, c.y)
            lineTo(c.x, c.y + inner)
            lineTo(c.x - inner, c.y)
            close()
        }
        drawPath(innerPath, color = Color(0xFF0A0A0C))
        drawPath(
            innerPath,
            color = accent.copy(alpha = 0.9f),
            style = Stroke(width = r * 0.03f)
        )
        drawCircle(color = Color.White, radius = r * 0.05f, center = c)
    }
}
