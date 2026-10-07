package com.example.notesapp.presentation.notes.editor

sealed interface NoteEditorIntent {
    data class TitleChanged(val title: String) : NoteEditorIntent
    data class ContentChanged(val content: String) : NoteEditorIntent
    data object PinClicked : NoteEditorIntent
    data object DeleteClicked : NoteEditorIntent
    data object DeleteConfirmed : NoteEditorIntent
    data object DeleteDismissed : NoteEditorIntent
    data object BackClicked : NoteEditorIntent
}
