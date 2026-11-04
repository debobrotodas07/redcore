package com.redcore.gameengine.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val customIconUri: String? = null,
    val manual: Boolean = false,
    val favorite: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null,
    val totalPlaySeconds: Long = 0L,
    val launchCount: Int = 0,
    /** Games detected from PackageManager vs. user added entries. */
    val detected: Boolean = true
)

@Entity(tableName = "game_profiles")
data class GameProfileEntity(
    @PrimaryKey val packageName: String,
    val profileId: String = PerformanceProfile.BALANCED.id,
    val crosshairId: String = CrosshairDesign.RING_DOT.id,
    val crosshairColor: Long = 0xFFFF2638,
    val crosshairSize: Int = 28,
    val crosshairOpacity: Int = 85,
    val audioPresetId: String = AudioPreset.FLAT.id,
    val brightnessPct: Int = 55,
    val refreshHz: Int = 0,
    val sidebarToolsCsv: String? = null,
    val notes: String = ""
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val gameLabel: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val durationSeconds: Long = 0,
    val profileId: String = PerformanceProfile.BALANCED.id
)
