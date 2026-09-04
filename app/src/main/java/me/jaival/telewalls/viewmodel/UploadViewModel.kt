package me.jaival.telewalls.viewmodel

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.jaival.telewalls.core.palette.PaletteExtractor
import me.jaival.telewalls.core.telegram.TelegramUploadEvent
import me.jaival.telewalls.core.telegram.WallpaperMetadata
import me.jaival.telewalls.data.repository.AuthRepository
import me.jaival.telewalls.data.repository.WallpaperRepository
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

sealed interface UploadState {
    data object Idle : UploadState
    data class Processing(val status: String) : UploadState
    data class Uploading(val progressPercent: Float, val bytesUploaded: Long, val totalBytes: Long) : UploadState
    data object Success : UploadState
    data class Error(val message: String) : UploadState
}

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    val categories: StateFlow<List<String>> = wallpaperRepository.categories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WallpaperRepository.DEFAULT_CATEGORIES
        )

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileName: StateFlow<String?> = _selectedFileName.asStateFlow()

    private val _detectedResolution = MutableStateFlow("1440x3200")
    val detectedResolution: StateFlow<String> = _detectedResolution.asStateFlow()

    private val _detectedColors = MutableStateFlow<List<String>>(emptyList())
    val detectedColors: StateFlow<List<String>> = _detectedColors.asStateFlow()

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

    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            name = cursor.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore query errors
            }
        }
        if (name.isNullOrBlank()) {
            name = uri.path?.let { File(it).name }
        }
        return name?.takeIf { it.isNotBlank() }
    }

    private fun getMimeTypeFromUri(context: Context, uri: Uri): String? {
        return if (uri.scheme == "content") {
            context.contentResolver.getType(uri)
        } else {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            if (extension != null) {
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            } else null
        }
    }

    fun selectImage(context: Context, uri: Uri) {
        _selectedImageUri.value = uri
        val extractedFileName = getFileNameFromUri(context, uri)
        _selectedFileName.value = extractedFileName
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(inputStream, null, options)
                    _detectedResolution.value = "${options.outWidth}x${options.outHeight}"
                }
                val colors = PaletteExtractor.extractColorsFromUri(context, uri)
                _detectedColors.value = colors.hexList
            } catch (e: Exception) {
                _detectedResolution.value = "1080x1920"
            }
        }
    }

    fun startUpload(
        context: Context,
        title: String,
        category: String,
        tags: String,
        description: String,
        author: String
    ) {
        val uri = _selectedImageUri.value ?: run {
            _uploadState.value = UploadState.Error("Please select a photo first")
            return
        }

        viewModelScope.launch {
            _uploadState.value = UploadState.Processing("Preparing document file...")
            val extractedFileName = getFileNameFromUri(context, uri)
            val file = copyUriToTempFile(context, uri, extractedFileName) ?: run {
                _uploadState.value = UploadState.Error("Failed to process image file")
                return@launch
            }

            val finalFileName = extractedFileName ?: file.name
            val wallpaperTitle = title.trim().ifBlank { finalFileName }

            val metadata = WallpaperMetadata(
                title = wallpaperTitle,
                category = category,
                tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                resolution = _detectedResolution.value,
                aspectRatio = "9:16",
                sizeBytes = file.length(),
                colors = _detectedColors.value,
                description = description,
                author = author.ifBlank { "TeleWalls User" },
                timestamp = System.currentTimeMillis()
            )

            val mimeType = getMimeTypeFromUri(context, uri) ?: "image/jpeg"

            authRepository.activeChannelIdFlow.collect { chatId ->
                val targetChatId = chatId ?: 99999L
                wallpaperRepository.uploadWallpaper(
                    chatId = targetChatId,
                    localPath = file.absolutePath,
                    fileName = finalFileName,
                    mimeType = mimeType,
                    metadata = metadata
                ).collect { event ->
                    when (event) {
                        is TelegramUploadEvent.Progress -> {
                            val total = event.totalBytes ?: file.length()
                            val percent = if (total > 0) (event.bytesUploaded.toFloat() / total) * 100f else 0f
                            _uploadState.value = UploadState.Uploading(percent, event.bytesUploaded, total)
                        }
                        is TelegramUploadEvent.Succeeded -> {
                            wallpaperRepository.saveUploadedWallpaperToDb(event.document)
                            _uploadState.value = UploadState.Success
                        }
                        is TelegramUploadEvent.Failed -> {
                            _uploadState.value = UploadState.Error(event.message)
                        }
                    }
                }
            }
        }
    }

    fun resetState() {
        _uploadState.value = UploadState.Idle
        _selectedImageUri.value = null
        _selectedFileName.value = null
        _detectedColors.value = emptyList()
    }

    private suspend fun copyUriToTempFile(context: Context, uri: Uri, customFileName: String?): File? = withContext(Dispatchers.IO) {
        try {
            val safeFileName = customFileName?.takeIf { it.isNotBlank() } ?: "upload_${System.currentTimeMillis()}.jpg"
            val tempFile = File(context.cacheDir, safeFileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}
