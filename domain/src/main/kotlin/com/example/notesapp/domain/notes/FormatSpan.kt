package com.example.notesapp.domain.notes

/** Tramo del contenido con un formato: abarca los caracteres de [start] (incluido) a [end] (excluido). */
data class FormatSpan(
    val start: Int,
    val end: Int,
    val format: TextFormat,
)
