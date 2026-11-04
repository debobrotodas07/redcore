package com.redcore.gameengine.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.redcore.gameengine.RedCoreApp

/**
 * Restores the floating sidebar after a reboot when the user asked for it.
 * Only starts the service when the overlay permission is still granted — the
 * service would otherwise stop itself immediately.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val settings = RedCoreApp.locator().settings.settings.value
        if (!settings.sidebarAutoStart) return
        if (!settings.sidebarEnabled && !settings.crosshairEnabled) return
        OverlayController.reconcile(context)
    }
}
