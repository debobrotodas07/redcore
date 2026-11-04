package com.redcore.gameengine.sys

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import java.io.File
import kotlin.math.roundToInt

data class DeviceSnapshot(
    val model: String = "",
    val manufacturer: String = "",
    val androidRelease: String = "",
    val sdkInt: Int = 0,
    val soc: String = "",
    val cores: Int = 0,
    val cpuUsagePercent: Float? = null,
    val cpuTempC: Float? = null,
    val batteryPercent: Int = 0,
    val batteryTempC: Float? = null,
    val charging: Boolean = false,
    val memTotalMb: Long = 0,
    val memUsedMb: Long = 0,
    val thermalStatus: String = "Unknown",
    val rxBytesPerSec: Long = 0,
    val txBytesPerSec: Long = 0,
    val uptimeMs: Long = 0,
    val freeStorageGb: Float = 0f,
    val refreshHz: Float = 0f
) {
    val memUsedPercent: Int
        get() = if (memTotalMb <= 0) 0 else ((memUsedMb.toFloat() / memTotalMb) * 100).roundToInt()
}

/**
 * Reads real device telemetry from public Android APIs and readable /sys + /proc nodes.
 * Every field is nullable where the platform may refuse access (SELinux), so the UI
 * can say "unavailable" instead of inventing a number.
 */
class DeviceProbe(private val context: Context) {

    private var lastTotal = 0L
    private var lastIdle = 0L
    private var lastRx = -1L
    private var lastTx = -1L
    private var lastNetAt = 0L

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    fun snapshot(): DeviceSnapshot {
        val battery = batteryInfo()
        val mem = memory()
        val net = network()
        val thermal = thermalStatus()
        return DeviceSnapshot(
            model = Build.MODEL ?: "",
            manufacturer = Build.MANUFACTURER ?: "",
            androidRelease = Build.VERSION.RELEASE ?: "",
            sdkInt = Build.VERSION.SDK_INT,
            soc = socName(),
            cores = Runtime.getRuntime().availableProcessors(),
            cpuUsagePercent = cpuUsage(),
            cpuTempC = cpuTemp(),
            batteryPercent = battery.first,
            batteryTempC = battery.second,
            charging = battery.third,
            memTotalMb = mem.first,
            memUsedMb = mem.second,
            thermalStatus = thermal,
            rxBytesPerSec = net.first,
            txBytesPerSec = net.second,
            uptimeMs = SystemClock.elapsedRealtime(),
            freeStorageGb = freeStorage(),
            refreshHz = refreshRate()
        )
    }

    private fun batteryInfo(): Triple<Int, Float?, Boolean> {
        val intent: Intent? = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        if (intent == null) return Triple(0, null, false)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val pct = if (level >= 0 && scale > 0) (level * 100f / scale).roundToInt() else 0
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temp = if (tempTenths == Int.MIN_VALUE || tempTenths <= 0) null else tempTenths / 10f
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return Triple(pct, temp, charging)
    }

    private fun memory(): Pair<Long, Long> {
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        val totalMb = info.totalMem / (1024 * 1024)
        val availMb = info.availMem / (1024 * 1024)
        return totalMb to (totalMb - availMb)
    }

    /** Reads total/idle jiffies; returns null when /proc/stat is unreadable. */
    private fun cpuUsage(): Float? {
        return try {
            val line = File("/proc/stat").useLines { it.firstOrNull { l -> l.startsWith("cpu ") } }
                ?: return null
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 8) return null
            val values = parts.drop(1).mapNotNull { it.toLongOrNull() }
            if (values.size < 5) return null
            val idle = values[3] + if (values.size > 4) values[4] else 0
            val total = values.sum()
            val dTotal = total - lastTotal
            val dIdle = idle - lastIdle
            lastTotal = total
            lastIdle = idle
            if (dTotal <= 0) null else ((dTotal - dIdle).toFloat() / dTotal * 100f).coerceIn(0f, 100f)
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * Temperature: scans thermal zones for a CPU/GPU soak label. Returns null when
     * no zone is readable (common on Android 12+ without elevated privileges).
     */
    private fun cpuTemp(): Float? {
        val base = File("/sys/class/thermal")
        val zones = base.listFiles { f -> f.name.startsWith("thermal_zone") } ?: return null
        var best: Float? = null
        for (zone in zones) {
            try {
                val type = File(zone, "type").readText().trim().lowercase()
                if (!(type.contains("cpu") || type.contains("gpu") || type.contains("soc") ||
                        type.contains("tsens") || type.contains("cluster"))
                ) continue
                val raw = File(zone, "temp").readText().trim().toFloatOrNull() ?: continue
                val celsius = if (raw > 1000f) raw / 1000f else if (raw > 200f) raw / 10f else raw
                if (celsius in -10f..125f && (best == null || celsius > best!!)) best = celsius
            } catch (_: Throwable) {
                // zone not readable — keep scanning
            }
        }
        return best
    }

    private fun thermalStatus(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "Unavailable (API < 29)"
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            when (pm.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "Nominal"
                PowerManager.THERMAL_STATUS_LIGHT -> "Light"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown imminent"
                else -> "Unknown"
            }
        } catch (t: Throwable) {
            "Unavailable"
        }
    }

    private fun socName(): String {
        val field = try {
            Build::class.java.getField("SOC_MODEL").get(null) as? String
        } catch (t: Throwable) {
            null
        }
        if (!field.isNullOrBlank()) return field
        val hardware = try {
            Build::class.java.getField("HARDWARE").get(null) as? String
        } catch (t: Throwable) {
            null
        }
        val fromProc = try {
            File("/proc/cpuinfo").useLines { lines ->
                lines.firstOrNull { it.startsWith("Hardware") }?.substringAfter(':')?.trim()
            }
        } catch (t: Throwable) {
            null
        }
        return listOfNotNull(hardware, fromProc).firstOrNull { !it.isNullOrBlank() }
            ?: "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    private fun network(): Pair<Long, Long> {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        if (rx < 0 || tx < 0) return 0L to 0L
        val now = SystemClock.elapsedRealtime()
        if (lastRx < 0) {
            lastRx = rx; lastTx = tx; lastNetAt = now
            return 0L to 0L
        }
        val dt = (now - lastNetAt).coerceAtLeast(1)
        val rxRate = ((rx - lastRx) * 1000 / dt).coerceAtLeast(0)
        val txRate = ((tx - lastTx) * 1000 / dt).coerceAtLeast(0)
        lastRx = rx; lastTx = tx; lastNetAt = now
        return rxRate to txRate
    }

    private fun freeStorage(): Float {
        return try {
            val stat = StatFs(Environment.getDataDirectory().absolutePath)
            (stat.availableBytes / (1024f * 1024f * 1024f))
        } catch (t: Throwable) {
            0f
        }
    }

    private fun refreshRate(): Float {
        return try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE)
                as android.view.WindowManager
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                wm.defaultDisplay
            }
            @Suppress("DEPRECATION")
            display?.refreshRate ?: 0f
        } catch (t: Throwable) {
            0f
        }
    }
}
