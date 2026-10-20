package com.example.notesapp.data.notes

import com.example.notesapp.domain.notes.FormatSpan
import com.example.notesapp.domain.notes.TextFormat

// Formato guardado en la columna `content_spans`: `start,end,F` separados por `;`, con F = B, U o S.
// Ejemplo: "0,5,B;3,9,U". Cadena vacía = sin formato.

private const val SPAN_SEPARATOR = ";"
private const val FIELD_SEPARATOR = ","

private val FormatCodes = mapOf(
    TextFormat.BOLD to "B",
    TextFormat.UNDERLINE to "U",
    TextFormat.STRIKETHROUGH to "S",
)

internal fun List<FormatSpan>.encode(): String =
    joinToString(SPAN_SEPARATOR) { span ->
        listOf(span.start, span.end, FormatCodes.getValue(span.format)).joinToString(FIELD_SEPARATOR)
    }

/**
 * Lee los tramos guardados. Descarta las entradas mal formadas y recorta las demás a [contentLength],
 * para que un dato corrupto nunca impida abrir la nota.
 */
internal fun String.decodeSpans(contentLength: Int): List<FormatSpan> =
    split(SPAN_SEPARATOR).mapNotNull { entry ->
        val fields = entry.split(FIELD_SEPARATOR)
        if (fields.size != 3) return@mapNotNull null
        val start = fields[0].toIntOrNull()?.coerceAtLeast(0) ?: return@mapNotNull null
        val end = fields[1].toIntOrNull()?.coerceAtMost(contentLength) ?: return@mapNotNull null
        val format = FormatCodes.entries.firstOrNull { it.value == fields[2] }?.key ?: return@mapNotNull null
        FormatSpan(start, end, format).takeIf { start < end }
    }
