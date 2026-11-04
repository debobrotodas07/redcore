package com.redcore.gameengine.service

import android.content.Context
import com.redcore.gameengine.RedCoreApp
import com.redcore.gameengine.sys.PermissionHelper

/**
 * Single source of truth for "should the overlay service be running?".
 *
 * Before this existed, the two switches that control the overlay (Dashboard and
 * Settings/More) took different paths: the Dashboard started the service, the
 * Settings screen only wrote the preference. The service could also stop itself
 * when the permission was missing and never come back after the user granted it,
 * which left the toggle showing "on" with no sidebar.
 *
 * Every entry point (app resume, toggle, boot) now calls [reconcile]; the service
 * is started only when the permission actually exists and a feature is enabled.
 */
object OverlayController {

    fun isServiceRunning(): Boolean = OverlayService.isRunning

    /** True when the user wants at least one overlay on screen. */
    fun wantsOverlay(context: Context): Boolean {
        val settings = RedCoreApp.locator().settings.settings.value
        return settings.sidebarEnabled || settings.crosshairEnabled
    }

    fun canDrawOverlays(context: Context): Boolean = PermissionHelper.overlay(context)

    /**
     * Brings the service state in line with permission + preferences.
     * Safe to call from onResume, boot, or a switch — it never duplicates windows
     * because the service itself owns exactly one of each window.
     */
    fun reconcile(context: Context) {
        val wanted = wantsOverlay(context)
        val permitted = canDrawOverlays(context)
        when {
            wanted && permitted -> OverlayService.start(context)
            !wanted -> OverlayService.stop(context)
            // Wanted but not permitted: make sure no stale window service lingers.
            else -> if (isServiceRunning()) OverlayService.stop(context)
        }
    }

    /** Called by UI toggles before they write the preference. */
    fun requestEnable(context: Context): Boolean {
        if (canDrawOverlays(context)) return true
        PermissionHelper.openOverlaySettings(context)
        return false
    }
}
