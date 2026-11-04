package com.redcore.gameengine.sys

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.Window
import android.view.WindowManager
import com.redcore.gameengine.data.model.PerformanceProfile
import kotlin.math.roundToInt

data class ProfileAction(
    val label: String,
    val applied: Boolean,
    val detail: String
)

/**
 * Applies what an unprivileged app is actually allowed to apply, and reports
 * honestly when a step was skipped. No CPU/GPU frequency or thermal limit is
 * ever touched — that requires root or a system-signed app.
 */
object ProfileApplier {

    fun applyProfile(context: Context, profile: PerformanceProfile): List<ProfileAction> {
        val actions = mutableListOf<ProfileAction>()

        // 1. Screen brightness (needs "Modify system settings")
        if (PermissionHelper.writeSettings(context)) {
            val value = (profile.brightnessPct / 100f * 255f).roundToInt().coerceIn(1, 255)
            val ok = try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    value
                )
            } catch (t: Throwable) {
                false
            }
            actions += ProfileAction(
                "Screen brightness ${profile.brightnessPct}%",
                ok,
                if (ok) "Applied to the system brightness setting"
                else "System rejected the write — open Permission manager"
            )
        } else {
            actions += ProfileAction(
                "Screen brightness ${profile.brightnessPct}%",
                false,
                "Needs \"Modify system settings\" — grant it in Permissions"
            )
        }

        // 2. Do Not Disturb (only when the profile asks for it and access is granted)
        if (profile.dnd) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                val ok = try {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    true
                } catch (t: Throwable) {
                    false
                }
                actions += ProfileAction(
                    "Do Not Disturb",
                    ok,
                    if (ok) "Interruption filter set to Priority only"
                    else "The system rejected the filter change"
                )
            } else {
                actions += ProfileAction(
                    "Do Not Disturb",
                    false,
                    "Needs Do Not Disturb access — grant it in Permissions"
                )
            }
        } else {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                try {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                } catch (_: Throwable) {
                }
            }
        }

        actions += ProfileAction(
            "Haptics",
            true,
            "Touch feedback follows Settings > Haptics in REDCORE"
        )
        actions += ProfileAction(
            "CPU / GPU governor",
            false,
            "Not available to user apps. Use your device's Game Mode — that is a system-signed component."
        )
        actions += ProfileAction(
            "Thermal limits",
            false,
            "Firmware-controlled. No app can raise them without root."
        )
        return actions
    }

    /**
     * Requests a refresh-rate mode on a specific window. Only affects REDCORE's
     * own window — games keep whatever the system gives them.
     */
    fun applyRefreshRate(window: Window, targetHz: Int?): String {
        if (targetHz == null) return "System default refresh rate kept"
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.context.display
        } else {
            @Suppress("DEPRECATION")
            window.windowManager.defaultDisplay
        } ?: return "No display available"
        val modes = display.supportedModes
        if (modes.isEmpty()) return "Display modes not reported by this device"
        val best = modes.minByOrNull { kotlin.math.abs(it.refreshRate - targetHz) } ?: return "No modes"
        return try {
            val attrs = window.attributes
            attrs.preferredDisplayModeId = best.modeId
            window.attributes = attrs
            "Refresh pinned to ${best.refreshRate.roundToInt()} Hz (REDCORE window only)"
        } catch (t: Throwable) {
            "This device refused a per-window refresh mode"
        }
    }

    fun maxSupportedHz(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay
        } ?: return 60
        val modes = display.supportedModes
        if (modes.isEmpty()) return display.refreshRate.roundToInt()
        return modes.maxOf { it.refreshRate }.roundToInt()
    }

    fun availableHz(context: Context): List<Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay
        } ?: return listOf(60)
        return display.supportedModes.map { it.refreshRate.roundToInt() }.distinct().sorted()
    }

    fun mediaVolumePercent(context: Context): Int {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return (am.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f / max).roundToInt()
    }

    fun setMediaVolumePercent(context: Context, percent: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = (percent.coerceIn(0, 100) / 100f * max).roundToInt().coerceIn(0, max)
        try {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        } catch (_: SecurityException) {
            // Do Not Disturb may block volume changes until policy access is granted.
        }
    }
}
