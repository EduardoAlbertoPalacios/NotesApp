package com.example.notesapp.presentation.notes.list

sealed interface NoteListEffect {
    /** `id` nulo significa crear una nota nueva. */
    data class NavigateToNote(val id: Long?) : NoteListEffect
    data object NavigateToSettings : NoteListEffect
}
