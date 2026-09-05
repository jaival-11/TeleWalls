package me.jaival.telewalls.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun loadThumbnailOnDemand(wallpaper: Wallpaper) {
        viewModelScope.launch {
            wallpaperRepository.loadThumbnailOnDemand(wallpaper)
        }
    }
}
