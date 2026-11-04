package com.redcore.gameengine.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.redcore.gameengine.navigation.Routes
import com.redcore.gameengine.ui.components.GlassCard
import com.redcore.gameengine.ui.components.InfoNote
import com.redcore.gameengine.ui.components.PrimaryButton
import com.redcore.gameengine.ui.components.ScreenColumn
import com.redcore.gameengine.ui.components.ScreenTopBar
import com.redcore.gameengine.ui.theme.TextMuted
import com.redcore.gameengine.ui.theme.TextPrimary
import com.redcore.gameengine.ui.theme.TextSecondary
import com.redcore.gameengine.ui.vm.SettingsViewModel

private data class Step(
    val title: String,
    val body: String,
    val bullets: List<String>,
    val icon: ImageVector,
    val action: String? = null,
    val route: String? = null
)

@Composable
fun TutorialScreen(nav: NavHostController) {
    val vm: SettingsViewModel = viewModel()
    val accent = MaterialTheme.colorScheme.primary
    var index by remember { mutableIntStateOf(0) }

    val steps = listOf(
        Step(
            title = "Welcome to REDCORE",
            body = "A game companion built on one rule: never show a number the phone did not actually produce. Here is the whole app in seven short cards.",
            bullets = listOf(
                "Everything below runs offline",
                "No account, no ads, no trackers",
                "You can replay this tour from More anytime"
            ),
            icon = Icons.Rounded.WavingHand
        ),
        Step(
            title = "Grant two things",
            body = "Overlay permission lets the floating sidebar and crosshair appear above games. Notifications keep those services from being killed silently.",
            bullets = listOf(
                "\"Display over other apps\" is the only required one",
                "Microphone and screen capture are asked only when you use those tools",
                "Screen capture consent is re-confirmed by Android every session"
            ),
            icon = Icons.Rounded.Shield,
            action = "Open permission centre",
            route = Routes.PERMISSIONS
        ),
        Step(
            title = "Build your library",
            body = "Tap the plus in Library to add an installed game. REDCORE stores its name, icon and your per-game notes, then tracks playtime when you start a session.",
            bullets = listOf(
                "Search filters by name instantly",
                "Long sessions feed the dashboard stats",
                "Swipe-free editing from the game detail page"
            ),
            icon = Icons.Rounded.SportsEsports,
            action = "Open library",
            route = Routes.LIBRARY
        ),
        Step(
            title = "Profiles that tell the truth",
            body = "ECO, Balanced, Beast and Extreme change only what Android allows: brightness, refresh-rate mode, DND and haptics. Anything needing root is listed as a suggestion, never faked.",
            bullets = listOf(
                "Each profile shows exactly what it will apply",
                "Suggestions are shortcuts to system screens",
                "Per-game profiles override the global one"
            ),
            icon = Icons.Rounded.Bolt,
            action = "Open boost",
            route = Routes.PERFORMANCE
        ),
        Step(
            title = "Read your device live",
            body = "Monitor samples battery, thermals, RAM, CPU load and network every second. If a sensor is blocked by the system it says unavailable instead of guessing.",
            bullets = listOf(
                "Adjust the sample rate in Settings",
                "Frame rate shows your display and app-window timing",
                "Per-game FPS needs root, so it is labelled that way"
            ),
            icon = Icons.Rounded.MonitorHeart,
            action = "Open monitor",
            route = Routes.MONITOR
        ),
        Step(
            title = "Sidebar and crosshair",
            body = "The floating handle stays over your game: FPS, network, brightness, volume, timer, profile switch, and capture buttons — all one tap away.",
            bullets = listOf(
                "Choose left or right edge, and drag to your thumb",
                "Design seven crosshair shapes in the studio",
                "Both overlays need overlay permission only"
            ),
            icon = Icons.Rounded.CenterFocusStrong,
            action = "Open crosshair studio",
            route = Routes.CROSSHAIR
        ),
        Step(
            title = "Capture and voice",
            body = "Screenshot in one tap or record at 720p/1080p 60 fps straight to your Movies folder. The audio studio colours your mic and can save clips locally.",
            bullets = listOf(
                "Recording uses Android's own MediaProjection",
                "Nothing is uploaded — files stay in your storage",
                "Voice effects apply to REDCORE's own playback chain"
            ),
            icon = Icons.Rounded.PhotoCamera,
            action = "Open capture",
            route = Routes.CAPTURE
        )
    )

    val step = steps[index.coerceIn(0, steps.lastIndex)]
    val last = index == steps.lastIndex

    ScreenColumn {
        ScreenTopBar(
            title = "Guided tour",
            subtitle = "Card ${index + 1} of ${steps.size}",
            onBack = {
                if (index > 0) index-- else nav.popBackStack()
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            steps.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (i <= index) accent else Color(0x1FFFFFFF))
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        AnimatedContent(
            targetState = index,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tour"
        ) { i ->
            val current = steps[i]
            Column {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(accent.copy(alpha = 0.13f))
                        .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(19.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(current.icon, contentDescription = null, tint = accent, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(current.title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text(current.body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                Spacer(Modifier.height(18.dp))
                GlassCard {
                    current.bullets.forEachIndexed { bulletIndex, bullet ->
                        if (bulletIndex > 0) Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                            )
                            Text(bullet, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        if (step.action != null && step.route != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x0AFFFFFF))
                    .border(1.dp, Color(0x16FFFFFF), RoundedCornerShape(14.dp))
                    .clickable { nav.navigate(step.route) }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(step.action, style = MaterialTheme.typography.labelMedium, color = accent, modifier = Modifier.weight(1f))
                Text("→", style = MaterialTheme.typography.labelMedium, color = accent)
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x0DFFFFFF))
                        .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                        .clickable { index-- }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Back", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    }
                }
            }
            PrimaryButton(
                text = if (last) "Start using REDCORE" else "Next",
                accent = accent,
                onClick = {
                    if (last) {
                        vm.setTutorialDone(true)
                        nav.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.DASHBOARD) { inclusive = false }
                            launchSingleTop = true
                        }
                    } else {
                        index++
                    }
                },
                modifier = Modifier.weight(1.2f)
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "Skip anytime — nothing here is required.",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier
                .clickable {
                    vm.setTutorialDone(true)
                    nav.popBackStack()
                }
                .padding(4.dp)
        )

        Spacer(Modifier.height(16.dp))
        InfoNote(
            text = "The tour only explains the app. It never turns a feature on for you — you stay in control of every switch.",
            accent = Color(0xFF6C7A89)
        )
        Spacer(Modifier.height(24.dp))
    }
}
