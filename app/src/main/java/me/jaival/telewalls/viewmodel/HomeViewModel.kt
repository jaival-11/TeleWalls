package me.jaival.telewalls.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.jaival.telewalls.data.repository.AuthRepository
import me.jaival.telewalls.data.repository.Wallpaper
import me.jaival.telewalls.data.repository.WallpaperRepository
import javax.inject.Inject

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val categories: StateFlow<List<String>> = wallpaperRepository.categories
        .map { list -> listOf("All") + list }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf("All") + WallpaperRepository.DEFAULT_CATEGORIES
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    val wallpapers: StateFlow<List<Wallpaper>> = combine(_selectedCategory, _searchQuery) { category, query ->
        Pair(category, query)
    }
    .flatMapLatest { (category, query) ->
        wallpaperRepository.searchWallpapers(query = query, category = category)
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favorites: StateFlow<List<Wallpaper>> = wallpaperRepository.favoriteWallpapers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        reindexChannel()
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun syncWallpapers() {
        reindexChannel()
    }

    fun reindexChannel() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            val chatId = authRepository.activeChannelIdFlow.firstOrNull() ?: 99999L
            val result = wallpaperRepository.reindexFromChannel(chatId)
            _isRefreshing.value = false

            result.onSuccess { (wallpapersCount, categoriesCount) ->
                _toastEvent.emit("Vault reindexed: $wallpapersCount wallpapers & $categoriesCount categories updated")
            }.onFailure { error ->
                _toastEvent.emit("Failed to reindex Vault channel: ${error.message ?: "Unknown error"}")
            }
        }
    }

    fun toggleFavorite(wallpaperId: String) {
        viewModelScope.launch {
            wallpaperRepository.toggleFavorite(wallpaperId)
        }
    }

    fun loadThumbnailOnDemand(wallpaper: Wallpaper) {
        viewModelScope.launch {
            wallpaperRepository.loadThumbnailOnDemand(wallpaper)
        }
    }
}
