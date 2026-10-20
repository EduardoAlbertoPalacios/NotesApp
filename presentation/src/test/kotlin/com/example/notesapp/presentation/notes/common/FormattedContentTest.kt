package com.example.notesapp.presentation.notes.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.style.TextDecoration
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

    private fun range(style: SpanStyle, start: Int, end: Int) = AnnotatedString.Range(style, start, end)

    private companion object {
        const val CONTENT = "Menos ruido, más foco."
    }
}
