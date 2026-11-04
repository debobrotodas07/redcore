package com.redcore.gameengine.ui.vm

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redcore.gameengine.BuildConfig
import com.redcore.gameengine.RedCoreApp
import com.redcore.gameengine.data.local.AppSettings
import com.redcore.gameengine.data.model.AccentTheme
import com.redcore.gameengine.data.model.SidebarTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val checking: Boolean = false,
    val message: String = "Update channel: this build ships through your project's GitHub Releases."
)

class SettingsViewModel : ViewModel() {

    private val locator = RedCoreApp.locator()
    val settings: StateFlow<AppSettings> = locator.settings.settings

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _update = MutableStateFlow(UpdateInfo())
    val update: StateFlow<UpdateInfo> = _update.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun setAccent(value: AccentTheme) = locator.settings.setAccent(value)
    fun setAnimations(value: Boolean) = locator.settings.setAnimations(value)
    fun setHaptics(value: Boolean) = locator.settings.setHaptics(value)
    fun setNotifications(value: Boolean) = locator.settings.setNotifications(value)
    fun setSidebarEnabled(value: Boolean) {
        val app = RedCoreApp.instance
        if (value && !com.redcore.gameengine.service.OverlayController.requestEnable(app)) {
            // Permission missing: keep the switch on so returning from Android
            // settings auto-starts the overlay (MainActivity re-checks on resume).
            locator.settings.setSidebarEnabled(true)
            _message.value = "Grant \"Display over other apps\", then come back — the sidebar starts automatically."
            return
        }
        locator.settings.setSidebarEnabled(value)
        com.redcore.gameengine.service.OverlayController.reconcile(app)
    }

    fun setCrosshairEnabled(value: Boolean) {
        val app = RedCoreApp.instance
        if (value && !com.redcore.gameengine.service.OverlayController.requestEnable(app)) {
            locator.settings.setCrosshairEnabled(true)
            _message.value = "Grant \"Display over other apps\", then come back — the crosshair starts automatically."
            return
        }
        locator.settings.setCrosshairEnabled(value)
        com.redcore.gameengine.service.OverlayController.reconcile(app)
    }
    fun setSidebarSideLeft(left: Boolean) = locator.settings.setSidebarSide(
        if (left) com.redcore.gameengine.data.model.SidebarSide.LEFT
        else com.redcore.gameengine.data.model.SidebarSide.RIGHT
    )
    fun resetSidebarPosition() {
        locator.settings.setSidebarY(0.42f)
    }
    fun setSidebarOpacity(value: Int) = locator.settings.setSidebarOpacity(value)
    fun setSidebarCompact(value: Boolean) = locator.settings.setSidebarCompact(value)
    fun setTools(tools: Collection<SidebarTool>) = locator.settings.setSidebarTools(tools)
    fun setSidebarAutoStart(value: Boolean) = locator.settings.setSidebarAutoStart(value)
    fun setBrightnessControl(value: Boolean) = locator.settings.setBrightnessControl(value)
    fun setKeepScreenOn(value: Boolean) = locator.settings.setKeepScreenOn(value)
    fun setRecordingQuality(value: String) = locator.settings.setRecordingQuality(value)
    fun setCrosshairDesign(value: com.redcore.gameengine.data.model.CrosshairDesign) =
        locator.settings.setCrosshairDesign(value)
    fun setCrosshairColor(value: Long) = locator.settings.setCrosshairColor(value)
    fun setCrosshairSize(value: Int) = locator.settings.setCrosshairSize(value)
    fun setCrosshairOpacity(value: Int) = locator.settings.setCrosshairOpacity(value)
    fun setTutorialDone(value: Boolean) = locator.settings.setTutorialDone(value)
    fun setMonitorInterval(value: Int) = locator.settings.setMonitorInterval(value)
    fun setSessionAlerts(value: Boolean) = locator.settings.setSessionAlerts(value)

    fun clearSessions() {
        viewModelScope.launch {
            locator.games.clearSessions()
            _message.value = "Session history cleared"
        }
    }

    fun wipeLibrary() {
        viewModelScope.launch {
            locator.games.clearEverything()
            _message.value = "Library and profiles cleared"
        }
    }

    fun export(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            val result = locator.backup.export(uri)
            _busy.value = false
            _message.value = result.getOrElse { "Backup failed: ${it.message}" }
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            val result = locator.backup.import(uri)
            _busy.value = false
            _message.value = result.getOrElse { "Restore failed: ${it.message}" }
        }
    }

    fun suggestedBackupName(): String = locator.backup.suggestedFileName()

    fun checkForUpdates(repoSlug: String) {
        if (repoSlug.isBlank()) {
            _update.value = UpdateInfo(
                message = "This build has no update channel configured. New APKs appear in your GitHub project's Releases page."
            )
            return
        }
        _update.value = UpdateInfo(checking = true, message = "Checking GitHub…")
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://api.github.com/repos/$repoSlug/releases/latest")
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        setRequestProperty("Accept", "application/vnd.github+json")
                    }
                    if (connection.responseCode != 200) {
                        return@withContext "No published release found (HTTP ${connection.responseCode})."
                    }
                    val body = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(body)
                    val tag = json.optString("tag_name", "?")
                    val name = json.optString("name", tag)
                    val installed = BuildConfig.VERSION_NAME
                    "Latest release: $name ($tag) • installed $installed"
                } catch (t: Throwable) {
                    "Update check failed: ${t.message}"
                }
            }
            _update.value = UpdateInfo(checking = false, message = result)
        }
    }
}
