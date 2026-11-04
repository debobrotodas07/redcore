package com.redcore.gameengine.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redcore.gameengine.RedCoreApp
import com.redcore.gameengine.sys.DeviceSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MonitorViewModel : ViewModel() {

    private val locator = RedCoreApp.locator()

    private val _snapshot = MutableStateFlow(DeviceSnapshot())
    val snapshot: StateFlow<DeviceSnapshot> = _snapshot.asStateFlow()

    private val _sampleCount = MutableStateFlow(0)
    val sampleCount: StateFlow<Int> = _sampleCount.asStateFlow()

    /** Highest CPU load seen since the screen opened — useful for spotting throttling. */
    private val _peakCpu = MutableStateFlow<Float?>(null)
    val peakCpu: StateFlow<Float?> = _peakCpu.asStateFlow()

    private val _peakBatteryTemp = MutableStateFlow<Float?>(null)
    val peakBatteryTemp: StateFlow<Float?> = _peakBatteryTemp.asStateFlow()

    private val intervalMs = 1000

    init {
        viewModelScope.launch {
            while (true) {
                val started = System.currentTimeMillis()
                val snap = locator.probe.snapshot()
                _snapshot.value = snap
                _sampleCount.value = _sampleCount.value + 1
                snap.cpuUsagePercent?.let { cpu ->
                    val prev = _peakCpu.value ?: 0f
                    if (cpu > prev) _peakCpu.value = cpu
                }
                snap.batteryTempC?.let { temp ->
                    val prev = _peakBatteryTemp.value ?: 0f
                    if (temp > prev) _peakBatteryTemp.value = temp
                }
                val elapsed = System.currentTimeMillis() - started
                delay((intervalMs - elapsed).coerceAtLeast(250))
            }
        }
    }

    fun resetPeaks() {
        _peakCpu.value = null
        _peakBatteryTemp.value = null
    }
}
