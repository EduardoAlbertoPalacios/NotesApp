package com.example.notesapp.presentation.notes.list

import com.example.notesapp.domain.notes.NoteCategory

/** Filtros del listado: todas las notas o solo las de una categoría. */
enum class NoteFilter(val category: NoteCategory?) {
    ALL(null),
    WORK(NoteCategory.WORK),
    PERSONAL(NoteCategory.PERSONAL),
}
