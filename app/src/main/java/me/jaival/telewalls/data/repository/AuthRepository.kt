package me.jaival.telewalls.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import dagger.hilt.android.qualifiers.ApplicationContext
import me.jaival.telewalls.core.telegram.TelegramCredentials
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "telewalls_prefs")

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val API_ID_KEY = stringPreferencesKey("telegram_api_id")
        private val API_HASH_KEY = stringPreferencesKey("telegram_api_hash")
        private val ACTIVE_CHANNEL_ID_KEY = longPreferencesKey("active_channel_id")
        private val IS_SETUP_COMPLETED_KEY = booleanPreferencesKey("is_setup_completed")
    }

    val isSetupCompletedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_SETUP_COMPLETED_KEY] ?: false
    }

    val credentialsFlow: Flow<TelegramCredentials?> = context.dataStore.data.map { prefs ->
        val apiId = prefs[API_ID_KEY]?.toIntOrNull() ?: 0
        val apiHash = prefs[API_HASH_KEY] ?: ""
        if (apiId != 0 && apiHash.isNotBlank()) {
            TelegramCredentials(apiId, apiHash)
        } else {
            null
        }
    }

    val activeChannelIdFlow: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[ACTIVE_CHANNEL_ID_KEY]
    }

    suspend fun saveCredentials(apiId: Int, apiHash: String) {
        context.dataStore.edit { prefs ->
            prefs[API_ID_KEY] = apiId.toString()
            prefs[API_HASH_KEY] = apiHash
        }
    }

    suspend fun saveActiveChannelId(channelId: Long) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_CHANNEL_ID_KEY] = channelId
        }
    }

    suspend fun setSetupCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_SETUP_COMPLETED_KEY] = completed
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(API_ID_KEY)
            prefs.remove(API_HASH_KEY)
            prefs.remove(ACTIVE_CHANNEL_ID_KEY)
            prefs.remove(IS_SETUP_COMPLETED_KEY)
        }
    }

    suspend fun getPreferencesSnapshot(): BackupPreferencesData {
        val prefs = context.dataStore.data.first()
        val apiIdStr = prefs[API_ID_KEY]
        val apiId = apiIdStr?.toIntOrNull()
        val apiHash = prefs[API_HASH_KEY]
        val activeChannelId = prefs[ACTIVE_CHANNEL_ID_KEY]
        val isSetupCompleted = prefs[IS_SETUP_COMPLETED_KEY] ?: false
        return BackupPreferencesData(
            apiId = apiId,
            apiHash = apiHash,
            activeChannelId = activeChannelId,
            isSetupCompleted = isSetupCompleted
        )
    }

    suspend fun restorePreferences(data: BackupPreferencesData) {
        context.dataStore.edit { prefs ->
            if (data.apiId != null) {
                prefs[API_ID_KEY] = data.apiId.toString()
            } else {
                prefs.remove(API_ID_KEY)
            }
            if (!data.apiHash.isNullOrBlank()) {
                prefs[API_HASH_KEY] = data.apiHash
            } else {
                prefs.remove(API_HASH_KEY)
            }
            if (data.activeChannelId != null) {
                prefs[ACTIVE_CHANNEL_ID_KEY] = data.activeChannelId
            } else {
                prefs.remove(ACTIVE_CHANNEL_ID_KEY)
            }
            prefs[IS_SETUP_COMPLETED_KEY] = data.isSetupCompleted
        }
    }
}
