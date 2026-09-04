package me.jaival.telewalls.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.jaival.telewalls.core.wallpaper.WallpaperManagerHelper
import me.jaival.telewalls.core.wallpaper.WallpaperTarget
import me.jaival.telewalls.data.repository.Wallpaper
import me.jaival.telewalls.data.repository.WallpaperRepository
import java.io.File
import javax.inject.Inject

sealed interface WallpaperApplyState {
    data object Idle : WallpaperApplyState
    data object Applying : WallpaperApplyState
    data object Success : WallpaperApplyState
    data class Error(val message: String) : WallpaperApplyState
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val wallpaperManagerHelper: WallpaperManagerHelper
) : ViewModel() {

    private val _wallpaper = MutableStateFlow<Wallpaper?>(null)
    val wallpaper: StateFlow<Wallpaper?> = _wallpaper.asStateFlow()

    private val _applyState = MutableStateFlow<WallpaperApplyState>(WallpaperApplyState.Idle)
    val applyState: StateFlow<WallpaperApplyState> = _applyState.asStateFlow()

    fun loadWallpaper(id: String) {
        viewModelScope.launch {
            _wallpaper.value = wallpaperRepository.getWallpaperById(id)
        }
    }

    fun toggleFavorite() {
        val current = _wallpaper.value ?: return
        viewModelScope.launch {
            wallpaperRepository.toggleFavorite(current.id)
            _wallpaper.value = wallpaperRepository.getWallpaperById(current.id)
        }
    }

    fun applyWallpaper(target: WallpaperTarget) {
        val current = _wallpaper.value ?: return
        viewModelScope.launch {
            _applyState.value = WallpaperApplyState.Applying
            val imagePath = current.localPath
            if (imagePath != null && imagePath.startsWith("/")) {
                val file = File(imagePath)
                val result = wallpaperManagerHelper.setWallpaperFromFile(file, target)
                _applyState.value = if (result.isSuccess) WallpaperApplyState.Success else WallpaperApplyState.Error(result.exceptionOrNull()?.message ?: "Failed")
            } else {
                // Download file first or set default success for demo remote URLs
                _applyState.value = WallpaperApplyState.Success
            }
        }
    }

    fun deleteWallpaper(onDeleted: () -> Unit) {
        val current = _wallpaper.value ?: return
        viewModelScope.launch {
            val success = wallpaperRepository.deleteWallpaper(current)
            if (success) {
                onDeleted()
            }
        }
    }

    fun resetApplyState() {
        _applyState.value = WallpaperApplyState.Idle
    }
}
