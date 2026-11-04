package com.redcore.gameengine.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.redcore.gameengine.RedCoreApp
import com.redcore.gameengine.MainActivity
import com.redcore.gameengine.data.local.AppSettings
import com.redcore.gameengine.data.model.CrosshairDesign
import com.redcore.gameengine.data.model.PerformanceProfile
import com.redcore.gameengine.data.model.SidebarSide
import com.redcore.gameengine.data.model.SidebarTool
import com.redcore.gameengine.sys.PermissionHelper
import com.redcore.gameengine.sys.ProfileApplier
import com.redcore.gameengine.ui.overlay.SidebarContent
import com.redcore.gameengine.ui.theme.RedCoreTheme
import com.redcore.gameengine.util.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground service that owns two overlay windows:
 *  1. the floating gaming sidebar (Compose, draggable, configurable)
 *  2. the crosshair overlay (a touch-through custom view)
 *
 * Overlay windows need SYSTEM_ALERT_WINDOW; the service type is specialUse
 * because Android 14 requires every FGS to declare what it is for.
 */
class OverlayService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager

    private var host: SidebarHost? = null
    private var sidebarParams: WindowManager.LayoutParams? = null
    private var crosshairView: CrosshairOverlayView? = null
    private var crosshairParams: WindowManager.LayoutParams? = null

    private var settings: AppSettings = AppSettings()
    private var dragY: Float = 0f
    private var screenHeight = 1920

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        screenHeight = resources.displayMetrics.heightPixels

        scope.launch {
            RedCoreApp.locator().settings.settings.collectLatest { updated ->
                settings = updated
                host?.settingsState?.value = updated
                host?.profileState?.value = updated.globalProfile
                if (updated.sidebarEnabled) {
                    ensureSidebar()
                } else {
                    removeSidebar()
                }
                updateCrosshair(updated)
                updateForegroundNotification(updated)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SYNC -> {
                val s = RedCoreApp.locator().settings.settings.value
                if (!s.sidebarEnabled && !s.crosshairEnabled) {
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }
        startInForeground()
        if (!PermissionHelper.overlay(this)) {
            Notifications.alert(
                this,
                "Overlay permission needed",
                "Allow \"Display over other apps\" to use the REDCORE sidebar."
            )
            stopSelf()
            return START_NOT_STICKY
        }
        if (settings.sidebarEnabled) ensureSidebar()
        updateCrosshair(settings)
        return START_STICKY
    }

    private fun startInForeground() {
        val notification = Notifications.overlayNotification(
            this,
            "${settings.globalProfile.label} profile active",
            actions = true
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    Notifications.ID_OVERLAY,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(Notifications.ID_OVERLAY, notification)
            }
        } catch (t: Throwable) {
            try {
                startForeground(Notifications.ID_OVERLAY, notification)
            } catch (_: Throwable) {
                stopSelf()
            }
        }
    }

    private fun updateForegroundNotification(s: AppSettings) {
        try {
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(Notifications.ID_OVERLAY, buildNotification(s))
        } catch (_: Throwable) {
        }
    }

    private fun buildNotification(s: AppSettings): Notification =
        Notifications.overlayNotification(this, "${s.globalProfile.label} profile active")

    // ---------------- sidebar ----------------

    private fun ensureSidebar() {
        if (host != null) {
            applySidebarParams(settings)
            return
        }
        val view = SidebarHost(this, actions)
        view.settingsState.value = settings
        view.profileState.value = settings.globalProfile
        val params = buildSidebarParams(settings)
        host = view
        sidebarParams = params
        try {
            windowManager.addView(view, params)
        } catch (t: Throwable) {
            host = null
            sidebarParams = null
            Notifications.alert(this, "Sidebar could not start", t.message ?: "Overlay was rejected")
        }
    }

    private fun buildSidebarParams(s: AppSettings): WindowManager.LayoutParams {
        val y = (s.sidebarY * (screenHeight - dp(320))).toInt().coerceAtLeast(0)
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or if (s.sidebarSide == SidebarSide.LEFT) Gravity.START else Gravity.END
            x = 0
            this.y = y
            dragY = y.toFloat()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun applySidebarParams(s: AppSettings) {
        val params = sidebarParams ?: return
        val hostView = host ?: return
        val currentlyRight = (params.gravity and Gravity.END) != 0
        val wantsRight = s.sidebarSide == SidebarSide.RIGHT
        params.gravity = Gravity.TOP or if (s.sidebarSide == SidebarSide.LEFT) Gravity.START else Gravity.END
        if (currentlyRight != wantsRight) {
            params.y = (s.sidebarY * (screenHeight - dp(320))).toInt().coerceAtLeast(0)
            dragY = params.y.toFloat()
        }
        try {
            windowManager.updateViewLayout(hostView, params)
        } catch (_: Throwable) {
        }
    }

    private fun removeSidebar() {
        host?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Throwable) {
            }
            it.dispose()
        }
        host = null
        sidebarParams = null
    }

    private val actions = object : SidebarActions {
        override fun drag(deltaY: Float) {
            val params = sidebarParams ?: return
            val view = host ?: return
            val max = (screenHeight - view.height.coerceAtLeast(dp(200))).coerceAtLeast(dp(120))
            dragY = (dragY + deltaY).coerceIn(0f, max.toFloat())
            params.y = dragY.toInt()
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: Throwable) {
            }
        }

        override fun dragFinished() {
            val max = (screenHeight - (host?.height ?: dp(200))).coerceAtLeast(1)
            val fraction = (dragY / max.toFloat()).coerceIn(0.05f, 0.95f)
            RedCoreApp.locator().settings.setSidebarY(fraction)
        }

        override fun setProfile(profile: PerformanceProfile) {
            val locator = RedCoreApp.locator()
            locator.settings.setGlobalProfile(profile)
            ProfileApplier.applyProfile(this@OverlayService, profile)
        }

        override fun setBrightness(percent: Int) {
            if (!PermissionHelper.writeSettings(this@OverlayService)) return
            val value = (percent.coerceIn(1, 100) / 100f * 255f).toInt().coerceIn(1, 255)
            try {
                android.provider.Settings.System.putInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                android.provider.Settings.System.putInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    value
                )
            } catch (_: Throwable) {
            }
        }

        override fun setVolume(percent: Int) = ProfileApplier.setMediaVolumePercent(this@OverlayService, percent)

        override fun openTool(tool: SidebarTool) {
            val route = when (tool) {
                SidebarTool.SCREENSHOT, SidebarTool.RECORD -> "capture"
                SidebarTool.QUICK_SETTINGS -> "settings"
                else -> null
            }
            val intent = Intent(this@OverlayService, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (route != null) putExtra(MainActivity.EXTRA_ROUTE, route)
            }
            try {
                startActivity(intent)
            } catch (_: Throwable) {
            }
        }

        override fun stopOverlay() {
            val settingsRepo = RedCoreApp.locator().settings
            scope.launch {
                settingsRepo.setSidebarEnabled(false)
                settingsRepo.setCrosshairEnabled(false)
                kotlinx.coroutines.delay(120)
                removeSidebar()
                removeCrosshair()
                stopSelf()
            }
        }
    }

    // ---------------- crosshair ----------------

    private fun updateCrosshair(s: AppSettings) {
        if (!s.crosshairEnabled) {
            removeCrosshair()
            return
        }
        if (crosshairView == null) {
            val view = CrosshairOverlayView(this)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }
            try {
                windowManager.addView(view, params)
                crosshairView = view
                crosshairParams = params
            } catch (_: Throwable) {
                return
            }
        }
        crosshairView?.configure(
            design = s.crosshairDesign,
            color = s.crosshairColor,
            sizeDp = s.crosshairSize,
            opacity = s.crosshairOpacity
        )
    }

    private fun removeCrosshair() {
        crosshairView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Throwable) {
            }
        }
        crosshairView = null
        crosshairParams = null
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        removeSidebar()
        removeCrosshair()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.redcore.gameengine.START_OVERLAY"
        const val ACTION_STOP = "com.redcore.gameengine.STOP_OVERLAY"
        const val ACTION_SYNC = "com.redcore.gameengine.SYNC_OVERLAY"

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java).setAction(ACTION_START)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Throwable) {
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java).setAction(ACTION_STOP)
            try {
                context.startService(intent)
            } catch (_: Throwable) {
            }
        }

        fun sync(context: Context) {
            val intent = Intent(context, OverlayService::class.java).setAction(ACTION_SYNC)
            try {
                context.startService(intent)
            } catch (_: Throwable) {
            }
        }
    }
}

