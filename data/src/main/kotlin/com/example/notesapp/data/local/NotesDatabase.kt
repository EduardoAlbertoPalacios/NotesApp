package com.example.notesapp.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class, NoteImageEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // v2: columna `category`, nula para las notas existentes.
        AutoMigration(from = 1, to = 2),
        // v3: tabla `note_images`; las notas existentes quedan sin imágenes.
        AutoMigration(from = 2, to = 3),
    ],
)
internal abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        const val NAME = "notes.db"
    }
}
