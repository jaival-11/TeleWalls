package me.jaival.telewalls.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.jaival.telewalls.core.telegram.TelegramClient
import me.jaival.telewalls.core.telegram.TelegramUploadEvent
import me.jaival.telewalls.core.telegram.WallpaperDocument
import me.jaival.telewalls.core.telegram.WallpaperMetadata
import me.jaival.telewalls.data.local.dao.WallpaperDao
import me.jaival.telewalls.data.local.entity.FavoriteEntity
import me.jaival.telewalls.data.local.entity.WallpaperEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class Wallpaper(
    val id: String,
    val messageId: Long,
    val chatId: Long,
    val fileId: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val title: String,
    val category: String,
    val tags: List<String>,
    val resolution: String,
    val aspectRatio: String,
    val colors: List<String>,
    val description: String,
    val author: String,
    val timestamp: Long,
    val localPath: String?,
    val thumbnailPath: String?,
    val isFavorite: Boolean
)

@Singleton
class WallpaperRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telegramClient: TelegramClient,
    private val wallpaperDao: WallpaperDao
) {
    companion object {
        private const val TAG = "WallpaperRepository"
    }

    val allWallpapers: Flow<List<Wallpaper>> = wallpaperDao.getAllWallpapers().map { entities ->
        entities.map { it.toDomain() }
    }

    val favoriteWallpapers: Flow<List<Wallpaper>> = wallpaperDao.getFavoriteWallpapers().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getWallpapersByCategory(category: String): Flow<List<Wallpaper>> {
        return if (category.equals("All", ignoreCase = true)) {
            allWallpapers
        } else {
            wallpaperDao.getWallpapersByCategory(category).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    fun searchWallpapers(query: String): Flow<List<Wallpaper>> {
        return wallpaperDao.searchWallpapers(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getWallpaperById(id: String): Wallpaper? {
        return wallpaperDao.getWallpaperById(id)?.toDomain()
    }

    suspend fun syncWallpapersFromChannel(chatId: Long): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val documents = telegramClient.fetchWallpapers(chatId, fromMessageId = 0L, limit = 50)
            if (documents.isNotEmpty()) {
                val entities = documents.map { doc ->
                    doc.toEntity(isFav = wallpaperDao.isFavorite("${doc.chatId}_${doc.messageId}"))
                }
                wallpaperDao.insertWallpapers(entities)
                Result.success(documents.size)
            } else {
                Result.success(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing wallpapers from channel", e)
            Result.failure(e)
        }
    }

    fun uploadWallpaper(
        chatId: Long,
        localPath: String,
        fileName: String,
        mimeType: String,
        metadata: WallpaperMetadata
    ): Flow<TelegramUploadEvent> {
        return telegramClient.uploadWallpaper(chatId, localPath, fileName, mimeType, metadata)
    }

    suspend fun saveUploadedWallpaperToDb(doc: WallpaperDocument) {
        wallpaperDao.insertWallpaper(doc.toEntity(isFav = false))
    }

    suspend fun toggleFavorite(wallpaperId: String) = withContext(Dispatchers.IO) {
        val currentlyFav = wallpaperDao.isFavorite(wallpaperId)
        val newFavState = !currentlyFav
        wallpaperDao.updateFavoriteStatus(wallpaperId, newFavState)
        if (newFavState) {
            wallpaperDao.addFavorite(FavoriteEntity(wallpaperId))
        } else {
            wallpaperDao.removeFavorite(wallpaperId)
        }
    }

    suspend fun deleteWallpaper(wallpaper: Wallpaper): Boolean = withContext(Dispatchers.IO) {
        val success = telegramClient.deleteWallpaper(wallpaper.chatId, wallpaper.messageId)
        if (success) {
            wallpaperDao.deleteWallpaperById(wallpaper.id)
        }
        success
    }

    suspend fun downloadWallpaperFile(fileId: String, fileName: String): String? = withContext(Dispatchers.IO) {
        val destFile = File(context.cacheDir, fileName)
        telegramClient.downloadWallpaperFile(fileId, destFile.absolutePath)
    }



    private fun WallpaperEntity.toDomain(): Wallpaper = Wallpaper(
        id = id,
        messageId = messageId,
        chatId = chatId,
        fileId = fileId,
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        title = title,
        category = category,
        tags = if (tagsCsv.isBlank()) emptyList() else tagsCsv.split(","),
        resolution = resolution,
        aspectRatio = aspectRatio,
        colors = if (colorsCsv.isBlank()) emptyList() else colorsCsv.split(","),
        description = description,
        author = author,
        timestamp = timestamp,
        localPath = localPath,
        thumbnailPath = thumbnailPath,
        isFavorite = isFavorite
    )

    private fun WallpaperDocument.toEntity(isFav: Boolean): WallpaperEntity = WallpaperEntity(
        id = "${chatId}_${messageId}",
        messageId = messageId,
        chatId = chatId,
        fileId = fileId,
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        title = metadata.title,
        category = metadata.category,
        tagsCsv = metadata.tags.joinToString(","),
        resolution = metadata.resolution,
        aspectRatio = metadata.aspectRatio,
        colorsCsv = metadata.colors.joinToString(","),
        description = metadata.description,
        author = metadata.author,
        timestamp = metadata.timestamp,
        localPath = localPath,
        thumbnailPath = thumbnailPath,
        isFavorite = isFav
    )
}
