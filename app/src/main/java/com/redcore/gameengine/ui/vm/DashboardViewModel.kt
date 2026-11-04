package com.redcore.gameengine.ui.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redcore.gameengine.RedCoreApp
import com.redcore.gameengine.data.model.GameEntity
import com.redcore.gameengine.data.model.PerformanceProfile
import com.redcore.gameengine.sys.DeviceSnapshot
import com.redcore.gameengine.sys.ProfileAction
import com.redcore.gameengine.sys.ProfileApplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel : ViewModel() {

    private val locator = RedCoreApp.locator()

    val settings = locator.settings.settings

    val games: StateFlow<List<GameEntity>> = locator.games.games
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), emptyList())

    val totalSeconds: StateFlow<Long> = locator.games.totalPlaySeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), 0L)

    val weeklySeconds: StateFlow<Long> = locator.games.weeklySeconds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), 0L)

    private val _lastActions = MutableStateFlow<List<ProfileAction>>(emptyList())
    val lastActions: StateFlow<List<ProfileAction>> = _lastActions.asStateFlow()

    /**
     * Dashboard telemetry: sampled off Main and only while the screen is visible.
     * The old loop ran on viewModelScope's Main dispatcher forever, even with the
     * app backgrounded, and blocked the frame with /proc + /sys reads.
     */
    val snapshot: StateFlow<DeviceSnapshot> = flow {
        while (true) {
            emit(withContext(Dispatchers.Default) { locator.probe.snapshot() })
            delay(2_500)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DeviceSnapshot()
    )

    fun selectProfile(profile: PerformanceProfile) {
        locator.settings.setGlobalProfile(profile)
    }

    fun applyProfile(context: Context, profile: PerformanceProfile) {
        locator.settings.setGlobalProfile(profile)
        _lastActions.value = ProfileApplier.applyProfile(context, profile)
    }

    fun toggleSidebar(context: Context, enabled: Boolean) {
        if (enabled && !com.redcore.gameengine.service.OverlayController.requestEnable(context)) {
            // Keep the preference so the overlay starts as soon as the user returns
            // from the system permission screen (MainActivity reconciles on resume).
            locator.settings.setSidebarEnabled(true)
            return
        }
        locator.settings.setSidebarEnabled(enabled)
        com.redcore.gameengine.service.OverlayController.reconcile(context)
    }

    fun toggleCrosshair(context: Context, enabled: Boolean) {
        if (enabled && !com.redcore.gameengine.service.OverlayController.requestEnable(context)) {
            locator.settings.setCrosshairEnabled(true)
            return
        }
        locator.settings.setCrosshairEnabled(enabled)
        com.redcore.gameengine.service.OverlayController.reconcile(context)
    }

    fun syncLibrary() {
        viewModelScope.launch { locator.games.syncDetectedGames() }
    }

    fun launch(context: Context, game: GameEntity) {
        viewModelScope.launch {
            val profile = locator.games.profileFor(game.packageName)
                ?.let { PerformanceProfile.fromId(it.profileId) }
                ?: settings.value.globalProfile
            locator.games.launch(game.packageName, game.label, profile)
        }
    }
}
