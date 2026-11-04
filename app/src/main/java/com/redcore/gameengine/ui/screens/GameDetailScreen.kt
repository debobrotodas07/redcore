package com.redcore.gameengine.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.redcore.gameengine.data.model.AudioPreset
import com.redcore.gameengine.data.model.CrosshairDesign
import com.redcore.gameengine.data.model.PerformanceProfile
import com.redcore.gameengine.data.model.SidebarTool
import com.redcore.gameengine.sys.ProfileApplier
import com.redcore.gameengine.ui.components.AppIcon
import com.redcore.gameengine.ui.components.CrosshairView
import com.redcore.gameengine.ui.components.GhostButton
import com.redcore.gameengine.ui.components.GlassCard
import com.redcore.gameengine.ui.components.InfoNote
import com.redcore.gameengine.ui.components.KeyValueRow
import com.redcore.gameengine.ui.components.LoadingState
import com.redcore.gameengine.ui.components.PillTag
import com.redcore.gameengine.ui.components.PrimaryButton
import com.redcore.gameengine.ui.components.ScreenColumn
import com.redcore.gameengine.ui.components.ScreenTopBar
import com.redcore.gameengine.ui.components.SectionHeader
import com.redcore.gameengine.ui.components.SelectRow
import com.redcore.gameengine.ui.theme.TextMuted
import com.redcore.gameengine.ui.theme.TextPrimary
import com.redcore.gameengine.ui.theme.TextSecondary
import com.redcore.gameengine.ui.vm.GameDetailViewModel
import com.redcore.gameengine.util.Format

private val crosshairColors = listOf(
    0xFFFF2638, 0xFF35D07F, 0xFF4FA8FF, 0xFFFFD400, 0xFFFFFFFF, 0xFFFF6B4A, 0xFF9B5CFF, 0xFF19D3DA
)

