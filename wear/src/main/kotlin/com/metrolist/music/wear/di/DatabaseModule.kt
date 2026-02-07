package com.metrolist.music.wear.di

import android.content.Context
import androidx.room.Room
import com.metrolist.music.wear.data.db.HistoryDao
import com.metrolist.music.wear.data.db.WearDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WearDatabase {
        return Room.databaseBuilder(
            context,
            WearDatabase::class.java,
            "wear_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideHistoryDao(database: WearDatabase): HistoryDao {
        return database.historyDao()
    }
}
