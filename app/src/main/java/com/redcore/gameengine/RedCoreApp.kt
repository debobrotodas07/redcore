package com.redcore.gameengine

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.redcore.gameengine.util.Notifications

class RedCoreApp : Application() {

    lateinit var locator: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        locator = ServiceLocator(this)
        instance = this
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        val overlay = NotificationChannel(
            Notifications.CHANNEL_OVERLAY,
            getString(R.string.overlay_service_label),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.overlay_service_text)
            setShowBadge(false)
        }
        val capture = NotificationChannel(
            Notifications.CHANNEL_CAPTURE,
            getString(R.string.capture_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.capture_channel_desc)
            setShowBadge(false)
        }
        val alerts = NotificationChannel(
            Notifications.CHANNEL_ALERTS,
            getString(R.string.alerts_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.alerts_channel_desc)
        }
        nm.createNotificationChannels(listOf(overlay, capture, alerts))
    }

    companion object {
        @Volatile
        lateinit var instance: RedCoreApp
            private set

        fun locator(): ServiceLocator = instance.locator
    }
}
