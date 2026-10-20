package com.example.notesapp.data.notes

import com.example.notesapp.domain.notes.FormatSpan
import com.example.notesapp.domain.notes.TextFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatSpanCodecTest {

    @Test
    fun `given spans with every format when encoding then uses start end and code separated by semicolons`() {
        val spans = listOf(
            span(0, 5, TextFormat.BOLD),
            span(3, 9, TextFormat.UNDERLINE),
            span(10, 12, TextFormat.STRIKETHROUGH),
        )

        assertEquals("0,5,B;3,9,U;10,12,S", spans.encode())
    }

    @Test
    fun `given several spans and formats when encoding and decoding then returns the same spans in order`() {
        val spans = listOf(
            span(0, 5, TextFormat.BOLD),
            span(3, 9, TextFormat.UNDERLINE),
            span(3, 9, TextFormat.BOLD),
            span(10, 12, TextFormat.STRIKETHROUGH),
        )

        assertEquals(spans, spans.encode().decodeSpans(contentLength = 20))
    }

    @Test
    fun `given empty list when encoding then returns empty string`() {
        assertEquals("", emptyList<FormatSpan>().encode())
    }

    @Test
    fun `given empty string when decoding then returns empty list`() {
        assertEquals(emptyList<FormatSpan>(), "".decodeSpans(contentLength = 10))
    }

    @Test
    fun `given malformed entries when decoding then discards them and keeps the valid ones`() {
        val stored = listOf(
            "0,2,B",
            "1,2",
            "1,2,U,4",
            "a,2,U",
            "1,b,U",
            "1,2,X",
            "1,2,b",
            "",
            "4,6,S",
        ).joinToString(";")

        assertEquals(
            listOf(span(0, 2, TextFormat.BOLD), span(4, 6, TextFormat.STRIKETHROUGH)),
            stored.decodeSpans(contentLength = 10),
        )
    }

    @Test
    fun `given span beyond content length when decoding then clips end to content length`() {
        assertEquals(listOf(span(2, 5, TextFormat.UNDERLINE)), "2,50,U".decodeSpans(contentLength = 5))
    }

    @Test
    fun `given span with negative start when decoding then clips start to zero`() {
        assertEquals(listOf(span(0, 3, TextFormat.BOLD)), "-4,3,B".decodeSpans(contentLength = 5))
    }

    @Test
    fun `given spans that become empty after clipping when decoding then discards them`() {
        val stored = "5,9,B;7,9,U;-3,0,S;1,3,S"

        assertEquals(listOf(span(1, 3, TextFormat.STRIKETHROUGH)), stored.decodeSpans(contentLength = 5))
    }

    @Test
    fun `given empty or inverted span when decoding then discards it`() {
        assertEquals(emptyList<FormatSpan>(), "2,2,B;4,1,U".decodeSpans(contentLength = 10))
    }

    @Test
    fun `given spans when content is empty then decoding discards all of them`() {
        assertEquals(emptyList<FormatSpan>(), "0,3,B".decodeSpans(contentLength = 0))
    }

    private fun span(start: Int, end: Int, format: TextFormat) = FormatSpan(start, end, format)
}
