package com.example.notesapp.domain.notes

/**
 * Notas que pasan el filtro de categoría o la búsqueda, ya ordenadas, junto con el total de notas del usuario.
 * [totalCount] distingue «no tienes notas» de «ninguna coincide».
 */
data class FilteredNotes(
    val notes: List<Note>,
    val totalCount: Int,
)
