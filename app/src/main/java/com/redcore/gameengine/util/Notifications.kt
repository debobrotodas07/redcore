package com.redcore.gameengine.util

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.redcore.gameengine.MainActivity
import com.redcore.gameengine.R

object Notifications {
    const val CHANNEL_OVERLAY = "redcore_overlay"
    const val CHANNEL_CAPTURE = "redcore_capture"
    const val CHANNEL_ALERTS = "redcore_alerts"

    const val ID_OVERLAY = 1001
    const val ID_CAPTURE = 1002
    const val ID_ALERT = 1003

    fun openAppIntent(context: Context, route: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (route != null) putExtra(MainActivity.EXTRA_ROUTE, route)
        }
        return PendingIntent.getActivity(
            context,
            route?.hashCode() ?: 11,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun overlayNotification(context: Context, text: String, actions: Boolean = true): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_OVERLAY)
            .setSmallIcon(R.drawable.ic_stat_core)
            .setContentTitle("REDCORE Sidebar")
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openAppIntent(context))
        if (actions) {
            builder.addAction(
                0, "Stop",
                PendingIntent.getService(
                    context, 21,
                    Intent(context, com.redcore.gameengine.service.OverlayService::class.java)
                        .setAction(com.redcore.gameengine.service.OverlayService.ACTION_STOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
        return builder.build()
    }

    fun captureNotification(
        context: Context,
        title: String,
        text: String,
        ongoing: Boolean,
        stopAction: Boolean = false
    ): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_CAPTURE)
            .setSmallIcon(R.drawable.ic_stat_core)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(ongoing)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppIntent(context, "capture"))
        if (stopAction) {
            builder.addAction(
                0, "Stop",
                PendingIntent.getService(
                    context, 22,
                    Intent(context, com.redcore.gameengine.service.CaptureService::class.java)
                        .setAction(com.redcore.gameengine.service.CaptureService.ACTION_STOP_RECORDING),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
        return builder.build()
    }

    fun alert(context: Context, title: String, text: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_core)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        try {
            nm.notify(ID_ALERT, n)
        } catch (_: SecurityException) {
            // Notifications declined by the user — silent no-op is correct.
        }
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.areNotificationsEnabled()
    }
}
