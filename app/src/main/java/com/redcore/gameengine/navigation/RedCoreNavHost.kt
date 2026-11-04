package com.redcore.gameengine.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.redcore.gameengine.ui.components.RedCoreBackground
import com.redcore.gameengine.ui.screens.AboutScreen
import com.redcore.gameengine.ui.screens.AudioStudioScreen
import com.redcore.gameengine.ui.screens.CaptureScreen
import com.redcore.gameengine.ui.screens.CrosshairStudioScreen
import com.redcore.gameengine.ui.screens.DashboardScreen
import com.redcore.gameengine.ui.screens.GameDetailScreen
import com.redcore.gameengine.ui.screens.LibraryScreen
import com.redcore.gameengine.ui.screens.MonitorScreen
import com.redcore.gameengine.ui.screens.MoreScreen
import com.redcore.gameengine.ui.screens.PerformanceScreen
import com.redcore.gameengine.ui.screens.PermissionsScreen
import com.redcore.gameengine.ui.screens.PrivacyScreen
import com.redcore.gameengine.ui.screens.SettingsScreen
import com.redcore.gameengine.ui.screens.TutorialScreen
import com.redcore.gameengine.ui.theme.BgDeep
import com.redcore.gameengine.ui.theme.TextMuted

object Routes {
    const val DASHBOARD = "dashboard"
    const val LIBRARY = "library"
    const val PERFORMANCE = "performance"
    const val MONITOR = "monitor"
    const val MORE = "more"
    const val SETTINGS = "settings"
    const val PERMISSIONS = "permissions"
    const val ABOUT = "about"
    const val PRIVACY = "privacy"
    const val TUTORIAL = "tutorial"
    const val AUDIO = "audio"
    const val CROSSHAIR = "crosshair"
    const val CAPTURE = "capture"
    const val GAME = "game/{pkg}"
    fun game(pkg: String) = "game/${Uri.encode(pkg)}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.DASHBOARD, "Home", Icons.Rounded.Home),
    Tab(Routes.LIBRARY, "Library", Icons.Rounded.SportsEsports),
    Tab(Routes.PERFORMANCE, "Boost", Icons.Rounded.Speed),
    Tab(Routes.MONITOR, "Monitor", Icons.Rounded.MonitorHeart),
    Tab(Routes.MORE, "More", Icons.Rounded.MoreHoriz)
)

@Composable
fun RedCoreNavHost(
    startRoute: String? = null,
    requestAudioPermission: () -> Unit = {}
) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val showBar = tabs.any { it.route == current }
    val start = when {
        startRoute == "capture" -> Routes.CAPTURE
        startRoute == "settings" -> Routes.SETTINGS
        else -> Routes.DASHBOARD
    }

    RedCoreBackground(accent = MaterialTheme.colorScheme.primary) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            NavHost(
                navController = navController,
                startDestination = start,
                enterTransition = { fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 12 } },
                exitTransition = { fadeOut(tween(120)) },
                popEnterTransition = { fadeIn(tween(180)) },
                popExitTransition = { fadeOut(tween(140)) + slideOutHorizontally(tween(180)) { it / 12 } },
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Routes.DASHBOARD) {
                    DashboardScreen(navController)
                }
                composable(Routes.LIBRARY) {
                    LibraryScreen(navController)
                }
                composable(Routes.PERFORMANCE) {
                    PerformanceScreen(navController)
                }
                composable(Routes.MONITOR) {
                    MonitorScreen(navController)
                }
                composable(Routes.MORE) {
                    MoreScreen(navController)
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(navController)
                }
                composable(Routes.PERMISSIONS) {
                    PermissionsScreen(navController)
                }
                composable(Routes.ABOUT) {
                    AboutScreen(navController)
                }
                composable(Routes.PRIVACY) {
                    PrivacyScreen(navController)
                }
                composable(Routes.TUTORIAL) {
                    TutorialScreen(navController)
                }
                composable(Routes.AUDIO) {
                    AudioStudioScreen(navController, requestAudioPermission)
                }
                composable(Routes.CROSSHAIR) {
                    CrosshairStudioScreen(navController)
                }
                composable(Routes.CAPTURE) {
                    CaptureScreen(navController)
                }
                composable(
                    route = Routes.GAME,
                    arguments = listOf(navArgument("pkg") { type = NavType.StringType })
                ) { backStackEntry ->
                    val pkg = backStackEntry.arguments?.getString("pkg").orEmpty()
                    GameDetailScreen(navController, pkg)
                }
            }

            if (showBar) {
                BottomBar(
                    current = current,
                    onSelect = { route ->
                        if (route != current) {
                            navController.navigate(route) {
                                popUpTo(Routes.DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomBar(
    current: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(BgDeep.copy(alpha = 0.86f))
            .then(
                Modifier.padding(1.dp)
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val active = tab.route == current
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelect(tab.route) }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    tab.icon,
                    contentDescription = tab.label,
                    tint = if (active) accent else TextMuted,
                    modifier = Modifier.size(21.dp)
                )
                Text(
                    tab.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = if (active) accent else TextMuted
                )
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .size(width = if (active) 14.dp else 0.dp, height = 2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (active) accent else Color.Transparent)
                )
            }
        }
    }
}

