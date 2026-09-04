package me.jaival.telewalls.core.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class WallpaperTarget {
    HOME_SCREEN,
    LOCK_SCREEN,
    BOTH
}

@Singleton
class WallpaperManagerHelper @Inject constructor(
    private val context: Context
) {
    suspend fun setWallpaperFromFile(
        imageFile: File,
        target: WallpaperTarget
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                ?: return@withContext Result.failure(Exception("Failed to decode image file"))
            setWallpaperBitmap(bitmap, target)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setWallpaperFromInputStream(
        inputStream: InputStream,
        target: WallpaperTarget
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeStream(inputStream)
                ?: return@withContext Result.failure(Exception("Failed to decode image stream"))
            setWallpaperBitmap(bitmap, target)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun setWallpaperBitmap(bitmap: Bitmap, target: WallpaperTarget): Result<Unit> {
        val wallpaperManager = WallpaperManager.getInstance(context)
        return try {
            when (target) {
                WallpaperTarget.HOME_SCREEN -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    } else {
                        wallpaperManager.setBitmap(bitmap)
                    }
                }
                WallpaperTarget.LOCK_SCREEN -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    } else {
                        wallpaperManager.setBitmap(bitmap)
                    }
                }
                WallpaperTarget.BOTH -> {
                    wallpaperManager.setBitmap(bitmap)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
