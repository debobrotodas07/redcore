package com.redcore.gameengine.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.redcore.gameengine.data.model.AccentTheme
import com.redcore.gameengine.data.model.AudioPreset
import com.redcore.gameengine.data.model.CrosshairDesign
import com.redcore.gameengine.data.model.PerformanceProfile
import com.redcore.gameengine.data.model.SidebarSide
import com.redcore.gameengine.data.model.SidebarTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "redcore_settings")

@androidx.compose.runtime.Immutable
data class AppSettings(
    val accent: AccentTheme = AccentTheme.CRIMSON,
    val animations: Boolean = true,
    val haptics: Boolean = true,
    val notifications: Boolean = true,
    val globalProfile: PerformanceProfile = PerformanceProfile.BALANCED,
    val sidebarEnabled: Boolean = false,
    val sidebarSide: SidebarSide = SidebarSide.LEFT,
    val sidebarY: Float = 0.42f,
    val sidebarOpacity: Int = 82,
    val sidebarCompact: Boolean = false,
    val sidebarTools: List<SidebarTool> = SidebarTool.parseCsv(null),
    val sidebarAutoStart: Boolean = false,
    val crosshairEnabled: Boolean = false,
    val crosshairDesign: CrosshairDesign = CrosshairDesign.RING_DOT,
    val crosshairColor: Long = 0xFFFF2638,
    val crosshairSize: Int = 28,
    val crosshairOpacity: Int = 85,
    val audioPreset: AudioPreset = AudioPreset.FLAT,
    val brightnessControl: Boolean = true,
    val keepScreenOn: Boolean = true,
    val monitorIntervalMs: Int = 1000,
    val recordingQuality: String = "1080",
    val tutorialDone: Boolean = false,
    val sessionAlerts: Boolean = true
)

class SettingsRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private object Keys {
        val accent = stringPreferencesKey("accent")
        val animations = booleanPreferencesKey("animations")
        val haptics = booleanPreferencesKey("haptics")
        val notifications = booleanPreferencesKey("notifications")
        val globalProfile = stringPreferencesKey("global_profile")
        val sidebarEnabled = booleanPreferencesKey("sidebar_enabled")
        val sidebarSide = stringPreferencesKey("sidebar_side")
        val sidebarY = floatPreferencesKey("sidebar_y")
        val sidebarOpacity = intPreferencesKey("sidebar_opacity")
        val sidebarCompact = booleanPreferencesKey("sidebar_compact")
        val sidebarTools = stringPreferencesKey("sidebar_tools")
        val sidebarAutoStart = booleanPreferencesKey("sidebar_autostart")
        val crosshairEnabled = booleanPreferencesKey("crosshair_enabled")
        val crosshairDesign = stringPreferencesKey("crosshair_design")
        val crosshairColor = longPreferencesKey("crosshair_color")
        val crosshairSize = intPreferencesKey("crosshair_size")
        val crosshairOpacity = intPreferencesKey("crosshair_opacity")
        val audioPreset = stringPreferencesKey("audio_preset")
        val brightnessControl = booleanPreferencesKey("brightness_control")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val monitorInterval = intPreferencesKey("monitor_interval")
        val recordingQuality = stringPreferencesKey("recording_quality")
        val tutorialDone = booleanPreferencesKey("tutorial_done")
        val sessionAlerts = booleanPreferencesKey("session_alerts")
    }

    private val defaults = AppSettings()

    private val mapping: (Preferences) -> AppSettings = { p ->
        AppSettings(
            accent = AccentTheme.fromId(p[Keys.accent] ?: defaults.accent.id),
            animations = p[Keys.animations] ?: defaults.animations,
            haptics = p[Keys.haptics] ?: defaults.haptics,
            notifications = p[Keys.notifications] ?: defaults.notifications,
            globalProfile = PerformanceProfile.fromId(p[Keys.globalProfile]),
            sidebarEnabled = p[Keys.sidebarEnabled] ?: defaults.sidebarEnabled,
            sidebarSide = SidebarSide.fromId(p[Keys.sidebarSide] ?: defaults.sidebarSide.id),
            sidebarY = p[Keys.sidebarY] ?: defaults.sidebarY,
            sidebarOpacity = p[Keys.sidebarOpacity] ?: defaults.sidebarOpacity,
            sidebarCompact = p[Keys.sidebarCompact] ?: defaults.sidebarCompact,
            sidebarTools = SidebarTool.parseCsv(p[Keys.sidebarTools]),
            sidebarAutoStart = p[Keys.sidebarAutoStart] ?: defaults.sidebarAutoStart,
            crosshairEnabled = p[Keys.crosshairEnabled] ?: defaults.crosshairEnabled,
            crosshairDesign = CrosshairDesign.fromId(p[Keys.crosshairDesign] ?: defaults.crosshairDesign.id),
            crosshairColor = p[Keys.crosshairColor] ?: defaults.crosshairColor,
            crosshairSize = p[Keys.crosshairSize] ?: defaults.crosshairSize,
            crosshairOpacity = p[Keys.crosshairOpacity] ?: defaults.crosshairOpacity,
            audioPreset = AudioPreset.fromId(p[Keys.audioPreset] ?: defaults.audioPreset.id),
            brightnessControl = p[Keys.brightnessControl] ?: defaults.brightnessControl,
            keepScreenOn = p[Keys.keepScreenOn] ?: defaults.keepScreenOn,
            monitorIntervalMs = p[Keys.monitorInterval] ?: defaults.monitorIntervalMs,
            recordingQuality = p[Keys.recordingQuality] ?: defaults.recordingQuality,
            tutorialDone = p[Keys.tutorialDone] ?: defaults.tutorialDone,
            sessionAlerts = p[Keys.sessionAlerts] ?: defaults.sessionAlerts
        )
    }

    val settings: StateFlow<AppSettings> = context.settingsStore.data
        .map(mapping)
        .stateIn(scope, SharingStarted.Eagerly, defaults)

    fun observe(): Flow<AppSettings> = context.settingsStore.data.map(mapping)

    suspend fun current(): AppSettings = context.settingsStore.data.first().let(mapping)

    private fun update(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        scope.launch { context.settingsStore.edit { block(it) } }
    }

    fun setAccent(value: AccentTheme) = update { it[Keys.accent] = value.id }
    fun setAnimations(value: Boolean) = update { it[Keys.animations] = value }
    fun setHaptics(value: Boolean) = update { it[Keys.haptics] = value }
    fun setNotifications(value: Boolean) = update { it[Keys.notifications] = value }
    fun setGlobalProfile(value: PerformanceProfile) = update { it[Keys.globalProfile] = value.id }
    fun setSidebarEnabled(value: Boolean) = update { it[Keys.sidebarEnabled] = value }
    fun setSidebarSide(value: SidebarSide) = update { it[Keys.sidebarSide] = value.id }
    fun setSidebarY(value: Float) = update { it[Keys.sidebarY] = value.coerceIn(0.05f, 0.95f) }
    fun setSidebarOpacity(value: Int) = update { it[Keys.sidebarOpacity] = value.coerceIn(35, 100) }
    fun setSidebarCompact(value: Boolean) = update { it[Keys.sidebarCompact] = value }
    fun setSidebarTools(value: Collection<SidebarTool>) = update { it[Keys.sidebarTools] = SidebarTool.toCsv(value) }
    fun setSidebarAutoStart(value: Boolean) = update { it[Keys.sidebarAutoStart] = value }
    fun setCrosshairEnabled(value: Boolean) = update { it[Keys.crosshairEnabled] = value }
    fun setCrosshairDesign(value: CrosshairDesign) = update { it[Keys.crosshairDesign] = value.id }
    fun setCrosshairColor(value: Long) = update { it[Keys.crosshairColor] = value }
    fun setCrosshairSize(value: Int) = update { it[Keys.crosshairSize] = value.coerceIn(10, 90) }
    fun setCrosshairOpacity(value: Int) = update { it[Keys.crosshairOpacity] = value.coerceIn(15, 100) }
    fun setAudioPreset(value: AudioPreset) = update { it[Keys.audioPreset] = value.id }
    fun setBrightnessControl(value: Boolean) = update { it[Keys.brightnessControl] = value }
    fun setKeepScreenOn(value: Boolean) = update { it[Keys.keepScreenOn] = value }
    fun setMonitorInterval(value: Int) = update { it[Keys.monitorInterval] = value }
    fun setRecordingQuality(value: String) = update { it[Keys.recordingQuality] = value }
    fun setTutorialDone(value: Boolean) = update { it[Keys.tutorialDone] = value }
    fun setSessionAlerts(value: Boolean) = update { it[Keys.sessionAlerts] = value }
}
