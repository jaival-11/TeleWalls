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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import me.jaival.telewalls.core.util.CharacterAuthorUtils
import me.jaival.telewalls.core.util.ColorSearchUtils
import me.jaival.telewalls.data.local.dao.CategoryDao
import me.jaival.telewalls.data.local.entity.CategoryEntity
import me.jaival.telewalls.data.local.entity.FavoriteEntity
import me.jaival.telewalls.data.local.entity.WallpaperEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import coil.imageLoader
import me.jaival.telewalls.BuildConfig
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
    val isFavorite: Boolean,
    val wallpaperType: String = "Phone"
)

@Singleton
class WallpaperRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telegramClient: TelegramClient,
    private val wallpaperDao: WallpaperDao,
    private val categoryDao: CategoryDao
) {
    companion object {
        private const val TAG = "WallpaperRepository"
        val DEFAULT_CATEGORIES = listOf(
            "AMOLED", "Nature", "Minimal", "Sci-Fi", "Architecture", "Abstract", "Cars"
        )
    }

    val categories: Flow<List<String>> = combine(
        categoryDao.getAllCategories(),
        wallpaperDao.getCategoriesFromWallpapers()
    ) { dbCategories, wallpaperCategories ->
        (DEFAULT_CATEGORIES + dbCategories + wallpaperCategories)
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
            .distinctBy { it.lowercase() }
    }

    val allWallpapers: Flow<List<Wallpaper>> = wallpaperDao.getAllWallpapers().map { entities ->
        entities.map { it.toDomain() }
    }

    val favoriteWallpapers: Flow<List<Wallpaper>> = wallpaperDao.getFavoriteWallpapers().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getWallpapersByCategory(category: String): Flow<List<Wallpaper>> {
        return searchWallpapers(query = "", category = category)
    }

    fun searchWallpapers(query: String, category: String = "All"): Flow<List<Wallpaper>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty() && category.equals("All", ignoreCase = true)) {
            return allWallpapers
        }

        return allWallpapers.map { list ->
            list.mapNotNull { wallpaper ->
                val (isMatch, score) = ColorSearchUtils.evaluateWallpaper(wallpaper, cleanQuery, category)
                if (isMatch) Pair(wallpaper, score) else null
            }
            .sortedWith(compareByDescending<Pair<Wallpaper, Double>> { it.second }.thenByDescending { it.first.timestamp })
            .map { it.first }
        }
    }

    suspend fun getWallpaperById(id: String): Wallpaper? {
        return wallpaperDao.getWallpaperById(id)?.toDomain()
    }

    suspend fun reindexFromChannel(chatId: Long): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[REINDEX DEBUG] Starting reindexFromChannel for chatId=$chatId")
        }
        try {
            val catResult = syncCategoriesFromChannel(chatId)
            val wpResult = syncWallpapersFromChannel(chatId)
            if (catResult.isFailure) {
                val err = catResult.exceptionOrNull() ?: Exception("Failed to sync categories")
                return@withContext Result.failure(err)
            }
            if (wpResult.isFailure) {
                val err = wpResult.exceptionOrNull() ?: Exception("Failed to sync wallpapers")
                return@withContext Result.failure(err)
            }
            val categoriesCount = catResult.getOrDefault(0)
            val wallpapersCount = wpResult.getOrDefault(0)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[REINDEX DEBUG] Reindex finished successfully: $wallpapersCount wallpapers, $categoriesCount categories for chatId=$chatId")
            }
            Result.success(Pair(wallpapersCount, categoriesCount))
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[REINDEX DEBUG] Error reindexing from channel for chatId=$chatId: ${e.message}", e)
            }
            Log.e(TAG, "Error reindexing from channel", e)
            Result.failure(e)
        }
    }

    suspend fun syncWallpapersFromChannel(chatId: Long): Result<Int> = withContext(Dispatchers.IO) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[REINDEX DEBUG] Starting syncWallpapersFromChannel for chatId=$chatId")
        }
        try {
            syncCategoriesFromChannel(chatId)
            val documents = telegramClient.fetchWallpapers(chatId, fromMessageId = 0L, limit = 50)
            val fetchedIds = documents.map { "${it.chatId}_${it.messageId}" }.toSet()

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[REINDEX DEBUG] Fetched ${documents.size} wallpaper documents from Telegram channel chatId=$chatId. Remote IDs: $fetchedIds")
            }

            val existingEntities = wallpaperDao.getWallpapersByChatId(chatId)
            val deletedEntities = existingEntities.filter { it.id !in fetchedIds }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[REINDEX DEBUG] Existing DB entities count=${existingEntities.size}, Orphaned/Deleted entities count=${deletedEntities.size}")
            }

            if (deletedEntities.isNotEmpty()) {
                val deletedIds = deletedEntities.map { it.id }
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[REINDEX DEBUG] Removing orphaned wallpaper IDs from DB: $deletedIds")
                }
                wallpaperDao.deleteWallpapersByIds(deletedIds)
                wallpaperDao.deleteOrphanFavorites()

                deletedEntities.forEach { entity ->
                    entity.localPath?.let { path ->
                        if (path.startsWith("/") && !path.startsWith("http")) {
                            try { File(path).delete() } catch (e: Exception) { Log.e(TAG, "Error deleting local cached file", e) }
                        }
                    }
                    entity.thumbnailPath?.let { path ->
                        if (path.startsWith("/") && !path.startsWith("http")) {
                            try { File(path).delete() } catch (e: Exception) { Log.e(TAG, "Error deleting local thumbnail file", e) }
                        }
                    }
                }
            }

            if (documents.isNotEmpty()) {
                val entities = documents.map { doc ->
                    val id = "${doc.chatId}_${doc.messageId}"
                    val existingEntity = wallpaperDao.getWallpaperById(id)
                    val isFav = existingEntity?.isFavorite ?: false
                    val existingLocalPath = existingEntity?.localPath?.takeIf {
                        it.isNotBlank() && (it.startsWith("http") || File(it).exists())
                    }
                    val existingThumbnailPath = existingEntity?.thumbnailPath?.takeIf {
                        it.isNotBlank() && (it.startsWith("http") || (File(it).exists() && File(it).length() > 0))
                    }
                    val finalThumbnailPath = doc.thumbnailPath?.takeIf {
                        it.isNotBlank() && (it.startsWith("http") || (File(it).exists() && File(it).length() > 0))
                    } ?: existingThumbnailPath
                    doc.toEntity(
                        isFav = isFav,
                        localPathOverride = existingLocalPath,
                        thumbnailPathOverride = finalThumbnailPath
                    )
                }
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[REINDEX DEBUG] Inserting/updating ${entities.size} entities into Room DB. Sample: ${entities.firstOrNull()?.let { "id=${it.id}, title='${it.title}', type='${it.wallpaperType}'" }}")
                }
                wallpaperDao.insertWallpapers(entities)
            }
            Result.success(documents.size)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[REINDEX DEBUG] Exception in syncWallpapersFromChannel for chatId=$chatId: ${e.message}", e)
            }
            Log.e(TAG, "Error syncing wallpapers from channel", e)
            Result.failure(e)
        }
    }

    suspend fun addCategory(name: String, chatId: Long?): Boolean = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        if (cleanName.isBlank() || cleanName.equals("All", ignoreCase = true)) return@withContext false

        categoryDao.insertCategory(CategoryEntity(name = cleanName))

        if (chatId != null && chatId != 0L) {
            val currentList = categories.first()
            val updatedList = (currentList + cleanName).map { it.trim() }.distinctBy { it.lowercase() }
            telegramClient.saveCategoriesMessage(chatId, updatedList)
        }
        true
    }

    suspend fun syncCategoriesFromChannel(chatId: Long): Result<Int> = withContext(Dispatchers.IO) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[REINDEX DEBUG] Starting syncCategoriesFromChannel for chatId=$chatId")
        }
        try {
            val remoteCategories = telegramClient.fetchCategoriesMessage(chatId)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[REINDEX DEBUG] Fetched remote categories count=${remoteCategories.size}: $remoteCategories")
            }
            if (remoteCategories.isNotEmpty()) {
                val entities = remoteCategories.map { CategoryEntity(name = it.trim()) }
                categoryDao.insertCategories(entities)
                Result.success(remoteCategories.size)
            } else {
                Result.success(0)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[REINDEX DEBUG] Exception in syncCategoriesFromChannel for chatId=$chatId: ${e.message}", e)
            }
            Log.e(TAG, "Error syncing categories from channel", e)
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
            wallpaperDao.removeFavorite(wallpaper.id)
        }
        success
    }

    suspend fun updateWallpaperMetadata(
        wallpaper: Wallpaper,
        title: String,
        author: String,
        category: String,
        tags: List<String>,
        description: String,
        wallpaperType: String = wallpaper.wallpaperType
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanTitle = title.ifBlank { "Untitled Wallpaper" }
        val cleanAuthor = author.trim().ifBlank { CharacterAuthorUtils.getRandomCharacterName() }
        val cleanCategory = category.ifBlank { "AMOLED" }
        val cleanTags = tags.map { it.trim() }.filter { it.isNotBlank() }
        val tagsCsv = cleanTags.joinToString(",")
        val cleanType = wallpaperType.ifBlank { "Phone" }

        wallpaperDao.updateWallpaperMetadata(
            id = wallpaper.id,
            title = cleanTitle,
            author = cleanAuthor,
            category = cleanCategory,
            tagsCsv = tagsCsv,
            description = description,
            wallpaperType = cleanType
        )

        val updatedMetadata = WallpaperMetadata(
            title = cleanTitle,
            category = cleanCategory,
            tags = cleanTags,
            resolution = wallpaper.resolution,
            aspectRatio = wallpaper.aspectRatio,
            sizeBytes = wallpaper.sizeBytes,
            colors = wallpaper.colors,
            description = description,
            author = cleanAuthor,
            timestamp = wallpaper.timestamp,
            wallpaperType = cleanType
        )

        if (wallpaper.chatId != 0L && wallpaper.messageId != 0L) {
            telegramClient.editWallpaperMetadata(
                chatId = wallpaper.chatId,
                messageId = wallpaper.messageId,
                metadata = updatedMetadata
            )
        }
        true
    }

    suspend fun downloadWallpaperFile(fileId: String, fileName: String): String? = withContext(Dispatchers.IO) {
        val safeName = if (fileName.isNotBlank()) "${fileId}_$fileName" else "wallpaper_$fileId.jpg"
        val destFile = File(context.cacheDir, safeName)
        telegramClient.downloadWallpaperFile(fileId, destFile.absolutePath)
    }

    suspend fun downloadFullWallpaper(wallpaper: Wallpaper): String? = withContext(Dispatchers.IO) {
        val currentLocal = wallpaper.localPath
        if (!currentLocal.isNullOrBlank() && (currentLocal.startsWith("http") || (File(currentLocal).exists() && File(currentLocal).length() > 0))) {
            return@withContext currentLocal
        }
        val downloadedPath = downloadWallpaperFile(wallpaper.fileId, wallpaper.fileName)
        if (!downloadedPath.isNullOrBlank() && (downloadedPath.startsWith("http") || (File(downloadedPath).exists() && File(downloadedPath).length() > 0))) {
            wallpaperDao.updateLocalPath(wallpaper.id, downloadedPath)
            return@withContext downloadedPath
        }
        null
    }

    suspend fun loadThumbnailOnDemand(wallpaper: Wallpaper): String? = withContext(Dispatchers.IO) {
        val currentThumb = wallpaper.thumbnailPath
        if (!currentThumb.isNullOrBlank() && (currentThumb.startsWith("http") || (File(currentThumb).exists() && File(currentThumb).length() > 0))) {
            return@withContext currentThumb
        }
        val currentLocal = wallpaper.localPath
        if (!currentLocal.isNullOrBlank() && (currentLocal.startsWith("http") || (File(currentLocal).exists() && File(currentLocal).length() > 0))) {
            return@withContext currentLocal
        }

        val downloadedPath = telegramClient.fetchThumbnail(wallpaper.chatId, wallpaper.messageId)
        if (!downloadedPath.isNullOrBlank()) {
            wallpaperDao.updateThumbnailPath(wallpaper.id, downloadedPath)
        }
        downloadedPath
    }

    suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        var total = 0L

        fun getFolderSize(dir: File?): Long {
            if (dir == null || !dir.exists()) return 0L
            if (dir.isFile) return dir.length()
            var size = 0L
            val files = dir.listFiles() ?: return 0L
            for (f in files) {
                size += getFolderSize(f)
            }
            return size
        }

        total += getFolderSize(context.cacheDir)
        context.externalCacheDir?.let { total += getFolderSize(it) }

        val tdlibDir = File(context.filesDir, "tdlib")
        if (tdlibDir.exists() && tdlibDir.isDirectory) {
            val mediaFolders = listOf("files", "photos", "thumbnails", "documents")
            for (folderName in mediaFolders) {
                val folder = File(tdlibDir, folderName)
                if (folder.exists()) {
                    total += getFolderSize(folder)
                }
            }
        }

        total
    }

    suspend fun clearImageCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Clear Coil memory and disk caches
            try {
                val loader = context.imageLoader
                loader.diskCache?.clear()
                loader.memoryCache?.clear()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing Coil cache", e)
            }

            // 2. Clear cacheDir
            fun deleteContents(dir: File?) {
                if (dir == null || !dir.exists()) return
                val files = dir.listFiles() ?: return
                for (f in files) {
                    if (f.isDirectory) {
                        f.deleteRecursively()
                    } else {
                        f.delete()
                    }
                }
            }

            deleteContents(context.cacheDir)

            // 3. Clear externalCacheDir if present
            deleteContents(context.externalCacheDir)

            // 4. Clear downloaded TDLib media subfolders
            val tdlibDir = File(context.filesDir, "tdlib")
            if (tdlibDir.exists() && tdlibDir.isDirectory) {
                val mediaFolders = listOf("files", "photos", "thumbnails", "documents")
                for (folderName in mediaFolders) {
                    val folder = File(tdlibDir, folderName)
                    if (folder.exists()) {
                        deleteContents(folder)
                    }
                }
            }

            // 5. Clear cached image paths in Room database
            wallpaperDao.clearCachedPaths()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing image cache", e)
            false
        }
    }

    private fun WallpaperEntity.toDomain(): Wallpaper = Wallpaper(
        id = id,
        messageId = messageId,
        chatId = chatId,
        fileId = fileId ?: "",
        fileName = fileName ?: "",
        mimeType = mimeType ?: "image/jpeg",
        sizeBytes = sizeBytes,
        title = title ?: "Untitled",
        category = category ?: "Uncategorized",
        tags = if (tagsCsv.isNullOrBlank()) emptyList() else tagsCsv.split(","),
        resolution = resolution ?: "1080x1920",
        aspectRatio = aspectRatio ?: "9:16",
        colors = if (colorsCsv.isNullOrBlank()) emptyList() else colorsCsv.split(","),
        description = description ?: "",
        author = author ?: "",
        timestamp = timestamp,
        localPath = localPath,
        thumbnailPath = thumbnailPath,
        isFavorite = isFavorite,
        wallpaperType = wallpaperType?.takeIf { !it.isNullOrBlank() } ?: "Phone"
    )

    private fun WallpaperDocument.toEntity(
        isFav: Boolean,
        localPathOverride: String? = this.localPath,
        thumbnailPathOverride: String? = this.thumbnailPath
    ): WallpaperEntity = WallpaperEntity(
        id = "${chatId}_${messageId}",
        messageId = messageId,
        chatId = chatId,
        fileId = fileId ?: "",
        fileName = fileName ?: "",
        mimeType = mimeType ?: "image/jpeg",
        sizeBytes = sizeBytes,
        title = metadata.title ?: "Untitled",
        category = metadata.category ?: "Uncategorized",
        tagsCsv = metadata.tags?.joinToString(",") ?: "",
        resolution = metadata.resolution ?: "1080x1920",
        aspectRatio = metadata.aspectRatio ?: "9:16",
        colorsCsv = metadata.colors?.joinToString(",") ?: "",
        description = metadata.description ?: "",
        author = metadata.author ?: "",
        timestamp = metadata.timestamp,
        localPath = localPathOverride,
        thumbnailPath = thumbnailPathOverride,
        isFavorite = isFav,
        wallpaperType = metadata.wallpaperType?.takeIf { !it.isNullOrBlank() } ?: "Phone"
    )
}
