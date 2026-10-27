package com.example.notesapp.domain.notes

/** La línea [line] del contenido (desde 0, separada por saltos de línea) es una tarea, hecha o pendiente. */
data class ChecklistItem(val line: Int, val isDone: Boolean)
