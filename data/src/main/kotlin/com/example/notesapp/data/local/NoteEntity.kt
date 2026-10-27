package com.example.notesapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
internal data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val color: String,
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    /** Nombre de `NoteCategory`, o nulo si la nota no tiene categoría. Desde la versión 2. */
    val category: String? = null,
    /** Tramos de formato del contenido, codificados como texto; vacío = sin formato. Desde la versión 4. */
    @ColumnInfo(name = "content_spans", defaultValue = "") val contentSpans: String = "",
    /** Líneas del contenido que son tareas, codificadas como texto; vacío = sin tareas. Desde la versión 6. */
    @ColumnInfo(name = "content_tasks", defaultValue = "") val contentTasks: String = "",
)
