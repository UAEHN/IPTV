package com.uaehn.shasha

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaehn.shasha.data.AppLanguage
import com.uaehn.shasha.data.BufferProfile
import com.uaehn.shasha.data.Catalog
import com.uaehn.shasha.data.CatalogRepository
import com.uaehn.shasha.data.Channel
import com.uaehn.shasha.data.Prefs
import com.uaehn.shasha.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RefreshState { Idle, Running, Done, Failed }

data class AppState(
    val catalog: Catalog = Catalog.Empty,
    val favorites: Set<String> = emptySet(),
    val recents: List<String> = emptyList(),
    val settings: Settings = Settings(),
    val loading: Boolean = true,
) {
    val favoriteChannels: List<Channel>
        get() = catalog.channels.filter { it.id in favorites }

    /** Recently watched, newest first, skipping ids no longer in the catalog. */
    val recentChannels: List<Channel>
        get() = recents.mapNotNull { id -> catalog.channel(id) }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = CatalogRepository(app)
    private val prefs = Prefs(app)

    private val catalog = MutableStateFlow(Catalog.Empty)
    private val loading = MutableStateFlow(true)

    private val _refresh = MutableStateFlow(RefreshState.Idle)
    val refresh: StateFlow<RefreshState> = _refresh.asStateFlow()

    val state: StateFlow<AppState> = combine(
        catalog, prefs.favorites, prefs.recents, prefs.settings, loading,
    ) { catalog, favorites, recents, settings, loading ->
        AppState(catalog, favorites, recents, settings, loading)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    init {
        viewModelScope.launch {
            catalog.value = repo.load()
            loading.value = false
        }
    }

    fun refreshCatalog() {
        if (_refresh.value == RefreshState.Running) return
        viewModelScope.launch {
            _refresh.value = RefreshState.Running
            repo.refresh()
                .onSuccess {
                    catalog.value = it
                    _refresh.value = RefreshState.Done
                }
                .onFailure { _refresh.value = RefreshState.Failed }
        }
    }

    fun toggleFavorite(id: String) = viewModelScope.launch { prefs.toggleFavorite(id) }
    fun noteWatched(id: String) = viewModelScope.launch { prefs.noteWatched(id) }
    fun clearRecents() = viewModelScope.launch { prefs.clearRecents() }
    fun setLanguage(language: AppLanguage) = viewModelScope.launch { prefs.setLanguage(language) }
    fun setBuffer(profile: BufferProfile) = viewModelScope.launch { prefs.setBuffer(profile) }
    fun setResume(enabled: Boolean) = viewModelScope.launch { prefs.setResumeLastChannel(enabled) }
}
