package me.jaival.telewalls.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.jaival.telewalls.core.telegram.TdLibTelegramClient
import me.jaival.telewalls.core.telegram.TelegramClient
import me.jaival.telewalls.data.local.TeleWallsDatabase
import me.jaival.telewalls.data.local.dao.WallpaperDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): TeleWallsDatabase {
        return Room.databaseBuilder(
            context,
            TeleWallsDatabase::class.java,
            "telewalls_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideWallpaperDao(db: TeleWallsDatabase): WallpaperDao {
        return db.wallpaperDao()
    }

    @Provides
    @Singleton
    fun provideTelegramClient(
        @ApplicationContext context: Context,
        gson: Gson
    ): TelegramClient {
        return TdLibTelegramClient(context, gson)
    }
}
