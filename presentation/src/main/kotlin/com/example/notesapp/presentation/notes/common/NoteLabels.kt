package com.example.notesapp.presentation.notes.common

import androidx.annotation.StringRes
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.settings.NotesView
import com.example.notesapp.presentation.R

/** Nombre visible de la categoría. */
@StringRes
internal fun NoteCategory.labelRes(): Int = when (this) {
    NoteCategory.WORK -> R.string.note_category_work
    NoteCategory.PERSONAL -> R.string.note_category_personal
}

/** Nombre visible de la vista de Mis notas. */
@StringRes
internal fun NotesView.labelRes(): Int = when (this) {
    NotesView.GRID -> R.string.notes_view_grid
    NotesView.LIST -> R.string.notes_view_list
}
