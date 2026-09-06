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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import me.jaival.telewalls.BuildConfig
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
    private val mockFavorites = mutableSetOf<String>()

    companion object {
        private const val TAG = "TdLibTelegramClient"
        private const val VAULT_CHANNEL_TITLE = "TeleWalls Vault"
        private const val METADATA_PREFIX = "{"
        private const val CATEGORIES_HASHTAG = "#Categories"
        private const val FAVORITES_HASHTAG = "#Favorites"
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

        val job = scope.launch {
            try {
                // Step 1: Create a compressed 600px thumbnail file from localPath
                val thumbFile = createThumbnailFile(localPath)
                val jsonCaption = buildCaptionString(metadata)

                val inputThumbnail = thumbFile?.let {
                    val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeFile(it.absolutePath, opts)
                    TdApi.InputThumbnail(TdApi.InputFileLocal(it.absolutePath), opts.outWidth, opts.outHeight)
                }

                val docContent = TdApi.InputMessageDocument().apply {
                    document = TdApi.InputFileLocal(localPath)
                    thumbnail = inputThumbnail
                    disableContentTypeDetection = true
                    caption = TdApi.FormattedText(jsonCaption, emptyArray())
                }

                val pendingMsgId = CompletableDeferred<Long>()
                val pendingFileId = CompletableDeferred<Int>()

                val updateCollector = launch {
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
                                    val doc = parseWallpaperFromMessage(update.message, metadata)
                                    if (doc != null) {
                                        val finalDoc = doc.copy(
                                            localPath = localPath,
                                            thumbnailPath = thumbFile?.absolutePath ?: doc.thumbnailPath
                                        )
                                        trySend(TelegramUploadEvent.Succeeded(finalDoc))
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

                val msg = sendTd<TdApi.Message>(TdApi.SendMessage(chatId, null, null, null, null, docContent))
                pendingMsgId.complete(msg.id)
                if (msg.content is TdApi.MessageDocument) {
                    val file = (msg.content as TdApi.MessageDocument).document.document
                    pendingFileId.complete(file.id)
                }
            } catch (e: Exception) {
                trySend(TelegramUploadEvent.Failed(e.message ?: "Upload failed"))
                close()
            }
        }

        awaitClose { job.cancel() }
    }

    private fun createThumbnailFile(localPath: String): File? {
        return try {
            val srcFile = File(localPath)
            if (!srcFile.exists()) return null
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeFile(localPath, options)
            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            val targetSize = 1000
            val scale = maxOf(1, maxOf(origWidth, origHeight) / targetSize)
            val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            val bitmap = android.graphics.BitmapFactory.decodeFile(localPath, decodeOptions) ?: return null
            val destFile = File(context.cacheDir, "thumb_${System.currentTimeMillis()}_${srcFile.name}")
            java.io.FileOutputStream(destFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
            }
            destFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating thumbnail file", e)
            null
        }
    }

    private suspend fun ensureChatLoaded(chatId: Long) {
        if (isMockMode || chatId == 0L) return
        try {
            try {
                sendTd<TdApi.Ok>(TdApi.LoadChats(TdApi.ChatListMain(), 100))
            } catch (e: Exception) {
                Log.d(TAG, "LoadChats note: ${e.message}")
            }

            try {
                sendTd<TdApi.Chat>(TdApi.GetChat(chatId))
                return
            } catch (e: Exception) {
                Log.w(TAG, "Direct GetChat failed for chatId=$chatId: ${e.message}, trying CreateSupergroupChat fallback")
            }

            if (chatId < -1000000000000L) {
                val supergroupId = -chatId - 1000000000000L
                try {
                    sendTd<TdApi.Chat>(TdApi.CreateSupergroupChat(supergroupId, false))
                    Log.d(TAG, "Successfully loaded chat via CreateSupergroupChat for supergroupId=$supergroupId")
                    return
                } catch (e: Exception) {
                    Log.e(TAG, "CreateSupergroupChat failed for supergroupId=$supergroupId", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ensureChatLoaded failed for chatId=$chatId: ${e.message}")
        }
    }

    override suspend fun fetchWallpapers(
        chatId: Long,
        fromMessageId: Long,
        limit: Int
    ): List<WallpaperDocument> {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[REINDEX DEBUG] fetchWallpapers starting for chatId=$chatId, fromMessageId=$fromMessageId, limit=$limit, isMockMode=$isMockMode")
        }
        if (isMockMode) return getMockWallpapers(chatId)

        ensureChatLoaded(chatId)

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
                    null
                )
            )
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[REINDEX DEBUG] SearchChatMessages returned ${searchResult.messages.size} raw messages from Telegram for chatId=$chatId")
            }
            for (msg in searchResult.messages) {
                val doc = parseWallpaperFromMessage(msg, null)
                if (doc != null) {
                    documents.add(doc)
                } else if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[REINDEX DEBUG] Message #${msg.id} in chatId=$chatId could not be parsed into WallpaperDocument")
                }
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[REINDEX DEBUG] Successfully parsed ${documents.size} / ${searchResult.messages.size} valid WallpaperDocuments for chatId=$chatId")
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[REINDEX DEBUG] Exception in fetchWallpapers for chatId=$chatId: ${e.message}", e)
            }
            Log.e(TAG, "Error fetching wallpapers from channel", e)
            throw e
        }
        return documents
    }

    override suspend fun fetchThumbnail(chatId: Long, messageId: Long): String? {
        if (isMockMode) return null
        return try {
            val cacheFile = File(context.cacheDir, "thumb_${chatId}_${messageId}.jpg")
            if (cacheFile.exists() && cacheFile.length() > 0) {
                return cacheFile.absolutePath
            }

            val msg = sendTd<TdApi.Message>(TdApi.GetMessage(chatId, messageId))
            val docParsed = parseWallpaperFromMessage(msg, null)

            // 1. Check thumbnailFileId in metadata if present
            val thumbFileIdFromMeta = docParsed?.metadata?.thumbnailFileId
            if (!thumbFileIdFromMeta.isNullOrBlank()) {
                val downloaded = downloadWallpaperFile(thumbFileIdFromMeta, cacheFile.name)
                if (!downloaded.isNullOrBlank() && File(downloaded).exists() && File(downloaded).length() > 0) {
                    return downloaded
                }
            }

            // 2. Check Document thumbnail
            when (val content = msg.content) {
                is TdApi.MessageDocument -> {
                    val thumbnail = content.document.thumbnail
                    if (thumbnail != null) {
                        val thumbFile = thumbnail.file
                        if (thumbFile.local?.isDownloadingCompleted == true && !thumbFile.local.path.isNullOrBlank() && File(thumbFile.local.path).exists()) {
                            return thumbFile.local.path
                        }
                        val downloaded = sendTd<TdApi.File>(
                            TdApi.DownloadFile(thumbFile.id, 32, 0, 0, true)
                        )
                        val path = downloaded.local?.path?.takeIf { it.isNotBlank() }
                        if (path != null && File(path).exists() && File(path).length() > 0) {
                            return path
                        }
                    }

                    // 3. Fallback: Download Document file if image
                    val doc = content.document
                    val docFile = doc.document
                    val mime = doc.mimeType.orEmpty()
                    val docFileName = doc.fileName.orEmpty()
                    if (mime.startsWith("image/", ignoreCase = true) || docFileName.endsWith(".jpg", ignoreCase = true) || docFileName.endsWith(".png", ignoreCase = true) || docFileName.endsWith(".jpeg", ignoreCase = true) || docFileName.endsWith(".webp", ignoreCase = true)) {
                        if (docFile.local?.isDownloadingCompleted == true && !docFile.local.path.isNullOrBlank() && File(docFile.local.path).exists()) {
                            return docFile.local.path
                        }
                        val downloaded = sendTd<TdApi.File>(
                            TdApi.DownloadFile(docFile.id, 32, 0, 0, true)
                        )
                        val path = downloaded.local?.path?.takeIf { it.isNotBlank() }
                        if (path != null && File(path).exists() && File(path).length() > 0) {
                            return path
                        }
                    }
                }
                is TdApi.MessagePhoto -> {
                    val sizes = content.photo.sizes
                    val targetSize = sizes.minByOrNull { kotlin.math.abs(it.width - 1000) }
                        ?: sizes.find { it.type == "m" || it.type == "x" || it.type == "y" }
                        ?: sizes.maxByOrNull { it.photo.size }
                    if (targetSize != null) {
                        val thumbFile = targetSize.photo
                        if (thumbFile.local?.isDownloadingCompleted == true && !thumbFile.local.path.isNullOrBlank() && File(thumbFile.local.path).exists()) {
                            return thumbFile.local.path
                        }
                        val downloaded = sendTd<TdApi.File>(
                            TdApi.DownloadFile(thumbFile.id, 32, 0, 0, true)
                        )
                        val path = downloaded.local?.path?.takeIf { it.isNotBlank() }
                        if (path != null && File(path).exists() && File(path).length() > 0) {
                            return path
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching thumbnail on demand for message $messageId", e)
            null
        }
    }

    private fun saveByteArrayToCache(fileName: String, data: ByteArray): String? {
        return try {
            val cacheFile = File(context.cacheDir, fileName)
            cacheFile.writeBytes(data)
            cacheFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving byte array to cache", e)
            null
        }
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
                localPath = null,
                thumbnailPath = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?q=80&w=800",
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
                localPath = null,
                thumbnailPath = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=800",
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
                localPath = null,
                thumbnailPath = "https://images.unsplash.com/photo-1448375240586-882707db888b?q=80&w=800",
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
                localPath = null,
                thumbnailPath = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?q=80&w=800",
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
                localPath = null,
                thumbnailPath = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?q=80&w=800",
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
                localPath = null,
                thumbnailPath = "https://images.unsplash.com/photo-1513694203232-719a280e022f?q=80&w=800",
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
        if (isMockMode) {
            return getMockFullImagePath(fileId, destinationPath)
        }
        try {
            val fileInfo = try {
                val fileIdInt = fileId.toIntOrNull()
                if (fileIdInt != null) {
                    sendTd<TdApi.File>(TdApi.GetFile(fileIdInt))
                } else {
                    sendTd<TdApi.File>(TdApi.GetRemoteFile(fileId, TdApi.FileTypeUnknown()))
                }
            } catch (e: Exception) {
                val fileIdInt = fileId.toIntOrNull() ?: return null
                sendTd<TdApi.File>(TdApi.GetFile(fileIdInt))
            }

            return downloadTdFile(fileInfo, destinationPath, timeoutMs = 15000)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file $fileId", e)
        }
        return null
    }

    private suspend fun downloadTdFile(file: TdApi.File, destinationPath: String, timeoutMs: Long = 15000): String? {
        try {
            val initialPath = file.local?.path
            if (file.local?.isDownloadingCompleted == true && !initialPath.isNullOrBlank() && File(initialPath).exists() && File(initialPath).length() > 0) {
                return copyToDestinationIfNeeded(initialPath, destinationPath)
            }

            val tdFileId = file.id
            sendTd<TdApi.Ok>(TdApi.DownloadFile(tdFileId, 32, 0, 0, false))

            val maxIterations = (timeoutMs / 100).toInt()
            for (i in 0..maxIterations) {
                delay(100)
                val updatedFile = sendTd<TdApi.File>(TdApi.GetFile(tdFileId))
                val currentPath = updatedFile.local?.path
                if (!currentPath.isNullOrBlank() && File(currentPath).exists() && File(currentPath).length() > 0) {
                    if (updatedFile.local?.isDownloadingCompleted == true || updatedFile.local?.downloadedSize == updatedFile.expectedSize) {
                        return copyToDestinationIfNeeded(currentPath, destinationPath)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading TDLib file ${file.id}", e)
        }
        return null
    }

    private fun copyToDestinationIfNeeded(srcPath: String, destinationPath: String): String? {
        if (destinationPath.isNotBlank() && destinationPath != srcPath) {
            try {
                val srcFile = File(srcPath)
                val destFile = File(destinationPath)
                if (srcFile.exists() && srcFile.length() > 0) {
                    destFile.parentFile?.mkdirs()
                    srcFile.copyTo(destFile, overwrite = true)
                    if (destFile.exists() && destFile.length() > 0) {
                        return destFile.absolutePath
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed copying downloaded file $srcPath to destinationPath $destinationPath", e)
            }
        }
        return if (srcPath.isNotBlank() && File(srcPath).exists() && File(srcPath).length() > 0) srcPath else null
    }

    private fun getMockFullImagePath(fileId: String, fallbackPath: String): String {
        return when (fileId) {
            "mock_file_1" -> "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?q=80&w=1080"
            "mock_file_2" -> "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=1080"
            "mock_file_3" -> "https://images.unsplash.com/photo-1448375240586-882707db888b?q=80&w=1080"
            "mock_file_4" -> "https://images.unsplash.com/photo-1541701494587-cb58502866ab?q=80&w=1080"
            "mock_file_5" -> "https://images.unsplash.com/photo-1503376780353-7e6692767b70?q=80&w=1080"
            "mock_file_6" -> "https://images.unsplash.com/photo-1513694203232-719a280e022f?q=80&w=1080"
            else -> {
                if (fallbackPath.startsWith("/") && File(fallbackPath).exists()) {
                    fallbackPath
                } else if (fallbackPath.startsWith("http://") || fallbackPath.startsWith("https://")) {
                    fallbackPath
                } else {
                    "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?q=80&w=1080"
                }
            }
        }
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

    override suspend fun editWallpaperMetadata(
        chatId: Long,
        messageId: Long,
        metadata: WallpaperMetadata
    ): Boolean {
        if (isMockMode) return true
        return try {
            val jsonCaption = buildCaptionString(metadata)
            sendTd<TdApi.Message>(
                TdApi.EditMessageCaption(
                    chatId,
                    messageId,
                    null,
                    TdApi.FormattedText(jsonCaption, emptyArray()),
                    false
                )
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error editing message caption $messageId", e)
            false
        }
    }

    override suspend fun fetchCategoriesMessage(chatId: Long): List<String> {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[REINDEX DEBUG] fetchCategoriesMessage starting for chatId=$chatId, isMockMode=$isMockMode")
        }
        if (isMockMode) {
            val prefs = context.getSharedPreferences("telewalls_mock_prefs", Context.MODE_PRIVATE)
            val saved = prefs.getStringSet("mock_categories", emptySet()) ?: emptySet()
            val defaults = emptyList<String>()
            return (defaults + mockCategories + saved).distinct()
        }

        ensureChatLoaded(chatId)

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
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[REINDEX DEBUG] fetchCategoriesMessage found ${searchResult.messages.size} messages with hashtag '$CATEGORIES_HASHTAG'")
            }
            val combinedCategories = mutableSetOf<String>()
            for (msg in searchResult.messages) {
                val categories = parseCategoriesFromMessage(msg)
                combinedCategories.addAll(categories)
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[REINDEX DEBUG] Parsed categories list: $combinedCategories")
            }
            if (combinedCategories.isNotEmpty()) {
                return combinedCategories.toList()
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[REINDEX DEBUG] Exception in fetchCategoriesMessage for chatId=$chatId: ${e.message}", e)
            }
            Log.e(TAG, "Error fetching categories message from Telegram channel", e)
            throw e
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

    override suspend fun fetchFavoritesMessage(chatId: Long): List<String> {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[REINDEX DEBUG] fetchFavoritesMessage starting for chatId=$chatId, isMockMode=$isMockMode")
        }
        if (isMockMode) {
            val prefs = context.getSharedPreferences("telewalls_mock_prefs", Context.MODE_PRIVATE)
            val saved = prefs.getStringSet("mock_favorites", emptySet()) ?: emptySet()
            return (mockFavorites + saved).distinct()
        }

        ensureChatLoaded(chatId)

        try {
            val searchResult = sendTd<TdApi.FoundChatMessages>(
                TdApi.SearchChatMessages(
                    chatId,
                    null,
                    FAVORITES_HASHTAG,
                    null,
                    0L,
                    0,
                    20,
                    null
                )
            )
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[REINDEX DEBUG] fetchFavoritesMessage found ${searchResult.messages.size} messages with hashtag '$FAVORITES_HASHTAG'")
            }
            val combinedFavorites = mutableSetOf<String>()
            for (msg in searchResult.messages) {
                val favorites = parseFavoritesFromMessage(msg)
                combinedFavorites.addAll(favorites)
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[REINDEX DEBUG] Parsed favorites list: $combinedFavorites")
            }
            if (combinedFavorites.isNotEmpty()) {
                return combinedFavorites.toList()
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[REINDEX DEBUG] Exception in fetchFavoritesMessage for chatId=$chatId: ${e.message}", e)
            }
            Log.e(TAG, "Error fetching favorites message from Telegram channel", e)
            throw e
        }
        return emptyList()
    }

    override suspend fun saveFavoritesMessage(chatId: Long, favorites: List<String>): Boolean {
        if (isMockMode) {
            mockFavorites.clear()
            mockFavorites.addAll(favorites)
            val prefs = context.getSharedPreferences("telewalls_mock_prefs", Context.MODE_PRIVATE)
            prefs.edit().putStringSet("mock_favorites", mockFavorites.toSet()).apply()
            return true
        }

        return try {
            val json = gson.toJson(favorites)
            val messageText = "$FAVORITES_HASHTAG\n$json"
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
                        FAVORITES_HASHTAG,
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
            Log.e(TAG, "Error saving favorites message to Telegram channel", e)
            false
        }
    }

    private fun parseFavoritesFromMessage(msg: TdApi.Message): List<String> {
        val text = when (val content = msg.content) {
            is TdApi.MessageText -> content.text.text.orEmpty()
            is TdApi.MessageDocument -> content.caption.text.orEmpty()
            is TdApi.MessagePhoto -> content.caption.text.orEmpty()
            else -> ""
        }
        if (!text.contains(FAVORITES_HASHTAG, ignoreCase = true)) return emptyList()

        val index = text.indexOf(FAVORITES_HASHTAG, ignoreCase = true)
        val afterHashtag = text.substring(index + FAVORITES_HASHTAG.length).trim()

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
            Log.e(TAG, "Failed parsing JSON favorites list from message", e)
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

    private fun parseWallpaperFromMessage(
        msg: TdApi.Message,
        fallbackMetadata: WallpaperMetadata?
    ): WallpaperDocument? {
        val file: TdApi.File
        val fileName: String
        val mimeType: String
        val captionText: String

        when (val content = msg.content) {
            is TdApi.MessageDocument -> {
                val doc = content.document
                file = doc.document
                fileName = doc.fileName
                mimeType = doc.mimeType
                captionText = content.caption.text.orEmpty()
            }
            is TdApi.MessagePhoto -> {
                val photoSize = content.photo.sizes.maxByOrNull { it.photo.size } ?: return null
                file = photoSize.photo
                fileName = "photo_${msg.id}.jpg"
                mimeType = "image/jpeg"
                captionText = content.caption.text.orEmpty()
            }
            else -> return null
        }

        val remoteId = file.remote?.id?.takeIf { it.isNotBlank() } ?: file.id.toString()

        val parsedMeta = parseMetadataFromCaption(captionText)
        if (parsedMeta == null && fallbackMetadata == null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[REINDEX DEBUG] Msg #${msg.id} caption metadata parsing returned null. Raw caption: '$captionText'")
            }
            return null
        }
        val metadata = parsedMeta ?: fallbackMetadata!!

        val resolvedType = if (!metadata.wallpaperType.isNullOrBlank()) {
            metadata.wallpaperType
        } else {
            val parts = (metadata.resolution ?: "").lowercase().split("x")
            if (parts.size == 2) {
                val w = parts[0].trim().toIntOrNull() ?: 0
                val h = parts[1].trim().toIntOrNull() ?: 0
                if (w >= h && w > 0 && h > 0) "Desktop/Tablet" else "Phone"
            } else "Phone"
        }
        val finalMetadata = metadata.copy(wallpaperType = resolvedType)

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[REINDEX DEBUG] Msg #${msg.id} parsed: title='${finalMetadata.title}', category='${finalMetadata.category}', type='${finalMetadata.wallpaperType}', resolution='${finalMetadata.resolution}'")
        }

        val localPath = file.local?.path?.takeIf {
            file.local?.isDownloadingCompleted == true && !it.isNullOrBlank() && File(it).exists()
        }

        var existingThumbPath: String? = null
        if (msg.content is TdApi.MessageDocument) {
            val thumb = (msg.content as TdApi.MessageDocument).document.thumbnail
            if (thumb != null && thumb.file.local?.isDownloadingCompleted == true && !thumb.file.local.path.isNullOrBlank() && File(thumb.file.local.path).exists()) {
                existingThumbPath = thumb.file.local.path
            }
        }

        return WallpaperDocument(
            messageId = msg.id,
            chatId = msg.chatId,
            fileId = remoteId,
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = file.size,
            localPath = localPath,
            thumbnailPath = existingThumbPath,
            metadata = finalMetadata
        )
    }

    private fun parseMetadataFromCaption(caption: String): WallpaperMetadata? {
        return try {
            val jsonStart = caption.indexOf(METADATA_PREFIX)
            if (jsonStart != -1) {
                val jsonStr = caption.substring(jsonStart)
                val raw = gson.fromJson(jsonStr, WallpaperMetadata::class.java) ?: return null
                raw.copy(
                    title = raw.title ?: "Wallpaper",
                    category = raw.category ?: "Uncategorized",
                    tags = raw.tags ?: emptyList(),
                    resolution = raw.resolution ?: "1080x1920",
                    aspectRatio = raw.aspectRatio ?: "9:16",
                    colors = raw.colors ?: emptyList(),
                    description = raw.description ?: "",
                    author = raw.author ?: "",
                    wallpaperType = raw.wallpaperType ?: "Phone"
                )
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
