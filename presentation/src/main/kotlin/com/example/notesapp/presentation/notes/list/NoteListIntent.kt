package com.example.notesapp.presentation.notes.list

sealed interface NoteListIntent {
    data class NoteClicked(val id: Long) : NoteListIntent
    data object CreateNoteClicked : NoteListIntent
    data object SettingsClicked : NoteListIntent
    data class FilterSelected(val filter: NoteFilter) : NoteListIntent
}
