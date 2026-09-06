package me.jaival.telewalls.core.upload

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.jaival.telewalls.core.palette.PaletteExtractor
import me.jaival.telewalls.core.telegram.TelegramUploadEvent
import me.jaival.telewalls.core.telegram.WallpaperMetadata
import me.jaival.telewalls.core.util.CharacterAuthorUtils
import me.jaival.telewalls.data.repository.AuthRepository
import me.jaival.telewalls.data.repository.WallpaperRepository
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class MassUploadService : Service() {

    @Inject
    lateinit var wallpaperRepository: WallpaperRepository

    @Inject
    lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "MassUploadService"
        const val EXTRA_IMAGE_URIS = "extra_image_uris"

        private const val PROGRESS_CHANNEL_ID = "mass_upload_progress_channel"
        private const val RESULT_CHANNEL_ID = "mass_upload_result_channel"
        private const val PROGRESS_NOTIFICATION_ID = 2001
        private const val RESULT_NOTIFICATION_ID = 2002

        fun startUpload(context: Context, uris: List<Uri>) {
            if (uris.isEmpty()) return
            val intent = Intent(context, MassUploadService::class.java).apply {
                putStringArrayListExtra(EXTRA_IMAGE_URIS, ArrayList(uris.map { it.toString() }))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uriStrings = intent?.getStringArrayListExtra(EXTRA_IMAGE_URIS) ?: emptyList()
        if (uriStrings.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val initialNotification = buildProgressNotification(
            current = 0,
            total = uriStrings.size,
            currentFileName = "Preparing batch upload..."
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(
                        PROGRESS_NOTIFICATION_ID,
                        initialNotification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } catch (e: Exception) {
                    startForeground(PROGRESS_NOTIFICATION_ID, initialNotification)
                }
            } else {
                startForeground(PROGRESS_NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service notification", e)
        }

        serviceScope.launch {
            processMassUpload(uriStrings.map { Uri.parse(it) })
        }

        return START_NOT_STICKY
    }

    private suspend fun processMassUpload(uris: List<Uri>) {
        val total = uris.size
        var successCount = 0
        var failureCount = 0
        val errorDetails = mutableListOf<String>()

        val chatId = authRepository.activeChannelIdFlow.first() ?: 99999L

        for ((index, uri) in uris.withIndex()) {
            val currentIndex = index + 1
            val rawFileName = getFileNameFromUri(uri)
            val cleanTitle = cleanFileNameForTitle(rawFileName ?: "photo_$currentIndex")

            updateProgressNotification(
                current = currentIndex,
                total = total,
                currentFileName = cleanTitle
            )

            val tempFile = copyUriToTempFile(uri, rawFileName, index)
            if (tempFile == null || !tempFile.exists()) {
                failureCount++
                errorDetails.add("File #$currentIndex ($cleanTitle): Failed to access image file.")
                continue
            }

            // Extract metadata as in single upload
            val (width, height) = detectResolution(uri)
            val resolutionStr = "${width}x${height}"
            val aspectRatioStr = computeAspectRatioString(width, height)
            val wallpaperTypeStr = if (width >= height) "Desktop/Tablet" else "Phone"
            val colorsList = PaletteExtractor.extractColorsFromUri(this, uri).hexList
            val authorName = CharacterAuthorUtils.getRandomCharacterName()

            val metadata = WallpaperMetadata(
                title = cleanTitle,
                category = "Uncategorized",
                tags = emptyList(),
                resolution = resolutionStr,
                aspectRatio = aspectRatioStr,
                sizeBytes = tempFile.length(),
                colors = colorsList,
                description = "",
                author = authorName,
                timestamp = System.currentTimeMillis(),
                wallpaperType = wallpaperTypeStr
            )

            val mimeType = getMimeTypeFromUri(uri) ?: "image/jpeg"
            val finalFileName = rawFileName ?: tempFile.name

            var uploadSuccess = false
            var errorMessage: String? = null

            try {
                wallpaperRepository.uploadWallpaper(
                    chatId = chatId,
                    localPath = tempFile.absolutePath,
                    fileName = finalFileName,
                    mimeType = mimeType,
                    metadata = metadata
                ).collect { event ->
                    when (event) {
                        is TelegramUploadEvent.Progress -> {
                            // Can optionally update fine-grained progress
                        }
                        is TelegramUploadEvent.Succeeded -> {
                            wallpaperRepository.saveUploadedWallpaperToDb(event.document)
                            uploadSuccess = true
                        }
                        is TelegramUploadEvent.Failed -> {
                            errorMessage = event.message
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
            }

            if (uploadSuccess) {
                successCount++
            } else {
                failureCount++
                val detail = errorMessage ?: "Upload failed"
                errorDetails.add("Photo #$currentIndex ($cleanTitle): $detail")

                // Handle FLOOD_WAIT if rate limited
                if (detail.contains("FLOOD_WAIT", ignoreCase = true) || detail.contains("rate limit", ignoreCase = true)) {
                    val waitSecs = detail.filter { it.isDigit() }.toLongOrNull() ?: 5L
                    Log.w(TAG, "Telegram rate limit encountered. Waiting $waitSecs seconds...")
                    delay(waitSecs * 1000L)
                }
            }

            // Telegram rate limit guideline: pause 1.5 seconds between uploads
            if (currentIndex < total) {
                delay(1500L)
            }
        }

        // Finish up: stop foreground service and present completion or error notification
        stopForegroundService()
        showFinalResultNotification(total, successCount, failureCount, errorDetails)
        stopSelf()
    }

    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun updateProgressNotification(current: Int, total: Int, currentFileName: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildProgressNotification(current, total, currentFileName)
        notificationManager.notify(PROGRESS_NOTIFICATION_ID, notification)
    }

    private fun buildProgressNotification(current: Int, total: Int, currentFileName: String): android.app.Notification {
        val titleText = if (current == 0) "Mass Upload Starting" else "Uploading Wallpapers ($current/$total)"
        val contentText = if (current == 0) currentFileName else "Uploading $currentFileName..."

        return NotificationCompat.Builder(this, PROGRESS_CHANNEL_ID)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(total, current, current == 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showFinalResultNotification(
        total: Int,
        successCount: Int,
        failureCount: Int,
        errorDetails: List<String>
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(this, RESULT_CHANNEL_ID)
            .setSmallIcon(if (failureCount == 0) android.R.drawable.stat_sys_upload_done else android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (failureCount == 0) {
            builder.setContentTitle("Mass Upload Complete")
                .setContentText("Successfully uploaded all $successCount wallpapers to Telegram Vault!")
        } else {
            builder.setContentTitle("Mass Upload Finished with Errors")
                .setContentText("$successCount uploaded successfully, $failureCount failed.")
            
            val bigText = StringBuilder()
                .append("Upload summary:\n")
                .append("• Successful: $successCount / $total\n")
                .append("• Failed: $failureCount / $total\n\n")
                .append("Error details:\n")
                .append(errorDetails.joinToString("\n"))
                .toString()

            builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
        }

        notificationManager.notify(RESULT_NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val progressChannel = NotificationChannel(
                PROGRESS_CHANNEL_ID,
                "Mass Upload Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time progress of batch wallpaper uploads"
            }

            val resultChannel = NotificationChannel(
                RESULT_CHANNEL_ID,
                "Mass Upload Results",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when mass wallpaper upload completes or encunters errors"
            }

            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(resultChannel)
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            name = cursor.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying file name from URI", e)
            }
        }
        if (name.isNullOrBlank()) {
            name = uri.path?.let { File(it).name }
        }
        return name?.takeIf { it.isNotBlank() }
    }

    private fun cleanFileNameForTitle(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
    }

    private fun getMimeTypeFromUri(uri: Uri): String? {
        return if (uri.scheme == "content") {
            contentResolver.getType(uri)
        } else {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            if (extension != null) {
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            } else null
        }
    }

    private fun detectResolution(uri: Uri): Pair<Int, Int> {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, options)
                val w = if (options.outWidth > 0) options.outWidth else 1080
                val h = if (options.outHeight > 0) options.outHeight else 1920
                Pair(w, h)
            } ?: Pair(1080, 1920)
        } catch (e: Exception) {
            Pair(1080, 1920)
        }
    }

    private fun computeAspectRatioString(width: Int, height: Int): String {
        return try {
            if (width > 0 && height > 0) {
                val g = gcd(width, height)
                "${width / g}:${height / g}"
            } else "9:16"
        } catch (e: Exception) {
            "9:16"
        }
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = a
        var y = b
        while (y != 0) {
            val t = y
            y = x % y
            x = t
        }
        return x
    }

    private fun copyUriToTempFile(uri: Uri, customFileName: String?, index: Int): File? {
        return try {
            val safeName = customFileName?.takeIf { it.isNotBlank() } ?: "mass_${System.currentTimeMillis()}_$index.jpg"
            val tempFile = File(cacheDir, "mass_${index}_$safeName")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists() && tempFile.length() > 0) tempFile else null
        } catch (e: Exception) {
            Log.e(TAG, "Error copying URI to temp file", e)
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
