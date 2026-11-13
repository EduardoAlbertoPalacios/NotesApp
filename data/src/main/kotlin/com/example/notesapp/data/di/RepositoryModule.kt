package com.example.notesapp.data.di

import com.example.notesapp.data.export.PdfNotesExporter
import com.example.notesapp.data.images.ImageStorageImpl
import com.example.notesapp.data.notes.NoteRepositoryImpl
import com.example.notesapp.data.settings.SettingsRepositoryImpl
import com.example.notesapp.domain.notes.ImageStorage
import com.example.notesapp.domain.notes.NoteRepository
import com.example.notesapp.domain.notes.export.NotesExporter
import com.example.notesapp.domain.settings.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds
    abstract fun bindImageStorage(impl: ImageStorageImpl): ImageStorage

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    abstract fun bindNotesExporter(impl: PdfNotesExporter): NotesExporter
}
