package me.jaival.telewalls.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.jaival.telewalls.data.repository.SettingsRepository
import me.jaival.telewalls.data.repository.WallpaperRepository
import me.jaival.telewalls.data.repository.WallpaperTypeFilter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val wallpaperRepository: WallpaperRepository
) : ViewModel() {

    val wallpaperType: StateFlow<WallpaperTypeFilter> = settingsRepository.wallpaperTypeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WallpaperTypeFilter.BOTH)

    val reduceAnimations: StateFlow<Boolean> = settingsRepository.reduceAnimationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hiddenCategories: StateFlow<Set<String>> = settingsRepository.hiddenCategoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val syncFavorites: StateFlow<Boolean> = settingsRepository.syncFavoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val allCategories: StateFlow<List<String>> = wallpaperRepository.rawCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WallpaperRepository.DEFAULT_CATEGORIES)

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    private val _isClearingCache = MutableStateFlow(false)
    val isClearingCache: StateFlow<Boolean> = _isClearingCache.asStateFlow()

    init {
        refreshCacheSize()
    }

    fun setWallpaperType(type: WallpaperTypeFilter) {
        viewModelScope.launch {
            settingsRepository.setWallpaperType(type)
        }
    }

    fun setReduceAnimations(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReduceAnimations(enabled)
        }
    }

    fun toggleCategoryHidden(categoryName: String) {
        viewModelScope.launch {
            settingsRepository.toggleCategoryHidden(categoryName)
        }
    }

    fun setSyncFavorites(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSyncFavorites(enabled)
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            _cacheSizeBytes.value = wallpaperRepository.getCacheSizeBytes()
        }
    }

    fun clearCache(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isClearingCache.value = true
            wallpaperRepository.clearImageCache()
            _cacheSizeBytes.value = wallpaperRepository.getCacheSizeBytes()
            _isClearingCache.value = false
            onComplete()
        }
    }

    fun formatCacheSize(bytes: Long): String {
        if (bytes <= 0) return "0.00 KB"
        val doubleBytes = bytes.toDouble()
        val kb = doubleBytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
            else -> String.format(Locale.US, "%.2f KB", kb)
        }
    }
}
