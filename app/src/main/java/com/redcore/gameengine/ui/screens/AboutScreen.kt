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
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.redcore.gameengine.BuildConfig
import com.redcore.gameengine.RedCoreApp
import com.redcore.gameengine.navigation.Routes
import com.redcore.gameengine.sys.DeviceSnapshot
import com.redcore.gameengine.ui.components.AnimatedCoreLogo
import com.redcore.gameengine.ui.components.GlassCard
import com.redcore.gameengine.ui.components.InfoNote
import com.redcore.gameengine.ui.components.KeyValueRow
import com.redcore.gameengine.ui.components.PillTag
import com.redcore.gameengine.ui.components.ScreenColumn
import com.redcore.gameengine.ui.components.ScreenTopBar
import com.redcore.gameengine.ui.components.SectionHeader
import com.redcore.gameengine.ui.theme.TextMuted
import com.redcore.gameengine.ui.theme.TextPrimary
import com.redcore.gameengine.ui.theme.TextSecondary

private const val FEEDBACK_URL = "https://github.com/zyvo9/ZYVO-AI/issues"

@Composable
fun AboutScreen(nav: NavHostController) {
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    val snapshot: DeviceSnapshot = remember { RedCoreApp.locator().probe.snapshot() }

    ScreenColumn {
        ScreenTopBar(
            title = "About",
            subtitle = "REDCORE GAME ENGINE",
            onBack = { nav.popBackStack() }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x14FFFFFF))
                .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(24.dp))
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedCoreLogo(size = 92.dp, accent = accent)
            Spacer(Modifier.height(16.dp))
            Text("REDCORE", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text(
                "GAME ENGINE",
                style = MaterialTheme.typography.labelMedium,
                color = accent
            )
            Spacer(Modifier.height(10.dp))
            PillTag("v${BuildConfig.VERSION_NAME} • ${BuildConfig.BUILD_TYPE}", accent = accent, filled = true)
            Spacer(Modifier.height(6.dp))
            Text(
                "Built for gamers who want the truth about their device.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "What this build does")
        GlassCard {
            Text(
                "REDCORE is a companion, not a cheat. It measures what Android lets an ordinary app measure, applies the levers Android actually grants, and puts your games, profiles and sessions in one clean place.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "No root. No fake counters. No hidden uploads.",
                style = MaterialTheme.typography.bodyMedium,
                color = accent
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "This device")
        GlassCard {
            KeyValueRow("Model", "${snapshot.manufacturer} ${snapshot.model}".trim().ifBlank { "Unknown" })
            KeyValueRow("Android", "${snapshot.androidRelease} (API ${snapshot.sdkInt})")
            KeyValueRow("Chipset", snapshot.soc.ifBlank { "Unavailable" })
            KeyValueRow("CPU cores", if (snapshot.cores > 0) "${snapshot.cores}" else "Unavailable")
            KeyValueRow("Display", if (snapshot.refreshHz > 0f) "${snapshot.refreshHz.toInt()} Hz" else "Unavailable")
            KeyValueRow("RAM", if (snapshot.memTotalMb > 0) "${snapshot.memTotalMb / 1024} GB" else "Unavailable")
            Spacer(Modifier.height(8.dp))
            Text(
                "Read live from public Android APIs, so it always matches your real hardware.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Project")
        GlassCard {
            LinkRow("Privacy policy", "How data is handled — short version: it is not", Icons.Rounded.PrivacyTip, accent) {
                nav.navigate(Routes.PRIVACY)
            }
            Spacer(Modifier.height(8.dp))
            LinkRow("Guided tour", "Replay the feature walkthrough", Icons.Rounded.School, accent) {
                nav.navigate(Routes.TUTORIAL)
            }
            Spacer(Modifier.height(8.dp))
            LinkRow("Report an issue", "Feedback, bugs and feature requests", Icons.Rounded.BugReport, accent) {
                openUrl(context, FEEDBACK_URL)
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Credits")
        GlassCard {
            Text(
                "Jetpack Compose and Material 3 by Google. Orbitron and Rajdhani typefaces under the SIL Open Font License. Built by gamers who got tired of fake boost apps.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text("Free, ad-free, offline-first.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }

        Spacer(Modifier.height(16.dp))
        InfoNote(
            text = "REDCORE cannot overclock, raise a thermal limit or force a per-game frame rate without root or a manufacturer API. Where that is the case, the app offers the closest legal lever and labels the rest clearly.",
            accent = Color(0xFF6C7A89)
        )

        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.Code, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text("Made with Kotlin + Compose", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LinkRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = TextMuted)
    }
}

internal fun openUrl(context: android.content.Context, url: String) {
    try {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Throwable) {
    }
}
