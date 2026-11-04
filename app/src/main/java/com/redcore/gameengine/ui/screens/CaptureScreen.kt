package com.redcore.gameengine.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
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
import com.redcore.gameengine.ui.vm.CaptureViewModel
import com.redcore.gameengine.ui.vm.SettingsViewModel
import com.redcore.gameengine.util.Format
import kotlinx.coroutines.delay

@Composable
fun CaptureScreen(nav: NavHostController) {
    val vm: CaptureViewModel = viewModel()
    val settingsVm: SettingsViewModel = viewModel()
    val settings by settingsVm.settings.collectAsState()
    val recording by vm.recording.collectAsState()
    val startedAt by vm.startedAt.collectAsState()
    val message by vm.message.collectAsState()
    val screenshot by vm.lastScreenshot.collectAsState()
    val video by vm.lastVideo.collectAsState()
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    var elapsed by remember { mutableLongStateOf(0L) }
    var micEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(recording) {
        while (recording) {
            val start = if (startedAt > 0) startedAt else System.currentTimeMillis()
            elapsed = (System.currentTimeMillis() - start) / 1000
            delay(500)
        }
        if (!recording) elapsed = 0
    }

    ScreenColumn {
        ScreenTopBar(
            title = "Capture",
            subtitle = "Screenshots and screen recording"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            if (recording) Color(0xFF3A0A10) else Color(0x14FFFFFF),
                            Color(0x0AFFFFFF)
                        )
                    )
                )
                .border(
                    1.dp,
                    if (recording) accent.copy(alpha = 0.5f) else Color(0x18FFFFFF),
                    RoundedCornerShape(24.dp)
                )
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (recording) Color(0xFFFF2638) else Color(0xFF3A3A44))
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    if (recording) "RECORDING" else "STANDBY",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (recording) accent else TextMuted
                )
                Spacer(Modifier.weight(1f))
                Text(
                    Format.clock(elapsed),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (recording)
                    "Android shows the capture indicator while this runs. Saving to Movies/REDCORE."
                else "Android asks for consent every time a capture starts — that is a platform rule, not a REDCORE limitation.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                text = "Screenshot",
                icon = Icons.Rounded.PhotoCamera,
                onClick = { vm.screenshot() },
                modifier = Modifier.weight(1f)
            )
            if (recording) {
                PrimaryButton(
                    text = "Stop",
                    icon = Icons.Rounded.Stop,
                    accent = Color(0xFFFF4D5E),
                    onClick = { vm.stopRecording() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                PrimaryButton(
                    text = "Record",
                    icon = Icons.Rounded.Videocam,
                    onClick = { vm.record(settings.recordingQuality, micEnabled) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Recording quality")
        GlassCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("720" to "720p", "1080" to "1080p").forEach { (value, label) ->
                    val selected = settings.recordingQuality == value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (selected) accent.copy(alpha = 0.15f) else Color(0x08FFFFFF))
                            .border(
                                1.dp,
                                if (selected) accent.copy(alpha = 0.55f) else Color(0x14FFFFFF),
                                RoundedCornerShape(13.dp)
                            )
                            .clickable { settingsVm.setRecordingQuality(value) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) accent else TextSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Frame rate is locked at 60 fps, H.264, and the bitrate scales with resolution.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Spacer(Modifier.height(6.dp))
            ToggleRow(
                title = "Record microphone audio",
                subtitle = "Mixes your mic into the video track",
                checked = micEnabled,
                accent = accent,
                onCheckedChange = { micEnabled = it }
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Latest captures")
        GlassCard {
            CaptureRow(
                label = "Last screenshot",
                uri = screenshot,
                accent = accent,
                onOpen = { uri ->
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW)
                                .setDataAndType(uri, "image/png")
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Throwable) {
                    }
                }
            )
            CaptureRow(
                label = "Last recording",
                uri = video,
                accent = accent,
                onOpen = { uri ->
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW)
                                .setDataAndType(uri, "video/mp4")
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Throwable) {
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Files live in Pictures/REDCORE and Movies/REDCORE. Delete them from your gallery to reclaim space.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        if (message != null) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.10f))
                    .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(message!!, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Dismiss",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { vm.clearMessage() }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        InfoNote(
            text = "Android 14 requires a visible foreground service for capture, and consent is granted per session. Sidebar capture buttons open this screen so you can approve that consent.",
            accent = accent
        )

        Spacer(Modifier.height(20.dp))
    }

}

@Composable
private fun CaptureRow(
    label: String,
    uri: Uri?,
    accent: Color,
    onOpen: (Uri) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0x08FFFFFF))
            .clickable(enabled = uri != null) { uri?.let(onOpen) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.FiberManualRecord, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
        if (uri != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillTag("Open", accent = accent, filled = true)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
            }
        } else {
            Text("none yet", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}
