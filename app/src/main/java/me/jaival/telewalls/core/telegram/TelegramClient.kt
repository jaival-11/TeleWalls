package me.jaival.telewalls.core.telegram

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TelegramClient {
    val authState: StateFlow<TelegramAuthState>
    val connectionState: StateFlow<TelegramConnectionState>

    suspend fun start(credentials: TelegramCredentials)
    suspend fun submitPhoneNumber(phoneNumber: String)
    suspend fun submitCode(code: String)
    suspend fun submitPassword(password: String)
    suspend fun requestQrCodeAuthentication()
    suspend fun resetAuthState()
    suspend fun logout()
    
    suspend fun ensureStorageChat(knownChatId: Long? = null): Long
    suspend fun listStorageChannels(): List<StorageChannel>
    suspend fun createStorageChannel(title: String): StorageChannel
    
    fun uploadWallpaper(
        chatId: Long,
        localPath: String,
        fileName: String,
        mimeType: String,
        metadata: WallpaperMetadata
    ): Flow<TelegramUploadEvent>
    
    suspend fun fetchWallpapers(
        chatId: Long,
        fromMessageId: Long = 0L,
        limit: Int = 50
    ): List<WallpaperDocument>

    suspend fun downloadWallpaperFile(
        fileId: String,
        destinationPath: String
    ): String?

    suspend fun fetchThumbnail(chatId: Long, messageId: Long): String?

    suspend fun deleteWallpaper(chatId: Long, messageId: Long): Boolean
    suspend fun editWallpaperMetadata(chatId: Long, messageId: Long, metadata: WallpaperMetadata): Boolean

    suspend fun fetchCategoriesMessage(chatId: Long): List<String>
    suspend fun saveCategoriesMessage(chatId: Long, categories: List<String>): Boolean

    suspend fun fetchFavoritesMessage(chatId: Long): List<String>
    suspend fun saveFavoritesMessage(chatId: Long, favorites: List<String>): Boolean
}
