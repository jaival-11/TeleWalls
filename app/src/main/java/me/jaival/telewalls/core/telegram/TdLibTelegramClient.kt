package me.jaival.telewalls.core.telegram

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import dagger.hilt.android.qualifiers.ApplicationContext
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TdLibTelegramClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : TelegramClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val clientMutex = Mutex()

    @Volatile
    private var client: Client? = null

    @Volatile
    private var credentials: TelegramCredentials? = null

    private val _authState = MutableStateFlow<TelegramAuthState>(TelegramAuthState.Uninitialized)
    override val authState: StateFlow<TelegramAuthState> = _authState.asStateFlow()

    private val _connectionState = MutableStateFlow(TelegramConnectionState.CONNECTING)
    override val connectionState: StateFlow<TelegramConnectionState> = _connectionState.asStateFlow()

    private val updates = MutableSharedFlow<TdApi.Object>(
        extraBufferCapacity = 4096,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var isMockMode = false
    private val mockCategories = mutableSetOf<String>()

    companion object {
        private const val TAG = "TdLibTelegramClient"
        private const val VAULT_CHANNEL_TITLE = "TeleWalls Vault"
        private const val METADATA_PREFIX = "{"
        private const val CATEGORIES_HASHTAG = "#Categories"
    }

    override suspend fun start(credentials: TelegramCredentials) {
        clientMutex.withLock {
            this.credentials = credentials
            if (credentials.apiId == 0 || credentials.apiHash.isBlank() || credentials.apiHash == "demo") {
                Log.d(TAG, "Starting in Demo / Mock mode")
                isMockMode = true
                _authState.value = TelegramAuthState.Ready
                _connectionState.value = TelegramConnectionState.READY
                return@withLock
            }

            isMockMode = false
            if (client == null || _authState.value is TelegramAuthState.Failed) {
                _authState.value = TelegramAuthState.Initializing
                try {
                    Client.execute(TdApi.SetLogVerbosityLevel(1))
                    client = Client.create(
                        { update -> handleUpdate(update) },
                        { throwable -> Log.e(TAG, "Update handler error", throwable) },
                        { throwable -> Log.e(TAG, "TDLib exception", throwable) }
                    )
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to initialize TDLib native library", e)
                    // Fallback gracefully so user can explore app UI
                    isMockMode = true
                    _authState.value = TelegramAuthState.Ready
                    _connectionState.value = TelegramConnectionState.READY
                }
            }
        }
    }

    private fun handleUpdate(update: TdApi.Object) {
        updates.tryEmit(update)
        when (update) {
            is TdApi.UpdateAuthorizationState -> handleAuthorizationState(update.authorizationState)
            is TdApi.UpdateConnectionState -> {
                _connectionState.value = when (update.state) {
                    is TdApi.ConnectionStateWaitingForNetwork -> TelegramConnectionState.WAITING_FOR_NETWORK
                    is TdApi.ConnectionStateConnecting,
                    is TdApi.ConnectionStateConnectingToProxy -> TelegramConnectionState.CONNECTING
                    is TdApi.ConnectionStateUpdating -> TelegramConnectionState.UPDATING
                    else -> TelegramConnectionState.READY
                }
            }
        }
    }

    private fun handleAuthorizationState(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val dbDir = File(context.filesDir, "tdlib").absolutePath
                val params = TdApi.SetTdlibParameters().apply {
                    databaseDirectory = dbDir
                    filesDirectory = dbDir
                    useTestDc = false
                    apiId = credentials?.apiId ?: 0
                    apiHash = credentials?.apiHash ?: ""
                    systemLanguageCode = "en"
                    deviceModel = "Android"
                    systemVersion = android.os.Build.VERSION.RELEASE
                    applicationVersion = "1.0.0"
                }
                client?.send(params, null)
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> _authState.value = TelegramAuthState.WaitingForPhoneNumber
            is TdApi.AuthorizationStateWaitCode -> {
                val info = state.codeInfo
                _authState.value = TelegramAuthState.WaitingForCode(
                    phoneNumber = info.phoneNumber,
                    codeLength = info.type.codeLength(),
                    resendTimeoutSeconds = info.timeout
                )
            }
            is TdApi.AuthorizationStateWaitPassword -> {
                _authState.value = TelegramAuthState.WaitingForPassword(state.passwordHint)
            }
            is TdApi.AuthorizationStateWaitOtherDeviceConfirmation -> {
                _authState.value = TelegramAuthState.WaitingForQrScan(state.link)
            }
            is TdApi.AuthorizationStateReady -> {
                _authState.value = TelegramAuthState.Ready
            }
            is TdApi.AuthorizationStateLoggingOut -> {
                _authState.value = TelegramAuthState.LoggingOut
            }
            is TdApi.AuthorizationStateClosed -> {
                _authState.value = TelegramAuthState.Closed
            }
        }
    }

    private fun TdApi.AuthenticationCodeType.codeLength(): Int = when (this) {
        is TdApi.AuthenticationCodeTypeTelegramMessage -> length
        is TdApi.AuthenticationCodeTypeSms -> length
        is TdApi.AuthenticationCodeTypeCall -> length
        else -> 5
    }

    override suspend fun submitPhoneNumber(phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            _authState.value = TelegramAuthState.Failed("Please enter a valid phone number including country code (e.g. +1234567890)")
            return
        }
        if (isMockMode) {
            _authState.value = TelegramAuthState.WaitingForCode(phoneNumber = phoneNumber)
            return
        }
        try {
            sendTd<TdApi.Ok>(TdApi.SetAuthenticationPhoneNumber(phoneNumber, null))
        } catch (e: Exception) {
            _authState.value = TelegramAuthState.Failed(e.message ?: "Failed to submit phone number. Please verify and try again.")
        }
    }

    override suspend fun submitCode(code: String) {
        if (code.isBlank()) {
            _authState.value = TelegramAuthState.Failed("Verification code cannot be empty.")
            return
        }
        if (isMockMode) {
            _authState.value = TelegramAuthState.Ready
            return
        }
        try {
            sendTd<TdApi.Ok>(TdApi.CheckAuthenticationCode(code))
        } catch (e: Exception) {
            _authState.value = TelegramAuthState.Failed(e.message ?: "Invalid verification code. Please check and try again.")
        }
    }

    override suspend fun submitPassword(password: String) {
        if (password.isBlank()) {
            _authState.value = TelegramAuthState.Failed("2FA Password cannot be empty.")
            return
        }
        if (isMockMode) {
            _authState.value = TelegramAuthState.Ready
            return
        }
        try {
            sendTd<TdApi.Ok>(TdApi.CheckAuthenticationPassword(password))
        } catch (e: Exception) {
            _authState.value = TelegramAuthState.Failed(e.message ?: "Invalid 2FA password. Please try again.")
        }
    }

    override suspend fun requestQrCodeAuthentication() {
        if (isMockMode) {
            _authState.value = TelegramAuthState.WaitingForQrScan("https://t.me/loginQRDemo")
            return
        }
        try {
            sendTd<TdApi.Ok>(TdApi.RequestQrCodeAuthentication(longArrayOf()))
        } catch (e: Exception) {
            _authState.value = TelegramAuthState.Failed(e.message ?: "Failed to generate QR code.")
        }
    }

    override suspend fun resetAuthState() {
        _authState.value = TelegramAuthState.WaitingForPhoneNumber
    }

    override suspend fun logout() {
        if (isMockMode) {
            _authState.value = TelegramAuthState.Uninitialized
            return
        }
        try {
            sendTd<TdApi.Ok>(TdApi.LogOut())
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
        }
        _authState.value = TelegramAuthState.Uninitialized
    }

    override suspend fun ensureStorageChat(knownChatId: Long?): Long {
        if (isMockMode) return 99999L
        if (knownChatId != null && knownChatId != 0L) return knownChatId
        
        val channels = listStorageChannels()
        if (channels.isNotEmpty()) {
            return channels.first().chatId
        }
        val created = createStorageChannel(VAULT_CHANNEL_TITLE)
        return created.chatId
    }

    override suspend fun listStorageChannels(): List<StorageChannel> {
        if (isMockMode) {
            return listOf(StorageChannel(99999L, "TeleWalls Vault (Demo)", 12))
        }
        val chats = mutableListOf<StorageChannel>()
        try {
            val result = sendTd<TdApi.Chats>(TdApi.GetChats(TdApi.ChatListMain(), 100))
            for (chatId in result.chatIds) {
                val chat = sendTd<TdApi.Chat>(TdApi.GetChat(chatId))
                if (chat.type is TdApi.ChatTypeSupergroup) {
                    val supergroup = chat.type as TdApi.ChatTypeSupergroup
                    if (supergroup.isChannel) {
                        chats.add(StorageChannel(chat.id, chat.title))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing storage channels", e)
        }
        return chats
    }

    override suspend fun createStorageChannel(title: String): StorageChannel {
        if (isMockMode) {
            return StorageChannel(99999L, title, 0)
        }
        val chat = sendTd<TdApi.Chat>(
            TdApi.CreateNewSupergroupChat(
                title,
                false,
                true,
                "TeleWalls Storage Vault",
                null,
                0,
                false
            )
        )
        return StorageChannel(chat.id, chat.title)
    }

    override fun uploadWallpaper(
        chatId: Long,
        localPath: String,
        fileName: String,
        mimeType: String,
        metadata: WallpaperMetadata
    ): Flow<TelegramUploadEvent> = callbackFlow {
        if (isMockMode) {
            for (progress in 1..10) {
                delay(150)
                trySend(TelegramUploadEvent.Progress(progress * 100000L, 1000000L))
            }
            val mockDoc = WallpaperDocument(
                messageId = System.currentTimeMillis(),
                chatId = chatId,
                fileId = "mock_file_${System.currentTimeMillis()}",
                fileName = fileName,
                mimeType = mimeType,
                sizeBytes = File(localPath).length().coerceAtLeast(2048000L),
                localPath = localPath,
                thumbnailPath = localPath,
                metadata = metadata
            )
            trySend(TelegramUploadEvent.Succeeded(mockDoc))
            close()
            return@callbackFlow
        }

        val jsonCaption = buildCaptionString(metadata)
        val content = TdApi.InputMessageDocument(
            TdApi.InputFileLocal(localPath),
            null,
            true,
            TdApi.FormattedText(jsonCaption, emptyArray())
        )

        val pendingMsgId = CompletableDeferred<Long>()
        val pendingFileId = CompletableDeferred<Int>()

        val job = scope.launch {
            updates.collect { update ->
                when (update) {
                    is TdApi.UpdateFile -> {
                        if (pendingFileId.isCompleted && update.file.id == pendingFileId.await()) {
                            val uploaded = update.file.remote?.uploadedSize ?: 0L
                            val total = update.file.size.takeIf { it > 0 } ?: update.file.expectedSize
                            trySend(TelegramUploadEvent.Progress(uploaded, total))
                        }
                    }
                    is TdApi.UpdateMessageSendSucceeded -> {
                        if (pendingMsgId.isCompleted && update.oldMessageId == pendingMsgId.await()) {
                            val doc = parseDocumentFromMessage(update.message, metadata)
                            if (doc != null) {
                                trySend(TelegramUploadEvent.Succeeded(doc))
                                close()
                            }
                        }
                    }
                    is TdApi.UpdateMessageSendFailed -> {
                        if (pendingMsgId.isCompleted && update.oldMessageId == pendingMsgId.await()) {
                            trySend(TelegramUploadEvent.Failed(update.error?.message ?: "Upload failed"))
                            close()
                        }
                    }
                }
            }
        }

        try {
            val msg = sendTd<TdApi.Message>(TdApi.SendMessage(chatId, null, null, null, null, content))
            pendingMsgId.complete(msg.id)
            if (msg.content is TdApi.MessageDocument) {
                val file = (msg.content as TdApi.MessageDocument).document.document
                pendingFileId.complete(file.id)
            }
        } catch (e: Exception) {
            trySend(TelegramUploadEvent.Failed(e.message ?: "Upload failed"))
            close()
        }

        awaitClose { job.cancel() }
    }

    override suspend fun fetchWallpapers(
        chatId: Long,
        fromMessageId: Long,
        limit: Int
    ): List<WallpaperDocument> {
        if (isMockMode) return getMockWallpapers(chatId)

        val documents = mutableListOf<WallpaperDocument>()
        try {
            val searchResult = sendTd<TdApi.FoundChatMessages>(
                TdApi.SearchChatMessages(
                    chatId,
                    null,
                    "",
                    null,
                    fromMessageId,
                    0,
                    limit,
                    TdApi.SearchMessagesFilterDocument()
                )
            )
            for (msg in searchResult.messages) {
                val doc = parseDocumentFromMessage(msg, null)
                if (doc != null) {
                    documents.add(doc)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching wallpapers from channel", e)
        }
        return documents
    }

    private fun getMockWallpapers(chatId: Long): List<WallpaperDocument> {
        return listOf(
            WallpaperDocument(
                messageId = 1001L,
                chatId = chatId,
                fileId = "mock_file_1",
                fileName = "neon_cyberpunk.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 3456789L,
                localPath = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?q=80&w=1080",
                thumbnailPath = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?q=80&w=400",
                metadata = WallpaperMetadata(
                    title = "Neon Cyberpunk Skyline",
                    category = "Sci-Fi",
                    tags = listOf("AMOLED", "Neon", "City", "Futuristic"),
                    resolution = "1440x3200",
                    aspectRatio = "9:20",
                    colors = listOf("#0F0C20", "#FF007F", "#00F0FF"),
                    description = "A stunning futuristic cyberpunk cityscape with vivid neon glows.",
                    author = "TeleWalls Curator",
                    timestamp = System.currentTimeMillis() - 86400000L
                )
            ),
            WallpaperDocument(
                messageId = 1002L,
                chatId = chatId,
                fileId = "mock_file_2",
                fileName = "cosmic_nebula.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 4123456L,
                localPath = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=1080",
                thumbnailPath = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=400",
                metadata = WallpaperMetadata(
                    title = "Deep Space Nebula",
                    category = "AMOLED",
                    tags = listOf("AMOLED", "Space", "Stars", "Dark"),
                    resolution = "1440x3200",
                    aspectRatio = "9:20",
                    colors = listOf("#05050A", "#4B0082", "#8A2BE2"),
                    description = "Pure black AMOLED space landscape featuring vibrant purple interstellar gas clouds.",
                    author = "TeleWalls Curator",
                    timestamp = System.currentTimeMillis() - 172800000L
                )
            ),
            WallpaperDocument(
                messageId = 1003L,
                chatId = chatId,
                fileId = "mock_file_3",
                fileName = "misty_forest_peaks.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 2987654L,
                localPath = "https://images.unsplash.com/photo-1448375240586-882707db888b?q=80&w=1080",
                thumbnailPath = "https://images.unsplash.com/photo-1448375240586-882707db888b?q=80&w=400",
                metadata = WallpaperMetadata(
                    title = "Misty Pine Forest",
                    category = "Nature",
                    tags = listOf("Nature", "Forest", "Mist", "Green"),
                    resolution = "1440x3200",
                    aspectRatio = "9:20",
                    colors = listOf("#1E2D24", "#3B5249", "#8AA29E"),
                    description = "Serene pine trees shrouded in morning mountain fog.",
                    author = "Nature Photography",
                    timestamp = System.currentTimeMillis() - 259200000L
                )
            ),
            WallpaperDocument(
                messageId = 1004L,
                chatId = chatId,
                fileId = "mock_file_4",
                fileName = "minimal_geometry.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 1876543L,
                localPath = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?q=80&w=1080",
                thumbnailPath = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?q=80&w=400",
                metadata = WallpaperMetadata(
                    title = "Abstract Geometric Waves",
                    category = "Minimal",
                    tags = listOf("Minimal", "Clean", "Vector", "Modern"),
                    resolution = "1440x3200",
                    aspectRatio = "9:20",
                    colors = listOf("#121212", "#E0E0E0", "#FF6B6B"),
                    description = "Clean aesthetic geometric composition with minimal lines.",
                    author = "Design Studio",
                    timestamp = System.currentTimeMillis() - 345600000L
                )
            ),
            WallpaperDocument(
                messageId = 1005L,
                chatId = chatId,
                fileId = "mock_file_5",
                fileName = "hypercar_night.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 3890123L,
                localPath = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?q=80&w=1080",
                thumbnailPath = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?q=80&w=400",
                metadata = WallpaperMetadata(
                    title = "Midnight Supercar",
                    category = "Cars",
                    tags = listOf("Cars", "Speed", "Black", "AMOLED"),
                    resolution = "1440x3200",
                    aspectRatio = "9:20",
                    colors = listOf("#080808", "#D32F2F", "#FFFFFF"),
                    description = "Sleek sports car lit by subtle atmospheric night lighting.",
                    author = "AutoVision",
                    timestamp = System.currentTimeMillis() - 432000000L
                )
            ),
            WallpaperDocument(
                messageId = 1006L,
                chatId = chatId,
                fileId = "mock_file_6",
                fileName = "gothic_cathedral.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 4500123L,
                localPath = "https://images.unsplash.com/photo-1513694203232-719a280e022f?q=80&w=1080",
                thumbnailPath = "https://images.unsplash.com/photo-1513694203232-719a280e022f?q=80&w=400",
                metadata = WallpaperMetadata(
                    title = "Monochrome Architecture",
                    category = "Architecture",
                    tags = listOf("Architecture", "Monochrome", "Structures", "Lines"),
                    resolution = "1440x3200",
                    aspectRatio = "9:20",
                    colors = listOf("#1A1A1A", "#777777", "#FFFFFF"),
                    description = "Dramatic architectural angles in black and white high contrast.",
                    author = "ArchDaily",
                    timestamp = System.currentTimeMillis() - 518400000L
                )
            )
        )
    }

    override suspend fun downloadWallpaperFile(fileId: String, destinationPath: String): String? {
        if (isMockMode) return destinationPath
        try {
            val fileIdInt = fileId.toIntOrNull() ?: return null
            sendTd<TdApi.Ok>(TdApi.DownloadFile(fileIdInt, 32, 0, 0, true))
            
            // Wait for file download completion
            for (i in 0..100) {
                delay(200)
                val fileInfo = sendTd<TdApi.File>(TdApi.GetFile(fileIdInt))
                if (fileInfo.local?.isDownloadingCompleted == true) {
                    return fileInfo.local.path
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file $fileId", e)
        }
        return null
    }

    override suspend fun deleteWallpaper(chatId: Long, messageId: Long): Boolean {
        if (isMockMode) return true
        return try {
            sendTd<TdApi.Ok>(TdApi.DeleteMessages(chatId, longArrayOf(messageId), true))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message $messageId", e)
            false
        }
    }

    override suspend fun fetchCategoriesMessage(chatId: Long): List<String> {
        if (isMockMode) {
            val prefs = context.getSharedPreferences("telewalls_mock_prefs", Context.MODE_PRIVATE)
            val saved = prefs.getStringSet("mock_categories", emptySet()) ?: emptySet()
            val defaults = listOf("AMOLED", "Nature", "Minimal", "Sci-Fi", "Architecture", "Abstract", "Cars")
            return (defaults + mockCategories + saved).distinct()
        }

        try {
            val searchResult = sendTd<TdApi.FoundChatMessages>(
                TdApi.SearchChatMessages(
                    chatId,
                    null,
                    CATEGORIES_HASHTAG,
                    null,
                    0L,
                    0,
                    20,
                    null
                )
            )
            val combinedCategories = mutableSetOf<String>()
            for (msg in searchResult.messages) {
                val categories = parseCategoriesFromMessage(msg)
                combinedCategories.addAll(categories)
            }
            if (combinedCategories.isNotEmpty()) {
                return combinedCategories.toList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching categories message from Telegram channel", e)
        }
        return emptyList()
    }

    override suspend fun saveCategoriesMessage(chatId: Long, categories: List<String>): Boolean {
        if (isMockMode) {
            mockCategories.addAll(categories)
            val prefs = context.getSharedPreferences("telewalls_mock_prefs", Context.MODE_PRIVATE)
            prefs.edit().putStringSet("mock_categories", mockCategories.toSet()).apply()
            return true
        }

        return try {
            val json = gson.toJson(categories)
            val messageText = "$CATEGORIES_HASHTAG\n$json"
            val inputContent = TdApi.InputMessageText(
                TdApi.FormattedText(messageText, emptyArray()),
                null,
                true
            )

            val searchResult = try {
                sendTd<TdApi.FoundChatMessages>(
                    TdApi.SearchChatMessages(
                        chatId,
                        null,
                        CATEGORIES_HASHTAG,
                        null,
                        0L,
                        0,
                        1,
                        null
                    )
                )
            } catch (e: Exception) { null }

            val existingMsg = searchResult?.messages?.firstOrNull()

            if (existingMsg != null) {
                sendTd<TdApi.Message>(
                    TdApi.EditMessageText(
                        chatId,
                        existingMsg.id,
                        null,
                        inputContent
                    )
                )
            } else {
                sendTd<TdApi.Message>(
                    TdApi.SendMessage(
                        chatId,
                        null,
                        null,
                        null,
                        null,
                        inputContent
                    )
                )
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving categories message to Telegram channel", e)
            false
        }
    }

    private fun parseCategoriesFromMessage(msg: TdApi.Message): List<String> {
        val text = when (val content = msg.content) {
            is TdApi.MessageText -> content.text.text.orEmpty()
            is TdApi.MessageDocument -> content.caption.text.orEmpty()
            is TdApi.MessagePhoto -> content.caption.text.orEmpty()
            else -> ""
        }
        if (!text.contains(CATEGORIES_HASHTAG, ignoreCase = true)) return emptyList()

        val index = text.indexOf(CATEGORIES_HASHTAG, ignoreCase = true)
        val afterHashtag = text.substring(index + CATEGORIES_HASHTAG.length).trim()

        try {
            val jsonStart = afterHashtag.indexOf("[")
            val jsonEnd = afterHashtag.lastIndexOf("]")
            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                val jsonStr = afterHashtag.substring(jsonStart, jsonEnd + 1)
                val listType = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                val parsed: List<String>? = gson.fromJson(jsonStr, listType)
                if (!parsed.isNullOrEmpty()) {
                    return parsed.map { it.trim() }.filter { it.isNotBlank() }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing JSON category list from message", e)
        }

        return afterHashtag.lines()
            .map { it.trim().removePrefix("-").removePrefix("*").trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
    }

    private fun buildCaptionString(metadata: WallpaperMetadata): String {
        val hashtags = (listOf(metadata.category) + metadata.tags)
            .filter { it.isNotBlank() }
            .joinToString(" ") { "#${it.replace(" ", "")}" }
        val json = gson.toJson(metadata)
        return "$hashtags\n$json"
    }

    private fun parseDocumentFromMessage(
        msg: TdApi.Message,
        fallbackMetadata: WallpaperMetadata?
    ): WallpaperDocument? {
        if (msg.content !is TdApi.MessageDocument) return null
        val docContent = msg.content as TdApi.MessageDocument
        val doc = docContent.document
        val captionText = docContent.caption.text.orEmpty()

        val metadata = parseMetadataFromCaption(captionText) ?: fallbackMetadata ?: WallpaperMetadata(
            title = doc.fileName.substringBeforeLast("."),
            category = "General",
            sizeBytes = doc.document.size
        )

        return WallpaperDocument(
            messageId = msg.id,
            chatId = msg.chatId,
            fileId = doc.document.id.toString(),
            fileName = doc.fileName,
            mimeType = doc.mimeType,
            sizeBytes = doc.document.size,
            localPath = doc.document.local.path.ifEmpty { null },
            thumbnailPath = doc.thumbnail?.file?.local?.path?.ifEmpty { null },
            metadata = metadata
        )
    }

    private fun parseMetadataFromCaption(caption: String): WallpaperMetadata? {
        return try {
            val jsonStart = caption.indexOf(METADATA_PREFIX)
            if (jsonStart != -1) {
                val jsonStr = caption.substring(jsonStart)
                gson.fromJson(jsonStr, WallpaperMetadata::class.java)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend inline fun <reified T : TdApi.Object> sendTd(query: TdApi.Function<out TdApi.Object>): T {
        val activeClient = client ?: throw IllegalStateException("TDLib client not initialized")
        return suspendCancellableCoroutine { continuation ->
            activeClient.send(query) { result ->
                when (result) {
                    is T -> continuation.resume(result)
                    is TdApi.Error -> continuation.resumeWithException(Exception(result.message))
                    else -> continuation.resumeWithException(Exception("Unexpected response: $result"))
                }
            }
        }
    }
}
