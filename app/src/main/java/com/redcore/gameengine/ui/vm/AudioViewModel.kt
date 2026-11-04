package com.redcore.gameengine.ui.vm

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.Equalizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redcore.gameengine.RedCoreApp
import com.redcore.gameengine.data.model.AudioPreset
import com.redcore.gameengine.data.model.VoiceEffect
import com.redcore.gameengine.util.AudioEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

data class StreamRow(val label: String, val streamType: Int, val percent: Int, val iconHint: String)

class AudioViewModel : ViewModel() {

    private val context = RedCoreApp.instance.applicationContext
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val settingsRepo = RedCoreApp.locator().settings

    private val _streams = MutableStateFlow<List<StreamRow>>(emptyList())
    val streams: StateFlow<List<StreamRow>> = _streams.asStateFlow()

    private val _outputs = MutableStateFlow<List<String>>(emptyList())
    val outputs: StateFlow<List<String>> = _outputs.asStateFlow()

    private val _sessions = MutableStateFlow<List<String>>(emptyList())
    val sessions: StateFlow<List<String>> = _sessions.asStateFlow()

    private val _preset = MutableStateFlow(AudioPreset.FLAT)
    val preset: StateFlow<AudioPreset> = _preset.asStateFlow()

    private val _equalizerStatus = MutableStateFlow("Equaliser not touched yet")
    val equalizerStatus: StateFlow<String> = _equalizerStatus.asStateFlow()

    private val _micLevel = MutableStateFlow(0f)
    val micLevel: StateFlow<Float> = _micLevel.asStateFlow()

    private val _micState = MutableStateFlow("Idle")
    val micState: StateFlow<String> = _micState.asStateFlow()

    private val _voiceStatus = MutableStateFlow("Record 6 seconds, then preview an effect.")
    val voiceStatus: StateFlow<String> = _voiceStatus.asStateFlow()

    private val _voiceBusy = MutableStateFlow(false)
    val voiceBusy: StateFlow<Boolean> = _voiceBusy.asStateFlow()

    private val _lastSamples = MutableStateFlow<ShortArray?>(null)
    private var eq: Equalizer? = null
    private var micJob: Job? = null

    init {
        viewModelScope.launch {
            _preset.value = settingsRepo.settings.value.audioPreset
        }
        refresh()
    }

    fun refresh() {
        val rows = listOf(
            Triple("Media", AudioManager.STREAM_MUSIC, "music"),
            Triple("Ring", AudioManager.STREAM_RING, "ring"),
            Triple("Notification", AudioManager.STREAM_NOTIFICATION, "notify"),
            Triple("Alarm", AudioManager.STREAM_ALARM, "alarm"),
            Triple("System", AudioManager.STREAM_SYSTEM, "system")
        ).map { (label, stream, hint) ->
            val max = audioManager.getStreamMaxVolume(stream).coerceAtLeast(1)
            val current = audioManager.getStreamVolume(stream)
            StreamRow(label, stream, (current * 100f / max).toInt(), hint)
        }
        _streams.value = rows

        _outputs.value = try {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).mapNotNull { device ->
                val type = deviceTypeName(device.type)
                val name = device.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Audio device"
                "$name • $type"
            }.distinct().ifEmpty { listOf("No output devices reported") }
        } catch (t: Throwable) {
            listOf("Device list unavailable on this build")
        }

