package me.jaival.telewalls.core.updater

import android.net.Uri
import java.io.File

data class AppReleaseInfo(
    val tagName: String,       // e.g. "v1.1.0"
    val versionName: String,   // formatted e.g. "v1.1.0"
    val releaseNotes: String,  // changelog body
    val htmlUrl: String,       // browser changelog link
    val downloadUrl: String    // APK download URL
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(
        val releaseInfo: AppReleaseInfo,
        val currentVersionName: String
    ) : UpdateState()
    data class Downloading(
        val releaseInfo: AppReleaseInfo,
        val currentVersionName: String,
        val progress: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : UpdateState()
    data class DownloadCompleted(
        val releaseInfo: AppReleaseInfo,
        val apkFile: File,
        val apkUri: Uri
    ) : UpdateState()
    data class Error(val message: String) : UpdateState()
}
