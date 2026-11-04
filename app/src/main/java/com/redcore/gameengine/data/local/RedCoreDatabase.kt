package com.redcore.gameengine.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.redcore.gameengine.data.model.GameEntity
import com.redcore.gameengine.data.model.GameProfileEntity
import com.redcore.gameengine.data.model.SessionEntity

@Database(
    entities = [GameEntity::class, GameProfileEntity::class, SessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RedCoreDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun sessionDao(): SessionDao
    abstract fun profileDao(): GameProfileDao

    companion object {
        @Volatile
        private var instance: RedCoreDatabase? = null

        fun get(context: Context): RedCoreDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RedCoreDatabase::class.java,
                    "redcore.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
