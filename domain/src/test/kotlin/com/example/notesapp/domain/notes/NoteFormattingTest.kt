package com.example.notesapp.domain.notes

import com.example.notesapp.domain.notes.TextFormat.BOLD
import com.example.notesapp.domain.notes.TextFormat.STRIKETHROUGH
import com.example.notesapp.domain.notes.TextFormat.UNDERLINE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteFormattingTest {

    private fun bold(start: Int, end: Int) = FormatSpan(start, end, BOLD)
    private fun underline(start: Int, end: Int) = FormatSpan(start, end, UNDERLINE)
    private fun strikethrough(start: Int, end: Int) = FormatSpan(start, end, STRIKETHROUGH)

    // isApplied

    @Test
    fun `given empty range when checking format then is not applied`() {
        val spans = listOf(bold(0, 10))

        assertFalse(NoteFormatting.isApplied(spans, start = 3, end = 3, format = BOLD))
    }

    @Test
    fun `given range covered by one span when checking format then is applied`() {
        val spans = listOf(bold(0, 10))

        assertTrue(NoteFormatting.isApplied(spans, start = 2, end = 8, format = BOLD))
    }

    @Test
    fun `given range covered by two contiguous spans when checking format then is applied`() {
        val spans = listOf(bold(3, 6), bold(0, 3))

        assertTrue(NoteFormatting.isApplied(spans, start = 1, end = 5, format = BOLD))
    }

    @Test
    fun `given range partially covered when checking format then is not applied`() {
        val spans = listOf(bold(0, 3))

        assertFalse(NoteFormatting.isApplied(spans, start = 0, end = 5, format = BOLD))
    }

    @Test
    fun `given range covered by another format when checking format then is not applied`() {
        val spans = listOf(underline(0, 6))

        assertFalse(NoteFormatting.isApplied(spans, start = 0, end = 6, format = BOLD))
    }

    // toggle

    @Test
    fun `given range without format when toggling then applies format to range`() {
        val result = NoteFormatting.toggle(emptyList(), start = 0, end = 4, format = BOLD)

        assertEquals(listOf(bold(0, 4)), result)
    }

    @Test
    fun `given range fully formatted when toggling then removes format`() {
        val result = NoteFormatting.toggle(listOf(bold(0, 4)), start = 0, end = 4, format = BOLD)

        assertEquals(emptyList<FormatSpan>(), result)
    }

    @Test
    fun `given middle of formatted span when toggling then splits span in two`() {
        val result = NoteFormatting.toggle(listOf(bold(0, 10)), start = 3, end = 6, format = BOLD)

        assertEquals(listOf(bold(0, 3), bold(6, 10)), result)
    }

    @Test
    fun `given range partially formatted when toggling then applies format to whole range`() {
        val result = NoteFormatting.toggle(listOf(bold(0, 3)), start = 0, end = 6, format = BOLD)

        assertEquals(listOf(bold(0, 6)), result)
    }

    @Test
    fun `given range with another format when toggling then keeps the other format`() {
        val result = NoteFormatting.toggle(listOf(underline(0, 6)), start = 2, end = 4, format = BOLD)

        assertEquals(listOf(underline(0, 6), bold(2, 4)), result)
    }

    @Test
    fun `given plain range when toggling three formats then range has all three`() {
        val withBold = NoteFormatting.toggle(emptyList(), start = 0, end = 5, format = BOLD)
        val withUnderline = NoteFormatting.toggle(withBold, start = 0, end = 5, format = UNDERLINE)

        val result = NoteFormatting.toggle(withUnderline, start = 0, end = 5, format = STRIKETHROUGH)

        assertEquals(listOf(bold(0, 5), underline(0, 5), strikethrough(0, 5)), result)
    }

    @Test
    fun `given range with three formats when removing one then keeps the other two`() {
        val spans = listOf(bold(0, 5), underline(0, 5), strikethrough(0, 5))

        val result = NoteFormatting.toggle(spans, start = 0, end = 5, format = BOLD)

        assertEquals(listOf(underline(0, 5), strikethrough(0, 5)), result)
    }

    @Test
    fun `given selection crossing line breaks when toggling then whole selection is formatted`() {
        val text = "uno\ndos\ntres"

        val result = NoteFormatting.toggle(emptyList(), start = 2, end = text.length, format = BOLD)

        assertEquals(listOf(bold(2, text.length)), result)
        assertTrue(NoteFormatting.isApplied(result, start = 2, end = text.length, format = BOLD))
    }

    @Test
    fun `given empty range when toggling then spans do not change`() {
        val spans = listOf(bold(0, 4), underline(2, 6))

        val result = NoteFormatting.toggle(spans, start = 3, end = 3, format = BOLD)

        assertEquals(spans, result)
    }

    // formatsAt

    @Test
    fun `given position zero when getting formats then returns none`() {
        val spans = listOf(bold(0, 4))

        assertEquals(emptySet<TextFormat>(), NoteFormatting.formatsAt(spans, position = 0))
    }

    @Test
    fun `given position inside span when getting formats then returns its format`() {
        val spans = listOf(bold(0, 4))

        assertEquals(setOf(BOLD), NoteFormatting.formatsAt(spans, position = 2))
    }

    @Test
    fun `given position at span end when getting formats then includes its format`() {
        val spans = listOf(bold(0, 4))

        assertEquals(setOf(BOLD), NoteFormatting.formatsAt(spans, position = 4))
    }

    @Test
    fun `given position at span start when getting formats then does not include its format`() {
        val spans = listOf(bold(2, 4))

        assertEquals(emptySet<TextFormat>(), NoteFormatting.formatsAt(spans, position = 2))
    }

    @Test
    fun `given overlapping formats when getting formats then returns all of them`() {
        val spans = listOf(bold(0, 5), underline(2, 5), strikethrough(3, 8))

        assertEquals(setOf(BOLD, UNDERLINE, STRIKETHROUGH), NoteFormatting.formatsAt(spans, position = 4))
    }

    // activeFormats

    @Test
    fun `given cursor at position zero when getting active formats then returns none`() {
        val spans = listOf(bold(0, 4))

        assertEquals(emptySet<TextFormat>(), NoteFormatting.activeFormats(spans, start = 0, end = 0))
    }

    @Test
    fun `given cursor inside span when getting active formats then returns its format`() {
        val spans = listOf(bold(0, 4))

        assertEquals(setOf(BOLD), NoteFormatting.activeFormats(spans, start = 2, end = 2))
    }

    @Test
    fun `given cursor at span end when getting active formats then includes its format`() {
        val spans = listOf(bold(0, 4))

        assertEquals(setOf(BOLD), NoteFormatting.activeFormats(spans, start = 4, end = 4))
    }

    @Test
    fun `given cursor at span start when getting active formats then does not include its format`() {
        val spans = listOf(bold(2, 4))

        assertEquals(emptySet<TextFormat>(), NoteFormatting.activeFormats(spans, start = 2, end = 2))
    }

    @Test
    fun `given selection covered by two formats when getting active formats then returns both`() {
        val spans = listOf(bold(0, 8), underline(2, 6))

        assertEquals(setOf(BOLD, UNDERLINE), NoteFormatting.activeFormats(spans, start = 2, end = 6))
    }

    @Test
    fun `given selection partially covered by a format when getting active formats then does not include it`() {
        val spans = listOf(bold(0, 8), strikethrough(0, 3))

        assertEquals(setOf(BOLD), NoteFormatting.activeFormats(spans, start = 1, end = 6))
    }

    @Test
    fun `given selection covered by two contiguous spans of same format when getting active formats then includes it`() {
        val spans = listOf(underline(3, 7), underline(0, 3))

        assertEquals(setOf(UNDERLINE), NoteFormatting.activeFormats(spans, start = 1, end = 6))
    }

    @Test
    fun `given selection without formats when getting active formats then returns none`() {
        val spans = listOf(bold(0, 2), underline(8, 10))

        assertEquals(emptySet<TextFormat>(), NoteFormatting.activeFormats(spans, start = 3, end = 7))
    }

    // onTextChanged

    @Test
    fun `given insertion before span when text changes then span shifts`() {
        val spans = listOf(bold(5, 10))

        val result = NoteFormatting.onTextChanged(spans, "hola mundo", "¡hola mundo", emptySet())

        assertEquals(listOf(bold(6, 11)), result)
    }

    @Test
    fun `given insertion at span start without active formats when text changes then span shifts`() {
        val spans = listOf(bold(5, 10))

        val result = NoteFormatting.onTextChanged(spans, "hola mundo", "hola Xmundo", emptySet())

        assertEquals(listOf(bold(6, 11)), result)
    }

    @Test
    fun `given insertion inside span without active formats when text changes then span splits`() {
        val spans = listOf(bold(5, 10))

        val result = NoteFormatting.onTextChanged(spans, "hola mundo", "hola muXndo", emptySet())

        assertEquals(listOf(bold(5, 7), bold(8, 11)), result)
    }

    @Test
    fun `given insertion inside span with same format active when text changes then span stays merged`() {
        val spans = listOf(bold(5, 10))

        val result = NoteFormatting.onTextChanged(spans, "hola mundo", "hola muXndo", setOf(BOLD))

        assertEquals(listOf(bold(5, 11)), result)
    }

    @Test
    fun `given insertion right after span end without active formats when text changes then span does not grow`() {
        val spans = listOf(bold(0, 4))

        val result = NoteFormatting.onTextChanged(spans, "hola mundo", "holaX mundo", emptySet())

        assertEquals(listOf(bold(0, 4)), result)
    }

    @Test
    fun `given typing at span end with same format active when text changes then span grows`() {
        val spans = listOf(bold(0, 4))

        val result = NoteFormatting.onTextChanged(spans, "hola", "holas", setOf(BOLD))

        assertEquals(listOf(bold(0, 5)), result)
    }

    @Test
    fun `given deletion inside span when text changes then span shortens`() {
        val spans = listOf(bold(5, 10))

        val result = NoteFormatting.onTextChanged(spans, "hola mundo", "hola mdo", emptySet())

        assertEquals(listOf(bold(5, 8)), result)
    }

    @Test
    fun `given deletion of whole span when text changes then span disappears and later spans shift`() {
        val spans = listOf(bold(5, 10), underline(11, 16))

        val result = NoteFormatting.onTextChanged(spans, "hola mundo feliz", "hola  feliz", emptySet())

        assertEquals(listOf(underline(6, 11)), result)
    }

    @Test
    fun `given deletion of all text when text changes then spans are empty`() {
        val spans = listOf(bold(0, 4), underline(5, 10))

        val result = NoteFormatting.onTextChanged(spans, "hola mundo", "", setOf(BOLD))

        assertEquals(emptyList<FormatSpan>(), result)
    }

    @Test
    fun `given replaced selection when text changes then new text takes active formats`() {
        val spans = listOf(bold(5, 10))

        val result = NoteFormatting.onTextChanged(spans, "hola mudno feliz", "hola mundo feliz", setOf(UNDERLINE))

        assertEquals(listOf(bold(5, 7), underline(7, 9), bold(9, 10)), result)
    }

    @Test
    fun `given pasted text with active formats when text changes then all pasted text has those formats`() {
        val spans = listOf(underline(0, 4))

        val result = NoteFormatting.onTextChanged(
            spans,
            old = "hola ",
            new = "hola mundo cruel",
            activeFormats = setOf(BOLD, STRIKETHROUGH),
        )

        assertEquals(listOf(underline(0, 4), bold(5, 16), strikethrough(5, 16)), result)
    }

    @Test
    fun `given same text when text changes then returns normalized spans`() {
        val spans = listOf(bold(2, 4), bold(0, 2), underline(3, 3))

        val result = NoteFormatting.onTextChanged(spans, "hola", "hola", setOf(STRIKETHROUGH))

        assertEquals(listOf(bold(0, 4)), result)
    }

    // normalización

    @Test
    fun `given overlapping spans of same format when normalizing then they merge`() {
        val spans = listOf(bold(3, 8), bold(0, 5))

        val result = NoteFormatting.onTextChanged(spans, "hola mundo", "hola mundo", emptySet())

        assertEquals(listOf(bold(0, 8)), result)
    }

    @Test
    fun `given contiguous spans of same format when normalizing then they merge`() {
        val spans = listOf(underline(0, 3), underline(3, 6))

        val result = NoteFormatting.onTextChanged(spans, "hola mundo", "hola mundo", emptySet())

        assertEquals(listOf(underline(0, 6)), result)
    }

    @Test
    fun `given empty or negative spans when normalizing then they are discarded`() {
        val spans = listOf(bold(2, 2), underline(-1, 3), strikethrough(5, 4), bold(6, 8))

        val result = NoteFormatting.onTextChanged(spans, "hola mundo", "hola mundo", emptySet())

        assertEquals(listOf(bold(6, 8)), result)
    }

    @Test
    fun `given unordered spans when normalizing then sorts by start and format`() {
        val spans = listOf(strikethrough(1, 3), underline(0, 2), bold(0, 2))

        val result = NoteFormatting.onTextChanged(spans, "hola", "hola", emptySet())

        assertEquals(listOf(bold(0, 2), underline(0, 2), strikethrough(1, 3)), result)
    }
}
