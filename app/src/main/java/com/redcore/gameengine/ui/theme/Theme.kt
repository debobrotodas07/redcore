package com.redcore.gameengine.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.redcore.gameengine.data.model.AccentTheme

data class AccentBundle(
    val primary: Color,
    val deep: Color,
    val glow: Color
)

fun accentFor(theme: AccentTheme): AccentBundle = when (theme) {
    AccentTheme.CRIMSON -> AccentBundle(PrimaryRed, DeepCrimson, Color(0xFFFF6B7A))
    AccentTheme.EMBER -> AccentBundle(EmberOrange, Color(0xFF8F3A18), Color(0xFFFFB08F))
    AccentTheme.CYAN -> AccentBundle(Color(0xFF19D3DA), Color(0xFF0B5F66), Color(0xFF8AF0F4))
    AccentTheme.VIOLET -> AccentBundle(Color(0xFF9B5CFF), Color(0xFF4A1F8F), Color(0xFFD2B6FF))
    AccentTheme.TOXIC -> AccentBundle(Color(0xFF9BFF33), Color(0xFF3E7A10), Color(0xFFD4FFA3))
}

private fun scheme(accent: AccentBundle) = darkColorScheme(
    primary = accent.primary,
    onPrimary = Color.White,
    primaryContainer = accent.deep,
    onPrimaryContainer = Color.White,
    secondary = accent.glow,
    onSecondary = Color(0xFF120608),
    secondaryContainer = BgCard,
    onSecondaryContainer = TextPrimary,
    tertiary = Info,
    background = BgDeep,
    onBackground = TextPrimary,
    surface = BgElevated,
    onSurface = TextPrimary,
    surfaceVariant = BgCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = BgCard,
    surfaceContainerHigh = Color(0xFF18181F),
    outline = Color(0xFF3A3A44),
    outlineVariant = Color(0xFF26262E),
    error = Color(0xFFFF4D5E),
    onError = Color.White
)

@Composable
fun RedCoreTheme(
    accentTheme: AccentTheme = AccentTheme.CRIMSON,
    content: @Composable () -> Unit
) {
    val accent = accentFor(accentTheme)
    val view = LocalView.current
    val context = LocalContext.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = BgDeep.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = scheme(accent),
        typography = RedCoreTypography,
        shapes = RedCoreShapes,
        content = content
    )
}
