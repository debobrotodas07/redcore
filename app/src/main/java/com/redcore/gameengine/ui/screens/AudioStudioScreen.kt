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
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.redcore.gameengine.data.model.AudioPreset
import com.redcore.gameengine.data.model.VoiceEffect
import com.redcore.gameengine.sys.PermissionHelper
import com.redcore.gameengine.ui.components.GhostButton
import com.redcore.gameengine.ui.components.GlassCard
import com.redcore.gameengine.ui.components.InfoNote
import com.redcore.gameengine.ui.components.MetricBar
import com.redcore.gameengine.ui.components.PillTag
import com.redcore.gameengine.ui.components.PrimaryButton
import com.redcore.gameengine.ui.components.ScreenColumn
import com.redcore.gameengine.ui.components.ScreenTopBar
import com.redcore.gameengine.ui.components.SectionHeader
import com.redcore.gameengine.ui.theme.TextMuted
import com.redcore.gameengine.ui.theme.TextPrimary
import com.redcore.gameengine.ui.theme.TextSecondary
import com.redcore.gameengine.ui.vm.AudioViewModel
import com.redcore.gameengine.util.AudioEngine

@Composable
fun AudioStudioScreen(nav: NavHostController, requestAudioPermission: () -> Unit) {
    val vm: AudioViewModel = viewModel()
    val context = LocalContext.current
    val streams by vm.streams.collectAsState()
    val preset by vm.preset.collectAsState()
    val eqStatus by vm.equalizerStatus.collectAsState()
    val outputs by vm.outputs.collectAsState()
    val sessions by vm.sessions.collectAsState()
    val micLevel by vm.micLevel.collectAsState()
    val micState by vm.micState.collectAsState()
    val voiceStatus by vm.voiceStatus.collectAsState()
    val voiceBusy by vm.voiceBusy.collectAsState()
    val accent = MaterialTheme.colorScheme.primary
    var effect by remember { mutableStateOf(VoiceEffect.NONE) }
    val micAllowed = PermissionHelper.recordAudio(context)

    ScreenColumn {
        ScreenTopBar(
            title = "Audio studio",
            subtitle = "Volumes, presets, microphone and voice effects"
        )

        SectionHeader(title = "Volume")
        GlassCard(accent = accent) {
            streams.forEach { row ->
                var value by remember(row.streamType, row.percent) { mutableStateOf(row.percent.toFloat()) }
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(row.label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
                        Text("${value.toInt()}%", style = MaterialTheme.typography.labelMedium, color = accent)
                    }
                    Slider(
                        value = value,
                        onValueChange = { value = it },
                        onValueChangeFinished = { vm.setVolume(row.streamType, value.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = Color(0x22FFFFFF)
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Gaming audio presets")
        GlassCard {
            AudioPreset.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { item ->
                        val selected = preset == item
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) accent.copy(alpha = 0.14f) else Color(0x08FFFFFF))
                                .border(
                                    1.dp,
                                    if (selected) accent.copy(alpha = 0.5f) else Color(0x14FFFFFF),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { vm.selectPreset(item) }
                                .padding(11.dp)
                        ) {
                            Text(
                                item.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selected) accent else TextPrimary
                            )
                            Text(item.description, style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 2)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(eqStatus, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            InfoNote(
                text = "Presets drive an Equalizer on the global output session. Some OEM builds block third-party equalisers — the message above always tells you which case you are in.",
                accent = accent
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Output devices")
        GlassCard {
            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                outputs.forEach { device ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Box(
                            modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(accent)
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(device, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Active audio sessions")
        GlassCard {
            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                sessions.forEach { session ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Icon(Icons.Rounded.Album, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(session, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            GhostButton(text = "Refresh session list", onClick = { vm.refresh() })
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Microphone test")
        GlassCard(accent = accent) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Mic, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Live input level", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(
                        if (micAllowed) AudioEngine.noiseFloorLabel(micLevel) else "Microphone permission not granted",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                PillTag(micState, accent = accent)
            }
            Spacer(Modifier.height(12.dp))
            MetricBar(progress = micLevel, accent = if (micLevel > 0.85f) Color(0xFFFF4D5E) else accent, height = 10)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!micAllowed) {
                    PrimaryButton(text = "Grant microphone", onClick = requestAudioPermission, modifier = Modifier.weight(1f))
                } else {
                    GhostButton(text = "Start", icon = Icons.Rounded.PlayArrow, onClick = { vm.startMicTest() })
                    GhostButton(text = "Stop", icon = Icons.Rounded.Stop, onClick = { vm.stopMicTest() })
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Voice studio")
        GlassCard {
            Text(
                "Record from this device's microphone, then shape it with real DSP effects: pitch shift, ring modulation, echo, band-limiting.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(12.dp))
            VoiceEffect.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { item ->
                        val selected = effect == item
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(13.dp))
                                .background(if (selected) accent.copy(alpha = 0.14f) else Color(0x08FFFFFF))
                                .border(
                                    1.dp,
                                    if (selected) accent.copy(alpha = 0.5f) else Color(0x14FFFFFF),
                                    RoundedCornerShape(13.dp)
                                )
                                .clickable { effect = item }
                                .padding(10.dp)
                        ) {
                            Text(item.label, style = MaterialTheme.typography.titleSmall, color = if (selected) accent else TextPrimary)
                            Text(item.description, style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    text = if (voiceBusy) "Working…" else "Record 6s",
                    icon = Icons.Rounded.Mic,
                    enabled = !voiceBusy && micAllowed,
                    onClick = { vm.recordVoice() },
                    modifier = Modifier.weight(1f)
                )
                GhostButton(text = "Preview", icon = Icons.Rounded.PlayArrow, enabled = !voiceBusy, onClick = { vm.preview(effect) })
                GhostButton(text = "Save", icon = Icons.Rounded.Save, enabled = !voiceBusy, onClick = { vm.save(effect) })
            }
            Spacer(Modifier.height(10.dp))
            Text(voiceStatus, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(10.dp))
            InfoNote(
                text = "Android does not allow an app to insert a voice changer into calls or other apps' microphones. This studio processes REDCORE's own recordings — that is the honest limit of the platform.",
                accent = accent
            )
        }

        Spacer(Modifier.height(16.dp))
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Game-specific audio presets live in each game's profile — open a game from the Library to pin a preset to it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}
