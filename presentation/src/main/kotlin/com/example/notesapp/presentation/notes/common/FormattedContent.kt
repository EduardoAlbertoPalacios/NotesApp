package com.example.notesapp.presentation.notes.common

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.notesapp.domain.notes.ChecklistItem
import com.example.notesapp.domain.notes.FormatSpan
import com.example.notesapp.domain.notes.TextFormat

/**
 * Cómo se pintan las tareas en el editor: hueco a la izquierda para la casilla, altura de cada fila de
 * tarea (la del diseño) y color de las hechas.
 */
internal data class TaskTextStyle(val indent: TextUnit, val lineHeight: TextUnit, val doneColor: Color)

/**
 * Contenido con sus estilos de formato. Se pinta por segmentos para que subrayado y tachado
 * se combinen en un mismo fragmento (dos estilos solapados con decoración se pisarían).
 *
 * Las tareas hechas se ven tachadas; ese tachado es solo visual y se combina con el formato guardado.
 * Con [taskStyle] (el editor), además toman [TaskTextStyle.doneColor] y cada tarea deja hueco para su casilla.
 *
 * La sangría solo existe por párrafo, y un párrafo que termina en `\n` dibuja una línea vacía de más.
 * Por eso, si hay tareas, cada línea es su propio párrafo y los `\n` se cambian por un espacio de ancho
 * cero: el texto mide lo mismo y las posiciones no cambian. Si el contenido termina en `\n`, ese último
 * se deja, para que la línea vacía final exista.
 */
internal fun buildFormattedContent(
    content: String,
    spans: List<FormatSpan>,
    tasks: List<ChecklistItem> = emptyList(),
    taskStyle: TaskTextStyle? = null,
): AnnotatedString = buildAnnotatedString {
    val usesTaskParagraphs = taskStyle != null && tasks.isNotEmpty()
    append(if (usesTaskParagraphs) content.withLineBreaksAsParagraphs() else content)
    val valid = spans.mapNotNull { span ->
        val start = span.start.coerceIn(0, content.length)
        val end = span.end.coerceIn(start, content.length)
        span.copy(start = start, end = end).takeIf { start < end }
    }
    val lines = lineRanges(content)
    val doneRanges = tasks.filter { it.isDone }.mapNotNull { lines.getOrNull(it.line) }.filter { it.first < it.last + 1 }
    val boundaries = (valid.flatMap { listOf(it.start, it.end) } + doneRanges.flatMap { listOf(it.first, it.last + 1) })
        .distinct()
        .sorted()
    boundaries.zipWithNext { start, end ->
        val formats = valid.filter { it.start <= start && it.end >= end }.mapTo(mutableSetOf()) { it.format }
        val isDone = doneRanges.any { it.first <= start && it.last + 1 >= end }
        if (formats.isNotEmpty() || isDone) addStyle(formats.toSpanStyle(isDone, taskStyle?.doneColor), start, end)
    }
    if (usesTaskParagraphs) {
        val taskLines = tasks.mapTo(mutableSetOf()) { it.line }
        val indentOf = { line: Int -> if (line in taskLines) taskStyle.indent else 0.sp }
        // Cada línea es un párrafo: sin recortar su altura, para que mantenga la del resto del texto.
        val noTrim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
        val endsWithEmptyLine = content.endsWith('\n')
        // La línea vacía final va dentro del párrafo anterior, con su propia sangría (restLine).
        val paragraphCount = if (endsWithEmptyLine) lines.size - 1 else lines.size
        for (index in 0 until paragraphCount) {
            val line = lines[index]
            val isLast = index == paragraphCount - 1
            val end = if (isLast) content.length else line.last + 2
            val restLine = if (isLast && endsWithEmptyLine) indentOf(index + 1) else indentOf(index)
            val paragraph = ParagraphStyle(
                textIndent = TextIndent(firstLine = indentOf(index), restLine = restLine),
                lineHeight = if (index in taskLines) taskStyle.lineHeight else TextUnit.Unspecified,
                lineHeightStyle = noTrim,
            )
            addStyle(paragraph, line.first, end)
        }
    }
}

/** Cambia los saltos de línea por espacios de ancho cero, salvo el último si el texto termina en uno. */
private fun String.withLineBreaksAsParagraphs(): String {
    val keepLast = endsWith('\n')
    return mapIndexed { index, char ->
        if (char == '\n' && !(keepLast && index == lastIndex)) ZERO_WIDTH_SPACE else char
    }.joinToString("")
}

private const val ZERO_WIDTH_SPACE = '\u200B'

/** Id del contenido en línea que dibuja una casilla pendiente en la tarjeta. */
internal const val CARD_TASK_PENDING = "card_task_pending"

/** Id del contenido en línea que dibuja una casilla hecha en la tarjeta. */
internal const val CARD_TASK_DONE = "card_task_done"

/**
 * Contenido para la tarjeta de Mis notas: el formato y el tachado de las hechas, más una casilla en línea
 * ([CARD_TASK_PENDING] o [CARD_TASK_DONE]) al inicio de cada tarea. Las casillas solo se ven: no se tocan.
 */
internal fun buildCardContent(
    content: String,
    spans: List<FormatSpan>,
    tasks: List<ChecklistItem>,
): AnnotatedString {
    val formatted = buildFormattedContent(content, spans, tasks)
    if (tasks.isEmpty()) return formatted
    val lines = lineRanges(content)
    return buildAnnotatedString {
        lines.forEachIndexed { index, line ->
            val task = tasks.firstOrNull { it.line == index }
            if (task != null) {
                // Texto alternativo invisible (no puede ser vacío): la casilla anuncia su estado con strings.xml.
                appendInlineContent(
                    id = if (task.isDone) CARD_TASK_DONE else CARD_TASK_PENDING,
                    alternateText = ZERO_WIDTH_SPACE.toString(),
                )
            }
            // La línea con su salto, conservando sus estilos.
            val end = if (index < lines.lastIndex) line.last + 2 else line.last + 1
            append(formatted.subSequence(line.first, end))
        }
    }
}

/** Rango de cada línea del contenido, sin su salto de línea (de `first` a `last`, incluidos). */
private fun lineRanges(content: String): List<IntRange> {
    var start = 0
    return content.split('\n').map { line ->
        (start until start + line.length).also { start += line.length + 1 }
    }
}

private fun Set<TextFormat>.toSpanStyle(isDone: Boolean, doneColor: Color?): SpanStyle {
    val decorations = buildList {
        if (TextFormat.UNDERLINE in this@toSpanStyle) add(TextDecoration.Underline)
        if (TextFormat.STRIKETHROUGH in this@toSpanStyle || isDone) add(TextDecoration.LineThrough)
    }
    return SpanStyle(
        color = if (isDone && doneColor != null) doneColor else Color.Unspecified,
        fontWeight = if (TextFormat.BOLD in this) FontWeight.Bold else null,
        textDecoration = decorations.takeIf { it.isNotEmpty() }?.let { TextDecoration.combine(it) },
    )
}

/** Pinta el formato en el campo de contenido sin cambiar el texto: las posiciones coinciden una a una. */
internal data class FormattedContentTransformation(
    val spans: List<FormatSpan>,
    val tasks: List<ChecklistItem> = emptyList(),
    val taskStyle: TaskTextStyle? = null,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(buildFormattedContent(text.text, spans, tasks, taskStyle), OffsetMapping.Identity)
}
