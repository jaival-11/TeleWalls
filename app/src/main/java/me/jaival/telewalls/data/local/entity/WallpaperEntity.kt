package me.jaival.telewalls.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallpapers")
data class WallpaperEntity(
    @PrimaryKey val id: String, // format: "chatId_messageId"
    val messageId: Long,
    val chatId: Long,
    val fileId: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val title: String,
    val category: String,
    val tagsCsv: String,
    val resolution: String,
    val aspectRatio: String,
    val colorsCsv: String,
    val description: String,
    val author: String,
    val timestamp: Long,
    val localPath: String? = null,
    val thumbnailPath: String? = null,
    val isFavorite: Boolean = false
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val wallpaperId: String,
    val addedAt: Long = System.currentTimeMillis()
)
