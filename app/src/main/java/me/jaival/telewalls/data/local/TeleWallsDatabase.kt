package me.jaival.telewalls.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import me.jaival.telewalls.data.local.dao.WallpaperDao
import me.jaival.telewalls.data.local.entity.FavoriteEntity
import me.jaival.telewalls.data.local.entity.WallpaperEntity

@Database(
    entities = [WallpaperEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TeleWallsDatabase : RoomDatabase() {
    abstract fun wallpaperDao(): WallpaperDao
}
