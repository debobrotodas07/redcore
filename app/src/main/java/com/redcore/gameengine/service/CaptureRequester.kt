package com.redcore.gameengine.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class CaptureMode { SCREENSHOT, RECORD }

data class CaptureRequest(
    val mode: CaptureMode,
    val quality: String = "1080",
    val mic: Boolean = false
)

/**
 * Screens ask for a capture; MainActivity owns the consent dialog because Android
 * only grants a projection token to an Activity.
 */
object CaptureRequester {
    private val _requests = MutableSharedFlow<CaptureRequest>(extraBufferCapacity = 2)
    val requests = _requests.asSharedFlow()

    fun screenshot() {
        _requests.tryEmit(CaptureRequest(CaptureMode.SCREENSHOT))
    }

    fun record(quality: String, mic: Boolean) {
        _requests.tryEmit(CaptureRequest(CaptureMode.RECORD, quality, mic))
    }
}
