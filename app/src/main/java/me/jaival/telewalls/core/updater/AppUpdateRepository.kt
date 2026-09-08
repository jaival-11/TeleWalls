package me.jaival.telewalls.core.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/jaival-11/TeleWalls/releases/latest"
    }

    suspend fun fetchLatestRelease(): AppReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_LATEST_RELEASE_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "TeleWalls-Android-App")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)

            val tagName = jsonObject.get("tag_name")?.asString ?: return@withContext null
            val htmlUrl = jsonObject.get("html_url")?.asString ?: "https://github.com/jaival-11/TeleWalls/releases"
            val releaseNotes = jsonObject.get("body")?.asString ?: ""

            var apkDownloadUrl = ""
            if (jsonObject.has("assets") && jsonObject.get("assets").isJsonArray) {
                val assets = jsonObject.getAsJsonArray("assets")
                val apkAssets = mutableMapOf<String, String>()

                for (assetElement in assets) {
                    if (assetElement.isJsonObject) {
                        val assetObj = assetElement.asJsonObject
                        val name = assetObj.get("name")?.asString ?: ""
                        val downloadUrl = assetObj.get("browser_download_url")?.asString ?: ""
                        if (name.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                            apkAssets[name] = downloadUrl
                        }
                    }
                }

                // 1. Try to match primary supported ABI of the user's device
                val supportedAbis = Build.SUPPORTED_ABIS ?: emptyArray()
                for (abi in supportedAbis) {
                    val matchingEntry = apkAssets.entries.firstOrNull { (name, _) ->
                        if (abi.equals("x86", ignoreCase = true)) {
                            name.contains("x86", ignoreCase = true) && !name.contains("x86_64", ignoreCase = true)
                        } else {
                            name.contains(abi, ignoreCase = true)
                        }
                    }
                    if (matchingEntry != null) {
                        apkDownloadUrl = matchingEntry.value
                        break
                    }
                }

                // 2. If no matching ABI APK was found, fallback to universal APK or first available APK
                if (apkDownloadUrl.isBlank()) {
                    apkDownloadUrl = apkAssets.entries.firstOrNull { (name, _) ->
                        name.contains("universal", ignoreCase = true)
                    }?.value ?: apkAssets.values.firstOrNull() ?: ""
                }
            }

            if (apkDownloadUrl.isBlank()) {
                val cleanTag = tagName.trim()
                val versionStr = cleanTag.removePrefix("v")
                apkDownloadUrl = "https://github.com/jaival-11/TeleWalls/releases/download/$cleanTag/TeleWalls-$versionStr-universal.apk"
            }

            AppReleaseInfo(
                tagName = tagName,
                versionName = SemVer.formatVersionName(tagName),
                releaseNotes = releaseNotes,
                htmlUrl = htmlUrl,
                downloadUrl = apkDownloadUrl
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (progress: Float, bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val destinationFile = File(context.cacheDir, "TeleWalls_update.apk")
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        var connection = URL(downloadUrl).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "TeleWalls-Android-App")
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.instanceFollowRedirects = true
        connection.connect()

        // Handle HTTP redirects (GitHub releases redirect to AWS S3)
        var responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
            responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
            responseCode == 307 || responseCode == 308) {
            val redirectUrl = connection.getHeaderField("Location")
            connection = URL(redirectUrl).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "TeleWalls-Android-App")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()
            responseCode = connection.responseCode
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Failed to download APK: HTTP $responseCode")
        }

        val fileLength = connection.contentLengthLong
        var inputStream: InputStream = connection.inputStream
        val outputStream = FileOutputStream(destinationFile)

        val buffer = ByteArray(8192)
        var totalBytesRead = 0L
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            totalBytesRead += bytesRead
            val progress = if (fileLength > 0) totalBytesRead.toFloat() / fileLength.toFloat() else 0f
            onProgress(progress, totalBytesRead, fileLength)
        }

        outputStream.flush()
        outputStream.close()
        inputStream.close()

        destinationFile
    }

    fun getApkUri(apkFile: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, apkFile)
    }

    fun promptInstallApk(apkFile: File): Boolean {
        return try {
            val apkUri = getApkUri(apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                }
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
