package me.jaival.telewalls.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "telewalls_settings")

enum class WallpaperTypeFilter(val label: String, val description: String) {
    BOTH("Both", "Show phone, desktop & tablet wallpapers"),
    PHONE("Phone", "Show mobile phone wallpapers (9:16)"),
    DESKTOP("Desktop / Tablet", "Show desktop & tablet wallpapers (16:9)")
}

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val WALLPAPER_TYPE_KEY = stringPreferencesKey("wallpaper_type_filter")
        private val REDUCE_ANIMATIONS_KEY = booleanPreferencesKey("reduce_animations")
        private val HIDDEN_CATEGORIES_KEY = stringSetPreferencesKey("hidden_categories")
        private val SYNC_FAVORITES_KEY = booleanPreferencesKey("sync_favorites")
    }

    val wallpaperTypeFlow: Flow<WallpaperTypeFilter> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[WALLPAPER_TYPE_KEY] ?: WallpaperTypeFilter.BOTH.name
        try {
            WallpaperTypeFilter.valueOf(raw)
        } catch (e: Exception) {
            WallpaperTypeFilter.BOTH
        }
    }

    val reduceAnimationsFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[REDUCE_ANIMATIONS_KEY] ?: false
    }

    val hiddenCategoriesFlow: Flow<Set<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[HIDDEN_CATEGORIES_KEY] ?: emptySet()
    }

    val syncFavoritesFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[SYNC_FAVORITES_KEY] ?: true
    }

    suspend fun setWallpaperType(type: WallpaperTypeFilter) {
        context.settingsDataStore.edit { prefs ->
            prefs[WALLPAPER_TYPE_KEY] = type.name
        }
    }

    suspend fun setReduceAnimations(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[REDUCE_ANIMATIONS_KEY] = enabled
        }
    }

    suspend fun setHiddenCategories(categories: Set<String>) {
        context.settingsDataStore.edit { prefs ->
            prefs[HIDDEN_CATEGORIES_KEY] = categories
        }
    }

    suspend fun toggleCategoryHidden(categoryName: String) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[HIDDEN_CATEGORIES_KEY] ?: emptySet()
            val newSet = current.toMutableSet()
            if (newSet.contains(categoryName)) {
                newSet.remove(categoryName)
            } else {
                newSet.add(categoryName)
            }
            prefs[HIDDEN_CATEGORIES_KEY] = newSet
        }
    }

    suspend fun setSyncFavorites(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SYNC_FAVORITES_KEY] = enabled
        }
    }
}
