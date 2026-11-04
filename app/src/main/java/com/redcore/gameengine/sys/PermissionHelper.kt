package com.redcore.gameengine.sys

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

enum class PermissionState { GRANTED, DENIED, RESTRICTED }

data class PermissionItem(
    val key: String,
    val title: String,
    val why: String,
    val required: Boolean,
    val state: PermissionState
)

object PermissionHelper {

    fun overlay(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun writeSettings(context: Context): Boolean = Settings.System.canWrite(context)

    fun notificationPolicy(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun notifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun recordAudio(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    fun wouldShowCameraRationale(): Boolean = true

    fun list(context: Context): List<PermissionItem> = listOf(
        PermissionItem(
            key = "overlay",
            title = "Display over other apps",
            why = "Required for the floating sidebar and the crosshair overlay.",
            required = true,
            state = if (overlay(context)) PermissionState.GRANTED else PermissionState.DENIED
        ),
        PermissionItem(
            key = "notifications",
            title = "Notifications (Android 13+)",
            why = "Keeps the sidebar and recording foreground services visible and dismissible.",
            required = false,
            state = if (notifications(context)) PermissionState.GRANTED else PermissionState.DENIED
        ),
        PermissionItem(
            key = "write_settings",
            title = "Modify system settings",
            why = "Only used by the brightness shortcut and profile brightness steps.",
            required = false,
            state = if (writeSettings(context)) PermissionState.GRANTED else PermissionState.DENIED
        ),
        PermissionItem(
            key = "policy",
            title = "Do Not Disturb access",
            why = "Only used by the DND shortcut and the EXTREME profile.",
            required = false,
            state = if (notificationPolicy(context)) PermissionState.GRANTED else PermissionState.DENIED
        ),
        PermissionItem(
            key = "audio",
            title = "Microphone",
            why = "Only used by the microphone test and voice studio recording.",
            required = false,
            state = if (recordAudio(context)) PermissionState.GRANTED else PermissionState.DENIED
        ),
        PermissionItem(
            key = "screen_capture",
            title = "Screen capture consent",
            why = "Android asks every session — you confirm each time you record or screenshot.",
            required = false,
            state = PermissionState.RESTRICTED
        )
    )

    fun openOverlaySettings(context: Context) {
        safeStart(context, Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ))
    }

    fun openWriteSettings(context: Context) {
        safeStart(context, Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ))
    }

    fun openPolicySettings(context: Context) {
        safeStart(context, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    fun openAppNotificationSettings(context: Context) {
        safeStart(context, Intent(
            Settings.ACTION_APP_NOTIFICATION_SETTINGS
        ).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
    }

    fun openBatteryOptimisation(context: Context) {
        safeStart(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    fun openDisplaySettings(context: Context) {
        safeStart(context, Intent(Settings.ACTION_DISPLAY_SETTINGS))
    }

    fun openSoundSettings(context: Context) {
        safeStart(context, Intent(Settings.ACTION_SOUND_SETTINGS))
    }

    fun openDevSettings(context: Context) {
        safeStart(context, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
    }

    fun openAppInfo(context: Context) {
        safeStart(context, Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ))
    }

    private fun safeStart(context: Context, intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (t: Throwable) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Throwable) {
                // Nothing else we can do without an activity.
            }
        }
    }
}