        _sessions.value = try {
            // getActivePlaybackConfigurations() already returns only live players.
            audioManager.getActivePlaybackConfigurations()
                .mapIndexed { index, config ->
                    val attrs = config.audioAttributes
                    val usage = usageName(attrs.usage)
                    "Player ${index + 1} • $usage"
                }
                .distinct()
                .ifEmpty { listOf("No active playback sessions") }
        } catch (t: Throwable) {
            listOf("Session info unavailable")
        }
    }

    fun setVolume(stream: Int, percent: Int) {
        val max = audioManager.getStreamMaxVolume(stream).coerceAtLeast(1)
        val target = (percent.coerceIn(0, 100) / 100f * max).toInt().coerceIn(0, max)
        try {
            audioManager.setStreamVolume(stream, target, 0)
        } catch (_: SecurityException) {
        }
        refresh()
    }

    fun selectPreset(preset: AudioPreset) {
        _preset.value = preset
        settingsRepo.setAudioPreset(preset)
        _equalizerStatus.value = applyEqualizer(preset)
    }

    /**
     * Applies an Equalizer to the global output session (id 0). Many devices allow
     * this; some OEM builds refuse it — we report exactly what happened.
     */
    private fun applyEqualizer(preset: AudioPreset): String {
        return try {
            if (eq == null) {
                eq = Equalizer(0, 0)
            }
            val equalizer = eq ?: return "Equaliser unavailable"
            equalizer.enabled = true
            val range = equalizer.bandLevelRange
            val minLevel = range[0].toInt()
            val maxLevel = range[1].toInt()
            val bands = equalizer.numberOfBands.toInt()
            for (band in 0 until bands) {
                val index = if (bands <= 1) 0 else (band * preset.gains.size / bands).coerceIn(0, preset.gains.size - 1)
                val milliBel = (preset.gains[index] * 100).toInt().coerceIn(minLevel, maxLevel)
                equalizer.setBandLevel(band.toShort(), milliBel.toShort())
            }
            "Equaliser active: ${preset.label} • $bands bands"
        } catch (t: Throwable) {
            "This device blocked the app-level equaliser (${t.javaClass.simpleName}). Volume and presets still work through the media stream."
        }
    }

    fun startMicTest() {
        if (micJob != null) return
        _micState.value = "Listening…"
        micJob = viewModelScope.launch {
            try {
                AudioEngine.meter { level -> _micLevel.value = level }
            } catch (t: Throwable) {
                _micState.value = "Microphone unavailable: ${t.message}"
            }
        }
    }

    fun stopMicTest() {
        micJob?.cancel()
        micJob = null
        _micLevel.value = 0f
        _micState.value = "Idle"
    }

    fun recordVoice() {
        if (_voiceBusy.value) return
        viewModelScope.launch {
            _voiceBusy.value = true
            _voiceStatus.value = "Recording 6 seconds — speak now"
            val samples = AudioEngine.record(context, 6)
            _busyFalse()
            if (samples == null) {
                _voiceStatus.value = "Microphone access failed. Grant the Microphone permission."
                return@launch
            }
            _lastSamples.value = samples
            val seconds = samples.size.toFloat() / AudioEngine.SAMPLE_RATE
            _voiceStatus.value = "Captured %.1fs of audio. Pick an effect to preview.".format(seconds)
            _busyFalse()
        }
    }

    fun preview(effect: VoiceEffect) {
        val samples = _lastSamples.value
        if (samples == null) {
            _voiceStatus.value = "Record something first."
            return
        }
        if (_voiceBusy.value) return
        viewModelScope.launch {
            _voiceBusy.value = true
            _voiceStatus.value = "Applying ${effect.label}…"
            val processed = AudioEngine.applyEffect(samples, effect)
            _voiceStatus.value = "Playing ${effect.label}"
            AudioEngine.play(processed)
            _voiceStatus.value = "${effect.label} preview finished"
            _busyFalse()
        }
    }

    fun save(effect: VoiceEffect) {
        val samples = _lastSamples.value
        if (samples == null) {
            _voiceStatus.value = "Record something first."
            return
        }
        viewModelScope.launch {
            _voiceBusy.value = true
            val processed = AudioEngine.applyEffect(samples, effect)
            val result = AudioEngine.saveWav(context, processed)
            _voiceStatus.value = result ?: "The file could not be saved"
            _busyFalse()
        }
    }

    private fun _busyFalse() {
        _voiceBusy.value = false
    }

    fun peakBand(samples: ShortArray?): Float {
        if (samples == null || samples.isEmpty()) return 0f
        var peak = 0
        for (s in samples) {
            val v = abs(s.toInt())
            if (v > peak) peak = v
        }
        return yieldPeak(peak)
    }

    private fun yieldPeak(peak: Int): Float = (peak / 32768f).coerceIn(0f, 1f)

    private fun deviceTypeName(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headset"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth audio"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth call"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB headset"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB audio"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI"
        else -> "Output ${type}"
    }

    private fun usageName(usage: Int): String = when (usage) {
        android.media.AudioAttributes.USAGE_MEDIA -> "Media"
        android.media.AudioAttributes.USAGE_GAME -> "Game"
        android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION -> "Voice call"
        android.media.AudioAttributes.USAGE_NOTIFICATION -> "Notification"
        android.media.AudioAttributes.USAGE_ALARM -> "Alarm"
        else -> "Usage $usage"
    }

    override fun onCleared() {
        micJob?.cancel()
        try {
            eq?.enabled = false
            eq?.release()
        } catch (_: Throwable) {
        }
        eq = null
        super.onCleared()
    }
}
