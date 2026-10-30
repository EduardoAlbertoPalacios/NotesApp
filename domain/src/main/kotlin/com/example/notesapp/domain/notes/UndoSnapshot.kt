package com.example.notesapp.domain.notes

/**
 * Foto de la nota antes de un paso: lo que vuelve a ponerse al deshacerlo. [field] es el campo que cambia
 * el paso, y la selección ([selectionStart] a [selectionEnd]) es la que tenía ese campo antes del paso.
 */
data class UndoSnapshot(
    val title: String,
    val content: String,
    val contentSpans: List<FormatSpan>,
    val contentTasks: List<ChecklistItem>,
    val field: EditField,
    val selectionStart: Int,
    val selectionEnd: Int,
)
