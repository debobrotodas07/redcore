package com.redcore.gameengine

import android.content.Context
import com.redcore.gameengine.data.local.RedCoreDatabase
import com.redcore.gameengine.data.local.SettingsRepository
import com.redcore.gameengine.data.repo.BackupManager
import com.redcore.gameengine.data.repo.GameRepository
import com.redcore.gameengine.sys.DeviceProbe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Small manual graph — one place that owns the singletons. */
class ServiceLocator(context: Context) {
    private val appContext = context.applicationContext
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database: RedCoreDatabase = RedCoreDatabase.get(appContext)
    val settings: SettingsRepository = SettingsRepository(appContext, scope)
    val games: GameRepository = GameRepository(appContext, database)
    val backup: BackupManager = BackupManager(appContext, database, settings)
    val probe: DeviceProbe by lazy { DeviceProbe(appContext) }
}
