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
                // If local database is completely empty, populate curated demo seed wallpapers
                val currentCount = wallpaperDao.getWallpaperById("seed_1")
                if (currentCount == null) {
                    seedDemoWallpapers()
                }
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

    private suspend fun seedDemoWallpapers() {
        val demoWallpapers = listOf(
            WallpaperEntity(
                id = "seed_1",
                messageId = 101,
                chatId = 99999,
                fileId = "file_1",
                fileName = "neon_cyber_city.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 4194304,
                title = "Neon Cyberpunk Skyline",
                category = "AMOLED",
                tagsCsv = "neon,cyberpunk,city,night,amoled",
                resolution = "1440x3200",
                aspectRatio = "9:16",
                colorsCsv = "#0F0F1A,#FF007A,#00F0FF,#1E1E2C",
                description = "Ultra high resolution AMOLED city skyline with vibrant neon pink and cyan highlights.",
                author = "TeleWalls Vault",
                timestamp = System.currentTimeMillis() - 3600000,
                localPath = "https://images.unsplash.com/photo-1519501025264-65ba15a82390?w=1080&q=80",
                thumbnailPath = "https://images.unsplash.com/photo-1519501025264-65ba15a82390?w=400&q=80"
            ),
            WallpaperEntity(
                id = "seed_2",
                messageId = 102,
                chatId = 99999,
                fileId = "file_2",
                fileName = "misty_pine_forest.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 5242880,
                title = "Misty Mountain Pines",
                category = "Nature",
                tagsCsv = "nature,forest,mountains,fog,green",
                resolution = "1440x3200",
                aspectRatio = "9:16",
                colorsCsv = "#0F2012,#2E5A35,#8FA892,#1A2E20",
                description = "Serene foggy pine forest shrouded in morning mist in mountain highlands.",
                author = "Jaival",
                timestamp = System.currentTimeMillis() - 7200000,
                localPath = "https://images.unsplash.com/photo-1448375240586-882707db888b?w=1080&q=80",
                thumbnailPath = "https://images.unsplash.com/photo-1448375240586-882707db888b?w=400&q=80"
            ),
            WallpaperEntity(
                id = "seed_3",
                messageId = 103,
                chatId = 99999,
                fileId = "file_3",
                fileName = "minimal_sand_dune.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 3145728,
                title = "Golden Dunes Sunset",
                category = "Minimal",
                tagsCsv = "minimal,desert,sand,dunes,sunset,golden",
                resolution = "1440x3200",
                aspectRatio = "9:16",
                colorsCsv = "#3A1F0D,#E69544,#F7C280,#1F1005",
                description = "Clean minimalist desert sand dune curves with warm golden hour sunlight shadows.",
                author = "TeleWalls Vault",
                timestamp = System.currentTimeMillis() - 10800000,
                localPath = "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=1080&q=80",
                thumbnailPath = "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=400&q=80"
            ),
            WallpaperEntity(
                id = "seed_4",
                messageId = 104,
                chatId = 99999,
                fileId = "file_4",
                fileName = "cosmic_nebula_violet.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 6291456,
                title = "Deep Space Nebula",
                category = "Sci-Fi",
                tagsCsv = "space,nebula,stars,galaxy,purple,amoled",
                resolution = "1440x3200",
                aspectRatio = "9:16",
                colorsCsv = "#0A0314,#7B1FA2,#E040FB,#120024",
                description = "Breathtaking deep cosmos space nebula rendered with glowing violet clusters.",
                author = "Jaival",
                timestamp = System.currentTimeMillis() - 14400000,
                localPath = "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?w=1080&q=80",
                thumbnailPath = "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?w=400&q=80"
            ),
            WallpaperEntity(
                id = "seed_5",
                messageId = 105,
                chatId = 99999,
                fileId = "file_5",
                fileName = "tokyo_night_street.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 4718592,
                title = "Rainy Tokyo Alley",
                category = "Architecture",
                tagsCsv = "japan,tokyo,street,rain,city,reflections",
                resolution = "1440x3200",
                aspectRatio = "9:16",
                colorsCsv = "#0B101E,#E91E63,#00BCD4,#1A237E",
                description = "Atmospheric rainy alley in Shinjuku Tokyo with glowing lantern reflections.",
                author = "TeleWalls Vault",
                timestamp = System.currentTimeMillis() - 18000000,
                localPath = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=1080&q=80",
                thumbnailPath = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=400&q=80"
            ),
            WallpaperEntity(
                id = "seed_6",
                messageId = 106,
                chatId = 99999,
                fileId = "file_6",
                fileName = "abstract_liquid_flow.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 3670016,
                title = "Chromatic Liquid Waves",
                category = "Abstract",
                tagsCsv = "abstract,liquid,3d,gradients,fluid",
                resolution = "1440x3200",
                aspectRatio = "9:16",
                colorsCsv = "#14002B,#FF0055,#7928CA,#00DFD8",
                description = "Smooth 3D fluid chromatic wave ribbons with vibrant gradient glows.",
                author = "Jaival",
                timestamp = System.currentTimeMillis() - 21600000,
                localPath = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1080&q=80",
                thumbnailPath = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=400&q=80"
            )
        )
        wallpaperDao.insertWallpapers(demoWallpapers)
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
