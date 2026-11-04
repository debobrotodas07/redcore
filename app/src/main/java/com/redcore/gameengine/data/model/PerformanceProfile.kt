package com.redcore.gameengine.data.model

import androidx.compose.ui.graphics.Color
import com.redcore.gameengine.ui.theme.BeastAccent
import com.redcore.gameengine.ui.theme.BalancedAccent
import com.redcore.gameengine.ui.theme.EcoAccent
import com.redcore.gameengine.ui.theme.ExtremeAccent

/**
 * Performance profiles are honest: they apply only what a normal app may apply
 * (screen brightness, refresh mode of our own window, do-not-disturb, haptics)
 * and they surface shortcuts into the system settings where the real CPU/GPU
 * governors live. They never claim to change frequencies or thermal limits.
 */
enum class PerformanceProfile(
    val id: String,
    val label: String,
    val tagline: String,
    val accent: Color,
    val brightnessPct: Int,
    val refreshHz: Int?,
    val dnd: Boolean,
    val keepsScreenOn: Boolean,
    val applies: List<String>,
    val suggests: List<String>
) {
    ECO(
        id = "eco",
        label = "ECO",
        tagline = "Long sessions, low heat",
        accent = EcoAccent,
        brightnessPct = 35,
        refreshHz = 60,
        dnd = false,
        keepsScreenOn = true,
        applies = listOf(
            "Screen brightness lowered to 35%",
            "Display refresh pinned to 60 Hz (app window)",
            "Sidebar compact mode"
        ),
        suggests = listOf(
            "Battery saver / balanced power mode in system settings",
            "Lower in-game graphics preset",
            "Close background apps before playing"
        )
    ),
    BALANCED(
        id = "balanced",
        label = "BALANCED",
        tagline = "Default competitive setup",
        accent = BalancedAccent,
        brightnessPct = 55,
        refreshHz = null,
        dnd = false,
        keepsScreenOn = true,
        applies = listOf(
            "Screen brightness at 55%",
            "System refresh rate respected",
            "Full sidebar toolkit"
        ),
        suggests = listOf(
            "Game mode / performance mode = Auto",
            "Keep thermal headroom by avoiding charging while playing"
        )
    ),
    BEAST(
        id = "beast",
        label = "BEAST",
        tagline = "Gaming focus, max refresh",
        accent = BeastAccent,
        brightnessPct = 75,
        refreshHz = 144,
        dnd = false,
        keepsScreenOn = true,
        applies = listOf(
            "Screen brightness at 75%",
            "Highest supported refresh rate requested for the app window",
            "Do Not Disturb candidate (only if you grant policy access)"
        ),
        suggests = listOf(
            "Enable your device's \"Performance/Game\" mode for real governor changes",
            "Disable battery saver",
            "Charge above 40% to avoid power throttling"
        )
    ),
    EXTREME(
        id = "extreme",
        label = "EXTREME",
        tagline = "Everything the platform permits",
        accent = ExtremeAccent,
        brightnessPct = 100,
        refreshHz = 165,
        dnd = true,
        keepsScreenOn = true,
        applies = listOf(
            "Screen brightness at 100%",
            "Maximum refresh rate mode requested for the app window",
            "Do Not Disturb when policy access is granted",
            "Sidebar expanded with all tools"
        ),
        suggests = listOf(
            "Manual fan / cooling if your device supports one",
            "Root or vendor game mode is required for CPU/GPU governor control",
            "Watch battery temperature in the Monitor screen"
        )
    );

    companion object {
        fun fromId(id: String?): PerformanceProfile =
            entries.firstOrNull { it.id == id } ?: BALANCED
    }
}
