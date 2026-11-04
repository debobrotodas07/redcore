package com.redcore.gameengine.service

import android.app.Notification
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import com.redcore.gameengine.util.Notifications
import java.io.File
import java.io.OutputStream

/**
 * Screen capture pipeline.
 *
 * Android 14 rules honoured here:
 *  - the foreground service with type mediaProjection starts BEFORE getMediaProjection()
 *  - the user's consent token is single-use, so we release the projection when done
 */
class CaptureService : Service() {

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private var width = 1080
    private var height = 1920
    private var density = 1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent.action) {
            ACTION_SCREENSHOT -> {
                startForegroundSafely(buildNotification("Preparing screenshot…", false))
                handleScreenshot(intent)
            }
            ACTION_START_RECORDING -> {
                startForegroundSafely(buildRecordingNotification("Recording starting…", 0))
                handleStartRecording(intent)
            }
            ACTION_STOP_RECORDING -> {
                if (CaptureState.recording.value) {
                    stopRecording()
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP -> stopSelf()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    // ---------------- screenshot ----------------

    private fun handleScreenshot(intent: Intent) {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val code = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (data == null || code == 0) {
            CaptureState.note("Screen capture consent was not granted")
            stopSelf()
            return
        }
        try {
            val metrics = metrics()
            width = metrics.widthPixels
            height = metrics.heightPixels
            density = metrics.densityDpi

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader = reader
            val proj = manager.getMediaProjection(code, data)
            projection = proj
            virtualDisplay = proj.createVirtualDisplay(
                "RedCoreScreenshot",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface, null, null
            )
            startHandler()
            handler?.postDelayed({ grabFrame() }, 700L)
        } catch (t: Throwable) {
            CaptureState.note("Screenshot failed: ${t.message}")
            stopSelf()
        }
    }

    private fun grabFrame() {
        try {
            val image = imageReader?.acquireLatestImage()
            if (image == null) {
                CaptureState.note("Screenshot failed: no frame available")
                releaseProjection()
                stopSelf()
                return
            }
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width
            val bmpWidth = width + rowPadding / pixelStride
            val bmp = Bitmap.createBitmap(bmpWidth, height, Bitmap.Config.ARGB_8888)
            bmp.copyPixelsFromBuffer(buffer)
            val cropped = Bitmap.createBitmap(bmp, 0, 0, width, height)
            saveBitmap(cropped)
            bmp.recycle()
            cropped.recycle()
            image.close()
        } catch (t: Throwable) {
            CaptureState.note("Screenshot failed: ${t.message}")
        } finally {
            releaseProjection()
            stopSelf()
        }
    }

    private fun saveBitmap(bitmap: Bitmap) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "REDCORE_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/REDCORE"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri = contentResolver.insert(collection, values)
        if (uri == null) {
            CaptureState.note("Could not create the screenshot file")
            return
        }
        try {
            val out: OutputStream? = contentResolver.openOutputStream(uri)
            out?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
            CaptureState.screenshotSaved(uri)
        } catch (t: Throwable) {
            contentResolver.delete(uri, null, null)
            CaptureState.note("Saving the screenshot failed: ${t.message}")
        }
    }

    // ---------------- recording ----------------

    private fun handleStartRecording(intent: Intent) {
        if (recorder != null) {
            // Already recording — toggle means stop.
            stopRecording()
            return
        }
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val code = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (data == null || code == 0) {
            CaptureState.note("Screen recording consent was not granted")
            stopSelf()
            return
        }
        val withMic = intent.getBooleanExtra(EXTRA_RECORD_MIC, false)
        val quality = intent.getStringExtra(EXTRA_QUALITY) ?: "1080"

        try {
            val metrics = metrics()
            val scale = when (quality) {
                "720" -> 0.66f
                "480" -> 0.44f
                else -> 1f
            }
            width = (metrics.widthPixels * scale).toInt().let { it - (it % 2) }
            height = (metrics.heightPixels * scale).toInt().let { it - (it % 2) }
            density = metrics.densityDpi

            val dir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "recordings")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "REDCORE_${System.currentTimeMillis()}.mp4")
            recordingFile = file

            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            applySource(rec, withMic)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            rec.setVideoSize(width, height)
            rec.setVideoFrameRate(60)
            rec.setVideoEncodingBitRate(bitRateFor(width, height))
            if (withMic) {
                rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                rec.setAudioEncodingBitRate(128_000)
                rec.setAudioSamplingRate(44_100)
            }
            rec.setOutputFile(file.absolutePath)
            rec.prepare()

            val proj = manager.getMediaProjection(code, data)
            projection = proj
            virtualDisplay = proj.createVirtualDisplay(
                "RedCoreRecorder",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                rec.surface, null, null
            )
            rec.start()
            recorder = rec
            CaptureState.setRecording(true, System.currentTimeMillis())
            updateNotification(buildRecordingNotification("Recording ${width}×$height", 1))
        } catch (t: Throwable) {
            CaptureState.note("Recording failed: ${t.message}")
            cleanupRecording()
            stopSelf()
        }
    }

    private fun applySource(rec: MediaRecorder, withMic: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rec.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        } else {
            @Suppress("DEPRECATION")
            rec.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        }
        // Android has no "no audio source" constant — leaving setAudioSource uncalled
        // is what produces a video-only recording.
        if (withMic) {
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
        }
    }

    private fun bitRateFor(w: Int, h: Int): Int {
        val pixels = w * h
        return when {
            pixels >= 1920 * 1080 -> 12_000_000
            pixels >= 1280 * 720 -> 6_000_000
            else -> 3_000_000
        }
    }

    private fun stopRecording() {
        val file = recordingFile
        try {
            recorder?.stop()
        } catch (t: Throwable) {
            CaptureState.note("Recording was too short to save")
        }
        cleanupRecording()
        if (file != null && file.exists() && file.length() > 0) {
            publishVideo(file)
            file.delete()
        }
        CaptureState.setRecording(false)
        stopSelf()
    }

    private fun publishVideo(file: File) {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/REDCORE")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val uri = contentResolver.insert(collection, values) ?: return
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
            CaptureState.videoSaved(uri)
        } catch (t: Throwable) {
            contentResolver.delete(uri, null, null)
            CaptureState.note("Saving the recording failed: ${t.message}")
        }
    }

    private fun cleanupRecording() {
        try {
            recorder?.reset()
            recorder?.release()
        } catch (_: Throwable) {
        }
        recorder = null
        releaseProjection()
    }

    private fun releaseProjection() {
        try {
            virtualDisplay?.release()
        } catch (_: Throwable) {
        }
        virtualDisplay = null
        try {
            imageReader?.close()
        } catch (_: Throwable) {
        }
        imageReader = null
        try {
            projection?.stop()
        } catch (_: Throwable) {
        }
        projection = null
        handlerThread?.quitSafely()
        handlerThread = null
        handler = null
    }

    private fun metrics(): DisplayMetrics {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            metrics.widthPixels = bounds.width()
            metrics.heightPixels = bounds.height()
            metrics.densityDpi = resources.displayMetrics.densityDpi
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
        }
        return metrics
    }

    private fun startHandler() {
        if (handlerThread == null) {
            handlerThread = HandlerThread("redcore-capture").also { it.start() }
            handler = Handler(handlerThread!!.looper)
        }
    }

    private fun buildNotification(text: String, ongoing: Boolean): android.app.Notification =
        Notifications.captureNotification(this, "REDCORE Capture", text, ongoing)

    private fun buildRecordingNotification(text: String, kind: Int): android.app.Notification =
        Notifications.captureNotification(this, "REDCORE Capture", text, true, stopAction = kind == 1)

    private var lastNotification: Notification? = null

    private fun startForegroundSafely(notification: android.app.Notification) {
        lastNotification = notification
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    Notifications.ID_CAPTURE,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(Notifications.ID_CAPTURE, notification)
            }
        } catch (t: Throwable) {
            CaptureState.note("Android blocked the capture service: ${t.message}")
            stopSelf()
        }
    }

    private fun updateNotification(notification: Notification) {
        lastNotification = notification
        try {
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(Notifications.ID_CAPTURE, notification)
        } catch (_: Throwable) {
        }
    }

    override fun onDestroy() {
        if (CaptureState.recording.value) {
            try {
                recorder?.stop()
            } catch (_: Throwable) {
            }
            CaptureState.setRecording(false)
        }
        cleanupRecording()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SCREENSHOT = "com.redcore.gameengine.SCREENSHOT"
        const val ACTION_START_RECORDING = "com.redcore.gameengine.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.redcore.gameengine.STOP_RECORDING"
        const val ACTION_STOP = "com.redcore.gameengine.STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_QUALITY = "quality"
        const val EXTRA_RECORD_MIC = "record_mic"

        fun screenshotIntent(context: Context, code: Int, data: Intent): Intent =
            Intent(context, CaptureService::class.java).apply {
                action = ACTION_SCREENSHOT
                putExtra(EXTRA_RESULT_CODE, code)
                putExtra(EXTRA_RESULT_DATA, data)
            }

        fun recordIntent(context: Context, code: Int, data: Intent, quality: String, mic: Boolean): Intent =
            Intent(context, CaptureService::class.java).apply {
                action = ACTION_START_RECORDING
                putExtra(EXTRA_RESULT_CODE, code)
                putExtra(EXTRA_RESULT_DATA, data)
                putExtra(EXTRA_QUALITY, quality)
                putExtra(EXTRA_RECORD_MIC, mic)
            }
    }
}
