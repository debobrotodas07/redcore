package com.redcore.gameengine.service

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Live state of the capture pipeline, shared between the service and the UI. */
object CaptureState {
    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording

    private val _startedAt = MutableStateFlow(0L)
    val startedAt: StateFlow<Long> = _startedAt

    private val _lastScreenshot = MutableStateFlow<Uri?>(null)
    val lastScreenshot: StateFlow<Uri?> = _lastScreenshot

    private val _lastVideo = MutableStateFlow<Uri?>(null)
    val lastVideo: StateFlow<Uri?> = _lastVideo

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun setRecording(active: Boolean, timestamp: Long = 0L) {
        _recording.value = active
        _startedAt.value = timestamp
    }

    fun screenshotSaved(uri: Uri) {
        _lastScreenshot.value = uri
        _message.value = "Screenshot saved to Pictures/REDCORE"
    }

    fun videoSaved(uri: Uri) {
        _lastVideo.value = uri
        _message.value = "Recording saved to Movies/REDCORE"
    }

    fun note(text: String?) {
        _message.value = text
    }

    fun clearMessage() {
        _message.value = null
    }
}
