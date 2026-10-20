package com.example.notesapp.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class, NoteImageEntity::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        // v2: columna `category`, nula para las notas existentes.
        AutoMigration(from = 1, to = 2),
        // v3: tabla `note_images`; las notas existentes quedan sin imágenes.
        AutoMigration(from = 2, to = 3),
        // v4: columna `content_spans`, vacía (sin formato) para las notas existentes.
        AutoMigration(from = 3, to = 4),
    ],
)
internal abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        const val NAME = "notes.db"
    }
}