@Composable
fun GameDetailScreen(nav: NavHostController, packageName: String) {
    val vm: GameDetailViewModel = viewModel(
        key = packageName,
        factory = viewModelFactory { initializer { GameDetailViewModel(packageName) } }
    )
    val game by vm.game.collectAsState()
    val profile by vm.profile.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    var confirmRemove by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var labelDraft by remember { mutableStateOf("") }

    val iconPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Throwable) {
            }
            vm.updateLabelAndIcon(game?.label ?: packageName, uri.toString())
        }
    }

    if (!loaded) {
        ScreenColumn {
            ScreenTopBar(title = "Game", onBack = { nav.popBackStack() })
            LoadingState("Loading game profile…")
        }
        return
    }

    ScreenColumn {
        ScreenTopBar(
            title = game?.label ?: packageName,
            subtitle = packageName,
            onBack = { nav.popBackStack() }
        ) {
            IconButton(onClick = { vm.toggleFavorite() }) {
                Icon(
                    if (game?.favorite == true) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Favourite",
                    tint = if (game?.favorite == true) Color(0xFFFFC24B) else TextMuted
                )
            }
            IconButton(onClick = { confirmRemove = true }) {
                Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = TextMuted)
            }
        }

        GlassCard(accent = accent, padding = 16) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(
                    packageName = packageName,
                    customUri = game?.customIconUri,
                    size = 62,
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        game?.label ?: packageName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PillTag(PerformanceProfile.fromId(profile.profileId).label, accent = accent, filled = true)
                        PillTag(AudioPreset.fromId(profile.audioPresetId).label, accent = accent)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            KeyValueRow("Last played", Format.ago(game?.lastPlayedAt))
            KeyValueRow("Total time", Format.duration(game?.totalPlaySeconds ?: 0))
            KeyValueRow("Sessions", "${game?.launchCount ?: 0}")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton(text = "Rename", icon = Icons.Rounded.Edit, onClick = {
                    labelDraft = game?.label ?: packageName
                    showRename = true
                })
                GhostButton(text = "Icon", icon = Icons.Rounded.Image, onClick = {
                    iconPicker.launch(arrayOf("image/*"))
                })
                GhostButton(text = "App info", icon = Icons.Rounded.Launch, onClick = {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:$packageName"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(intent)
                    } catch (_: Throwable) {
                    }
                })
            }
        }

        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            text = "Launch game",
            icon = Icons.Rounded.PlayArrow,
            onClick = { vm.launch(context) }
        )

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "Performance profile")
        GlassCard {
            PerformanceProfile.entries.forEach { p ->
                SelectRow(
                    title = p.label,
                    subtitle = p.tagline,
                    selected = PerformanceProfile.fromId(profile.profileId) == p,
                    accent = p.accent,
                    onClick = {
                        vm.updateProfile(p)
                        ProfileApplier.applyProfile(context, p)
                    }
                )
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "Crosshair")
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0B0B0F))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CrosshairView(
                        design = CrosshairDesign.fromId(profile.crosshairId),
                        color = Color(profile.crosshairColor),
                        sizeDp = 46,
                        opacity = profile.crosshairOpacity,
                        modifier = Modifier.size(52.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Aim point", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(
                        "Saved with this game. Apply it from Crosshair Studio when you want it on screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CrosshairDesign.entries.toList()) { design ->
                    val selected = CrosshairDesign.fromId(profile.crosshairId) == design
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) accent.copy(alpha = 0.16f) else Color(0x0AFFFFFF))
                            .border(
                                1.dp,
                                if (selected) accent.copy(alpha = 0.6f) else Color(0x14FFFFFF),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { vm.updateCrosshair(design) },
                        contentAlignment = Alignment.Center
                    ) {
                        CrosshairView(
                            design = design,
                            color = Color(profile.crosshairColor),
                            sizeDp = 26,
                            opacity = 95,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(crosshairColors) { value ->
                    val selected = profile.crosshairColor == value
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(value))
                            .border(
                                if (selected) 2.dp else 1.dp,
                                if (selected) Color.White else Color(0x33FFFFFF),
                                CircleShape
                            )
                            .clickable { vm.updateCrosshairColor(value) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            LabeledSlider(
                label = "Size",
                value = profile.crosshairSize.toFloat(),
                range = 10f..90f,
                suffix = "dp",
                accent = accent,
                onChange = { vm.updateCrosshairSize(it.toInt()) }
            )
            LabeledSlider(
                label = "Opacity",
                value = profile.crosshairOpacity.toFloat(),
                range = 15f..100f,
                suffix = "%",
                accent = accent,
                onChange = { vm.updateCrosshairOpacity(it.toInt()) }
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "Audio preset")
        GlassCard {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AudioPreset.entries.toList()) { preset ->
                    val selected = AudioPreset.fromId(profile.audioPresetId) == preset
                    Column(
                        modifier = Modifier
                            .width(120.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) accent.copy(alpha = 0.14f) else Color(0x0AFFFFFF))
                            .border(
                                1.dp,
                                if (selected) accent.copy(alpha = 0.5f) else Color(0x14FFFFFF),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { vm.updateAudio(preset) }
                            .padding(11.dp)
                    ) {
                        Text(preset.label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(preset.description, style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 2)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            InfoNote(
                text = "Presets tune REDCORE's own equaliser for this game's session. Apply them from Audio Studio.",
                accent = accent
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "Display")
        GlassCard {
            LabeledSlider(
                label = "Brightness",
                value = profile.brightnessPct.toFloat(),
                range = 10f..100f,
                suffix = "%",
                accent = accent,
                onChange = { vm.updateBrightness(it.toInt()) }
            )
            Spacer(Modifier.height(6.dp))
            val hzOptions = ProfileApplier.availableHz(context)
            Text("Refresh target", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    HzChip(
                        label = "Auto",
                        selected = profile.refreshHz == 0,
                        accent = accent
                    ) { vm.updateRefresh(0) }
                }
                items(hzOptions) { hz ->
                    HzChip(
                        label = "$hz Hz",
                        selected = profile.refreshHz == hz,
                        accent = accent
                    ) { vm.updateRefresh(hz) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Refresh mode applies to REDCORE's own window. Games keep the mode the system gives them.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "Sidebar shortcuts for this game")
        GlassCard {
            val current = SidebarTool.parseCsv(profile.sidebarToolsCsv)
            SidebarTool.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { tool ->
                        val selected = current.contains(tool)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) accent.copy(alpha = 0.14f) else Color(0x0AFFFFFF))
                                .border(
                                    1.dp,
                                    if (selected) accent.copy(alpha = 0.5f) else Color(0x14FFFFFF),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    val next = if (selected) current - tool else current + tool
                                    vm.updateTools(next.ifEmpty { listOf(SidebarTool.FPS) })
                                }
                                .padding(10.dp)
                        ) {
                            Text(
                                tool.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) TextPrimary else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "Notes")
        GlassCard {
            TextField(
                value = profile.notes,
                onValueChange = {
                    vm.updateNotes(it)
                },
                placeholder = {
                    Text("Sensitivity, loadout, settings you want to remember", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = accent.copy(alpha = 0.5f),
                    unfocusedIndicatorColor = Color(0x14FFFFFF),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = accent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            GhostButton(text = "Save notes", onClick = { vm.updateNotes(profile.notes) })
        }

        Spacer(Modifier.height(20.dp))
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            containerColor = Color(0xFF121218),
            title = { Text("Remove from library?", color = TextPrimary) },
            text = {
                Text(
                    "The game stays installed on your device. Only the REDCORE entry, its profile and its notes are deleted.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Text(
                    "REMOVE",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFF4D5E),
                    modifier = Modifier
                        .clickable {
                            confirmRemove = false
                            vm.remove { nav.popBackStack() }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            },
            dismissButton = {
                Text(
                    "CANCEL",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    modifier = Modifier
                        .clickable { confirmRemove = false }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        )
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            containerColor = Color(0xFF121218),
            title = { Text("Rename in library", color = TextPrimary) },
            text = {
                TextField(
                    value = labelDraft,
                    onValueChange = { labelDraft = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0x0AFFFFFF),
                        unfocusedContainerColor = Color(0x08FFFFFF),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = accent
                    )
                )
            },
            confirmButton = {
                Text(
                    "SAVE",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    modifier = Modifier
                        .clickable {
                            vm.updateLabelAndIcon(labelDraft.ifBlank { packageName }, game?.customIconUri)
                            showRename = false
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            },
            dismissButton = {
                Text(
                    "CANCEL",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    modifier = Modifier
                        .clickable { showRename = false }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        )
    }
}

@Composable
private fun HzChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.copy(alpha = 0.16f) else Color(0x0AFFFFFF))
            .border(1.dp, if (selected) accent.copy(alpha = 0.55f) else Color(0x14FFFFFF), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) accent else TextSecondary)
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    accent: Color,
    onChange: (Float) -> Unit
) {
    var local by remember(value) { mutableStateOf(value) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(1f))
            Text("${local.toInt()}$suffix", style = MaterialTheme.typography.labelMedium, color = accent)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color(0x22FFFFFF)
            )
        )
    }
}

