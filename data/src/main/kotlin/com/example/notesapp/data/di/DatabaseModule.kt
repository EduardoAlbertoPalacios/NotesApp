package com.example.notesapp.data.di

import android.content.Context
import androidx.room.Room
import com.example.notesapp.data.local.NoteDao
import com.example.notesapp.data.local.NotesDatabase
import com.example.notesapp.data.local.SeedNotesCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NotesDatabase =
        Room.databaseBuilder(context, NotesDatabase::class.java, NotesDatabase.NAME)
            .addCallback(SeedNotesCallback())
            .build()

    @Provides
    fun provideNoteDao(database: NotesDatabase): NoteDao = database.noteDao()
}
