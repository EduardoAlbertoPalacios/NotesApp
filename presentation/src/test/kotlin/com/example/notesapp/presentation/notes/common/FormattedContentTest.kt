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
import org.junit.Assert.assertFalse
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

    @Test
    fun `given tasks without flow when building card content then has no task break`() {
        val result = buildCardContent("Uno\nDos\nTres", emptyList(), allTasks(3))

        assertFalse(TASK_BREAK in result.text)
        assertEquals("${Z}Uno\n${Z}Dos\n${Z}Tres", result.text)
    }

    @Test
    fun `given no tasks with flow when building card content then has no task break and keeps line breaks`() {
        val result = buildCardContent("Uno\nDos", emptyList(), emptyList(), flowTasks = true)

        assertEquals("Uno\nDos", result.text)
    }

    @Test
    fun `given consecutive tasks with flow when building card content then puts a mark between each`() {
        val result = buildCardContent("Uno\nDos\nTres", emptyList(), allTasks(3), flowTasks = true)

        assertEquals("${Z}Uno$TASK_BREAK${Z}Dos$TASK_BREAK${Z}Tres", result.text)
    }

    @Test
    fun `given tasks separated by text with flow when building card content then keeps line breaks and has no mark`() {
        val tasks = listOf(ChecklistItem(line = 0, isDone = false), ChecklistItem(line = 2, isDone = false))

        val result = buildCardContent("Uno\nTexto\nDos", emptyList(), tasks, flowTasks = true)

        assertEquals("${Z}Uno\nTexto\n${Z}Dos", result.text)
    }

    @Test
    fun `given text before and after tasks with flow when building card content then only tasks are joined`() {
        val tasks = listOf(ChecklistItem(line = 1, isDone = false), ChecklistItem(line = 2, isDone = false))

        val result = buildCardContent("Antes\nUno\nDos\nDespués", emptyList(), tasks, flowTasks = true)

        assertEquals("Antes\n${Z}Uno$TASK_BREAK${Z}Dos\nDespués", result.text)
    }

    @Test
    fun `given empty lines and trailing break with flow when building card content then keeps those breaks`() {
        val tasks = listOf(ChecklistItem(line = 2, isDone = false), ChecklistItem(line = 3, isDone = false))

        val result = buildCardContent("Antes\n\nUno\nDos\n\nDespués\n", emptyList(), tasks, flowTasks = true)

        assertEquals("Antes\n\n${Z}Uno$TASK_BREAK${Z}Dos\n\nDespués\n", result.text)
    }

    @Test
    fun `given done task with flow when building card content then only its text is struck through`() {
        val tasks = listOf(ChecklistItem(line = 0, isDone = false), ChecklistItem(line = 1, isDone = true))

        val result = buildCardContent(LIST, emptyList(), tasks, flowTasks = true)

        assertEquals("${Z}Tomates$TASK_BREAK${Z}Pan", result.text)
        assertEquals(listOf(range(SpanStyle(textDecoration = TextDecoration.LineThrough), 10, 13)), result.spanStyles)
    }

    @Test
    fun `given format across tasks with flow when building card content then styles stay on the text and skip the mark`() {
        val tasks = listOf(ChecklistItem(line = 0, isDone = false), ChecklistItem(line = 1, isDone = true))

        val result = buildCardContent(LIST, listOf(FormatSpan(0, LIST.length, TextFormat.BOLD)), tasks, flowTasks = true)

        assertEquals(
            listOf(
                range(SpanStyle(fontWeight = FontWeight.Bold), 1, 8),
                range(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.LineThrough), 10, 13),
            ),
            result.spanStyles,
        )
    }

    @Test
    fun `given tasks with flow when building card content then each keeps its checkbox`() {
        val tasks = listOf(ChecklistItem(line = 0, isDone = false), ChecklistItem(line = 1, isDone = true))

        val result = buildCardContent(LIST, emptyList(), tasks, flowTasks = true)

        assertEquals(
            listOf(CARD_TASK_PENDING to (0 until 1), CARD_TASK_DONE to (9 until 10)),
            result.getStringAnnotations(0, result.length).map { it.item to (it.start until it.end) },
        )
    }

    @Test
    fun `given text without mark when wrapping tasks then returns the same object without measuring`() {
        val text = AnnotatedString("Sin tareas\nseguidas")

        val result = text.wrapTasks(MAX_WIDTH, SEPARATOR_WIDTH) { error("No debe medir") }

        assertSame(text, result)
    }

    @Test
    fun `given all tasks fit when wrapping then joins them with the separator on one line`() {
        val result = marked("Aa", "Bb", "Cc").wrapTasks(maxWidth = 100, separatorWidth = SEPARATOR_WIDTH, measure = byLength)

        assertEquals("Aa${TASK_SEPARATOR}Bb${TASK_SEPARATOR}Cc", result.text)
    }

    @Test
    fun `given task fits exactly up to the limit when wrapping then stays on the line`() {
        val result = marked("Aaaa", "Bbbb").wrapTasks(maxWidth = 9, separatorWidth = SEPARATOR_WIDTH, measure = byLength)

        assertEquals("Aaaa${TASK_SEPARATOR}Bbbb", result.text)
    }

    @Test
    fun `given task exceeds the limit by one when wrapping then starts on the next line`() {
        val result = marked("Aaaa", "Bbbb").wrapTasks(maxWidth = 8, separatorWidth = SEPARATOR_WIDTH, measure = byLength)

        assertEquals("Aaaa\nBbbb", result.text)
    }

    @Test
    fun `given separator is what does not fit when wrapping then starts on the next line`() {
        val result = marked("Aaaa", "Bbbb").wrapTasks(maxWidth = 9, separatorWidth = 2, measure = byLength)

        assertEquals("Aaaa\nBbbb", result.text)
    }

    @Test
    fun `given task wider than the line when wrapping then starts on its own line and the next one too`() {
        val result = marked("Aa", "Bbbbbbbbbbbb", "Cc")
            .wrapTasks(maxWidth = 9, separatorWidth = SEPARATOR_WIDTH, measure = byLength)

        assertEquals("Aa\nBbbbbbbbbbbb\nCc", result.text)
    }

    @Test
    fun `given first task wider than the line when wrapping then it keeps the first line without a blank one`() {
        val result = marked("Aaaaaaaaaaaa", "Bb").wrapTasks(maxWidth = 9, separatorWidth = SEPARATOR_WIDTH, measure = byLength)

        assertEquals("Aaaaaaaaaaaa\nBb", result.text)
    }

    @Test
    fun `given many tasks when wrapping then fills several rows and width restarts on each`() {
        val result = marked("Aaaa", "Bbbb", "Cccc", "Dddd", "Eeee")
            .wrapTasks(maxWidth = 9, separatorWidth = SEPARATOR_WIDTH, measure = byLength)

        assertEquals(
            "Aaaa${TASK_SEPARATOR}Bbbb\nCccc${TASK_SEPARATOR}Dddd\nEeee",
            result.text,
        )
    }

    @Test
    fun `given groups separated by text when wrapping then width restarts after the text`() {
        val text = AnnotatedString("Aaaa${TASK_BREAK}Bbbb\nTexto\nCccc${TASK_BREAK}Dddd")

        val result = text.wrapTasks(maxWidth = 9, separatorWidth = SEPARATOR_WIDTH, measure = byLength)

        assertEquals("Aaaa${TASK_SEPARATOR}Bbbb\nTexto\nCccc${TASK_SEPARATOR}Dddd", result.text)
    }

    @Test
    fun `given long text before the tasks when wrapping then only the line of the task counts`() {
        val text = AnnotatedString("Un texto mucho más largo que la línea\nAaaa${TASK_BREAK}Bbbb")

        val result = text.wrapTasks(maxWidth = 9, separatorWidth = SEPARATOR_WIDTH, measure = byLength)

        assertEquals("Un texto mucho más largo que la línea\nAaaa${TASK_SEPARATOR}Bbbb", result.text)
    }

    @Test
    fun `given long text after a task when wrapping then the task is measured without that text`() {
        val text = AnnotatedString("Aaaa${TASK_BREAK}Bbbb\nUn texto mucho más largo que la línea")

        val result = text.wrapTasks(maxWidth = 9, separatorWidth = SEPARATOR_WIDTH, measure = byLength)

        assertEquals("Aaaa${TASK_SEPARATOR}Bbbb\nUn texto mucho más largo que la línea", result.text)
    }

    @Test
    fun `given any width when wrapping then the result never contains the mark`() {
        val text = marked("Aaaa", "Bbbb", "Cccc", "Dddd")

        (1..30).forEach { maxWidth ->
            val result = text.wrapTasks(maxWidth, SEPARATOR_WIDTH, byLength)

            assertFalse("Ancho $maxWidth", TASK_BREAK in result.text)
        }
    }

    @Test
    fun `given card content when wrapped on one line then styles and checkboxes move with their text`() {
        val tasks = listOf(ChecklistItem(line = 0, isDone = false), ChecklistItem(line = 1, isDone = true))
        val content = buildCardContent(LIST, listOf(FormatSpan(0, 7, TextFormat.BOLD)), tasks, flowTasks = true)

        val result = content.wrapTasks(maxWidth = 100, separatorWidth = SEPARATOR_WIDTH, measure = byLength)

        val secondCheckbox = 8 + TASK_SEPARATOR.length
        assertEquals("${Z}Tomates$TASK_SEPARATOR${Z}Pan", result.text)
        assertEquals(
            listOf(
                range(SpanStyle(fontWeight = FontWeight.Bold), 1, 8),
                range(SpanStyle(textDecoration = TextDecoration.LineThrough), secondCheckbox + 1, secondCheckbox + 4),
            ),
            result.spanStyles,
        )
        assertEquals(
            listOf(CARD_TASK_PENDING to 0, CARD_TASK_DONE to secondCheckbox),
            result.getStringAnnotations(0, result.length).map { it.item to it.start },
        )
    }

    @Test
    fun `given card content when wrapped on two lines then styles and checkboxes move with their text`() {
        val tasks = listOf(ChecklistItem(line = 0, isDone = false), ChecklistItem(line = 1, isDone = true))
        val content = buildCardContent(LIST, emptyList(), tasks, flowTasks = true)

        val result = content.wrapTasks(maxWidth = 9, separatorWidth = SEPARATOR_WIDTH, measure = byLength)

        assertEquals("${Z}Tomates\n${Z}Pan", result.text)
        assertEquals(listOf(range(SpanStyle(textDecoration = TextDecoration.LineThrough), 10, 13)), result.spanStyles)
        assertEquals(
            listOf(CARD_TASK_PENDING to 0, CARD_TASK_DONE to 9),
            result.getStringAnnotations(0, result.length).map { it.item to it.start },
        )
    }

    private val byLength: (AnnotatedString) -> Int = { it.length }

    private fun marked(vararg tasks: String) = AnnotatedString(tasks.joinToString(TASK_BREAK.toString()))

    private fun allTasks(count: Int) = List(count) { ChecklistItem(line = it, isDone = false) }

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
        const val Z = "\u200B"
        const val MAX_WIDTH = 100
        const val SEPARATOR_WIDTH = 1
    }
}
