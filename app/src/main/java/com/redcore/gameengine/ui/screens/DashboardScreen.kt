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
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.redcore.gameengine.data.model.GameEntity
import com.redcore.gameengine.data.model.PerformanceProfile
import com.redcore.gameengine.navigation.Routes
import com.redcore.gameengine.ui.components.AnimatedCoreLogo
import com.redcore.gameengine.ui.components.AppIcon
import com.redcore.gameengine.ui.components.GlassCard
import com.redcore.gameengine.ui.components.HaloGlow
import com.redcore.gameengine.ui.components.MetricBar
import com.redcore.gameengine.ui.components.PillTag
import com.redcore.gameengine.ui.components.ScreenColumn
import com.redcore.gameengine.ui.components.SectionHeader
import com.redcore.gameengine.ui.components.StatTile
import com.redcore.gameengine.ui.theme.TextMuted
import com.redcore.gameengine.ui.theme.TextPrimary
import com.redcore.gameengine.ui.theme.TextSecondary
import com.redcore.gameengine.ui.vm.DashboardViewModel
import com.redcore.gameengine.util.Format

@Composable
fun DashboardScreen(nav: NavHostController) {
    val vm: DashboardViewModel = viewModel()
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    val snap by vm.snapshot.collectAsState()
    val games by vm.games.collectAsState()
    val totalSeconds by vm.totalSeconds.collectAsState()
    val weeklySeconds by vm.weeklySeconds.collectAsState()
    val accent = MaterialTheme.colorScheme.primary
    val profile = settings.globalProfile

    ScreenColumn(bottomPadding = 96) {
        // ---- Header ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                HaloGlow(accent = accent, modifier = Modifier.size(78.dp), intensity = 0.16f)
                AnimatedCoreLogo(size = 52.dp, accent = accent, animated = settings.animations)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "REDCORE",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    letterSpacing = 4.sp
                )
                Text(
                    "GAME ENGINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    letterSpacing = 3.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x12FFFFFF))
                    .border(1.dp, Color(0x1FFFFFFF), CircleShape)
                    .clickable { nav.navigate(Routes.SETTINGS) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }

        // ---- Device hero ----
        GlassCard(accent = accent, padding = 16) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        snap.model.ifBlank { "Android device" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Text(
                        "${snap.manufacturer} • Android ${snap.androidRelease} (API ${snap.sdkInt})",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                PillTag(text = profile.label, accent = profile.accent, filled = true)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                snap.soc.take(46),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillTag("${snap.cores} cores", accent = accent)
                PillTag("${snap.refreshHz.toInt()} Hz", accent = accent)
                PillTag(if (snap.charging) "Charging" else "On battery", accent = accent)
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- Core stats ----
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "CPU load",
                value = Format.percent(snap.cpuUsagePercent),
                icon = Icons.Rounded.Memory,
                accent = profile.accent,
                progress = (snap.cpuUsagePercent ?: 0f) / 100f,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Battery",
                value = "${snap.batteryPercent}",
                unit = "%",
                icon = Icons.Rounded.BatteryChargingFull,
                accent = if (snap.batteryPercent < 20) Color(0xFFFF4D5E) else profile.accent,
                progress = snap.batteryPercent / 100f,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "Temperature",
                value = Format.temp(snap.batteryTempC),
                icon = Icons.Rounded.Thermostat,
                accent = when {
                    (snap.batteryTempC ?: 0f) >= 43f -> Color(0xFFFF4D5E)
                    (snap.batteryTempC ?: 0f) >= 38f -> Color(0xFFFFB020)
                    else -> profile.accent
                },
                caption = "Battery sensor • ${snap.thermalStatus}",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Memory",
                value = "${snap.memUsedPercent}",
                unit = "%",
                icon = Icons.Rounded.Widgets,
                accent = profile.accent,
                caption = "${snap.memUsedMb / 1024} / ${snap.memTotalMb / 1024} GB used",
                progress = snap.memUsedPercent / 100f,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(
            title = "Performance profile",
            trailing = "Tune",
            onTrailingClick = { nav.navigate(Routes.PERFORMANCE) }
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(PerformanceProfile.entries.toList()) { p ->
                val selected = p == profile
                Column(
                    modifier = Modifier
                        .width(132.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (selected) Brush.verticalGradient(
                                listOf(p.accent.copy(alpha = 0.26f), Color(0x10FFFFFF))
                            ) else Brush.verticalGradient(listOf(Color(0x0DFFFFFF), Color(0x06FFFFFF)))
                        )
                        .border(
                            1.dp,
                            if (selected) p.accent.copy(alpha = 0.65f) else Color(0x14FFFFFF),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable {
                            vm.selectProfile(p)
                            vm.applyProfile(context, p)
                        }
                        .padding(13.dp)
                ) {
                    Icon(
                        Icons.Rounded.Speed,
                        contentDescription = null,
                        tint = if (selected) p.accent else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        p.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) p.accent else TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        p.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 2
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "Quick actions")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(
                icon = Icons.Rounded.Widgets,
                title = if (settings.sidebarEnabled) "Sidebar ON" else "Sidebar",
                subtitle = "Floating toolkit",
                active = settings.sidebarEnabled,
                modifier = Modifier.weight(1f)
            ) {
                vm.toggleSidebar(context, !settings.sidebarEnabled)
            }
            QuickAction(
                icon = Icons.Rounded.MonitorHeart,
                title = if (settings.crosshairEnabled) "Cross ON" else "Crosshair",
                subtitle = "Overlay aim point",
                active = settings.crosshairEnabled,
                modifier = Modifier.weight(1f)
            ) {
                vm.toggleCrosshair(context, !settings.crosshairEnabled)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(
                icon = Icons.Rounded.PhotoCamera,
                title = "Screenshot",
                subtitle = "System consent",
                modifier = Modifier.weight(1f)
            ) { nav.navigate(Routes.CAPTURE) }
            QuickAction(
                icon = Icons.Rounded.Videocam,
                title = "Record",
                subtitle = "Screen capture",
                modifier = Modifier.weight(1f)
            ) { nav.navigate(Routes.CAPTURE) }
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(
            title = "Recently played",
            trailing = if (games.isEmpty()) null else "Library",
            onTrailingClick = { nav.navigate(Routes.LIBRARY) }
        )
        if (games.isEmpty()) {
            GlassCard(accent = accent) {
                Text(
                    "No games yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Open the Library and let REDCORE detect your installed games, or add one manually.",
                    style = MaterialTheme.typography.bodySmall,
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
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(games.take(10)) { game ->
                    RecentGameCard(game) {
                        vm.launch(context, game)
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "Session statistics")
        GlassCard(accent = accent) {
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TOTAL", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        Format.duration(totalSeconds),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("THIS WEEK", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        Format.duration(weeklySeconds),
                        style = MaterialTheme.typography.headlineMedium,
                        color = accent
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("GAMES", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        "${games.size}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            val target = 8f * 3600f
            MetricBar(
                progress = (weeklySeconds / target).coerceIn(0f, 1f),
                accent = accent
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Weekly playtime against an 8-hour baseline. Sessions are tracked locally on this device only.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Spacer(Modifier.height(18.dp))
        GlassCard(accent = accent, onClick = { nav.navigate(Routes.MONITOR) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MonitorHeart, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Live system monitor", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        "CPU • memory • thermal • network • frame pacing",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "REDCORE reads real sensors only. Per-game FPS and CPU frequency control require root or a system-signed app — where Android refuses, the screen says so instead of inventing numbers.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) accent.copy(alpha = 0.14f) else Color(0x0AFFFFFF))
            .border(1.dp, if (active) accent.copy(alpha = 0.5f) else Color(0x14FFFFFF), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (active) accent else TextSecondary, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(9.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1)
        }
    }
}

@Composable
private fun RecentGameCard(game: GameEntity, onLaunch: () -> Unit) {
    Column(
        modifier = Modifier
            .width(118.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0x12FFFFFF), Color(0x08FFFFFF))))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(18.dp))
            .clickable { onLaunch() }
            .padding(11.dp)
    ) {
        AppIcon(
            packageName = game.packageName,
            customUri = game.customIconUri,
            size = 44,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(9.dp))
        Text(
            game.label,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(3.dp))
        Text(
            Format.ago(game.lastPlayedAt),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            maxLines = 1
        )
    }
}
