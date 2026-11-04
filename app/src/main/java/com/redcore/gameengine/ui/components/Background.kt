package com.redcore.gameengine.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.redcore.gameengine.ui.theme.BgDeep
import com.redcore.gameengine.ui.theme.DeepCrimson
import kotlin.math.sin

/**
 * Layered animated backdrop: deep black base, two slow red light wells, a faint
 * technical grid and a vignette. Motion is subtle on purpose — premium, not arcade.
 */
@Composable
fun RedCoreBackground(
    accent: Color = Color(0xFFFF2638),
    animated: Boolean = true,
    grid: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "bg")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val t = if (animated) phase else 0.25f

    Box(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val drift = sin(t * Math.PI * 2).toFloat()

            // Top-left ember well
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(w * (0.18f + drift * 0.03f), h * 0.06f),
                    radius = w * 0.95f
                ),
                size = Size(w, h)
            )
            // Bottom-right crimson well
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(DeepCrimson.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(w * (0.85f - drift * 0.04f), h * 0.92f),
                    radius = w * 1.05f
                ),
                size = Size(w, h)
            )
            // Centre haze
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF14141A), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = w * 0.7f
                ),
                size = Size(w, h)
            )

            if (grid) {
                val step = 56f
                val lineColor = Color(0x0AFFFFFF)
                var x = 0f
                while (x < w) {
                    drawLine(lineColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y < h) {
                    drawLine(lineColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                    y += step
                }
            }

            // Sweeping scan line for a sense of live hardware
            if (animated) {
                val y = h * ((t * 2f) % 1f)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, accent.copy(alpha = 0.10f), Color.Transparent)
                    ),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 2f
                )
            }

            // Vignette edges
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x66000000), Color.Transparent, Color(0x88000000))
                ),
                size = Size(w, h),
                style = Stroke(width = 0f)
            )
        }
        content()
    }
}

/** Soft accent halo used behind hero widgets. */
@Composable
fun HaloGlow(accent: Color, modifier: Modifier = Modifier, intensity: Float = 0.22f) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = intensity), Color.Transparent),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.minDimension / 1.4f
            )
        )
    }
}
