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
    /** Momento en que entró en la papelera, en milisegundos desde epoch; nulo fuera de ella. Desde la versión 7. */
    @ColumnInfo(name = "trashed_at") val trashedAt: Long? = null,
)

/**
 * Lo que cambia al guardar una nota: todas las columnas menos `trashed_at`. Así un guardado nunca saca una nota
 * de la papelera ni la mete en ella (decisión D1 del plan 012).
 */
internal data class NoteContentUpdate(
    val id: Long,
    val title: String,
    val content: String,
    val color: String,
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    val category: String?,
    @ColumnInfo(name = "content_spans") val contentSpans: String,
    @ColumnInfo(name = "content_tasks") val contentTasks: String,
)

internal fun NoteEntity.toContentUpdate() = NoteContentUpdate(
    id = id,
    title = title,
    content = content,
    color = color,
    isPinned = isPinned,
    updatedAt = updatedAt,
    category = category,
    contentSpans = contentSpans,
    contentTasks = contentTasks,
)
