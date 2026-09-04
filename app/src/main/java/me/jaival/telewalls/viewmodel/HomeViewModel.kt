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

import kotlinx.coroutines.flow.map

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

    val wallpapers: StateFlow<List<Wallpaper>> = _selectedCategory
        .flatMapLatest { category ->
            if (_searchQuery.value.isNotBlank()) {
                wallpaperRepository.searchWallpapers(_searchQuery.value)
            } else {
                wallpaperRepository.getWallpapersByCategory(category)
            }
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
        syncWallpapers()
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        // re-trigger query filter
        _selectedCategory.value = _selectedCategory.value
    }

    fun syncWallpapers() {
        viewModelScope.launch {
            _isRefreshing.value = true
            authRepository.activeChannelIdFlow.collect { chatId ->
                val channel = chatId ?: 99999L
                wallpaperRepository.syncWallpapersFromChannel(channel)
                _isRefreshing.value = false
            }
        }
    }

    fun toggleFavorite(wallpaperId: String) {
        viewModelScope.launch {
            wallpaperRepository.toggleFavorite(wallpaperId)
        }
    }
}
