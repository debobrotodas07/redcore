package com.redcore.gameengine.data.repo

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.redcore.gameengine.data.local.RedCoreDatabase
import com.redcore.gameengine.data.model.GameEntity
import com.redcore.gameengine.data.model.GameProfileEntity
import com.redcore.gameengine.data.model.PerformanceProfile
import com.redcore.gameengine.data.model.SessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

data class AppEntry(
    val packageName: String,
    val label: String,
    val isGame: Boolean
)

class GameRepository(
    private val context: Context,
    private val db: RedCoreDatabase
) {
    private val gameDao = db.gameDao()
    private val sessionDao = db.sessionDao()
    private val profileDao = db.profileDao()
    private val pm: PackageManager get() = context.packageManager

    val games: Flow<List<GameEntity>> = gameDao.observeAll()
    val recentSessions: Flow<List<SessionEntity>> = sessionDao.observeRecent(50)
    val totalPlaySeconds: Flow<Long> = sessionDao.observeTotalSeconds()

    /** Phase-4 scan: every launchable app, flagged when Android marks it as a game. */
    suspend fun installedApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = try {
            pm.queryIntentActivities(intent, 0)
        } catch (t: Throwable) {
            emptyList()
        }
        resolved.asSequence()
            .mapNotNull { it.activityInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .mapNotNull { info ->
                val label = try {
                    pm.getApplicationLabel(info).toString()
                } catch (t: Throwable) {
                    info.packageName
                }
                val isGame = (info.category == ApplicationInfo.CATEGORY_GAME) ||
                    (info.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                AppEntry(info.packageName, label, isGame)
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** Adds every detected game that is not in the library yet. Returns how many. */
    suspend fun syncDetectedGames(): Int = withContext(Dispatchers.IO) {
        val existing = gameDao.getAll().associateBy { it.packageName }
        val detected = installedApps().filter { it.isGame && existing[it.packageName] == null }
        if (detected.isEmpty()) return@withContext 0
        gameDao.upsertAll(
            detected.map {
                GameEntity(
                    packageName = it.packageName,
                    label = it.label,
                    manual = false,
                    detected = true
                )
            }
        )
        detected.size
    }

    suspend fun addManual(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (gameDao.find(packageName) != null) return@withContext false
        val label = try {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (t: Throwable) {
            packageName
        }
        gameDao.upsert(
            GameEntity(packageName = packageName, label = label, manual = true, detected = false)
        )
        true
    }

    suspend fun remove(entry: GameEntity) = withContext(Dispatchers.IO) {
        gameDao.delete(entry)
        profileDao.delete(entry.packageName)
    }

    suspend fun setFavorite(packageName: String, favorite: Boolean) =
        gameDao.setFavorite(packageName, favorite)

    suspend fun updateLabelAndIcon(packageName: String, label: String, iconUri: String?) =
        gameDao.updateLabelAndIcon(packageName, label, iconUri)

    suspend fun profileFor(packageName: String): GameProfileEntity? = profileDao.find(packageName)

    fun observeProfile(packageName: String): Flow<GameProfileEntity?> = profileDao.observe(packageName)

    suspend fun saveProfile(profile: GameProfileEntity) = profileDao.upsert(profile)

    suspend fun deleteProfile(packageName: String) = profileDao.delete(packageName)

    suspend fun launch(packageName: String, label: String, profile: PerformanceProfile): Boolean {
        return withContext(Dispatchers.IO) {
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent == null) {
                false
            } else {
                // Close a stale open session before starting a new one.
                closeOpenSessions()
                val now = System.currentTimeMillis()
                sessionDao.insert(
                    SessionEntity(
                        packageName = packageName,
                        gameLabel = label,
                        startedAt = now,
                        profileId = profile.id
                    )
                )
                gameDao.markPlayed(packageName, now)
                try {
                    context.startActivity(
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    true
                } catch (t: Throwable) {
                    false
                }
            }
        }
    }

    suspend fun closeOpenSessions() {
        val now = System.currentTimeMillis()
        sessionDao.openSessions().forEach { open ->
            val seconds = ((now - open.startedAt) / 1000L).coerceAtLeast(0)
            // Ignore accidental sub-10-second opens.
            if (seconds < 10) {
                sessionDao.close(open.id, now, 0)
            } else {
                sessionDao.close(open.id, now, seconds)
            }
        }
    }

    suspend fun clearSessions() = sessionDao.clearAll()

    suspend fun clearEverything() = withContext(Dispatchers.IO) {
        sessionDao.clearAll()
        profileDao.clearAll()
        gameDao.getAll().forEach { gameDao.delete(it) }
    }

    fun weeklySeconds(): Flow<Long> {
        val since = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        return sessionDao.observeSecondsSince(since)
    }

    fun combinedPlayTime(): Flow<Long> = combine(totalPlaySeconds, weeklySeconds()) { total, week ->
        if (week >= 0) total else total
    }
}
