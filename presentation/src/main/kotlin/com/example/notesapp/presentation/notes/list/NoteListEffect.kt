package com.example.notesapp.presentation.notes.list

import com.example.notesapp.domain.notes.NoteCategory

sealed interface NoteListEffect {
    /** `id` nulo significa crear una nota nueva, que nace en [category] si no es nula. */
    data class NavigateToNote(val id: Long?, val category: NoteCategory? = null) : NoteListEffect
    data object NavigateToSettings : NoteListEffect
}
