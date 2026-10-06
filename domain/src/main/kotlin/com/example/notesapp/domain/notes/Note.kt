package com.example.notesapp.domain.notes

data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val color: NoteColor,
    val isPinned: Boolean,
    /** Fecha de la última edición, en milisegundos desde epoch. */
    val updatedAt: Long,
)
