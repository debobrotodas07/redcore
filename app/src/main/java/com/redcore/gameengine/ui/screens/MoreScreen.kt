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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.redcore.gameengine.BuildConfig
import com.redcore.gameengine.navigation.Routes
import com.redcore.gameengine.sys.PermissionHelper
import com.redcore.gameengine.ui.components.GlassCard
import com.redcore.gameengine.ui.components.InfoNote
import com.redcore.gameengine.ui.components.PillTag
import com.redcore.gameengine.ui.components.ScreenColumn
import com.redcore.gameengine.ui.components.ScreenTopBar
import com.redcore.gameengine.ui.components.SectionHeader
import com.redcore.gameengine.ui.components.ToggleRow
import com.redcore.gameengine.ui.theme.TextMuted
import com.redcore.gameengine.ui.theme.TextPrimary
import com.redcore.gameengine.ui.theme.TextSecondary
import com.redcore.gameengine.ui.vm.SettingsViewModel

private data class ToolEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun MoreScreen(nav: NavHostController) {
    val vm: SettingsViewModel = viewModel()
    val settings by vm.settings.collectAsState()
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    val overlayGranted = PermissionHelper.overlay(context)

    val tools = listOf(
        ToolEntry("Capture", "Screenshots and 60 fps recording", Icons.Rounded.PhotoCamera, Routes.CAPTURE),
        ToolEntry("Crosshair studio", "Design an overlay aim reference", Icons.Rounded.CenterFocusStrong, Routes.CROSSHAIR),
        ToolEntry("Audio studio", "Voice changer and mic monitor", Icons.Rounded.GraphicEq, Routes.AUDIO),
        ToolEntry("Permissions", "What is granted, what is not", Icons.Rounded.Key, Routes.PERMISSIONS),
        ToolEntry("Settings", "Accent, sidebar, backup", Icons.Rounded.Settings, Routes.SETTINGS)
    )

    ScreenColumn {
        ScreenTopBar(
            title = "More",
            subtitle = "Tools, setup and information"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0x14FFFFFF))
                .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Smartphone, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Text("Floating sidebar", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                PillTag(
                    text = when {
                        !overlayGranted -> "Needs permission"
                        settings.sidebarEnabled -> "Active"
                        else -> "Off"
                    },
                    accent = if (settings.sidebarEnabled && overlayGranted) accent else TextMuted,
                    filled = settings.sidebarEnabled && overlayGranted
                )
            }
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                title = "Enable over games",
                subtitle = if (overlayGranted) "Tap the handle anytime to open tools" else "Grant \"Display over other apps\" first",
                checked = settings.sidebarEnabled,
                accent = accent,
                onCheckedChange = { enabled ->
                    if (!overlayGranted) {
                        PermissionHelper.openOverlaySettings(context)
                    } else {
                        vm.setSidebarEnabled(enabled)
                    }
                }
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Studios")
        GlassCard {
            tools.forEachIndexed { index, tool ->
                if (index > 0) Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { nav.navigate(tool.route) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(tool.icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tool.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(tool.subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    Text("›", style = MaterialTheme.typography.titleLarge, color = TextMuted)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "System shortcuts")
        GlassCard {
            Text(
                "These open Android's own screens. REDCORE cannot change them silently — that needs root, and this build stays honest about it.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShortcutTile("Display", Icons.Rounded.DisplaySettings, Modifier.weight(1f)) {
                    PermissionHelper.openDisplaySettings(context)
                }
                ShortcutTile("Sound", Icons.Rounded.VolumeUp, Modifier.weight(1f)) {
                    PermissionHelper.openSoundSettings(context)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShortcutTile("Battery", Icons.Rounded.BatterySaver, Modifier.weight(1f)) {
                    PermissionHelper.openBatteryOptimisation(context)
                }
                ShortcutTile("Dev options", Icons.Rounded.DeveloperMode, Modifier.weight(1f)) {
                    PermissionHelper.openDevSettings(context)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Learn")
        GlassCard {
            NavRow("Guided tour", "Every feature in two minutes", Icons.Rounded.School, accent) {
                nav.navigate(Routes.TUTORIAL)
            }
            Spacer(Modifier.height(8.dp))
            NavRow("Privacy", "Nothing leaves your device", Icons.Rounded.PrivacyTip, accent) {
                nav.navigate(Routes.PRIVACY)
            }
            Spacer(Modifier.height(8.dp))
            NavRow("About", "Version ${BuildConfig.VERSION_NAME}", Icons.Rounded.Info, accent) {
                nav.navigate(Routes.ABOUT)
            }
        }

        Spacer(Modifier.height(16.dp))
        InfoNote(
            text = "REDCORE never fakes a reading. If a value needs root or a manufacturer API we do not have, the app says so instead of inventing a number.",
            accent = accent
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ShortcutTile(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x08FFFFFF))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

@Composable
private fun NavRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = TextMuted)
    }
}
