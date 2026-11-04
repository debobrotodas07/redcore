package com.redcore.gameengine.ui.overlay

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redcore.gameengine.data.local.AppSettings
import com.redcore.gameengine.data.model.PerformanceProfile
import com.redcore.gameengine.data.model.SidebarSide
import com.redcore.gameengine.data.model.SidebarTool
import com.redcore.gameengine.sys.DeviceProbe
import com.redcore.gameengine.sys.PermissionHelper
import com.redcore.gameengine.sys.ProfileApplier
import com.redcore.gameengine.ui.components.AnimatedCoreLogo
import com.redcore.gameengine.ui.theme.BgCard
import com.redcore.gameengine.ui.theme.TextMuted
import com.redcore.gameengine.ui.theme.TextPrimary
import com.redcore.gameengine.ui.theme.TextSecondary
import com.redcore.gameengine.util.Format
import kotlinx.coroutines.delay

/**
 * The floating gaming panel. Drawn inside the overlay window, so every control
 * here acts on the device through a real API — no mock switches.
 */
@Composable
fun SidebarContent(
    settings: AppSettings,
    profile: PerformanceProfile,
    side: SidebarSide,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onProfileChange: (PerformanceProfile) -> Unit,
    onExternalTool: (SidebarTool) -> Unit,
    onBrightness: (Int) -> Unit,
    onVolume: (Int) -> Unit,
    onStop: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val alpha = (settings.sidebarOpacity / 100f).coerceIn(0.35f, 1f)
    var activeTool by remember { mutableStateOf<SidebarTool?>(null) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (side == SidebarSide.RIGHT) {
            Panel(settings, profile, expanded, activeTool, accent, alpha, side, onProfileChange, onExternalTool, onBrightness, onVolume, onStop, context) {
                activeTool = it
                if (it == null) onToggleExpand()
            }
        }

        // Drag handle
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(112.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() }
                    ) { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.y)
                    }
                }
                .clip(
                    if (side == SidebarSide.LEFT) RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
                    else RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                )
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.85f * alpha), Color(0xFF1A0306).copy(alpha = alpha))
                    )
                )
                .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .onTap(expanded) { onToggleExpand() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedCoreLogo(size = 18.dp, animated = true)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (side == SidebarSide.LEFT) "›" else "‹",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (side == SidebarSide.LEFT) {
            Panel(settings, profile, expanded, activeTool, accent, alpha, side, onProfileChange, onExternalTool, onBrightness, onVolume, onStop, context) {
                activeTool = it
                if (it == null) onToggleExpand()
            }
        }
    }
}

