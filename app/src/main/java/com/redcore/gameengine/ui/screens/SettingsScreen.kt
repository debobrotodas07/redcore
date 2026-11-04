package com.redcore.gameengine.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.redcore.gameengine.BuildConfig
import com.redcore.gameengine.data.model.AccentTheme
import com.redcore.gameengine.data.model.SidebarSide
import com.redcore.gameengine.navigation.Routes
import com.redcore.gameengine.ui.components.GlassCard
import com.redcore.gameengine.ui.components.InfoNote
import com.redcore.gameengine.ui.components.PillTag
import com.redcore.gameengine.ui.components.PrimaryButton
import com.redcore.gameengine.ui.components.ScreenColumn
import com.redcore.gameengine.ui.components.ScreenTopBar
import com.redcore.gameengine.ui.components.SectionHeader
import com.redcore.gameengine.ui.components.SelectRow
import com.redcore.gameengine.ui.components.ToggleRow
import com.redcore.gameengine.ui.theme.TextMuted
import com.redcore.gameengine.ui.theme.TextPrimary
import com.redcore.gameengine.ui.theme.TextSecondary
import com.redcore.gameengine.ui.vm.SettingsViewModel

@Composable
fun SettingsScreen(nav: NavHostController) {
    val vm: SettingsViewModel = viewModel()
    val settings by vm.settings.collectAsState()
    val message by vm.message.collectAsState()
    val busy by vm.busy.collectAsState()
    val update by vm.update.collectAsState()
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    var showWipe by remember { mutableStateOf(false) }
    var showClearSessions by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? -> uri?.let { vm.export(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { vm.import(it) } }

    ScreenColumn {
        ScreenTopBar(
            title = "Settings",
            subtitle = "Tune REDCORE to your device",
            onBack = { nav.popBackStack() }
        )

        SectionHeader(title = "Accent")
        GlassCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentTheme.entries.forEach { theme ->
                    val selected = settings.accent == theme
                    val swatch = themeSwatchColor(theme)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(swatch.copy(alpha = 0.18f))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) swatch else Color(0x22FFFFFF),
                                shape = CircleShape
                            )
                            .clickable { vm.setAccent(theme) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (selected) 22.dp else 16.dp)
                                .clip(CircleShape)
                                .background(swatch)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                settings.accent.label + " • applies across the app immediately",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Interface")
        GlassCard {
            ToggleRow(
                title = "Animations",
                subtitle = "Turn off for a lighter, battery-friendly feel",
                checked = settings.animations,
                accent = accent,
                onCheckedChange = { vm.setAnimations(it) }
            )
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                title = "Haptic feedback",
                subtitle = "Short vibrations on taps and toggles",
                checked = settings.haptics,
                accent = accent,
                onCheckedChange = { vm.setHaptics(it) }
            )
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                title = "Notifications",
                subtitle = "Required to keep overlay and capture services alive",
                checked = settings.notifications,
                accent = accent,
                onCheckedChange = { vm.setNotifications(it) }
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Floating sidebar")
        GlassCard {
            ToggleRow(
                title = "Enable sidebar",
                subtitle = "Requires \"Display over other apps\"",
                checked = settings.sidebarEnabled,
                accent = accent,
                onCheckedChange = { vm.setSidebarEnabled(it) }
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SidebarSide.entries.forEach { side ->
                    val selected = settings.sidebarSide == side
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (selected) accent.copy(alpha = 0.15f) else Color(0x08FFFFFF))
                            .border(
                                1.dp,
                                if (selected) accent.copy(alpha = 0.5f) else Color(0x14FFFFFF),
                                RoundedCornerShape(13.dp)
                            )
                            .clickable { vm.setSidebarSideLeft(side == SidebarSide.LEFT) }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (side == SidebarSide.LEFT) "Left edge" else "Right edge",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) accent else TextSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            ToggleRow(
                title = "Compact handle",
                subtitle = "Smaller bar with icons only",
                checked = settings.sidebarCompact,
                accent = accent,
                onCheckedChange = { vm.setSidebarCompact(it) }
            )
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                title = "Restore after reboot",
                subtitle = "Re-opens the sidebar when the device restarts",
                checked = settings.sidebarAutoStart,
                accent = accent,
                onCheckedChange = { vm.setSidebarAutoStart(it) }
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Opacity", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
                Text("${settings.sidebarOpacity}%", style = MaterialTheme.typography.labelMedium, color = accent)
            }
            Slider(
                value = settings.sidebarOpacity.toFloat(),
                onValueChange = { vm.setSidebarOpacity(it.toInt()) },
                valueRange = 35f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = Color(0x1FFFFFFF)
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vertical position", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.resetSidebarPosition() }) {
                    Text("Reset", color = accent, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(
                "Drag the handle on screen to place it exactly where your thumb rests.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "In-game tools")
        GlassCard {
            ToggleRow(
                title = "Crosshair overlay",
                subtitle = "Draws a static aim reference over your game",
                checked = settings.crosshairEnabled,
                accent = accent,
                onCheckedChange = { vm.setCrosshairEnabled(it) }
            )
            Spacer(Modifier.height(8.dp))
            SelectRow(
                title = "Crosshair design",
                subtitle = "${settings.crosshairDesign.label} • ${settings.crosshairSize}dp",
                selected = false,
                accent = accent,
                onClick = { nav.navigate(Routes.CROSSHAIR) },
                trailing = { PillTag("Open", accent = accent) }
            )
            Spacer(Modifier.height(8.dp))
            SelectRow(
                title = "Capture & recording",
                subtitle = "Quality ${settings.recordingQuality}p",
                selected = false,
                accent = accent,
                onClick = { nav.navigate(Routes.CAPTURE) },
                trailing = { PillTag("Open", accent = accent) }
            )
            Spacer(Modifier.height(8.dp))
            SelectRow(
                title = "Permission centre",
                subtitle = "See exactly what is granted",
                selected = false,
                accent = accent,
                onClick = { nav.navigate(Routes.PERMISSIONS) },
                trailing = { PillTag("Open", accent = accent) }
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Monitoring")
        GlassCard {
            val intervals = listOf(500 to "0.5 s", 1000 to "1 s", 2000 to "2 s")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                intervals.forEach { (ms, label) ->
                    val selected = settings.monitorIntervalMs == ms
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (selected) accent.copy(alpha = 0.15f) else Color(0x08FFFFFF))
                            .border(
                                1.dp,
                                if (selected) accent.copy(alpha = 0.5f) else Color(0x14FFFFFF),
                                RoundedCornerShape(13.dp)
                            )
                            .clickable { vm.setMonitorInterval(ms) }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) accent else TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Faster polling costs more battery. 0.5 s is useful while actively watching thermals.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                title = "Session alerts",
                subtitle = "Warn when a game session passes 60 minutes",
                checked = settings.sessionAlerts,
                accent = accent,
                onCheckedChange = { vm.setSessionAlerts(it) }
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Backup")
        GlassCard {
            Text(
                "Your library, game profiles and settings export as a single JSON file you own. Nothing leaves the device unless you share the file.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    text = "Export",
                    onClick = { exportLauncher.launch(vm.suggestedBackupName()) },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = "Restore",
                    accent = Color(0xFF6C7A89),
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            SelectRow(
                title = "Clear session history",
                subtitle = "Keeps games and profiles",
                selected = false,
                accent = accent,
                onClick = { showClearSessions = true },
                trailing = { PillTag("Clear", accent = accent) }
            )
            Spacer(Modifier.height(8.dp))
            SelectRow(
                title = "Wipe library",
                subtitle = "Deletes every game, profile and session",
                selected = false,
                accent = Color(0xFFFF5A5A),
                onClick = { showWipe = true },
                trailing = { PillTag("Wipe", accent = Color(0xFFFF5A5A), filled = true) }
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "About")
        GlassCard {
            SelectRow(
                title = "How REDCORE works",
                subtitle = "Two-minute guided tour",
                selected = false,
                accent = accent,
                onClick = { nav.navigate(Routes.TUTORIAL) },
                trailing = { PillTag("Open", accent = accent) }
            )
            Spacer(Modifier.height(8.dp))
            SelectRow(
                title = "Privacy",
                subtitle = "No servers, no trackers, no analytics",
                selected = false,
                accent = accent,
                onClick = { nav.navigate(Routes.PRIVACY) },
                trailing = { PillTag("Read", accent = accent) }
            )
            Spacer(Modifier.height(8.dp))
            SelectRow(
                title = "About REDCORE",
                subtitle = "Version ${BuildConfig.VERSION_NAME}",
                selected = false,
                accent = accent,
                onClick = { nav.navigate(Routes.ABOUT) },
                trailing = { PillTag("Info", accent = accent) }
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                text = if (update.checking) "Checking…" else "Check for updates",
                onClick = { vm.checkForUpdates(BuildConfig.UPDATE_REPO) },
                enabled = !update.checking,
                accent = accent,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                update.message,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        if (message != null) {
            Spacer(Modifier.height(14.dp))
            InfoNote(text = message!!, accent = accent)
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "REDCORE GAME ENGINE ${BuildConfig.VERSION_NAME} • built ${BuildConfig.BUILD_TYPE}",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(Modifier.height(20.dp))
    }

    if (showWipe) {
        AlertDialog(
            onDismissRequest = { showWipe = false },
            title = { Text("Wipe everything?") },
            text = { Text("Every game, profile and session will be deleted. Your settings survive, and an exported backup can bring the library back.") },
            confirmButton = {
                TextButton(onClick = {
                    showWipe = false
                    vm.wipeLibrary()
                }) { Text("Wipe", color = Color(0xFFFF5A5A)) }
            },
            dismissButton = {
                TextButton(onClick = { showWipe = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = Color(0xFF14141A),
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    if (showClearSessions) {
        AlertDialog(
            onDismissRequest = { showClearSessions = false },
            title = { Text("Clear session history?") },
            text = { Text("Playtime totals stay, but every recorded session entry is removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearSessions = false
                    vm.clearSessions()
                }) { Text("Clear", color = accent) }
            },
            dismissButton = {
                TextButton(onClick = { showClearSessions = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = Color(0xFF14141A),
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

internal fun themeSwatchColor(theme: AccentTheme): Color = when (theme) {
    AccentTheme.CRIMSON -> Color(0xFFFF2638)
    AccentTheme.EMBER -> Color(0xFFFF7A1A)
    AccentTheme.CYAN -> Color(0xFF19D2FF)
    AccentTheme.VIOLET -> Color(0xFF9A5CFF)
    AccentTheme.TOXIC -> Color(0xFF9BFF3D)
}
