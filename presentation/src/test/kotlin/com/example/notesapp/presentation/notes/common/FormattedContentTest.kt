package com.example.notesapp.presentation.notes.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.notesapp.domain.notes.ChecklistItem
import com.example.notesapp.domain.notes.FormatSpan
import com.example.notesapp.domain.notes.TextFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattedContentTest {

    @Test
    fun `given no spans when building then keeps the text without styles`() {
        val result = buildFormattedContent(CONTENT, emptyList())

        assertEquals(CONTENT, result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `given bold span when building then styles that range with bold weight only`() {
        val result = buildFormattedContent(CONTENT, listOf(FormatSpan(0, 5, TextFormat.BOLD)))

        assertEquals(listOf(range(SpanStyle(fontWeight = FontWeight.Bold), 0, 5)), result.spanStyles)
    }

    @Test
    fun `given underline span when building then styles that range with underline`() {
        val result = buildFormattedContent(CONTENT, listOf(FormatSpan(6, 11, TextFormat.UNDERLINE)))

        assertEquals(listOf(range(SpanStyle(textDecoration = TextDecoration.Underline), 6, 11)), result.spanStyles)
    }

    @Test
    fun `given overlapping underline and strikethrough when building then combines them in the shared segment`() {
        val spans = listOf(FormatSpan(0, 6, TextFormat.UNDERLINE), FormatSpan(3, 9, TextFormat.STRIKETHROUGH))

        val result = buildFormattedContent(CONTENT, spans)

        assertEquals(
            listOf(
                range(SpanStyle(textDecoration = TextDecoration.Underline), 0, 3),
                range(
                    SpanStyle(
                        textDecoration = TextDecoration.combine(
                            listOf(TextDecoration.Underline, TextDecoration.LineThrough),
                        ),
                    ),
                    3,
                    6,
                ),
                range(SpanStyle(textDecoration = TextDecoration.LineThrough), 6, 9),
            ),
            result.spanStyles,
        )
    }

    @Test
    fun `given overlapping bold and underline when building then emits one style per segment`() {
        val spans = listOf(FormatSpan(0, 4, TextFormat.BOLD), FormatSpan(2, 6, TextFormat.UNDERLINE))

        val result = buildFormattedContent(CONTENT, spans)

        assertEquals(
            listOf(
                range(SpanStyle(fontWeight = FontWeight.Bold), 0, 2),
                range(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline), 2, 4),
                range(SpanStyle(textDecoration = TextDecoration.Underline), 4, 6),
            ),
            result.spanStyles,
        )
    }

    @Test
    fun `given separate spans when building then leaves the gap between them unstyled`() {
        val spans = listOf(FormatSpan(0, 2, TextFormat.BOLD), FormatSpan(6, 8, TextFormat.BOLD))

        val result = buildFormattedContent(CONTENT, spans)

        assertEquals(
            listOf(
                range(SpanStyle(fontWeight = FontWeight.Bold), 0, 2),
                range(SpanStyle(fontWeight = FontWeight.Bold), 6, 8),
            ),
            result.spanStyles,
        )
    }

    @Test
    fun `given spans out of range when building then clips them to the text`() {
        val spans = listOf(
            FormatSpan(-3, 2, TextFormat.BOLD),
            FormatSpan(CONTENT.length - 2, CONTENT.length + 10, TextFormat.UNDERLINE),
            FormatSpan(CONTENT.length + 1, CONTENT.length + 5, TextFormat.STRIKETHROUGH),
        )

        val result = buildFormattedContent(CONTENT, spans)

        assertEquals(CONTENT, result.text)
        assertEquals(
            listOf(
                range(SpanStyle(fontWeight = FontWeight.Bold), 0, 2),
                range(SpanStyle(textDecoration = TextDecoration.Underline), CONTENT.length - 2, CONTENT.length),
            ),
            result.spanStyles,
        )
    }

    @Test
    fun `given empty content with spans when building then has no styles`() {
        val result = buildFormattedContent("", listOf(FormatSpan(0, 4, TextFormat.BOLD)))

        assertEquals("", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `given transformation when filtering then styles the same text with identity offsets`() {
        val spans = listOf(FormatSpan(0, 5, TextFormat.BOLD))

        val transformed = FormattedContentTransformation(spans).filter(AnnotatedString(CONTENT))

        assertEquals(buildFormattedContent(CONTENT, spans), transformed.text)
        assertSame(OffsetMapping.Identity, transformed.offsetMapping)
    }

    @Test
    fun `given done and pending tasks when building then only the done line is struck through`() {
        val result = buildFormattedContent(LIST, emptyList(), tasks)

        assertEquals(LIST, result.text)
        assertEquals(listOf(range(SpanStyle(textDecoration = TextDecoration.LineThrough), 8, 11)), result.spanStyles)
    }

    @Test
    fun `given underline on a done task when building then combines underline and strikethrough`() {
        val result = buildFormattedContent(LIST, listOf(FormatSpan(8, 11, TextFormat.UNDERLINE)), tasks)

        assertEquals(
            listOf(
                range(
                    SpanStyle(
                        textDecoration = TextDecoration.combine(
                            listOf(TextDecoration.Underline, TextDecoration.LineThrough),
                        ),
                    ),
                    8,
                    11,
                ),
            ),
            result.spanStyles,
        )
    }

    @Test
    fun `given bold on a done task when building then keeps bold and adds strikethrough`() {
        val result = buildFormattedContent(LIST, listOf(FormatSpan(8, 11, TextFormat.BOLD)), tasks)

        assertEquals(
            listOf(range(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.LineThrough), 8, 11)),
            result.spanStyles,
        )
    }

    @Test
    fun `given underline crossing into a done task when building then splits segments at the task start`() {
        val result = buildFormattedContent(LIST, listOf(FormatSpan(0, 10, TextFormat.UNDERLINE)), tasks)

        assertEquals(
            listOf(
                range(SpanStyle(textDecoration = TextDecoration.Underline), 0, 8),
                range(
                    SpanStyle(
                        textDecoration = TextDecoration.combine(
                            listOf(TextDecoration.Underline, TextDecoration.LineThrough),
                        ),
                    ),
                    8,
                    10,
                ),
                range(SpanStyle(textDecoration = TextDecoration.LineThrough), 10, 11),
            ),
            result.spanStyles,
        )
    }

    @Test
    fun `given tasks without task style when building then has no paragraph styles and keeps line breaks`() {
        val result = buildFormattedContent(LIST, emptyList(), tasks)

        assertTrue(result.paragraphStyles.isEmpty())
        assertEquals(LIST, result.text)
    }

    @Test
    fun `given task style without tasks when building then has no paragraph styles and keeps line breaks`() {
        val result = buildFormattedContent(LIST, emptyList(), emptyList(), taskStyle)

        assertTrue(result.paragraphStyles.isEmpty())
        assertEquals(LIST, result.text)
    }

    @Test
    fun `given task style and done task when building then done line also takes the done color`() {
        val result = buildFormattedContent(LIST, emptyList(), tasks, taskStyle)

        assertEquals(
            listOf(range(SpanStyle(color = DONE_COLOR, textDecoration = TextDecoration.LineThrough), 8, 11)),
            result.spanStyles,
        )
    }

    @Test
    fun `given task style and tasks when building then each line is a paragraph indented only on tasks`() {
        val content = "Tomates\nPan\nNota"

        val result = buildFormattedContent(content, emptyList(), tasks, taskStyle)

        assertEquals(
            listOf(
                AnnotatedString.Range(paragraph(INDENT, INDENT, LINE_HEIGHT), 0, 8),
                AnnotatedString.Range(paragraph(INDENT, INDENT, LINE_HEIGHT), 8, 12),
                AnnotatedString.Range(paragraph(0.sp, 0.sp, TextUnit.Unspecified), 12, 16),
            ),
            result.paragraphStyles,
        )
    }

    @Test
    fun `given task style and tasks when building then line breaks become zero width spaces with same length`() {
        val content = "Tomates\nPan\nNota"

        val result = buildFormattedContent(content, emptyList(), tasks, taskStyle)

        assertEquals(content.length, result.length)
        assertEquals("Tomates\u200BPan\u200BNota", result.text)
    }

    @Test
    fun `given task style and content ending in line break when building then keeps the last line break`() {
        val content = "Tomates\nPan\n"

        val result = buildFormattedContent(content, emptyList(), tasks, taskStyle)

        assertEquals("Tomates\u200BPan\n", result.text)
        assertEquals(
            listOf(
                AnnotatedString.Range(paragraph(INDENT, INDENT, LINE_HEIGHT), 0, 8),
                AnnotatedString.Range(paragraph(INDENT, 0.sp, LINE_HEIGHT), 8, 12),
            ),
            result.paragraphStyles,
        )
    }

    @Test
    fun `given transformation with tasks and task style when filtering then keeps length with identity offsets`() {
        val transformed = FormattedContentTransformation(emptyList(), tasks, taskStyle).filter(AnnotatedString(LIST))

        assertEquals(LIST.length, transformed.text.length)
        assertEquals(buildFormattedContent(LIST, emptyList(), tasks, taskStyle), transformed.text)
        assertSame(OffsetMapping.Identity, transformed.offsetMapping)
    }

    @Test
    fun `given no tasks when building card content then equals formatted content`() {
        val spans = listOf(FormatSpan(0, 5, TextFormat.BOLD), FormatSpan(6, 11, TextFormat.UNDERLINE))

        val result = buildCardContent(CONTENT, spans, emptyList())

        assertEquals(buildFormattedContent(CONTENT, spans), result)
    }

    @Test
    fun `given tasks when building card content then adds an invisible checkbox at the start of each task`() {
        val content = "Tomates\nPan\nNota"

        val result = buildCardContent(content, emptyList(), tasks)

        assertEquals(content.length + tasks.size, result.length)
        assertEquals(content, result.text.replace("\u200B", ""))
        assertEquals(
            listOf(CARD_TASK_PENDING to (0 until 1), CARD_TASK_DONE to (9 until 10)),
            result.getStringAnnotations(0, result.length).map { it.item to (it.start until it.end) },
        )
    }

    @Test
    fun `given done task with bold when building card content then visible text keeps bold and strikethrough`() {
        val content = "Tomates\nPan\nNota"

        val result = buildCardContent(content, listOf(FormatSpan(12, 16, TextFormat.BOLD)), tasks)

        assertEquals(
            listOf(
                range(SpanStyle(textDecoration = TextDecoration.LineThrough), 10, 13),
                range(SpanStyle(fontWeight = FontWeight.Bold), 14, 18),
            ),
            result.spanStyles,
        )
    }

    private fun paragraph(firstLine: TextUnit, restLine: TextUnit, lineHeight: TextUnit) = ParagraphStyle(
        textIndent = TextIndent(firstLine = firstLine, restLine = restLine),
        lineHeight = lineHeight,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )

    private val tasks = listOf(ChecklistItem(line = 0, isDone = false), ChecklistItem(line = 1, isDone = true))

    private val taskStyle = TaskTextStyle(indent = INDENT, lineHeight = LINE_HEIGHT, doneColor = DONE_COLOR)

    private fun range(style: SpanStyle, start: Int, end: Int) = AnnotatedString.Range(style, start, end)

    private companion object {
        const val CONTENT = "Menos ruido, más foco."
        const val LIST = "Tomates\nPan"
        val INDENT = 28.sp
        val LINE_HEIGHT = 32.sp
        val DONE_COLOR = Color.Gray
    }
}
