package com.redcore.gameengine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.redcore.gameengine.ui.components.GlassCard
import com.redcore.gameengine.ui.components.InfoNote
import com.redcore.gameengine.ui.components.MetricBar
import com.redcore.gameengine.ui.components.PillTag
import com.redcore.gameengine.ui.components.ScreenColumn
import com.redcore.gameengine.ui.components.ScreenTopBar
import com.redcore.gameengine.ui.components.SectionHeader
import com.redcore.gameengine.ui.components.StatTile
import com.redcore.gameengine.ui.theme.BgCard
import com.redcore.gameengine.ui.theme.TextMuted
import com.redcore.gameengine.ui.theme.TextPrimary
import com.redcore.gameengine.ui.theme.TextSecondary
import com.redcore.gameengine.ui.vm.MonitorViewModel
import com.redcore.gameengine.util.Format
import kotlin.math.roundToInt

@Composable
fun MonitorScreen(nav: NavHostController) {
    val vm: MonitorViewModel = viewModel()
    val snap by vm.snapshot.collectAsState()
    val peakCpu by vm.peakCpu.collectAsState()
    val peakTemp by vm.peakBatteryTemp.collectAsState()
    val samples by vm.sampleCount.collectAsState()
    val accent = MaterialTheme.colorScheme.primary

    // Frame pacing of REDCORE's own window — measured, not estimated.
    var uiFps by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        var frames = 0
        var windowStart = 0L
        while (true) {
            withFrameNanos { now ->
                if (windowStart == 0L) windowStart = now
                frames++
                val elapsed = now - windowStart
                if (elapsed >= 1_000_000_000L) {
                    uiFps = (frames * 1_000_000_000L / elapsed).toInt()
                    frames = 0
                    windowStart = now
                }
            }
        }
    }

    ScreenColumn {
        ScreenTopBar(
            title = "System monitor",
            subtitle = "$samples samples • ${Format.temp(snap.batteryTempC)} battery"
        ) {
            IconButton(onClick = { vm.resetPeaks() }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Reset peaks", tint = TextSecondary)
            }
        }

        // FPS panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.20f), BgCard.copy(alpha = 0.7f))
                    )
                )
                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MonitorHeart, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("FRAME PACING", style = MaterialTheme.typography.labelSmall, color = accent)
                Spacer(Modifier.weight(1f))
                PillTag("${snap.refreshHz.roundToInt()} Hz panel", accent = accent)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (uiFps > 0) "$uiFps" else "—",
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "FPS",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 9.dp)
                )
            }
            Text(
                "Measured on REDCORE's own window using the platform frame clock.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(12.dp))
            MetricBar(
                progress = if (snap.refreshHz > 0) (uiFps / snap.refreshHz).coerceIn(0f, 1f) else 0f,
                accent = accent,
                height = 8
            )
        }

        Spacer(Modifier.height(12.dp))
        InfoNote(
            text = "Per-game FPS needs a privileged hook (root, Shizuku or a vendor game mode). Android exposes no public API for another app's frame rate, so REDCORE shows your panel rate and its own pacing instead of a made-up number.",
            accent = accent
        )

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Live load")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "CPU",
                value = Format.percent(snap.cpuUsagePercent),
                icon = Icons.Rounded.Memory,
                accent = accent,
                progress = (snap.cpuUsagePercent ?: 0f) / 100f,
                caption = peakCpu?.let { "peak ${it.toInt()}%" } ?: "measuring…",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "CPU temp",
                value = Format.temp(snap.cpuTempC),
                icon = Icons.Rounded.Thermostat,
                accent = accent,
                caption = if (snap.cpuTempC == null) "not exposed by firmware" else "thermal zone read",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "Memory",
                value = "${snap.memUsedPercent}",
                unit = "%",
                icon = Icons.Rounded.DeveloperBoard,
                accent = accent,
                caption = "${snap.memUsedMb / 1024} / ${snap.memTotalMb / 1024} GB",
                progress = snap.memUsedPercent / 100f,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Battery",
                value = "${snap.batteryPercent}",
                unit = "%",
                icon = Icons.Rounded.BatteryChargingFull,
                accent = if (snap.batteryPercent < 20) Color(0xFFFF4D5E) else accent,
                caption = if (snap.charging) "Charging" else "Discharging",
                progress = snap.batteryPercent / 100f,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Thermals")
        GlassCard(accent = accent) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Battery temperature", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(
                        peakTemp?.let { "Peak ${Format.temp(it)} this session" } ?: "Reading…",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Text(
                    Format.temp(snap.batteryTempC),
                    style = MaterialTheme.typography.headlineMedium,
                    color = when {
                        (snap.batteryTempC ?: 0f) >= 43f -> Color(0xFFFF4D5E)
                        (snap.batteryTempC ?: 0f) >= 38f -> Color(0xFFFFB020)
                        else -> accent
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
            MetricBar(
                progress = ((snap.batteryTempC ?: 0f) - 25f) / 30f,
                accent = when {
                    (snap.batteryTempC ?: 0f) >= 43f -> Color(0xFFFF4D5E)
                    (snap.batteryTempC ?: 0f) >= 38f -> Color(0xFFFFB020)
                    else -> accent
                }
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillTag("Thermal status: ${snap.thermalStatus}", accent = accent)
                if (snap.thermalStatus.contains("Severe", true) || snap.thermalStatus.contains("Critical", true)) {
                    PillTag("Throttling now", accent = Color(0xFFFF4D5E), filled = true)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Network & uptime")
        GlassCard {
            NetworkRow(Icons.Rounded.Wifi, "Download", Format.bytesPerSecond(snap.rxBytesPerSec), accent)
            NetworkRow(Icons.Rounded.SwapVert, "Upload", Format.bytesPerSecond(snap.txBytesPerSec), accent)
            NetworkRow(Icons.Rounded.Timer, "Uptime", Format.duration(snap.uptimeMs / 1000), accent)
            NetworkRow(Icons.Rounded.DeveloperBoard, "Free storage", "%.1f GB".format(snap.freeStorageGb), accent)
            Spacer(Modifier.height(6.dp))
            Text(
                "Traffic is device-wide since boot, sampled every second — not a per-app meter.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Spacer(Modifier.height(16.dp))
        GlassCard(accent = accent) {
            Text("Device", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            Text("${snap.manufacturer} ${snap.model}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
                "${snap.soc} • ${snap.cores} cores • Android ${snap.androidRelease} (API ${snap.sdkInt})",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Sampling every second. Values come from /proc/stat, ActivityManager, BatteryManager, TrafficStats and PowerManager thermal status.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun NetworkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = accent.copy(alpha = 0.8f), modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(9.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
    }
}
