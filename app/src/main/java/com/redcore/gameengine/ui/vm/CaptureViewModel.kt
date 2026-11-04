package com.redcore.gameengine.ui.vm

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redcore.gameengine.RedCoreApp
import com.redcore.gameengine.service.CaptureRequester
import com.redcore.gameengine.service.CaptureService
import com.redcore.gameengine.service.CaptureState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CaptureViewModel : ViewModel() {

    val recording: StateFlow<Boolean> = CaptureState.recording
    val startedAt: StateFlow<Long> = CaptureState.startedAt
    val lastScreenshot = CaptureState.lastScreenshot
    val lastVideo = CaptureState.lastVideo
    val message = CaptureState.message

    fun screenshot() = CaptureRequester.screenshot()

    fun record(quality: String, mic: Boolean) = CaptureRequester.record(quality, mic)

    fun stopRecording() {
        viewModelScope.launch {
            val ctx = RedCoreApp.instance
            try {
                ctx.startService(
                    Intent(ctx, CaptureService::class.java)
                        .setAction(CaptureService.ACTION_STOP_RECORDING)
                )
            } catch (_: Throwable) {
                CaptureState.note("The recorder is already stopped")
            }
        }
    }

    fun clearMessage() = CaptureState.clearMessage()
}
