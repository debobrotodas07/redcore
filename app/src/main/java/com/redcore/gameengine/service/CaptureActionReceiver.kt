package com.redcore.gameengine.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Handles the notification "Stop" action for recordings without opening the app. */
class CaptureActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP_RECORDING) {
            val service = Intent(context, CaptureService::class.java)
                .setAction(CaptureService.ACTION_STOP_RECORDING)
            try {
                context.startService(service)
            } catch (_: Throwable) {
                // Service already gone — nothing to stop.
            }
        }
    }

    companion object {
        const val ACTION_STOP_RECORDING = "com.redcore.gameengine.STOP_RECORDING"
    }
}
