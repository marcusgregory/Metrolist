package com.metrolist.music.wear.di

import android.app.Application
import android.content.Context
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.AudioOutputRepository
import com.google.android.horologist.audio.SystemAudioRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @OptIn(ExperimentalHorologistApi::class)
    @Provides
    @Singleton
    fun provideAudioOutputRepository(
        @ApplicationContext context: Context
    ): AudioOutputRepository {
        return SystemAudioRepository.fromContext(context.applicationContext as Application)
    }
}
