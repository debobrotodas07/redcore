package com.redcore.gameengine.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redcore.gameengine.RedCoreApp
import com.redcore.gameengine.sys.DeviceSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

class MonitorViewModel : ViewModel() {

    private val locator = RedCoreApp.locator()

    private val _sampleCount = MutableStateFlow(0)
    val sampleCount: StateFlow<Int> = _sampleCount.asStateFlow()

    /** Highest CPU load seen since the screen opened — useful for spotting throttling. */
    private val _peakCpu = MutableStateFlow<Float?>(null)
    val peakCpu: StateFlow<Float?> = _peakCpu.asStateFlow()

    private val _peakBatteryTemp = MutableStateFlow<Float?>(null)
    val peakBatteryTemp: StateFlow<Float?> = _peakBatteryTemp.asStateFlow()

    /**
     * Samples are produced only while a screen is actually watching
     * (WhileSubscribed) and always off the main thread — /proc and /sys reads used
     * to run on Main, which is what made the whole app stutter every second.
     * The polling rate follows the user's setting.
     */
    val snapshot: StateFlow<DeviceSnapshot> = locator.settings.settings
        .map { it.monitorIntervalMs }
        .distinctUntilChanged()
        .flatMapLatest { intervalMs -> sampleLoop(intervalMs) }
        .onEach { snap ->
            _sampleCount.value += 1
            snap.cpuUsagePercent?.let { cpu ->
                if (cpu > (_peakCpu.value ?: 0f)) _peakCpu.value = cpu
            }
            snap.batteryTempC?.let { temp ->
                if (temp > (_peakBatteryTemp.value ?: 0f)) _peakBatteryTemp.value = temp
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DeviceSnapshot()
        )

    private fun sampleLoop(intervalMs: Int) = flow {
        while (true) {
            val started = System.currentTimeMillis()
            emit(withContext(Dispatchers.Default) { locator.probe.snapshot() })
            val elapsed = System.currentTimeMillis() - started
            delay((intervalMs - elapsed).coerceAtLeast(250))
        }
    }

    fun resetPeaks() {
        _peakCpu.value = null
        _peakBatteryTemp.value = null
    }
}
