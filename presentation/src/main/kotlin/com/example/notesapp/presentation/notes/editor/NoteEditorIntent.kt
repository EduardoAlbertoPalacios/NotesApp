package com.example.notesapp.presentation.notes.editor

import com.example.notesapp.domain.notes.NoteCategory

sealed interface NoteEditorIntent {
    data class TitleChanged(val title: String) : NoteEditorIntent
    data class ContentChanged(val content: String) : NoteEditorIntent
    data object PinClicked : NoteEditorIntent
    /** Elegir la categoría ya seleccionada la quita. */
    data class CategoryClicked(val category: NoteCategory) : NoteEditorIntent
    data object DeleteClicked : NoteEditorIntent
    data object DeleteConfirmed : NoteEditorIntent
    data object DeleteDismissed : NoteEditorIntent
    data object BackClicked : NoteEditorIntent
}
