package me.jaival.telewalls.core.telegram

data class TelegramCredentials(
    val apiId: Int,
    val apiHash: String
) {
    override fun toString(): String = "TelegramCredentials(apiId=***, apiHash=***)"
}

enum class TelegramConnectionState {
    CONNECTING,
    READY,
    UPDATING,
    WAITING_FOR_NETWORK
}

sealed interface TelegramAuthState {
    data object Uninitialized : TelegramAuthState
    data object Initializing : TelegramAuthState
    data object WaitingForPhoneNumber : TelegramAuthState
    data class WaitingForCode(
        val phoneNumber: String,
        val codeLength: Int? = 5,
        val resendTimeoutSeconds: Int = 60
    ) : TelegramAuthState
    data class WaitingForPassword(val passwordHint: String? = null) : TelegramAuthState
    data class WaitingForQrScan(val link: String) : TelegramAuthState
    data class Failed(val message: String) : TelegramAuthState
    data object Ready : TelegramAuthState
    data object LoggingOut : TelegramAuthState
    data object Closed : TelegramAuthState
}

data class StorageChannel(
    val chatId: Long,
    val title: String,
    val documentCount: Int = 0,
    val description: String = ""
)

data class TelegramUser(
    val id: Long = 0L,
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val profilePhotoPath: String? = null
)

data class WallpaperMetadata(
    val title: String,
    val category: String,
    val tags: List<String> = emptyList(),
    val resolution: String = "1080x1920",
    val aspectRatio: String = "9:16",
    val sizeBytes: Long = 0L,
    val colors: List<String> = emptyList(),
    val description: String = "",
    val author: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val thumbnailFileId: String? = null,
    val wallpaperType: String = "Phone"
)

data class WallpaperDocument(
    val messageId: Long,
    val chatId: Long,
    val fileId: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localPath: String? = null,
    val thumbnailPath: String? = null,
    val metadata: WallpaperMetadata
)

sealed interface TelegramUploadEvent {
    data class Progress(val bytesUploaded: Long, val totalBytes: Long?) : TelegramUploadEvent
    data class Succeeded(val document: WallpaperDocument) : TelegramUploadEvent
    data class Failed(val message: String) : TelegramUploadEvent
}
