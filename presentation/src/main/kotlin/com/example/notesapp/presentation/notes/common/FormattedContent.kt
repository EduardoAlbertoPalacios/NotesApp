package com.example.notesapp.presentation.notes.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import com.example.notesapp.domain.notes.FormatSpan
import com.example.notesapp.domain.notes.TextFormat

/**
 * Contenido con sus estilos de formato. Se pinta por segmentos para que subrayado y tachado
 * se combinen en un mismo fragmento (dos estilos solapados con decoración se pisarían).
 */
internal fun buildFormattedContent(content: String, spans: List<FormatSpan>): AnnotatedString =
    buildAnnotatedString {
        append(content)
        val valid = spans.mapNotNull { span ->
            val start = span.start.coerceIn(0, content.length)
            val end = span.end.coerceIn(start, content.length)
            span.copy(start = start, end = end).takeIf { start < end }
        }
        val boundaries = valid.flatMap { listOf(it.start, it.end) }.distinct().sorted()
        boundaries.zipWithNext { start, end ->
            val formats = valid.filter { it.start <= start && it.end >= end }.mapTo(mutableSetOf()) { it.format }
            if (formats.isNotEmpty()) addStyle(formats.toSpanStyle(), start, end)
        }
    }

private fun Set<TextFormat>.toSpanStyle(): SpanStyle {
    val decorations = buildList {
        if (TextFormat.UNDERLINE in this@toSpanStyle) add(TextDecoration.Underline)
        if (TextFormat.STRIKETHROUGH in this@toSpanStyle) add(TextDecoration.LineThrough)
    }
    return SpanStyle(
        fontWeight = if (TextFormat.BOLD in this) FontWeight.Bold else null,
        textDecoration = decorations.takeIf { it.isNotEmpty() }?.let { TextDecoration.combine(it) },
    )
}

/** Pinta el formato en el campo de contenido sin cambiar el texto: las posiciones coinciden una a una. */
internal data class FormattedContentTransformation(val spans: List<FormatSpan>) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(buildFormattedContent(text.text, spans), OffsetMapping.Identity)
}
