package com.redcore.gameengine.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.redcore.gameengine.data.model.GameEntity
import com.redcore.gameengine.data.model.GameProfileEntity
import com.redcore.gameengine.data.model.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY favorite DESC, lastPlayedAt DESC, label ASC")
    fun observeAll(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE packageName = :pkg")
    suspend fun find(pkg: String): GameEntity?

    @Query("SELECT * FROM games")
    suspend fun getAll(): List<GameEntity>

    @Upsert
    suspend fun upsert(game: GameEntity)

    @Upsert
    suspend fun upsertAll(games: List<GameEntity>)

    @Delete
    suspend fun delete(game: GameEntity)

    @Query("UPDATE games SET lastPlayedAt = :time WHERE packageName = :pkg")
    suspend fun markPlayed(pkg: String, time: Long)

    @Query("UPDATE games SET favorite = :fav WHERE packageName = :pkg")
    suspend fun setFavorite(pkg: String, fav: Boolean)

    @Query("UPDATE games SET label = :label, customIconUri = :icon WHERE packageName = :pkg")
    suspend fun updateLabelAndIcon(pkg: String, label: String, icon: String?)

    @Query("SELECT COUNT(*) FROM games")
    suspend fun count(): Int
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 40): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC")
    suspend fun openSessions(): List<SessionEntity>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM sessions")
    fun observeTotalSeconds(): Flow<Long>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM sessions WHERE startedAt >= :since")
    fun observeSecondsSince(since: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Query("UPDATE sessions SET endedAt = :end, durationSeconds = :duration WHERE id = :id")
    suspend fun close(id: Long, end: Long, duration: Long)

    @Query("DELETE FROM sessions")
    suspend fun clearAll()
}

@Dao
interface GameProfileDao {
    @Query("SELECT * FROM game_profiles WHERE packageName = :pkg")
    suspend fun find(pkg: String): GameProfileEntity?

    @Query("SELECT * FROM game_profiles WHERE packageName = :pkg")
    fun observe(pkg: String): Flow<GameProfileEntity?>

    @Query("SELECT * FROM game_profiles")
    suspend fun all(): List<GameProfileEntity>

    @Upsert
    suspend fun upsert(profile: GameProfileEntity)

    @Query("DELETE FROM game_profiles WHERE packageName = :pkg")
    suspend fun delete(pkg: String)

    @Query("DELETE FROM game_profiles")
    suspend fun clearAll()
}
