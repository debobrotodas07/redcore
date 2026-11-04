package com.redcore.gameengine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.redcore.gameengine.data.model.CrosshairDesign
import com.redcore.gameengine.navigation.Routes
import com.redcore.gameengine.sys.PermissionHelper
import com.redcore.gameengine.ui.components.CrosshairView
import com.redcore.gameengine.ui.components.GlassCard
import com.redcore.gameengine.ui.components.InfoNote
import com.redcore.gameengine.ui.components.PillTag
import com.redcore.gameengine.ui.components.PrimaryButton
import com.redcore.gameengine.ui.components.ScreenColumn
import com.redcore.gameengine.ui.components.ScreenTopBar
import com.redcore.gameengine.ui.components.SectionHeader
import com.redcore.gameengine.ui.components.ToggleRow
import com.redcore.gameengine.ui.theme.TextMuted
import com.redcore.gameengine.ui.theme.TextPrimary
import com.redcore.gameengine.ui.theme.TextSecondary
import com.redcore.gameengine.ui.vm.SettingsViewModel

private val palette = listOf(
    0xFFFF2638, 0xFF35D07F, 0xFF4FA8FF, 0xFFFFD400, 0xFFFFFFFF,
    0xFFFF6B4A, 0xFF9B5CFF, 0xFF19D3DA, 0xFF00FF95, 0xFFFF00A0
)

@Composable
fun CrosshairStudioScreen(nav: NavHostController) {
    val vm: SettingsViewModel = viewModel()
    val settings by vm.settings.collectAsState()
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    var size by remember(settings.crosshairSize) { mutableStateOf(settings.crosshairSize.toFloat()) }
    var opacity by remember(settings.crosshairOpacity) { mutableStateOf(settings.crosshairOpacity.toFloat()) }
    val overlayAllowed = PermissionHelper.overlay(context)

    ScreenColumn {
        ScreenTopBar(
            title = "Crosshair studio",
            subtitle = "Design the aim point, then float it over your game"
        )

        // Preview stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1B1F24), Color(0xFF0C0E11))
                    )
                )
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().height(260.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(6) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x0FFFFFFF))
                    )
                }
            }
            CrosshairView(
                design = settings.crosshairDesign,
                color = Color(settings.crosshairColor),
                sizeDp = size.toInt(),
                opacity = opacity.toInt(),
                modifier = Modifier.size(size.dp + 40.dp)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                PillTag(
                    text = if (settings.crosshairEnabled) "Overlay ON" else "Preview only",
                    accent = accent,
                    filled = settings.crosshairEnabled
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Design")
        GlassCard {
            CrosshairDesign.entries.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { design ->
                        val selected = settings.crosshairDesign == design
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) accent.copy(alpha = 0.15f) else Color(0x08FFFFFF))
                                .border(
                                    1.dp,
                                    if (selected) accent.copy(alpha = 0.55f) else Color(0x14FFFFFF),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { vm.setCrosshairDesign(design) }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CrosshairView(
                                design = design,
                                color = if (selected) Color(settings.crosshairColor) else TextSecondary,
                                sizeDp = 26,
                                opacity = 100,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                design.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) TextPrimary else TextMuted,
                                maxLines = 1
                            )
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Colour")
        GlassCard {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(palette) { value ->
                    val selected = settings.crosshairColor == value
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(value))
                            .border(
                                if (selected) 2.dp else 1.dp,
                                if (selected) Color.White else Color(0x33FFFFFF),
                                CircleShape
                            )
                            .clickable { vm.setCrosshairColor(value) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Size & opacity")
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SIZE", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(1f))
                Text("${size.toInt()} dp", style = MaterialTheme.typography.labelMedium, color = accent)
            }
            Slider(
                value = size,
                onValueChange = { size = it },
                onValueChangeFinished = { vm.setCrosshairSize(size.toInt()) },
                valueRange = 10f..90f,
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Color(0x22FFFFFF))
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("OPACITY", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(1f))
                Text("${opacity.toInt()}%", style = MaterialTheme.typography.labelMedium, color = accent)
            }
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                onValueChangeFinished = { vm.setCrosshairOpacity(opacity.toInt()) },
                valueRange = 15f..100f,
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Color(0x22FFFFFF))
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Overlay")
        GlassCard(accent = accent) {
            ToggleRow(
                title = "Float the crosshair over other apps",
                subtitle = if (overlayAllowed)
                    "Uses the overlay permission. Touch passes straight through to your game."
                else "Grant \"Display over other apps\" first.",
                checked = settings.crosshairEnabled,
                accent = accent,
                leadingIcon = Icons.Rounded.CenterFocusStrong,
                onCheckedChange = { enabled ->
                    if (!overlayAllowed) {
                        PermissionHelper.openOverlaySettings(context)
                    } else {
                        vm.setCrosshairEnabled(enabled)
                        com.redcore.gameengine.service.OverlayService.start(context)
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = if (settings.crosshairEnabled) "Turn crosshair off" else "Show crosshair overlay",
                enabled = overlayAllowed,
                onClick = {
                    vm.setCrosshairEnabled(!settings.crosshairEnabled)
                    com.redcore.gameengine.service.OverlayService.start(context)
                }
            )
            if (!overlayAllowed) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Without the overlay permission the crosshair can only be previewed inside REDCORE.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Per-game crosshairs")
        GlassCard {
            Text(
                "Every game stores its own crosshair, colour and opacity. Open a game from the Library to pin one.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "OPEN LIBRARY →",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                modifier = Modifier.clickable { nav.navigate(Routes.LIBRARY) }
            )
        }

        Spacer(Modifier.height(16.dp))
        InfoNote(
            text = "Limits: apps that hide overlays (banking, DRM video, some launchers) will not show it, and secure screens stay black in captures. Competitive shooters may treat any overlay as external assistance — use it in your device's own practice modes.",
            accent = accent
        )

        Spacer(Modifier.height(20.dp))
    }
}
