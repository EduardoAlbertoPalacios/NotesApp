package com.example.notesapp.domain.notes.trash

import com.example.notesapp.domain.notes.Note

/** Una nota de la papelera y el momento, en milisegundos desde epoch, en que entró en ella. */
data class TrashedNote(
    val note: Note,
    val trashedAt: Long,
)
