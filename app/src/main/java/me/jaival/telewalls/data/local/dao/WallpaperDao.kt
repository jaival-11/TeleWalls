package me.jaival.telewalls.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.jaival.telewalls.data.local.entity.FavoriteEntity
import me.jaival.telewalls.data.local.entity.WallpaperEntity

@Dao
interface WallpaperDao {

    @Query("SELECT * FROM wallpapers ORDER BY timestamp DESC")
    fun getAllWallpapers(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE category = :category ORDER BY timestamp DESC")
    fun getWallpapersByCategory(category: String): Flow<List<WallpaperEntity>>

    @Query("SELECT DISTINCT category FROM wallpapers WHERE category IS NOT NULL AND category != ''")
    fun getCategoriesFromWallpapers(): Flow<List<String>>

    @Query("SELECT * FROM wallpapers WHERE title LIKE '%' || :query || '%' OR tagsCsv LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR colorsCsv LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchWallpapers(query: String): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteWallpapers(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE id = :id LIMIT 1")
    suspend fun getWallpaperById(id: String): WallpaperEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpapers(wallpapers: List<WallpaperEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpaper(wallpaper: WallpaperEntity)

    @Query("UPDATE wallpapers SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

    @Query("UPDATE wallpapers SET localPath = :localPath WHERE id = :id")
    suspend fun updateLocalPath(id: String, localPath: String?)

    @Query("UPDATE wallpapers SET thumbnailPath = :thumbnailPath WHERE id = :id")
    suspend fun updateThumbnailPath(id: String, thumbnailPath: String?)

    @Query("UPDATE wallpapers SET localPath = CASE WHEN localPath LIKE 'http%' THEN localPath ELSE NULL END, thumbnailPath = CASE WHEN thumbnailPath LIKE 'http%' THEN thumbnailPath ELSE NULL END")
    suspend fun clearCachedPaths()

    @Query("UPDATE wallpapers SET title = :title, author = :author, category = :category, tagsCsv = :tagsCsv, description = :description, wallpaperType = :wallpaperType WHERE id = :id")
    suspend fun updateWallpaperMetadata(
        id: String,
        title: String,
        author: String,
        category: String,
        tagsCsv: String,
        description: String,
        wallpaperType: String
    )

    @Query("UPDATE wallpapers SET category = :newName WHERE category = :oldName")
    suspend fun updateWallpaperCategory(oldName: String, newName: String)

    @Query("DELETE FROM wallpapers WHERE id = :id")
    suspend fun deleteWallpaperById(id: String)

    @Query("SELECT * FROM wallpapers WHERE chatId = :chatId OR id LIKE :chatId || '_%'")
    suspend fun getWallpapersByChatId(chatId: Long): List<WallpaperEntity>

    @Query("DELETE FROM wallpapers WHERE id IN (:ids)")
    suspend fun deleteWallpapersByIds(ids: List<String>)

    @Query("DELETE FROM wallpapers WHERE chatId != :chatId")
    suspend fun deleteAllWallpapersExceptChatId(chatId: Long)

    @Query("DELETE FROM favorites WHERE wallpaperId NOT IN (SELECT id FROM wallpapers)")
    suspend fun deleteOrphanFavorites()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE wallpaperId = :wallpaperId")
    suspend fun removeFavorite(wallpaperId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE wallpaperId = :wallpaperId)")
    suspend fun isFavorite(wallpaperId: String): Boolean

    @Query("SELECT * FROM wallpapers")
    suspend fun getAllWallpaperEntities(): List<WallpaperEntity>

    @Query("SELECT * FROM favorites")
    suspend fun getAllFavoriteEntities(): List<FavoriteEntity>

    @Query("DELETE FROM wallpapers")
    suspend fun clearWallpapers()

    @Query("DELETE FROM favorites")
    suspend fun clearFavorites()

    @Query("UPDATE wallpapers SET isFavorite = 0")
    suspend fun clearAllFavoriteFlags()

    @Query("UPDATE wallpapers SET isFavorite = 1 WHERE id IN (:ids) OR messageId IN (:messageIds)")
    suspend fun setFavoriteFlags(ids: List<String>, messageIds: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(favorites: List<FavoriteEntity>)
}
