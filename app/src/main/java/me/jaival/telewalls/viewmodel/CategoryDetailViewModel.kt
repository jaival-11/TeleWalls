package me.jaival.telewalls.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.jaival.telewalls.data.repository.Wallpaper
import me.jaival.telewalls.data.repository.WallpaperRepository
import javax.inject.Inject

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val categoryName: String = savedStateHandle.get<String>("categoryName")?.let {
        Uri.decode(it)
    } ?: "Category"

    val wallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository
        .getWallpapersByCategory(categoryName)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categories: StateFlow<List<String>> = wallpaperRepository.categories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WallpaperRepository.DEFAULT_CATEGORIES
        )

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

    fun deleteWallpapers(wallpapers: List<Wallpaper>, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val deletedCount = wallpaperRepository.deleteWallpapers(wallpapers)
            onComplete(deletedCount)
        }
    }

    fun batchUpdateWallpapers(
        wallpapers: List<Wallpaper>,
        author: String?,
        category: String?,
        tags: List<String>?,
        wallpaperType: String?,
        onComplete: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val updatedCount = wallpaperRepository.batchUpdateWallpaperMetadata(
                wallpapers = wallpapers,
                author = author,
                category = category,
                tags = tags,
                wallpaperType = wallpaperType
            )
            onComplete(updatedCount)
        }
    }
}

