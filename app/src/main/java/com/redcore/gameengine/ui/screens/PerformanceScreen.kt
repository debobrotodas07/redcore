package com.redcore.gameengine.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.redcore.gameengine.data.model.PerformanceProfile
import com.redcore.gameengine.sys.PermissionHelper
import com.redcore.gameengine.sys.ProfileAction
import com.redcore.gameengine.ui.components.GhostButton
import com.redcore.gameengine.ui.components.GlassCard
import com.redcore.gameengine.ui.components.InfoNote
import com.redcore.gameengine.ui.components.KeyValueRow
import com.redcore.gameengine.ui.components.PillTag
import com.redcore.gameengine.ui.components.PrimaryButton
import com.redcore.gameengine.ui.components.ScreenColumn
import com.redcore.gameengine.ui.components.ScreenTopBar
import com.redcore.gameengine.ui.components.SectionHeader
import com.redcore.gameengine.ui.theme.TextMuted
import com.redcore.gameengine.ui.theme.TextPrimary
import com.redcore.gameengine.ui.theme.TextSecondary
import com.redcore.gameengine.ui.vm.DashboardViewModel
import com.redcore.gameengine.util.Format

@Composable
fun PerformanceScreen(nav: NavHostController) {
    val vm: DashboardViewModel = viewModel()
    val settings by vm.settings.collectAsState()
    val snap by vm.snapshot.collectAsState()
    val actions by vm.lastActions.collectAsState()
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    var expanded by remember { mutableStateOf<PerformanceProfile?>(settings.globalProfile) }

    ScreenColumn {
        ScreenTopBar(
            title = "Performance",
            subtitle = "Honest controls, no fake overclocking"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillTag("${snap.cores} cores", accent = accent)
            PillTag(Format.temp(snap.batteryTempC), accent = accent)
            PillTag("${snap.refreshHz.toInt()} Hz", accent = accent)
            PillTag(snap.thermalStatus, accent = accent)
        }

        Spacer(Modifier.height(14.dp))

        PerformanceProfile.entries.forEach { profile ->
            val selected = settings.globalProfile == profile
            val isOpen = expanded == profile
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        if (selected) Brush.linearGradient(
                            listOf(profile.accent.copy(alpha = 0.18f), Color(0x0AFFFFFF))
                        ) else Brush.linearGradient(listOf(Color(0x0EFFFFFF), Color(0x06FFFFFF)))
                    )
                    .border(
                        1.dp,
                        if (selected) profile.accent.copy(alpha = 0.55f) else Color(0x14FFFFFF),
                        RoundedCornerShape(22.dp)
                    )
                    .clickable { expanded = if (isOpen) null else profile }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(profile.accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Speed, contentDescription = null, tint = profile.accent, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                profile.label,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (selected) profile.accent else TextPrimary
                            )
                            if (selected) {
                                Spacer(Modifier.width(8.dp))
                                PillTag("Active", accent = profile.accent, filled = true)
                            }
                        }
                        Text(profile.tagline, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Text(
                        if (isOpen) "−" else "+",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextMuted
                    )
                }

                AnimatedVisibility(visible = isOpen) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        Text("APPLIES", style = MaterialTheme.typography.labelSmall, color = profile.accent)
                        Spacer(Modifier.height(6.dp))
                        profile.applies.forEach { item ->
                            BulletLine(item, profile.accent, Icons.Rounded.Check)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("SUGGESTED IN SYSTEM SETTINGS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(Modifier.height(6.dp))
                        profile.suggests.forEach { item ->
                            BulletLine(item, TextMuted, Icons.Rounded.Launch)
                        }
                        Spacer(Modifier.height(14.dp))
                        PrimaryButton(
                            text = if (selected) "Re-apply ${profile.label}" else "Switch to ${profile.label}",
                            accent = profile.accent,
                            onClick = { vm.applyProfile(context, profile) }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = actions.isNotEmpty()) {
            Column {
                SectionHeader(title = "Last applied result")
                GlassCard(accent = accent) {
                    actions.forEach { action -> ActionRow(action) }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "System shortcuts")
        GlassCard {
            Text(
                "These open the real controls. They are shortcuts, not switches REDCORE owns.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(10.dp))
            Column {
                ShortcutRow(Icons.Rounded.BatteryChargingFull, "Battery optimisation", "Allow REDCORE and games to run unrestricted") {
                    PermissionHelper.openBatteryOptimisation(context)
                }
                ShortcutRow(Icons.Rounded.DeveloperBoard, "Developer options", "Refresh rate, animation scale, GPU settings") {
                    PermissionHelper.openDevSettings(context)
                }
                ShortcutRow(Icons.Rounded.Speed, "Display settings", "Peak refresh rate and resolution") {
                    PermissionHelper.openDisplaySettings(context)
                }
                ShortcutRow(Icons.Rounded.Info, "App info", "Battery usage and permissions for REDCORE") {
                    PermissionHelper.openAppInfo(context)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        InfoNote(
            text = "CPU/GPU frequencies, thermal limits and per-game boost are firmware-level. A normal app cannot touch them — Android and your device vendor own those interfaces. REDCORE changes only what it is allowed to change, and labels every skipped step.",
            accent = accent
        )

        Spacer(Modifier.height(16.dp))
        GlassCard {
            KeyValueRow("Governor control", "Not available to user apps")
            KeyValueRow("Thermal limits", "Firmware controlled")
            KeyValueRow("Per-window refresh", "Supported (REDCORE only)")
            KeyValueRow("Brightness override", if (PermissionHelper.writeSettings(context)) "Granted" else "Needs system permission")
            KeyValueRow("Do Not Disturb", if (PermissionHelper.notificationPolicy(context)) "Granted" else "Needs policy access")
            Spacer(Modifier.height(8.dp))
            GhostButton(text = "Open permission manager", onClick = { nav.navigate(com.redcore.gameengine.navigation.Routes.PERMISSIONS) })
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun BulletLine(text: String, tint: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp).padding(top = 2.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun ActionRow(action: ProfileAction) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(
                    if (action.applied) Color(0x2235D07F) else Color(0x22FFB020)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (action.applied) Icons.Rounded.Check else Icons.Rounded.Close,
                contentDescription = null,
                tint = if (action.applied) Color(0xFF35D07F) else Color(0xFFFFB020),
                modifier = Modifier.size(11.dp)
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                action.label,
                style = MaterialTheme.typography.titleSmall,
                color = if (action.applied) TextPrimary else TextSecondary
            )
            Text(action.detail, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

@Composable
private fun ShortcutRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x08FFFFFF))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Icon(Icons.Rounded.Launch, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp))
    }
}
