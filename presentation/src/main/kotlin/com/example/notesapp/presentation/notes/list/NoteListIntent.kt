package com.example.notesapp.presentation.notes.list

import com.example.notesapp.domain.settings.NotesView

sealed interface NoteListIntent {
    data class NoteClicked(val id: Long) : NoteListIntent
    data object CreateNoteClicked : NoteListIntent
    data object SettingsClicked : NoteListIntent
    data class FilterSelected(val filter: NoteFilter) : NoteListIntent
    data class QueryChanged(val query: String) : NoteListIntent
    data object ClearQueryClicked : NoteListIntent
    data class NotesViewSelected(val view: NotesView) : NoteListIntent
}
