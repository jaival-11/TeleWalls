package me.jaival.telewalls.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.jaival.telewalls.data.repository.Wallpaper
import me.jaival.telewalls.data.repository.WallpaperRepository
import javax.inject.Inject

data class WallpaperCollection(
    val categoryName: String,
    val wallpaperCount: Int,
    val coverWallpaper: Wallpaper?
)

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository
) : ViewModel() {

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    val categories: StateFlow<List<String>> = wallpaperRepository.categories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val collections: StateFlow<List<WallpaperCollection>> = combine(
        wallpaperRepository.categories,
        wallpaperRepository.allWallpapers
    ) { categories, wallpapers ->
        categories.map { categoryName ->
            val matching = wallpapers.filter { it.category.equals(categoryName, ignoreCase = true) }
            WallpaperCollection(
                categoryName = categoryName,
                wallpaperCount = matching.size,
                coverWallpaper = matching.firstOrNull()
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun moveCategoryUp(index: Int) {
        val currentList = categories.value.toMutableList()
        if (index > 0 && index < currentList.size) {
            val temp = currentList[index]
            currentList[index] = currentList[index - 1]
            currentList[index - 1] = temp
            reorderCategories(currentList)
        }
    }

    fun moveCategoryDown(index: Int) {
        val currentList = categories.value.toMutableList()
        if (index >= 0 && index < currentList.size - 1) {
            val temp = currentList[index]
            currentList[index] = currentList[index + 1]
            currentList[index + 1] = temp
            reorderCategories(currentList)
        }
    }

    fun reorderCategories(newList: List<String>) {
        viewModelScope.launch {
            val success = wallpaperRepository.reorderCategories(newList)
            if (!success) {
                _toastEvent.emit("Failed to reorder categories on Telegram")
            }
        }
    }

    fun deleteCategory(categoryName: String) {
        viewModelScope.launch {
            val success = wallpaperRepository.deleteCategory(categoryName)
            if (!success) {
                _toastEvent.emit("Failed to delete category \"$categoryName\" on Telegram")
            }
        }
    }

    fun renameCategory(oldName: String, newName: String) {
        viewModelScope.launch {
            val success = wallpaperRepository.renameCategory(oldName, newName)
            if (!success) {
                _toastEvent.emit("Failed to rename category on Telegram")
            }
        }
    }

    fun addCategory(categoryName: String) {
        viewModelScope.launch {
            val success = wallpaperRepository.addCategory(categoryName)
            if (!success) {
                _toastEvent.emit("Failed to add category \"$categoryName\" on Telegram")
            }
        }
    }

    fun loadThumbnailOnDemand(wallpaper: Wallpaper) {
        viewModelScope.launch {
            wallpaperRepository.loadThumbnailOnDemand(wallpaper)
        }
    }
}

