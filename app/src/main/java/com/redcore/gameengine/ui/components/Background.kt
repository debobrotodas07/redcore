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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import com.redcore.gameengine.ui.theme.BgDeep
import com.redcore.gameengine.ui.theme.DeepCrimson
import kotlin.math.sin

/**
 * Layered animated backdrop: deep black base, two slow light wells, a faint
 * technical grid and a vignette.
 *
 * Performance note: the previous version read the animation value during
 * composition, which re-composed the whole screen body ~60x per second. Every
 * animated value is now read inside the draw phase (and the static layers are
 * cached per size), so the only per-frame cost is a handful of GPU draw calls —
 * no recomposition, no per-frame allocations.
 */
@Composable
fun RedCoreBackground(
    accent: Color = Color(0xFFFF2638),
    animated: Boolean = true,
    grid: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "bg")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val step = 56f

                val gridPath = if (grid) {
                    Path().apply {
                        var x = 0f
                        while (x < w) {
                            moveTo(x, 0f)
                            lineTo(x, h)
                            x += step
                        }
                        var y = 0f
                        while (y < h) {
                            moveTo(0f, y)
                            lineTo(w, y)
                            y += step
                        }
                    }
                } else null

                val topWell = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(w * 0.18f, h * 0.06f),
                    radius = w * 0.95f
                )
                val bottomWell = Brush.radialGradient(
                    colors = listOf(DeepCrimson.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.92f),
                    radius = w * 1.05f
                )
                val haze = Brush.radialGradient(
                    colors = listOf(Color(0xFF14141A), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = w * 0.7f
                )
                val vignette = Brush.verticalGradient(
                    colors = listOf(Color(0x66000000), Color.Transparent, Color(0x88000000))
                )
                val scanBrush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, accent.copy(alpha = 0.10f), Color.Transparent)
                )
                val driftPx = w * 0.04f

                onDrawBehind {
                    drawRect(topWell)
                    drawRect(bottomWell)
                    drawRect(haze)

                    gridPath?.let { drawPath(it, Color(0x0AFFFFFF), style = stroke1) }

                    if (animated) {
                        // Draw-phase read: invalidates drawing only, never composition.
                        val t = phase.value
                        val drift = sin(t * Math.PI * 2).toFloat() * driftPx
                        if (drift != 0f) {
                            // The wells above are static; a subtle sheen sweeps instead.
                            translate(drift, 0f) {
                                drawRect(
                                    Brush.radialGradient(
                                        colors = listOf(accent.copy(alpha = 0.05f), Color.Transparent),
                                        center = Offset(w * 0.5f, h * 0.1f),
                                        radius = w * 0.6f
                                    )
                                )
                            }
                        }
                        val y = h * ((t * 2f) % 1f)
                        drawLine(
                            brush = scanBrush,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 2f
                        )
                    }

                    drawRect(vignette)
                }
            }
    ) {
        content()
    }
}

private val stroke1 = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)

/** Soft accent halo used behind hero widgets (static, cheap). */
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
