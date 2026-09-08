package me.jaival.telewalls.core.telegram

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
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

@Keep
data class StorageChannel(
    val chatId: Long,
    val title: String,
    val documentCount: Int = 0,
    val description: String = ""
)

@Keep
data class TelegramUser(
    val id: Long = 0L,
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val profilePhotoPath: String? = null
)

@Keep
data class WallpaperMetadata(
    @SerializedName(value = "a", alternate = ["title"])
    val title: String,

    @SerializedName(value = "b", alternate = ["category"])
    val category: String,

    @SerializedName(value = "c", alternate = ["tags"])
    val tags: List<String> = emptyList(),

    @SerializedName(value = "d", alternate = ["resolution"])
    val resolution: String = "1080x1920",

    @SerializedName(value = "e", alternate = ["aspectRatio"])
    val aspectRatio: String = "9:16",

    @SerializedName(value = "f", alternate = ["sizeBytes"])
    val sizeBytes: Long = 0L,

    @SerializedName(value = "g", alternate = ["colors"])
    val colors: List<String> = emptyList(),

    @SerializedName(value = "h", alternate = ["description"])
    val description: String = "",

    @SerializedName(value = "i", alternate = ["author"])
    val author: String = "",

    @SerializedName(value = "j", alternate = ["timestamp"])
    val timestamp: Long = System.currentTimeMillis(),

    @SerializedName(value = "l", alternate = ["thumbnailFileId"])
    val thumbnailFileId: String? = null,

    @SerializedName(value = "k", alternate = ["wallpaperType"])
    val wallpaperType: String = "Phone"
)

@Keep
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

