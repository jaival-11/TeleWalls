package me.jaival.telewalls.core.palette

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object PaletteExtractor {

    data class WallpaperColors(
        val dominant: String,
        val vibrant: String,
        val darkVibrant: String,
        val lightVibrant: String,
        val muted: String,
        val hexList: List<String>
    )

    suspend fun extractColorsFromUri(context: Context, uri: Uri): WallpaperColors = withContext(Dispatchers.IO) {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        extractColorsFromBitmap(bitmap)
    }

    fun extractColorsFromBitmap(bitmap: Bitmap?): WallpaperColors {
        if (bitmap == null) return defaultColors()
        val palette = Palette.from(bitmap).generate()

        val dominant = palette.getDominantColor(0xFF1E1E2C.toInt())
        val vibrant = palette.getVibrantColor(0xFFFF007A.toInt())
        val darkVibrant = palette.getDarkVibrantColor(0xFF0F0F1A.toInt())
        val lightVibrant = palette.getLightVibrantColor(0xFF00F0FF.toInt())
        val muted = palette.getMutedColor(0xFF2A2D3A.toInt())

        val hexList = listOf(dominant, vibrant, darkVibrant, lightVibrant, muted)
            .map { String.format("#%06X", 0xFFFFFF and it) }
            .distinct()

        return WallpaperColors(
            dominant = String.format("#%06X", 0xFFFFFF and dominant),
            vibrant = String.format("#%06X", 0xFFFFFF and vibrant),
            darkVibrant = String.format("#%06X", 0xFFFFFF and darkVibrant),
            lightVibrant = String.format("#%06X", 0xFFFFFF and lightVibrant),
            muted = String.format("#%06X", 0xFFFFFF and muted),
            hexList = hexList
        )
    }

    private fun defaultColors(): WallpaperColors = WallpaperColors(
        dominant = "#1E1E2C",
        vibrant = "#FF007A",
        darkVibrant = "#0F0F1A",
        lightVibrant = "#00F0FF",
        muted = "#2A2D3A",
        hexList = listOf("#1E1E2C", "#FF007A", "#0F0F1A", "#00F0FF")
    )
}
