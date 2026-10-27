package com.example.notesapp.domain.notes

data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val color: NoteColor,
    val isPinned: Boolean,
    /** Nula si la nota no tiene categoría. */
    val category: NoteCategory?,
    /** Fecha de la última edición, en milisegundos desde epoch. */
    val updatedAt: Long,
    val images: List<NoteImage> = emptyList(),
    /** Formato del contenido; vacío en texto plano y en las notas anteriores al formato. */
    val contentSpans: List<FormatSpan> = emptyList(),
    /** Líneas del contenido que son tareas; vacía en las notas sin tareas y en las anteriores a ellas. */
    val contentTasks: List<ChecklistItem> = emptyList(),
) {
    val isBlank: Boolean get() = title.isBlank() && content.isBlank() && images.isEmpty()

    companion object {
        /** Id de una nota que todavía no se ha guardado. */
        const val NEW_ID = 0L
    }
}