interface SidebarActions {
    fun drag(deltaY: Float)
    fun dragFinished()
    fun setProfile(profile: PerformanceProfile)
    fun setBrightness(percent: Int)
    fun setVolume(percent: Int)
    fun openTool(tool: SidebarTool)
    fun stopOverlay()
}

/** Compose host that provides the view-tree owners a ComposeView requires. */
private class SidebarHost(
    context: Context,
    private val actions: SidebarActions
) : android.widget.FrameLayout(context),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val viewModelStore = ViewModelStore()

    val settingsState = mutableStateOf(AppSettings())
    val profileState = mutableStateOf(PerformanceProfile.BALANCED)
    val expandedState = mutableStateOf(false)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = viewModelStore

    init {
        setViewTreeLifecycleOwner(this)
        setViewTreeSavedStateRegistryOwner(this)
        setViewTreeViewModelStoreOwner(this)
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val compose = ComposeView(context).apply {
            setContent {
                val settings = settingsState.value
                RedCoreTheme(accentTheme = settings.accent) {
                    SidebarContent(
                        settings = settings,
                        profile = profileState.value,
                        side = settings.sidebarSide,
                        expanded = expandedState.value,
                        onToggleExpand = { expandedState.value = !expandedState.value },
                        onDrag = { dy -> actions.drag(dy) },
                        onDragEnd = { actions.dragFinished() },
                        onProfileChange = { actions.setProfile(it) },
                        onExternalTool = { actions.openTool(it) },
                        onBrightness = { actions.setBrightness(it) },
                        onVolume = { actions.setVolume(it) },
                        onStop = { actions.stopOverlay() }
                    )
                }
            }
        }
        addView(compose, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    fun dispose() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}

/** Touch-through crosshair window drawn with a plain Canvas. */
class CrosshairOverlayView(context: Context) : View(context) {

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private var design: CrosshairDesign = CrosshairDesign.RING_DOT
    private var color: Int = 0xFFFF2638.toInt()
    private var sizeDp: Int = 28
    private var opacity: Int = 85

    fun configure(design: CrosshairDesign, color: Long, sizeDp: Int, opacity: Int) {
        this.design = design
        this.color = color.toInt()
        this.sizeDp = sizeDp
        this.opacity = opacity
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val box = sizeDp * resources.displayMetrics.density
        val alpha = (opacity / 100f * 255).toInt().coerceIn(26, 255)
        val cx = width / 2f
        val cy = height / 2f
        val r = box / 2f
        val unit = box / 100f
        val strokeWidth = (unit * 4.2f).coerceAtLeast(2f)

        canvas.save()
        canvas.translate(cx, cy)

        val shadowColor = AndroidColor.argb((alpha * 0.55f).toInt(), 0, 0, 0)
        drawShape(canvas, box * 1.06f, shadowColor, strokeWidth * 1.5f)

        val main = AndroidColor.argb(alpha, AndroidColor.red(color), AndroidColor.green(color), AndroidColor.blue(color))
        drawShape(canvas, box, main, strokeWidth)
        canvas.restore()

        @Suppress("UNUSED_EXPRESSION")
        r
    }

    private fun drawShape(canvas: Canvas, box: Float, paintColor: Int, strokeWidth: Float) {
        val r = box / 2f
        val unit = box / 100f
        stroke.color = paintColor
        stroke.strokeWidth = strokeWidth
        fill.color = paintColor

        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            canvas.drawLine(x1, y1, x2, y2, stroke)
        }

        when (design) {
            CrosshairDesign.DOT -> {
                canvas.drawCircle(0f, 0f, unit * 7f, fill)
                canvas.drawCircle(0f, 0f, unit * 13f, stroke)
            }
            CrosshairDesign.CROSS -> {
                val gap = r * 0.18f
                val len = r * 0.52f
                line(0f, -gap, 0f, -gap - len)
                line(0f, gap, 0f, gap + len)
                line(-gap, 0f, -gap - len, 0f)
                line(gap, 0f, gap + len, 0f)
            }
            CrosshairDesign.RING -> canvas.drawCircle(0f, 0f, r * 0.62f, stroke)
            CrosshairDesign.RING_DOT -> {
                canvas.drawCircle(0f, 0f, r * 0.58f, stroke)
                canvas.drawCircle(0f, 0f, unit * 6.5f, fill)
            }
            CrosshairDesign.T_CROSS -> {
                val gap = r * 0.16f
                line(-r * 0.55f, 0f, -gap, 0f)
                line(gap, 0f, r * 0.55f, 0f)
                line(0f, gap, 0f, r * 0.62f)
                canvas.drawCircle(0f, 0f, unit * 4f, fill)
            }
            CrosshairDesign.CHEVRON -> {
                line(-r * 0.42f, -r * 0.30f, 0f, -r * 0.02f)
                line(r * 0.42f, -r * 0.30f, 0f, -r * 0.02f)
                line(0f, r * 0.20f, 0f, r * 0.55f)
            }
            CrosshairDesign.X_MARK -> {
                val len = r * 0.45f
                line(-len, -len, len, len)
                line(-len, len, len, -len)
            }
        }
    }
}
