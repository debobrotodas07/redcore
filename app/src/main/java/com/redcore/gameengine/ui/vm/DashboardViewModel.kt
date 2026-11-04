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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val locator = RedCoreApp.locator()

    val settings = locator.settings.settings

    val games: StateFlow<List<GameEntity>> = locator.games.games
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), emptyList())

    val totalSeconds: StateFlow<Long> = locator.games.totalPlaySeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), 0L)

    val weeklySeconds: StateFlow<Long> = locator.games.weeklySeconds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), 0L)

    private val _snapshot = MutableStateFlow(DeviceSnapshot())
    val snapshot: StateFlow<DeviceSnapshot> = _snapshot.asStateFlow()

    private val _lastActions = MutableStateFlow<List<ProfileAction>>(emptyList())
    val lastActions: StateFlow<List<ProfileAction>> = _lastActions.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _snapshot.value = locator.probe.snapshot()
                delay(2500)
            }
        }
    }

    fun selectProfile(profile: PerformanceProfile) {
        locator.settings.setGlobalProfile(profile)
    }

    fun applyProfile(context: Context, profile: PerformanceProfile) {
        locator.settings.setGlobalProfile(profile)
        _lastActions.value = ProfileApplier.applyProfile(context, profile)
    }

    fun toggleSidebar(context: Context, enabled: Boolean) {
        locator.settings.setSidebarEnabled(enabled)
        if (enabled) {
            com.redcore.gameengine.service.OverlayService.start(context)
        } else {
            com.redcore.gameengine.service.OverlayService.sync(context)
        }
    }

    fun toggleCrosshair(context: Context, enabled: Boolean) {
        locator.settings.setCrosshairEnabled(enabled)
        if (enabled) {
            com.redcore.gameengine.service.OverlayService.start(context)
        } else {
            com.redcore.gameengine.service.OverlayService.sync(context)
        }
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
