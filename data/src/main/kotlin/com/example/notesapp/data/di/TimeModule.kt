package com.example.notesapp.data.di

import com.example.notesapp.common.time.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object TimeModule {

    @Provides
    fun provideTimeProvider(): TimeProvider = TimeProvider(System::currentTimeMillis)
}
