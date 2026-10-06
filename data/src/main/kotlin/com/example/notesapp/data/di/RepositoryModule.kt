package com.example.notesapp.data.di

import com.example.notesapp.data.notes.NoteRepositoryImpl
import com.example.notesapp.domain.notes.NoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository
}
