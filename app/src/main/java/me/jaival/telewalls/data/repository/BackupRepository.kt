package me.jaival.telewalls.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.jaival.telewalls.core.telegram.TelegramClient
import me.jaival.telewalls.core.telegram.TelegramCredentials
import me.jaival.telewalls.data.local.dao.CategoryDao
import me.jaival.telewalls.data.local.dao.WallpaperDao
import me.jaival.telewalls.data.local.entity.CategoryEntity
import me.jaival.telewalls.data.local.entity.FavoriteEntity
import me.jaival.telewalls.data.local.entity.WallpaperEntity
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Keep
data class TeleWallsBackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val preferences: BackupPreferencesData,
    val wallpapers: List<WallpaperEntity> = emptyList(),
    val favorites: List<FavoriteEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val mockCategories: List<String> = emptyList(),
    val tdlibSessionFiles: Map<String, String>? = null
)

@Keep
data class BackupPreferencesData(
    val apiId: Int? = null,
    val apiHash: String? = null,
    val activeChannelId: Long? = null,
    val isSetupCompleted: Boolean = false
)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val wallpaperDao: WallpaperDao,
    private val categoryDao: CategoryDao,
    private val telegramClient: TelegramClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "BackupRepository"
        private const val MOCK_PREFS_NAME = "telewalls_mock_prefs"
        private const val MOCK_CATEGORIES_KEY = "mock_categories"
        private const val MAX_TDLIB_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB limit per session file
    }

    private val prettyGson: Gson by lazy {
        GsonBuilder().setPrettyPrinting().create()
    }

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val prefsSnapshot = authRepository.getPreferencesSnapshot()
        val wallpapers = wallpaperDao.getAllWallpaperEntities()
        val favorites = wallpaperDao.getAllFavoriteEntities()
        val categories = categoryDao.getAllCategoryEntities()

        val mockPrefs = context.getSharedPreferences(MOCK_PREFS_NAME, Context.MODE_PRIVATE)
        val mockCatSet = mockPrefs.getStringSet(MOCK_CATEGORIES_KEY, emptySet()) ?: emptySet()

        val tdlibFilesMap = readTdLibSessionFiles()

        val backupData = TeleWallsBackupData(
            version = 1,
            timestamp = System.currentTimeMillis(),
            appVersion = "1.0.0",
            preferences = prefsSnapshot,
            wallpapers = wallpapers,
            favorites = favorites,
            categories = categories,
            mockCategories = mockCatSet.toList(),
            tdlibSessionFiles = tdlibFilesMap
        )

        prettyGson.toJson(backupData)
    }

    suspend fun restoreBackupJson(jsonString: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backupData = gson.fromJson(jsonString, TeleWallsBackupData::class.java)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid or empty backup file format."))

            // Restore preferences (API credentials, active channel ID, setup state)
            authRepository.restorePreferences(backupData.preferences)

            // Restore database content
            wallpaperDao.clearWallpapers()
            if (backupData.wallpapers.isNotEmpty()) {
                wallpaperDao.insertWallpapers(backupData.wallpapers)
            }

            wallpaperDao.clearFavorites()
            if (backupData.favorites.isNotEmpty()) {
                wallpaperDao.insertFavorites(backupData.favorites)
            }

            categoryDao.clearCategories()
            if (backupData.categories.isNotEmpty()) {
                categoryDao.insertCategories(backupData.categories)
            }

            // Restore mock categories preference if present
            if (backupData.mockCategories.isNotEmpty()) {
                val mockPrefs = context.getSharedPreferences(MOCK_PREFS_NAME, Context.MODE_PRIVATE)
                mockPrefs.edit().putStringSet(MOCK_CATEGORIES_KEY, backupData.mockCategories.toSet()).apply()
            }

            // Restore TDLib session files if present
            if (!backupData.tdlibSessionFiles.isNullOrEmpty()) {
                restoreTdLibSessionFiles(backupData.tdlibSessionFiles)
            }

            // Restart Telegram client with restored credentials if valid
            val apiId = backupData.preferences.apiId ?: 0
            val apiHash = backupData.preferences.apiHash.orEmpty()
            if (apiId > 0 && apiHash.isNotBlank()) {
                telegramClient.start(TelegramCredentials(apiId, apiHash))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore backup", e)
            Result.failure(e)
        }
    }

    suspend fun exportBackupToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = createBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            } ?: return@withContext Result.failure(IllegalStateException("Could not open file output stream."))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing backup to Uri $uri", e)
            Result.failure(e)
        }
    }

    suspend fun restoreBackupFromUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } ?: return@withContext Result.failure(IllegalStateException("Could not open file input stream."))
            restoreBackupJson(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading backup from Uri $uri", e)
            Result.failure(e)
        }
    }

    private fun readTdLibSessionFiles(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val tdlibDir = File(context.filesDir, "tdlib")
            if (tdlibDir.exists() && tdlibDir.isDirectory) {
                tdlibDir.walkTopDown().forEach { file ->
                    if (file.isFile && file.length() <= MAX_TDLIB_FILE_SIZE_BYTES) {
                        val relativePath = file.relativeTo(tdlibDir).path
                        try {
                            val bytes = file.readBytes()
                            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                            result[relativePath] = base64
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading TDLib session file $relativePath", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading TDLib directory", e)
        }
        return result
    }

    private fun restoreTdLibSessionFiles(filesMap: Map<String, String>) {
        try {
            val tdlibDir = File(context.filesDir, "tdlib")
            if (!tdlibDir.exists()) {
                tdlibDir.mkdirs()
            }
            filesMap.forEach { (relativePath, base64Str) ->
                try {
                    val file = File(tdlibDir, relativePath)
                    file.parentFile?.mkdirs()
                    val bytes = Base64.decode(base64Str, Base64.NO_WRAP)
                    file.writeBytes(bytes)
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring TDLib file $relativePath", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring TDLib session directory", e)
        }
    }
}