@Composable
private fun Panel(
    settings: AppSettings,
    profile: PerformanceProfile,
    expanded: Boolean,
    activeTool: SidebarTool?,
    accent: Color,
    alpha: Float,
    side: SidebarSide,
    onProfileChange: (PerformanceProfile) -> Unit,
    onExternalTool: (SidebarTool) -> Unit,
    onBrightness: (Int) -> Unit,
    onVolume: (Int) -> Unit,
    onStop: () -> Unit,
    context: Context,
    onSelectTool: (SidebarTool?) -> Unit
) {
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(tween(180)) + expandHorizontally(
            tween(200), expandFrom = if (side == SidebarSide.LEFT) androidx.compose.ui.Alignment.Start
            else androidx.compose.ui.Alignment.End
        ),
        exit = fadeOut(tween(140)) + shrinkHorizontally(
            tween(160), shrinkTowards = if (side == SidebarSide.LEFT) androidx.compose.ui.Alignment.Start
            else androidx.compose.ui.Alignment.End
        )
    ) {
        Column(
            modifier = Modifier
                .width(238.dp)
                .padding(6.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF15151B).copy(alpha = 0.96f * alpha),
                            Color(0xFF0B0B0E).copy(alpha = 0.94f * alpha)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(listOf(Color(0x33FFFFFF), accent.copy(alpha = 0.45f))),
                    RoundedCornerShape(22.dp)
                )
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedCoreLogo(size = 26.dp, animated = false)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "REDCORE",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "${profile.label} • ${settings.sidebarTools.size} tools",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .onTap { onStop() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Stop overlay", tint = TextSecondary, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            // Tool grid (2 columns)
            settings.sidebarTools.chunked(2).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { tool ->
                        ToolButton(
                            tool = tool,
                            active = activeTool == tool,
                            accent = accent,
                            modifier = Modifier.weight(1f)
                        ) {
                            val next = if (activeTool == tool) null else tool
                            onSelectTool(next)
                            when (tool) {
                                SidebarTool.SCREENSHOT,
                                SidebarTool.RECORD,
                                SidebarTool.QUICK_SETTINGS -> if (next != null) onExternalTool(tool)
                                SidebarTool.DND -> if (next != null) toggleDnd(context, accent)
                                else -> Unit
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }

            AnimatedVisibility(visible = activeTool != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x66000000))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .padding(10.dp)
                ) {
                    when (activeTool) {
                        SidebarTool.FPS -> TelemetryBlock(context, accent)
                        SidebarTool.NETWORK -> TelemetryBlock(context, accent)
                        SidebarTool.BRIGHTNESS -> BrightnessBlock(context, accent, onBrightness)
                        SidebarTool.VOLUME -> VolumeBlock(context, accent, onVolume)
                        SidebarTool.TIMER -> TimerBlock(accent)
                        SidebarTool.PROFILE -> ProfileBlock(profile, accent, onProfileChange)
                        SidebarTool.DND -> Text(
                            "Interruption filter toggled. Needs Do Not Disturb access in REDCORE settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        SidebarTool.SCREENSHOT -> Text(
                            "Opening REDCORE to collect Android's one-time screen capture consent…",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        SidebarTool.RECORD -> Text(
                            "Opening REDCORE recording controls…",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        SidebarTool.QUICK_SETTINGS -> Text(
                            "Opening REDCORE settings…",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        null -> Unit
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "Drag the red handle to move • tap to collapse",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ToolButton(
    tool: SidebarTool,
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (active) accent.copy(alpha = 0.18f) else Color(0x0DFFFFFF))
            .border(1.dp, if (active) accent.copy(alpha = 0.5f) else Color(0x14FFFFFF), RoundedCornerShape(13.dp))
            .onTap(tool) { onClick() }
            .padding(horizontal = 9.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(toolIcon(tool), contentDescription = null, tint = if (active) accent else TextSecondary, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            tool.label,
            style = MaterialTheme.typography.bodySmall,
            color = if (active) TextPrimary else TextSecondary,
            maxLines = 1
        )
    }
}

private fun toolIcon(tool: SidebarTool): ImageVector = when (tool) {
    SidebarTool.FPS -> Icons.Rounded.Speed
    SidebarTool.NETWORK -> Icons.Rounded.Wifi
    SidebarTool.BRIGHTNESS -> Icons.Rounded.Brightness6
    SidebarTool.VOLUME -> Icons.Rounded.GraphicEq
    SidebarTool.TIMER -> Icons.Rounded.Timer
    SidebarTool.SCREENSHOT -> Icons.Rounded.PhotoCamera
    SidebarTool.RECORD -> Icons.Rounded.Videocam
    SidebarTool.DND -> Icons.Rounded.DoNotDisturbOn
    SidebarTool.PROFILE -> Icons.Rounded.Widgets
    SidebarTool.QUICK_SETTINGS -> Icons.Rounded.Settings
}

@Composable
private fun TelemetryBlock(context: Context, accent: Color) {
    val probe = remember { DeviceProbe(context) }
    var snap by remember { mutableStateOf(probe.snapshot()) }
    LaunchedEffect(Unit) {
        while (true) {
            snap = probe.snapshot()
            delay(1500)
        }
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.DeveloperBoard, null, tint = accent, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text("DEVICE LOAD", style = MaterialTheme.typography.labelSmall, color = accent)
        }
        Spacer(Modifier.height(6.dp))
        MiniStat(Icons.Rounded.Memory, "CPU", Format.percent(snap.cpuUsagePercent), accent)
        MiniStat(Icons.Rounded.BatteryStd, "Battery", "${snap.batteryPercent}%", accent)
        MiniStat(Icons.Rounded.Thermostat, "Temp", Format.temp(snap.batteryTempC), accent)
        MiniStat(Icons.Rounded.SwapVert, "RAM", "${snap.memUsedPercent}%", accent)
        MiniStat(Icons.Rounded.Wifi, "Down", Format.bytesPerSecond(snap.rxBytesPerSec), accent)
        Spacer(Modifier.height(6.dp))
        Text(
            "Display ${snap.refreshHz.toInt()} Hz • per-game FPS needs root",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@Composable
private fun MiniStat(icon: ImageVector, label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextMuted, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
    }
}

@Composable
private fun BrightnessBlock(context: Context, accent: Color, onBrightness: (Int) -> Unit) {
    var value by remember { mutableStateOf(currentBrightnessPercent(context)) }
    Column {
        Text("BRIGHTNESS", style = MaterialTheme.typography.labelSmall, color = accent)
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onBrightness(value.toInt()) },
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Color(0x22FFFFFF))
        )
        Text(
            if (PermissionHelper.writeSettings(context)) "${value.toInt()}%"
            else "Grant \"Modify system settings\" in REDCORE ▸ Permissions to change system brightness.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@Composable
private fun VolumeBlock(context: Context, accent: Color, onVolume: (Int) -> Unit) {
    var value by remember { mutableStateOf(ProfileApplier.mediaVolumePercent(context).toFloat()) }
    Column {
        Text("MEDIA VOLUME", style = MaterialTheme.typography.labelSmall, color = accent)
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onVolume(value.toInt()) },
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Color(0x22FFFFFF))
        )
        Text("${value.toInt()}%", style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

@Composable
private fun TimerBlock(accent: Color) {
    var running by remember { mutableStateOf(false) }
    var seconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(running) {
        while (running) {
            delay(1000)
            seconds += 1
        }
    }
    Column {
        Text("SESSION TIMER", style = MaterialTheme.typography.labelSmall, color = accent)
        Text(
            Format.clock(seconds),
            style = MaterialTheme.typography.displayMedium,
            color = TextPrimary,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OverlayChip(if (running) "Pause" else "Start", accent) { running = !running }
            OverlayChip("Reset", Color(0xFF8A8A94)) {
                running = false
                seconds = 0
            }
        }
    }
}

@Composable
private fun ProfileBlock(
    profile: PerformanceProfile,
    accent: Color,
    onProfileChange: (PerformanceProfile) -> Unit
) {
    Column {
        Text("PERFORMANCE PROFILE", style = MaterialTheme.typography.labelSmall, color = accent)
        Spacer(Modifier.height(6.dp))
        PerformanceProfile.entries.forEach { p ->
            val selected = p == profile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) p.accent.copy(alpha = 0.16f) else Color.Transparent)
                    .onTap(p) { onProfileChange(p) }
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (selected) p.accent else Color(0x33FFFFFF))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    p.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) TextPrimary else TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Icon(Icons.Rounded.Check, null, tint = p.accent, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@Composable
private fun OverlayChip(text: String, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(9.dp))
            .onTap(text) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
    }
}

private fun Modifier.onTap(key: Any? = Unit, onTap: () -> Unit): Modifier =
    this.pointerInput(key) { detectTapGestures { onTap() } }

private fun currentBrightnessPercent(context: Context): Float {
    return try {
        val value = android.provider.Settings.System.getInt(
            context.contentResolver,
            android.provider.Settings.System.SCREEN_BRIGHTNESS,
            128
        )
        (value * 100f / 255f).coerceIn(0f, 100f)
    } catch (t: Throwable) {
        50f
    }
}

private fun toggleDnd(context: Context, accent: Color) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    if (!nm.isNotificationPolicyAccessGranted) {
        PermissionHelper.openPolicySettings(context)
        return
    }
    val next = if (nm.currentInterruptionFilter == android.app.NotificationManager.INTERRUPTION_FILTER_ALL) {
        android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY
    } else {
        android.app.NotificationManager.INTERRUPTION_FILTER_ALL
    }
    try {
        nm.setInterruptionFilter(next)
    } catch (_: Throwable) {
    }
    @Suppress("UNUSED_EXPRESSION")
    accent
}
