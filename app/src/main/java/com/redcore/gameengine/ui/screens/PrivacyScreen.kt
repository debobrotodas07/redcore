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
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.redcore.gameengine.ui.components.GlassCard
import com.redcore.gameengine.ui.components.InfoNote
import com.redcore.gameengine.ui.components.ScreenColumn
import com.redcore.gameengine.ui.components.ScreenTopBar
import com.redcore.gameengine.ui.components.SectionHeader
import com.redcore.gameengine.ui.theme.TextMuted
import com.redcore.gameengine.ui.theme.TextPrimary
import com.redcore.gameengine.ui.theme.TextSecondary

@Composable
fun PrivacyScreen(nav: NavHostController) {
    val accent = MaterialTheme.colorScheme.primary

    ScreenColumn {
        ScreenTopBar(
            title = "Privacy",
            subtitle = "Plain language, no legal fog",
            onBack = { nav.popBackStack() }
        )

        GlassCard(accent = accent) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("No servers. No accounts. No analytics.", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "REDCORE has no backend. There is no login, no dashboard collecting your data and no crash reporter phoning home. The only network request the app can ever make is the optional \"check for updates\" button, which asks GitHub for a version number.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "What stays on your device")
        GlassCard {
            PrivacyRow(
                Icons.Rounded.Storage,
                "Your library and sessions",
                "Game names, playtime, profiles and session history live in a local Room database. Nothing syncs anywhere."
            )
            Spacer(Modifier.height(12.dp))
            PrivacyRow(
                Icons.Rounded.Storage,
                "Your settings",
                "Accent, sidebar position, crosshair design and tool choices sit in Android DataStore — private app storage."
            )
            Spacer(Modifier.height(12.dp))
            PrivacyRow(
                Icons.Rounded.Storage,
                "Your captures",
                "Screenshots and recordings are written to your own Pictures and Movies folders. Delete them like any other file."
            )
            Spacer(Modifier.height(12.dp))
            PrivacyRow(
                Icons.Rounded.Storage,
                "Your voice recordings",
                "Audio studio clips are saved locally only when you press save. None of it is uploaded."
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "What REDCORE reads")
        GlassCard {
            Text(
                "Device telemetry comes from standard Android APIs: battery level and temperature, memory, network counters, thermal status, display refresh rate, and CPU load from /proc/stat. These values are displayed, never transmitted.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "The game list comes from the packages Android already shows you in the launcher. REDCORE does not read your messages, files, photos or contacts.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "What REDCORE never does")
        GlassCard {
            Bullet("No advertising SDK, no tracker, no fingerprinting.")
            Bullet("No background upload of your library or usage.")
            Bullet("No accessibility service with screen-reading power.")
            Bullet("No root request, no hidden SU call.")
            Bullet("No fake numbers — if a sensor is blocked, the UI says unavailable.")
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "Permissions, one by one")
        GlassCard {
            Text(
                "Overlay: draws the sidebar and crosshair. Notifications: keeps those services alive. Modify system settings: brightness shortcut. DND access: the quiet toggle. Microphone: audio studio only. Screen capture: consent asked by Android every session. Each one is optional except overlay, and each maps to a visible feature.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "Backups")
        GlassCard {
            Text(
                "Export writes a JSON file to the location you choose through Android's file picker. You decide where it goes. Restore reads only the file you pick.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(18.dp))
        InfoNote(
            text = "If a future build ever adds a network feature, it will be opt-in, documented here first, and off by default.",
            accent = accent
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PrivacyRow(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0x0DFFFFFF))
                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.height(3.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(5.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}
