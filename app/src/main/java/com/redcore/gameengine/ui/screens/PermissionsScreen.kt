package com.redcore.gameengine.ui.screens

import android.Manifest
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.redcore.gameengine.sys.PermissionHelper
import com.redcore.gameengine.sys.PermissionItem
import com.redcore.gameengine.sys.PermissionState
import com.redcore.gameengine.ui.components.GlassCard
import com.redcore.gameengine.ui.components.InfoNote
import com.redcore.gameengine.ui.components.PillTag
import com.redcore.gameengine.ui.components.PrimaryButton
import com.redcore.gameengine.ui.components.ScreenColumn
import com.redcore.gameengine.ui.components.ScreenTopBar
import com.redcore.gameengine.ui.components.SectionHeader
import com.redcore.gameengine.ui.theme.TextMuted
import com.redcore.gameengine.ui.theme.TextPrimary
import com.redcore.gameengine.ui.theme.TextSecondary

@Composable
fun PermissionsScreen(nav: NavHostController) {
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    var refresh by remember { mutableIntStateOf(0) }

    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }

    val items = remember(refresh) { PermissionHelper.list(context) }
    val granted = items.count { it.state == PermissionState.GRANTED }

    ScreenColumn {
        ScreenTopBar(
            title = "Permissions",
            subtitle = "$granted of ${items.size} granted",
            onBack = { nav.popBackStack() }
        )

        InfoNote(
            text = "Every permission here exists for one visible feature. REDCORE asks only when that feature is used, and nothing is ever sent off the device.",
            accent = accent
        )

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Required")
        items.filter { it.required }.forEach { item ->
            PermissionCard(item, accent) {
                when (item.key) {
                    "overlay" -> PermissionHelper.openOverlaySettings(context)
                    "notifications" -> PermissionHelper.openAppNotificationSettings(context)
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(6.dp))
        SectionHeader(title = "Optional — used only by their feature")
        items.filterNot { it.required }.forEach { item ->
            PermissionCard(item, accent) {
                when (item.key) {
                    "write_settings" -> PermissionHelper.openWriteSettings(context)
                    "policy" -> PermissionHelper.openPolicySettings(context)
                    "notifications" -> PermissionHelper.openAppNotificationSettings(context)
                    "audio" -> audioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    "screen_capture" -> nav.navigate(com.redcore.gameengine.navigation.Routes.CAPTURE)
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(10.dp))
        PrimaryButton(
            text = "Refresh status",
            onClick = { refresh++ },
            accent = accent,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Deliberately not requested")
        GlassCard {
            NotRequestedRow("Accessibility service", "Could read every screen you open. REDCORE uses none.")
            Spacer(Modifier.height(10.dp))
            NotRequestedRow("Root / SU", "This build never asks for root. Root-only levers are labelled unavailable instead.")
            Spacer(Modifier.height(10.dp))
            NotRequestedRow("Usage access", "Playtime is measured while REDCORE itself is in the foreground session, not by spying on the system.")
            Spacer(Modifier.height(10.dp))
            NotRequestedRow("Location, contacts, SMS", "No feature needs them.")
        }

        Spacer(Modifier.height(16.dp))
        InfoNote(
            text = "Android does not let any app grant itself these permissions. Each button opens the exact system screen where you can flip the switch.",
            accent = Color(0xFF6C7A89)
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionCard(
    item: PermissionItem,
    accent: Color,
    onAction: () -> Unit
) {
    val isGranted = item.state == PermissionState.GRANTED
    val restricted = item.state == PermissionState.RESTRICTED
    val statusColor = when {
        isGranted -> Color(0xFF42D98A)
        restricted -> Color(0xFFE0A73A)
        else -> Color(0xFFFF5A5A)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x0CFFFFFF))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when {
                    isGranted -> Icons.Rounded.CheckCircle
                    restricted -> Icons.Rounded.LockClock
                    else -> Icons.Rounded.ErrorOutline
                },
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            PillTag(
                text = when (item.state) {
                    PermissionState.GRANTED -> "Granted"
                    PermissionState.RESTRICTED -> "Per session"
                    PermissionState.DENIED -> "Not granted"
                },
                accent = statusColor,
                filled = isGranted
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(item.why, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        if (!isGranted) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.32f), RoundedCornerShape(11.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (restricted) "Open capture screen" else "Open settings",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    modifier = Modifier.clickable(onClick = onAction)
                )
                Spacer(Modifier.width(6.dp))
                Text("→", style = MaterialTheme.typography.labelMedium, color = accent)
            }
        }
    }
}

@Composable
private fun NotRequestedRow(title: String, why: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(5.dp)
                .clip(RoundedCornerShape(50))
                .background(TextMuted)
        )
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            Text(why, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

