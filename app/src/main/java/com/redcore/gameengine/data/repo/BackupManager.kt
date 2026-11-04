package com.redcore.gameengine.data.repo

import android.content.Context
import android.net.Uri
import com.redcore.gameengine.data.local.AppSettings
import com.redcore.gameengine.data.local.RedCoreDatabase
import com.redcore.gameengine.data.local.SettingsRepository
import com.redcore.gameengine.data.model.AccentTheme
import com.redcore.gameengine.data.model.AudioPreset
import com.redcore.gameengine.data.model.CrosshairDesign
import com.redcore.gameengine.data.model.GameEntity
import com.redcore.gameengine.data.model.GameProfileEntity
import com.redcore.gameengine.data.model.PerformanceProfile
import com.redcore.gameengine.data.model.SidebarSide
import com.redcore.gameengine.data.model.SidebarTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Settings + library backup as a plain JSON document the user owns
 * (created through the system file picker, never uploaded anywhere).
 */
class BackupManager(
    private val context: Context,
    private val db: RedCoreDatabase,
    private val settingsRepo: SettingsRepository
) {

    suspend fun export(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val s = settingsRepo.current()
            val root = JSONObject().apply {
                put("format", "redcore.backup")
                put("version", 1)
                put("createdAt", System.currentTimeMillis())
                put("settings", JSONObject().apply {
                    put("accent", s.accent.id)
                    put("animations", s.animations)
                    put("haptics", s.haptics)
                    put("notifications", s.notifications)
                    put("globalProfile", s.globalProfile.id)
                    put("sidebarSide", s.sidebarSide.id)
                    put("sidebarY", s.sidebarY.toDouble())
                    put("sidebarOpacity", s.sidebarOpacity)
                    put("sidebarCompact", s.sidebarCompact)
                    put("sidebarTools", SidebarTool.toCsv(s.sidebarTools))
                    put("crosshairDesign", s.crosshairDesign.id)
                    put("crosshairColor", s.crosshairColor)
                    put("crosshairSize", s.crosshairSize)
                    put("crosshairOpacity", s.crosshairOpacity)
                    put("audioPreset", s.audioPreset.id)
                    put("brightnessControl", s.brightnessControl)
                    put("keepScreenOn", s.keepScreenOn)
                    put("monitorIntervalMs", s.monitorIntervalMs)
                    put("recordingQuality", s.recordingQuality)
                })
                put("games", JSONArray().apply {
                    db.gameDao().getAll().forEach { g ->
                        put(JSONObject().apply {
                            put("packageName", g.packageName)
                            put("label", g.label)
                            put("customIconUri", g.customIconUri ?: "")
                            put("manual", g.manual)
                            put("favorite", g.favorite)
                            put("lastPlayedAt", g.lastPlayedAt ?: 0L)
                            put("totalPlaySeconds", g.totalPlaySeconds)
                        })
                    }
                })
                put("profiles", JSONArray().apply {
                    db.profileDao().all().forEach { p ->
                        put(JSONObject().apply {
                            put("packageName", p.packageName)
                            put("profileId", p.profileId)
                            put("crosshairId", p.crosshairId)
                            put("crosshairColor", p.crosshairColor)
                            put("crosshairSize", p.crosshairSize)
                            put("crosshairOpacity", p.crosshairOpacity)
                            put("audioPresetId", p.audioPresetId)
                            put("brightnessPct", p.brightnessPct)
                            put("refreshHz", p.refreshHz)
                            put("sidebarToolsCsv", p.sidebarToolsCsv ?: "")
                            put("notes", p.notes)
                        })
                    }
                })
            }
            val written = context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(root.toString(2).toByteArray())
                true
            } ?: false
            if (written) Result.success("Backup written (${root.getJSONArray("games").length()} games)")
            else Result.failure(IllegalStateException("Could not open the target file"))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun import(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                ?: return@withContext Result.failure(IllegalStateException("Could not read the file"))
            val root = JSONObject(text)
            if (root.optString("format") != "redcore.backup") {
                return@withContext Result.failure(IllegalArgumentException("Not a REDCORE backup file"))
            }
            root.optJSONObject("settings")?.let { s ->
                restoreSettings(s)
            }
            val games = root.optJSONArray("games")
            var restored = 0
            if (games != null) {
                for (i in 0 until games.length()) {
                    val o = games.optJSONObject(i) ?: continue
                    val pkg = o.optString("packageName")
                    if (pkg.isBlank()) continue
                    db.gameDao().upsert(
                        GameEntity(
                            packageName = pkg,
                            label = o.optString("label", pkg),
                            customIconUri = o.optString("customIconUri").ifBlank { null },
                            manual = o.optBoolean("manual"),
                            favorite = o.optBoolean("favorite"),
                            lastPlayedAt = o.optLong("lastPlayedAt").takeIf { it > 0 },
                            totalPlaySeconds = o.optLong("totalPlaySeconds")
                        )
                    )
                    restored++
                }
            }
            root.optJSONArray("profiles")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val pkg = o.optString("packageName")
                    if (pkg.isBlank()) continue
                    db.profileDao().upsert(
                        GameProfileEntity(
                            packageName = pkg,
                            profileId = o.optString("profileId", PerformanceProfile.BALANCED.id),
                            crosshairId = o.optString("crosshairId", CrosshairDesign.RING_DOT.id),
                            crosshairColor = o.optLong("crosshairColor", 0xFFFF2638),
                            crosshairSize = o.optInt("crosshairSize", 28),
                            crosshairOpacity = o.optInt("crosshairOpacity", 85),
                            audioPresetId = o.optString("audioPresetId", AudioPreset.FLAT.id),
                            brightnessPct = o.optInt("brightnessPct", 55),
                            refreshHz = o.optInt("refreshHz", 0),
                            sidebarToolsCsv = o.optString("sidebarToolsCsv").ifBlank { null },
                            notes = o.optString("notes")
                        )
                    )
                }
            }
            Result.success("Restored $restored games and their profiles")
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private suspend fun restoreSettings(s: JSONObject) {
        val repo = settingsRepo
        repo.setAccent(AccentTheme.fromId(s.optString("accent")))
        repo.setAnimations(s.optBoolean("animations", true))
        repo.setHaptics(s.optBoolean("haptics", true))
        repo.setNotifications(s.optBoolean("notifications", true))
        repo.setGlobalProfile(PerformanceProfile.fromId(s.optString("globalProfile")))
        repo.setSidebarSide(SidebarSide.fromId(s.optString("sidebarSide")))
        repo.setSidebarY(s.optDouble("sidebarY", 0.42).toFloat())
        repo.setSidebarOpacity(s.optInt("sidebarOpacity", 82))
        repo.setSidebarCompact(s.optBoolean("sidebarCompact", false))
        repo.setSidebarTools(SidebarTool.parseCsv(s.optString("sidebarTools")))
        repo.setCrosshairDesign(CrosshairDesign.fromId(s.optString("crosshairDesign")))
        repo.setCrosshairColor(s.optLong("crosshairColor", 0xFFFF2638))
        repo.setCrosshairSize(s.optInt("crosshairSize", 28))
        repo.setCrosshairOpacity(s.optInt("crosshairOpacity", 85))
        repo.setAudioPreset(AudioPreset.fromId(s.optString("audioPreset")))
        repo.setBrightnessControl(s.optBoolean("brightnessControl", true))
        repo.setKeepScreenOn(s.optBoolean("keepScreenOn", true))
        repo.setMonitorInterval(s.optInt("monitorIntervalMs", 1000))
        repo.setRecordingQuality(s.optString("recordingQuality", "1080"))
    }

    fun suggestedFileName(): String {
        val stamp = android.text.format.DateFormat.format("yyyyMMdd-HHmm", System.currentTimeMillis())
        return "redcore-backup-$stamp.json"
    }

    @Suppress("unused")
    private fun noop(settings: AppSettings) = settings
}
