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

import android.util.Log
import me.jaival.telewalls.BuildConfig

import me.jaival.telewalls.core.telegram.TelegramAuthState
import me.jaival.telewalls.core.telegram.TelegramClient

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val authRepository: AuthRepository,
    private val telegramClient: TelegramClient
) : ViewModel() {

    private val _selectedCategories = MutableStateFlow<Set<String>>(setOf("All"))
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories.asStateFlow()

    val selectedCategory: StateFlow<String> = _selectedCategories.map { set ->
        if (set.isEmpty() || set.any { it.equals("All", ignoreCase = true) }) "All" else set.first()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "All"
    )

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

    val wallpapers: StateFlow<List<Wallpaper>> = combine(_selectedCategories, _searchQuery) { categories, query ->
        Pair(categories, query)
    }
    .flatMapLatest { (categories, query) ->
        wallpaperRepository.searchWallpapers(query = query, categories = categories)
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

    private var hasInitialReindexed = false

    init {
        viewModelScope.launch {
            combine(telegramClient.authState, authRepository.activeChannelIdFlow) { state, channelId ->
                Pair(state, channelId)
            }.collect { (authState, chatId) ->
                if (authState is TelegramAuthState.Ready && chatId != null && chatId != 0L) {
                    if (!hasInitialReindexed) {
                        hasInitialReindexed = true
                        reindexChannel()
                    }
                }
            }
        }
    }

    fun selectCategory(category: String) {
        if (category.equals("All", ignoreCase = true)) {
            _selectedCategories.value = setOf("All")
            return
        }

        val currentSet = _selectedCategories.value.filterNot { it.equals("All", ignoreCase = true) }.toMutableSet()
        val existing = currentSet.find { it.equals(category, ignoreCase = true) }
        if (existing != null) {
            currentSet.remove(existing)
        } else {
            currentSet.add(category)
        }

        if (currentSet.isEmpty()) {
            _selectedCategories.value = setOf("All")
        } else {
            _selectedCategories.value = currentSet
        }
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
            val chatId = authRepository.activeChannelIdFlow.firstOrNull() ?: 0L
            if (chatId == 0L) {
                if (BuildConfig.DEBUG) {
                    Log.d("HomeViewModel", "[REINDEX DEBUG] activeChannelId is 0, skipping reindex")
                }
                return@launch
            }
            if (telegramClient.authState.value !is TelegramAuthState.Ready) {
                if (BuildConfig.DEBUG) {
                    Log.d("HomeViewModel", "[REINDEX DEBUG] Telegram client is not Ready yet, skipping reindex")
                }
                _toastEvent.emit("Telegram client is connecting, please wait...")
                return@launch
            }
            _isRefreshing.value = true
            if (BuildConfig.DEBUG) {
                Log.d("HomeViewModel", "[REINDEX DEBUG] Triggered reindexChannel for chatId=$chatId")
            }
            val result = wallpaperRepository.reindexFromChannel(chatId)
            _isRefreshing.value = false

            result.onSuccess { (wallpapersCount, categoriesCount) ->
                if (BuildConfig.DEBUG) {
                    Log.d("HomeViewModel", "[REINDEX DEBUG] Reindex succeeded: wallpapers=$wallpapersCount, categories=$categoriesCount")
                }
                _toastEvent.emit("Vault reindexed: $wallpapersCount wallpapers & $categoriesCount categories updated")
            }.onFailure { error ->
                if (BuildConfig.DEBUG) {
                    Log.d("HomeViewModel", "[REINDEX DEBUG] Reindex failed: ${error.message}", error)
                }
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
