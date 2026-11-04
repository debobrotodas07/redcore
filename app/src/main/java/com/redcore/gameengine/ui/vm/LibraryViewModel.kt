package com.redcore.gameengine.ui.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redcore.gameengine.RedCoreApp
import com.redcore.gameengine.data.model.AudioPreset
import com.redcore.gameengine.data.model.CrosshairDesign
import com.redcore.gameengine.data.model.GameEntity
import com.redcore.gameengine.data.model.GameProfileEntity
import com.redcore.gameengine.data.model.PerformanceProfile
import com.redcore.gameengine.data.model.SidebarTool
import com.redcore.gameengine.data.repo.AppEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LibrarySort(val label: String) { RECENT("Recent"), NAME("A–Z"), PLAYED("Most played"), FAVORITE("Favourites first") }

data class LibraryUiState(
    val games: List<GameEntity> = emptyList(),
    val query: String = "",
    val sort: LibrarySort = LibrarySort.RECENT,
    val loading: Boolean = true
)

data class AddableApp(val packageName: String, val label: String, val isGame: Boolean, val alreadyAdded: Boolean)

class LibraryViewModel : ViewModel() {

    private val locator = RedCoreApp.locator()

    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(LibrarySort.RECENT)
    private val loading = MutableStateFlow(true)

    private val _addable = MutableStateFlow<List<AddableApp>>(emptyList())
    val addable: StateFlow<List<AddableApp>> = _addable.asStateFlow()

    val state: StateFlow<LibraryUiState> = combine(
        locator.games.games, query, sort, loading
    ) { games, q, s, isLoading ->
        val filtered = games.filter {
            q.isBlank() || it.label.contains(q, ignoreCase = true) ||
                it.packageName.contains(q, ignoreCase = true)
        }
        val sorted = when (s) {
            LibrarySort.RECENT -> filtered.sortedByDescending { it.lastPlayedAt ?: 0L }
            LibrarySort.NAME -> filtered.sortedBy { it.label.lowercase() }
            LibrarySort.PLAYED -> filtered.sortedByDescending { it.totalPlaySeconds }
            LibrarySort.FAVORITE -> filtered.sortedWith(
                compareByDescending<GameEntity> { it.favorite }.thenBy { it.label.lowercase() }
            )
        }
        LibraryUiState(sorted, q, s, isLoading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), LibraryUiState())

    init {
        viewModelScope.launch {
            locator.games.syncDetectedGames()
            loading.value = false
        }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun setSort(value: LibrarySort) {
        sort.value = value
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            locator.games.syncDetectedGames()
            loading.value = false
        }
    }

    fun loadAddable() {
        viewModelScope.launch {
            val installed = locator.games.installedApps()
            val existing = state.value.games.map { it.packageName }.toSet()
            _addable.value = installed.map {
                AddableApp(it.packageName, it.label, it.isGame, existing.contains(it.packageName))
            }.sortedWith(compareByDescending<AddableApp> { it.isGame }.thenBy { it.label.lowercase() })
        }
    }

    fun add(packageName: String) {
        viewModelScope.launch {
            locator.games.addManual(packageName)
            loadAddable()
        }
    }

    fun remove(game: GameEntity) {
        viewModelScope.launch { locator.games.remove(game) }
    }

    fun toggleFavorite(game: GameEntity) {
        viewModelScope.launch { locator.games.setFavorite(game.packageName, !game.favorite) }
    }

    fun launch(context: Context, game: GameEntity) {
        viewModelScope.launch {
            val profile = locator.games.profileFor(game.packageName)
                ?.let { PerformanceProfile.fromId(it.profileId) }
                ?: locator.settings.current().globalProfile
            locator.games.launch(game.packageName, game.label, profile)
        }
    }
}

class GameDetailViewModel(private val packageName: String) : ViewModel() {

    private val locator = RedCoreApp.locator()

    private val _game = MutableStateFlow<GameEntity?>(null)
    val game: StateFlow<GameEntity?> = _game.asStateFlow()

    private val _profile = MutableStateFlow(GameProfileEntity(packageName))
    val profile: StateFlow<GameProfileEntity> = _profile.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        viewModelScope.launch {
            _game.value = locator.games.games.first()
                .firstOrNull { it.packageName == packageName }
                ?: locator.games.installedApps()
                    .firstOrNull { it.packageName == packageName }
                    ?.let { GameEntity(packageName = it.packageName, label = it.label) }
            val existing = locator.games.profileFor(packageName)
            _profile.value = existing ?: GameProfileEntity(
                packageName = packageName,
                profileId = locator.settings.current().globalProfile.id
            )
            _loaded.value = true
        }
    }

    fun updateProfile(profile: PerformanceProfile) {
        _profile.value = _profile.value.copy(profileId = profile.id)
        persist()
    }

    fun updateCrosshair(design: CrosshairDesign) {
        _profile.value = _profile.value.copy(crosshairId = design.id)
        persist()
    }

    fun updateCrosshairColor(color: Long) {
        _profile.value = _profile.value.copy(crosshairColor = color)
        persist()
    }

    fun updateCrosshairSize(size: Int) {
        _profile.value = _profile.value.copy(crosshairSize = size)
        persist()
    }

    fun updateCrosshairOpacity(opacity: Int) {
        _profile.value = _profile.value.copy(crosshairOpacity = opacity)
        persist()
    }

    fun updateAudio(preset: AudioPreset) {
        _profile.value = _profile.value.copy(audioPresetId = preset.id)
        persist()
    }

    fun updateBrightness(percent: Int) {
        _profile.value = _profile.value.copy(brightnessPct = percent)
        persist()
    }

    fun updateRefresh(hz: Int) {
        _profile.value = _profile.value.copy(refreshHz = hz)
        persist()
    }

    fun updateTools(tools: Collection<SidebarTool>) {
        _profile.value = _profile.value.copy(sidebarToolsCsv = SidebarTool.toCsv(tools))
        persist()
    }

    fun updateNotes(text: String) {
        _profile.value = _profile.value.copy(notes = text)
    }

    fun updateLabelAndIcon(label: String, iconUri: String?) {
        viewModelScope.launch {
            locator.games.updateLabelAndIcon(packageName, label, iconUri)
            _game.value = _game.value?.copy(label = label, customIconUri = iconUri)
        }
    }

    fun toggleFavorite() {
        val game = _game.value ?: return
        viewModelScope.launch {
            locator.games.setFavorite(packageName, !game.favorite)
            _game.value = game.copy(favorite = !game.favorite)
        }
    }

    fun launch(context: Context) {
        val game = _game.value ?: return
        viewModelScope.launch {
            locator.games.launch(
                game.packageName,
                game.label,
                PerformanceProfile.fromId(_profile.value.profileId)
            )
        }
    }

    fun remove(onDone: () -> Unit) {
        val game = _game.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { locator.games.remove(game) }
            onDone()
        }
    }

    private fun persist() {
        val value = _profile.value
        viewModelScope.launch { locator.games.saveProfile(value) }
    }
}
