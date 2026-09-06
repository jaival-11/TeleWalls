package me.jaival.telewalls.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import me.jaival.telewalls.core.telegram.TelegramAuthState
import me.jaival.telewalls.core.telegram.TelegramClient
import me.jaival.telewalls.core.telegram.TelegramConnectionState
import me.jaival.telewalls.core.telegram.TelegramUploadEvent
import me.jaival.telewalls.core.telegram.WallpaperDocument
import me.jaival.telewalls.core.telegram.WallpaperMetadata
import me.jaival.telewalls.core.telegram.StorageChannel
import me.jaival.telewalls.data.local.dao.CategoryDao
import me.jaival.telewalls.data.local.dao.WallpaperDao
import me.jaival.telewalls.data.local.entity.CategoryEntity
import me.jaival.telewalls.data.local.entity.FavoriteEntity
import me.jaival.telewalls.data.local.entity.WallpaperEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperRepositoryTest {

    private class FakeWallpaperDao : WallpaperDao {
        val wallpapers = mutableMapOf<String, WallpaperEntity>()
        val favorites = mutableSetOf<String>()

        override fun getAllWallpapers(): Flow<List<WallpaperEntity>> = flowOf(wallpapers.values.toList())
        override fun getWallpapersByCategory(category: String): Flow<List<WallpaperEntity>> = flowOf(wallpapers.values.filter { it.category == category })
        override fun getCategoriesFromWallpapers(): Flow<List<String>> = flowOf(wallpapers.values.map { it.category }.distinct())
        override fun searchWallpapers(query: String): Flow<List<WallpaperEntity>> = flowOf(wallpapers.values.toList())
        override fun getFavoriteWallpapers(): Flow<List<WallpaperEntity>> = flowOf(wallpapers.values.filter { it.isFavorite })

        override suspend fun getWallpaperById(id: String): WallpaperEntity? = wallpapers[id]
        override suspend fun insertWallpapers(wallpapers: List<WallpaperEntity>) {
            wallpapers.forEach { this.wallpapers[it.id] = it }
        }
        override suspend fun insertWallpaper(wallpaper: WallpaperEntity) {
            wallpapers[wallpaper.id] = wallpaper
        }
        override suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean) {
            wallpapers[id]?.let { wallpapers[id] = it.copy(isFavorite = isFavorite) }
        }
        override suspend fun updateLocalPath(id: String, localPath: String?) {
            wallpapers[id]?.let { wallpapers[id] = it.copy(localPath = localPath) }
        }
        override suspend fun updateThumbnailPath(id: String, thumbnailPath: String?) {
            wallpapers[id]?.let { wallpapers[id] = it.copy(thumbnailPath = thumbnailPath) }
        }
        override suspend fun clearCachedPaths() {}
        override suspend fun updateWallpaperMetadata(id: String, title: String, author: String, category: String, tagsCsv: String, description: String, wallpaperType: String) {
            wallpapers[id]?.let {
                wallpapers[id] = it.copy(
                    title = title,
                    author = author,
                    category = category,
                    tagsCsv = tagsCsv,
                    description = description,
                    wallpaperType = wallpaperType
                )
            }
        }
        override suspend fun updateWallpaperCategory(oldName: String, newName: String) {
            wallpapers.values.filter { it.category == oldName }.forEach { w ->
                wallpapers[w.id] = w.copy(category = newName)
            }
        }
        override suspend fun deleteWallpaperById(id: String) {
            wallpapers.remove(id)
        }
        override suspend fun getWallpapersByChatId(chatId: Long): List<WallpaperEntity> {
            return wallpapers.values.filter { it.chatId == chatId || it.id.startsWith("${chatId}_") }
        }
        override suspend fun deleteWallpapersByIds(ids: List<String>) {
            ids.forEach { wallpapers.remove(it) }
        }
        override suspend fun deleteOrphanFavorites() {
            favorites.retainAll(wallpapers.keys)
        }
        override suspend fun addFavorite(favorite: FavoriteEntity) {
            favorites.add(favorite.wallpaperId)
        }
        override suspend fun removeFavorite(wallpaperId: String) {
            favorites.remove(wallpaperId)
        }
        override suspend fun isFavorite(wallpaperId: String): Boolean = favorites.contains(wallpaperId)
        override suspend fun getAllWallpaperEntities(): List<WallpaperEntity> = wallpapers.values.toList()
        override suspend fun getAllFavoriteEntities(): List<FavoriteEntity> = favorites.map { FavoriteEntity(it) }
        override suspend fun clearWallpapers() { wallpapers.clear() }
        override suspend fun clearFavorites() { favorites.clear() }
        override suspend fun clearAllFavoriteFlags() {
            wallpapers.keys.forEach { id ->
                wallpapers[id]?.let { wallpapers[id] = it.copy(isFavorite = false) }
            }
        }
        override suspend fun setFavoriteFlags(ids: List<String>, messageIds: List<Long>) {
            ids.forEach { id ->
                wallpapers[id]?.let { wallpapers[id] = it.copy(isFavorite = true) }
            }
            wallpapers.values.filter { it.messageId in messageIds }.forEach { w ->
                wallpapers[w.id] = w.copy(isFavorite = true)
            }
        }
        override suspend fun insertFavorites(favorites: List<FavoriteEntity>) { favorites.forEach { this.favorites.add(it.wallpaperId) } }
    }

    private class FakeCategoryDao : CategoryDao {
        val categories = mutableListOf<CategoryEntity>()

        override fun getAllCategories(): Flow<List<String>> = flowOf(categories.sortedBy { it.sortOrder }.map { it.name })
        override suspend fun insertCategories(categories: List<CategoryEntity>) {
            this.categories.clear()
            this.categories.addAll(categories)
        }
        override suspend fun insertCategory(category: CategoryEntity) {
            categories.add(category)
        }
        override suspend fun getCategoryList(): List<String> = categories.sortedBy { it.sortOrder }.map { it.name }
        override suspend fun getAllCategoryEntities(): List<CategoryEntity> = categories.sortedBy { it.sortOrder }
        override suspend fun deleteCategory(name: String) {
            categories.removeAll { it.name == name }
        }
        override suspend fun renameCategory(oldName: String, newName: String) {
            val idx = categories.indexOfFirst { it.name == oldName }
            if (idx != -1) {
                val oldEntity = categories[idx]
                categories[idx] = oldEntity.copy(name = newName)
            }
        }
        override suspend fun clearCategories() { categories.clear() }
    }

    private class FakeTelegramClient(
        var remoteWallpapers: List<WallpaperDocument> = emptyList()
    ) : TelegramClient {
        override val authState = MutableStateFlow<TelegramAuthState>(TelegramAuthState.Ready)
        override val connectionState = MutableStateFlow<TelegramConnectionState>(TelegramConnectionState.READY)

        override suspend fun start(credentials: me.jaival.telewalls.core.telegram.TelegramCredentials) {}
        override suspend fun submitPhoneNumber(phoneNumber: String) {}
        override suspend fun submitCode(code: String) {}
        override suspend fun submitPassword(password: String) {}
        override suspend fun requestQrCodeAuthentication() {}
        override suspend fun resetAuthState() {}
        override suspend fun logout() {}
        override suspend fun getMe(): me.jaival.telewalls.core.telegram.TelegramUser? = null
        override suspend fun ensureStorageChat(knownChatId: Long?): Long = 100L
        override suspend fun listStorageChannels(): List<StorageChannel> = emptyList()
        override suspend fun createStorageChannel(title: String): StorageChannel = StorageChannel(100L, title)
        override fun uploadWallpaper(chatId: Long, localPath: String, fileName: String, mimeType: String, metadata: WallpaperMetadata): Flow<TelegramUploadEvent> = flowOf()
        override suspend fun fetchWallpapers(chatId: Long, fromMessageId: Long, limit: Int): List<WallpaperDocument> = remoteWallpapers
        override suspend fun downloadWallpaperFile(fileId: String, destinationPath: String): String? = null
        override suspend fun fetchThumbnail(chatId: Long, messageId: Long): String? = null
        override suspend fun deleteWallpaper(chatId: Long, messageId: Long): Boolean = true
        override suspend fun editWallpaperMetadata(chatId: Long, messageId: Long, metadata: WallpaperMetadata): Boolean = true
        override suspend fun fetchCategoriesMessage(chatId: Long): List<String> = emptyList()
        override suspend fun saveCategoriesMessage(chatId: Long, categories: List<String>): Boolean = true

        var remoteFavorites: List<String> = emptyList()
        var savedFavorites: List<String> = emptyList()
        override suspend fun fetchFavoritesMessage(chatId: Long): List<String> = remoteFavorites
        override suspend fun saveFavoritesMessage(chatId: Long, favorites: List<String>): Boolean {
            savedFavorites = favorites
            return true
        }
    }

    @Test
    fun testReindexDeletesWallpapersNoLongerInTelegramChannel() = runBlocking {
        val chatId = 12345L
        val dao = FakeWallpaperDao()

        // Local DB has 2 wallpapers
        val wp1 = WallpaperEntity(
            id = "${chatId}_1", messageId = 1L, chatId = chatId, fileId = "f1",
            fileName = "wp1.jpg", mimeType = "image/jpeg", sizeBytes = 100L,
            title = "Wallpaper 1", category = "AMOLED", tagsCsv = "", resolution = "1080x1920",
            aspectRatio = "9:16", colorsCsv = "", description = "", author = "Author 1",
            timestamp = 1000L
        )
        val wp2 = WallpaperEntity(
            id = "${chatId}_2", messageId = 2L, chatId = chatId, fileId = "f2",
            fileName = "wp2.jpg", mimeType = "image/jpeg", sizeBytes = 200L,
            title = "Wallpaper 2", category = "Nature", tagsCsv = "", resolution = "1080x1920",
            aspectRatio = "9:16", colorsCsv = "", description = "", author = "Author 2",
            timestamp = 2000L
        )
        dao.wallpapers[wp1.id] = wp1
        dao.wallpapers[wp2.id] = wp2
        dao.favorites.add(wp2.id)

        assertEquals(2, dao.wallpapers.size)

        // Telegram channel now ONLY contains Wallpaper 1 (Wallpaper 2 was deleted from Telegram channel)
        val remoteDoc = WallpaperDocument(
            messageId = 1L, chatId = chatId, fileId = "f1", fileName = "wp1.jpg",
            mimeType = "image/jpeg", sizeBytes = 100L, localPath = null, thumbnailPath = null,
            metadata = WallpaperMetadata(
                title = "Wallpaper 1", category = "AMOLED", tags = emptyList(),
                resolution = "1080x1920", aspectRatio = "9:16", colors = emptyList(),
                description = "", author = "Author 1", timestamp = 1000L
            )
        )
        val fetchedIds = listOf(remoteDoc).map { "${it.chatId}_${it.messageId}" }.toSet()

        // Reindex detection logic
        val existingEntities = dao.wallpapers.values.filter { it.chatId == chatId }
        val deletedEntities = existingEntities.filter { it.id !in fetchedIds }
        if (deletedEntities.isNotEmpty()) {
            val deletedIds = deletedEntities.map { it.id }
            dao.deleteWallpapersByIds(deletedIds)
            dao.deleteOrphanFavorites()
        }

        assertEquals(1, dao.wallpapers.size)
        assertTrue(dao.wallpapers.containsKey("${chatId}_1"))
        assertFalse(dao.wallpapers.containsKey("${chatId}_2"))
        assertFalse(dao.favorites.contains("${chatId}_2"))
    }

    @Test
    fun testFavoritesSyncWithTelegramChannel() = runBlocking {
        val chatId = 12345L
        val dao = FakeWallpaperDao()
        val telegramClient = FakeTelegramClient()
        telegramClient.remoteFavorites = listOf("${chatId}_1", "${chatId}_2")

        val remoteFavorites = telegramClient.fetchFavoritesMessage(chatId)
        val localFavEntities = dao.getAllFavoriteEntities()
        val localFavIds = localFavEntities.map { it.wallpaperId }.toSet()
        val mergedFavIds = (remoteFavorites + localFavIds).distinct()

        val entities = mergedFavIds.map { FavoriteEntity(wallpaperId = it) }
        dao.insertFavorites(entities)
        for (favId in mergedFavIds) {
            dao.updateFavoriteStatus(favId, true)
        }

        assertEquals(2, dao.favorites.size)
        assertTrue(dao.favorites.contains("${chatId}_1"))
        assertTrue(dao.favorites.contains("${chatId}_2"))
    }

    @Test
    fun testCategorySortingAndDeletionDoesNotDeleteWallpapers() = runBlocking {
        val catDao = FakeCategoryDao()
        val wpDao = FakeWallpaperDao()

        // Insert initial categories
        val initialCategories = listOf("Abstract", "AMOLED", "Nature")
        catDao.insertCategories(initialCategories.mapIndexed { idx, name -> CategoryEntity(name = name, sortOrder = idx) })

        // Insert a wallpaper under "AMOLED"
        val wp = WallpaperEntity(
            id = "100_1", messageId = 1L, chatId = 100L, fileId = "f1",
            fileName = "wp1.jpg", mimeType = "image/jpeg", sizeBytes = 100L,
            title = "AMOLED Glow", category = "AMOLED", tagsCsv = "", resolution = "1080x1920",
            aspectRatio = "9:16", colorsCsv = "", description = "", author = "Author 1",
            timestamp = 1000L
        )
        wpDao.insertWallpaper(wp)

        // Verify initial list and wallpaper existence
        assertEquals(listOf("Abstract", "AMOLED", "Nature"), catDao.getCategoryList())
        assertNotNull(wpDao.getWallpaperById("100_1"))

        // Reorder categories: Move "AMOLED" to top
        val reordered = listOf("AMOLED", "Abstract", "Nature")
        catDao.clearCategories()
        catDao.insertCategories(reordered.mapIndexed { idx, name -> CategoryEntity(name = name, sortOrder = idx) })

        assertEquals(listOf("AMOLED", "Abstract", "Nature"), catDao.getCategoryList())

        // Delete "AMOLED" category
        catDao.deleteCategory("AMOLED")

        // Verify "AMOLED" is deleted from category list
        assertEquals(listOf("Abstract", "Nature"), catDao.getCategoryList())

        // Verify wallpaper in "AMOLED" category is NOT deleted
        val wallpaperAfterDelete = wpDao.getWallpaperById("100_1")
        assertNotNull(wallpaperAfterDelete)
        assertEquals("AMOLED Glow", wallpaperAfterDelete?.title)
    }

    @Test
    fun testDefaultCategoriesIsEmpty() {
        assertTrue(WallpaperRepository.DEFAULT_CATEGORIES.isEmpty())
    }

    @Test
    fun testRenameCategory() = runBlocking {
        val catDao = FakeCategoryDao()
        val wpDao = FakeWallpaperDao()
        val telegramClient = FakeTelegramClient()

        catDao.insertCategories(
            listOf(
                CategoryEntity("Abstract", sortOrder = 0),
                CategoryEntity("AMOLED", sortOrder = 1)
            )
        )

        val wp = WallpaperEntity(
            id = "100_1", messageId = 1L, chatId = 100L, fileId = "f1",
            fileName = "wp1.jpg", mimeType = "image/jpeg", sizeBytes = 100L,
            title = "OLED Dark", category = "AMOLED", tagsCsv = "dark", resolution = "1080x1920",
            aspectRatio = "9:16", colorsCsv = "", description = "", author = "Unknown",
            timestamp = System.currentTimeMillis()
        )
        wpDao.insertWallpaper(wp)

        // Perform rename: "AMOLED" -> "Dark AMOLED"
        catDao.renameCategory("AMOLED", "Dark AMOLED")
        wpDao.updateWallpaperCategory("AMOLED", "Dark AMOLED")

        assertEquals(listOf("Abstract", "Dark AMOLED"), catDao.getCategoryList())
        val updatedWp = wpDao.getWallpaperById("100_1")
        assertNotNull(updatedWp)
        assertEquals("Dark AMOLED", updatedWp?.category)
    }
}


