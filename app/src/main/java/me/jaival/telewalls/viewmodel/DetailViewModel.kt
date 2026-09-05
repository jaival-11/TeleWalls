package me.jaival.telewalls.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import me.jaival.telewalls.core.util.ImageUtils
import me.jaival.telewalls.core.wallpaper.WallpaperManagerHelper
import me.jaival.telewalls.core.wallpaper.WallpaperTarget
import me.jaival.telewalls.data.repository.AuthRepository
import me.jaival.telewalls.data.repository.Wallpaper
import me.jaival.telewalls.data.repository.WallpaperRepository
import java.io.File
import java.net.URL
import javax.inject.Inject

sealed interface WallpaperApplyState {
    data object Idle : WallpaperApplyState
    data object Applying : WallpaperApplyState
    data object Success : WallpaperApplyState
    data class Error(val message: String) : WallpaperApplyState
}

sealed interface WallpaperDownloadState {
    data object Idle : WallpaperDownloadState
    data object Downloading : WallpaperDownloadState
    data object Success : WallpaperDownloadState
    data class Error(val message: String) : WallpaperDownloadState
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val wallpaperManagerHelper: WallpaperManagerHelper,
    private val authRepository: AuthRepository
) : ViewModel() {

    val categories: StateFlow<List<String>> = wallpaperRepository.categories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WallpaperRepository.DEFAULT_CATEGORIES
        )

    private val _wallpaper = MutableStateFlow<Wallpaper?>(null)
    val wallpaper: StateFlow<Wallpaper?> = _wallpaper.asStateFlow()

    private val _isLoadingFullImage = MutableStateFlow(false)
    val isLoadingFullImage: StateFlow<Boolean> = _isLoadingFullImage.asStateFlow()

    private val _imageRefreshKey = MutableStateFlow(0L)
    val imageRefreshKey: StateFlow<Long> = _imageRefreshKey.asStateFlow()

    private val _applyState = MutableStateFlow<WallpaperApplyState>(WallpaperApplyState.Idle)
    val applyState: StateFlow<WallpaperApplyState> = _applyState.asStateFlow()

    private val _downloadState = MutableStateFlow<WallpaperDownloadState>(WallpaperDownloadState.Idle)
    val downloadState: StateFlow<WallpaperDownloadState> = _downloadState.asStateFlow()

    fun loadWallpaper(id: String) {
        viewModelScope.launch {
            val loaded = wallpaperRepository.getWallpaperById(id)
            _wallpaper.value = loaded
            if (loaded != null) {
                val hasFullImage = !loaded.localPath.isNullOrBlank() && (loaded.localPath.startsWith("http") || (File(loaded.localPath).exists() && File(loaded.localPath).length() > 0))
                if (!hasFullImage) {
                    val hasThumb = !loaded.thumbnailPath.isNullOrBlank() && (loaded.thumbnailPath.startsWith("http") || (File(loaded.thumbnailPath).exists() && File(loaded.thumbnailPath).length() > 0))
                    if (!hasThumb) {
                        val thumbPath = wallpaperRepository.loadThumbnailOnDemand(loaded)
                        if (thumbPath != null) {
                            _wallpaper.value = wallpaperRepository.getWallpaperById(id)
                        }
                    }
                    _isLoadingFullImage.value = true
                    val fullPath = wallpaperRepository.downloadFullWallpaper(loaded)
                    val updated = wallpaperRepository.getWallpaperById(id)
                    if (updated != null) {
                        _wallpaper.value = updated
                    }
                    _imageRefreshKey.value = System.currentTimeMillis()
                    _isLoadingFullImage.value = false
                } else {
                    _isLoadingFullImage.value = false
                }
            } else {
                _isLoadingFullImage.value = false
            }
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

            var imagePath = current.localPath
            if (imagePath.isNullOrBlank() || (!imagePath.startsWith("http") && (!File(imagePath).exists() || File(imagePath).length() == 0L))) {
                _isLoadingFullImage.value = true
                imagePath = wallpaperRepository.downloadFullWallpaper(current)
                _isLoadingFullImage.value = false
            }

            if (!imagePath.isNullOrBlank()) {
                val updated = wallpaperRepository.getWallpaperById(current.id)
                if (updated != null) {
                    _wallpaper.value = updated
                    _imageRefreshKey.value = System.currentTimeMillis()
                }
            }

            if (imagePath.isNullOrBlank()) {
                imagePath = current.thumbnailPath
            }

            if (imagePath.isNullOrBlank()) {
                _applyState.value = WallpaperApplyState.Error("Image file not available to set wallpaper")
                return@launch
            }

            if (imagePath.startsWith("/")) {
                val file = File(imagePath)
                if (file.exists()) {
                    val result = wallpaperManagerHelper.setWallpaperFromFile(file, target)
                    _applyState.value = if (result.isSuccess) {
                        WallpaperApplyState.Success
                    } else {
                        WallpaperApplyState.Error(result.exceptionOrNull()?.message ?: "Failed to set wallpaper")
                    }
                } else {
                    _applyState.value = WallpaperApplyState.Error("Local wallpaper file not found")
                }
            } else if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                withContext(Dispatchers.IO) {
                    try {
                        val url = URL(imagePath)
                        url.openStream().use { stream ->
                            val result = wallpaperManagerHelper.setWallpaperFromInputStream(stream, target)
                            _applyState.value = if (result.isSuccess) {
                                WallpaperApplyState.Success
                            } else {
                                WallpaperApplyState.Error(result.exceptionOrNull()?.message ?: "Failed to set wallpaper")
                            }
                        }
                    } catch (e: Exception) {
                        _applyState.value = WallpaperApplyState.Error(e.message ?: "Failed to download image to set wallpaper")
                    }
                }
            } else {
                _applyState.value = WallpaperApplyState.Error("Invalid wallpaper image path")
            }
        }
    }

    fun downloadWallpaperToGallery(context: Context) {
        val current = _wallpaper.value ?: return
        viewModelScope.launch {
            _downloadState.value = WallpaperDownloadState.Downloading

            var imagePath = current.localPath
            if (imagePath.isNullOrBlank() || (!imagePath.startsWith("http") && (!File(imagePath).exists() || File(imagePath).length() == 0L))) {
                _isLoadingFullImage.value = true
                imagePath = wallpaperRepository.downloadFullWallpaper(current)
                _isLoadingFullImage.value = false
            }

            if (!imagePath.isNullOrBlank()) {
                val updated = wallpaperRepository.getWallpaperById(current.id)
                if (updated != null) {
                    _wallpaper.value = updated
                    _imageRefreshKey.value = System.currentTimeMillis()
                }
            }

            if (imagePath.isNullOrBlank()) {
                imagePath = current.thumbnailPath
            }

            if (imagePath.isNullOrBlank()) {
                _downloadState.value = WallpaperDownloadState.Error("Image file not available to download")
                return@launch
            }

            val result = ImageUtils.saveImageToGallery(context, imagePath, current.title, current.mimeType)
            if (result.isSuccess) {
                _downloadState.value = WallpaperDownloadState.Success
            } else {
                _downloadState.value = WallpaperDownloadState.Error(result.exceptionOrNull()?.message ?: "Failed to save image to gallery")
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

    fun createCategory(categoryName: String, onCategoryCreated: (String) -> Unit) {
        val name = categoryName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            val chatId = authRepository.activeChannelIdFlow.first() ?: 99999L
            val success = wallpaperRepository.addCategory(name, chatId)
            if (success) {
                onCategoryCreated(name)
            }
        }
    }

    fun updateMetadata(
        title: String,
        author: String,
        category: String,
        tags: String,
        description: String,
        wallpaperType: String,
        onUpdated: () -> Unit
    ) {
        val current = _wallpaper.value ?: return
        viewModelScope.launch {
            val tagList = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
            wallpaperRepository.updateWallpaperMetadata(
                wallpaper = current,
                title = title,
                author = author,
                category = category,
                tags = tagList,
                description = description,
                wallpaperType = wallpaperType
            )
            _wallpaper.value = wallpaperRepository.getWallpaperById(current.id)
            onUpdated()
        }
    }

    fun resetApplyState() {
        _applyState.value = WallpaperApplyState.Idle
    }

    fun resetDownloadState() {
        _downloadState.value = WallpaperDownloadState.Idle
    }
}
