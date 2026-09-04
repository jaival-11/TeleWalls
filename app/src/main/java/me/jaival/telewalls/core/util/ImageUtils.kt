package me.jaival.telewalls.core.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

object ImageUtils {
    /**
     * Resolves an image path (local file path, HTTP/HTTPS URL, content URI, file URI)
     * into a Coil-compatible model object (java.io.File or String URL).
     */
    fun resolveImageModel(primaryPath: String?, secondaryPath: String? = null): Any? {
        fun toModel(path: String?): Any? {
            if (path.isNullOrBlank()) return null
            return when {
                path.startsWith("/") -> {
                    val file = File(path)
                    if (file.exists() && file.length() > 0) file else null
                }
                path.startsWith("http://") || path.startsWith("https://") ||
                path.startsWith("content://") || path.startsWith("file://") -> {
                    path
                }
                else -> null
            }
        }
        return toModel(primaryPath) ?: toModel(secondaryPath)
    }

    /**
     * Saves an image from a local file path or remote HTTP URL directly into the Android device's
     * Gallery / MediaStore under Pictures/TeleWalls.
     */
    suspend fun saveImageToGallery(
        context: Context,
        imagePath: String,
        title: String,
        mimeType: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bytes = when {
                imagePath.startsWith("http://") || imagePath.startsWith("https://") -> {
                    URL(imagePath).openStream().use { it.readBytes() }
                }
                imagePath.startsWith("/") && File(imagePath).exists() -> {
                    File(imagePath).readBytes()
                }
                else -> {
                    return@withContext Result.failure(Exception("Image file not available to save"))
                }
            }

            val cleanTitle = title.replace("[^a-zA-Z0-9_-]".toRegex(), "_").ifBlank { "wallpaper" }
            val extension = if (mimeType.contains("png", ignoreCase = true)) "png" else "jpg"
            val fileName = "TeleWalls_${cleanTitle}_${System.currentTimeMillis()}.$extension"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, if (mimeType.isNotBlank()) mimeType else "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TeleWalls")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore image entry"))

            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(bytes)
                outputStream.flush()
            } ?: return@withContext Result.failure(Exception("Failed to write image data"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
