package com.redcore.gameengine

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.redcore.gameengine.navigation.RedCoreNavHost
import com.redcore.gameengine.service.CaptureMode
import com.redcore.gameengine.service.CaptureRequest
import com.redcore.gameengine.service.CaptureRequester
import com.redcore.gameengine.service.CaptureService
import com.redcore.gameengine.ui.theme.RedCoreTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pending: CaptureRequest? = null

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val request = pending
        pending = null
        if (result.resultCode == Activity.RESULT_OK && result.data != null && request != null) {
            val data = result.data!!
            val serviceIntent = when (request.mode) {
                CaptureMode.SCREENSHOT -> CaptureService.screenshotIntent(this, result.resultCode, data)
                CaptureMode.RECORD -> CaptureService.recordIntent(
                    this, result.resultCode, data, request.quality, request.mic
                )
            }
            try {
                ContextCompat.startForegroundService(this, serviceIntent)
            } catch (t: Throwable) {
                com.redcore.gameengine.service.CaptureState.note(
                    "Android refused the capture service: ${t.message}"
                )
            }
        } else {
            com.redcore.gameengine.service.CaptureState.note("Capture cancelled")
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private var requestJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        requestJob = lifecycleScope.launch {
            CaptureRequester.requests.collect { request ->
                val manager = getSystemService(MediaProjectionManager::class.java)
                pending = request
                try {
                    projectionLauncher.launch(manager.createScreenCaptureIntent())
                } catch (t: Throwable) {
                    pending = null
                    com.redcore.gameengine.service.CaptureState.note(
                        "Screen capture is not available on this device"
                    )
                }
            }
        }

        val startRoute = intent?.getStringExtra(EXTRA_ROUTE)

        setContent {
            val settings by RedCoreApp.locator().settings.settings.collectAsState()
            RedCoreTheme(accentTheme = settings.accent) {
                RedCoreNavHost(
                    startRoute = startRoute,
                    requestAudioPermission = {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            RedCoreApp.locator().games.closeOpenSessions()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Nothing to do; notifications permission handled at start.
        }
    }

    override fun onDestroy() {
        requestJob?.cancel()
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    companion object {
        const val EXTRA_ROUTE = "redcore.route"
    }
}
