package com.example.notesapp.domain.notes.export

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteExportNamingTest {

    private val utc = ZoneId.of("UTC")
    private val mexicoCity = ZoneId.of("America/Mexico_City")

    private fun millis(iso: String): Long = Instant.parse(iso).toEpochMilli()

    private fun names(vararg titles: String, untitled: String = UNTITLED): List<String> =
        NoteExportNaming.pdfNames(titles.toList(), untitled)

    // RF-14: nombre del .zip

    @Test
    fun `given instant when building archive name then is Notas with date and zip extension`() {
        val name = NoteExportNaming.archiveName(millis("2026-11-12T15:30:00Z"), utc)

        assertEquals("Notas 2026-11-12.zip", name)
    }

    @Test
    fun `given instant on different days in UTC and Mexico City when building archive name then uses date of zone`() {
        // 03:00 UTC del día 5 sigue siendo el día 4 en Ciudad de México (UTC-6)
        val epochMillis = millis("2026-01-05T03:00:00Z")

        val inUtc = NoteExportNaming.archiveName(epochMillis, utc)
        val inMexicoCity = NoteExportNaming.archiveName(epochMillis, mexicoCity)

        assertEquals("Notas 2026-01-05.zip", inUtc)
        assertEquals("Notas 2026-01-04.zip", inMexicoCity)
    }

    @Test
    fun `given single digit month and day when building archive name then pads them with zero`() {
        val name = NoteExportNaming.archiveName(millis("2026-03-07T12:00:00Z"), utc)

        assertEquals("Notas 2026-03-07.zip", name)
    }

    // RF-27: nombre con el título y extensión .pdf

    @Test
    fun `given titles when building pdf names then keeps order and size and appends pdf extension`() {
        val result = names("Compras", "Viaje", "Ideas")

        assertEquals(listOf("Compras.pdf", "Viaje.pdf", "Ideas.pdf"), result)
    }

    @Test
    fun `given empty list when building pdf names then returns empty list`() {
        val result = NoteExportNaming.pdfNames(emptyList(), UNTITLED)

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `given title with surrounding spaces when building pdf names then trims it`() {
        val result = names("  Compras  ")

        assertEquals(listOf("Compras.pdf"), result)
    }

    @Test
    fun `given title with accents and emojis when building pdf names then preserves them`() {
        val result = names("Canción de año 🎵😀")

        assertEquals(listOf("Canción de año 🎵😀.pdf"), result)
    }

    // RF-28: nota sin título

    @Test
    fun `given empty title when building pdf names then uses untitled argument`() {
        val result = names("")

        assertEquals(listOf("$UNTITLED.pdf"), result)
    }

    @Test
    fun `given whitespace only title when building pdf names then uses untitled argument`() {
        val result = names("   \t\n ")

        assertEquals(listOf("$UNTITLED.pdf"), result)
    }

    @Test
    fun `given custom untitled argument when title is blank then uses that argument`() {
        val result = names(" ", untitled = "Untitled")

        assertEquals(listOf("Untitled.pdf"), result)
    }

    // RF-29: caracteres no permitidos

    @Test
    fun `given each invalid character when building pdf names then replaces it with underscore`() {
        val invalid = listOf("/", "\\", ":", "*", "?", "\"", "<", ">", "|")

        // Cada carácter se evalúa en una lista propia para no mezclar sufijos de duplicados
        val result = invalid.map { names("a${it}b").single() }

        assertEquals(invalid.map { "a_b.pdf" }, result)
    }

    @Test
    fun `given title with several invalid characters when building pdf names then replaces all of them`() {
        val result = names("a/b\\c:d*e?f\"g<h>i|j")

        assertEquals(listOf("a_b_c_d_e_f_g_h_i_j.pdf"), result)
    }

    @Test
    fun `given title with only invalid characters when building pdf names then uses underscores`() {
        val result = names("///")

        assertEquals(listOf("___.pdf"), result)
    }

    // RF-30: máximo de 100 caracteres

    @Test
    fun `given title longer than 100 characters when building pdf names then cuts it to 100`() {
        val title = "a".repeat(150)

        val result = names(title)

        assertEquals(listOf("a".repeat(100) + ".pdf"), result)
    }

    @Test
    fun `given title of exactly 100 characters when building pdf names then keeps it whole`() {
        val title = "b".repeat(100)

        val result = names(title)

        assertEquals(listOf("$title.pdf"), result)
    }

    @Test
    fun `given emoji at cut boundary when building pdf names then does not split surrogate pair`() {
        // 99 letras + 2 emojis = 101 puntos de código; el corte cae justo después del primer emoji
        val title = "a".repeat(99) + "😀😀"

        val name = names(title).single().removeSuffix(".pdf")

        assertEquals("a".repeat(99) + "😀", name)
        assertEquals(100, name.codePointCount(0, name.length))
    }

    @Test
    fun `given title made of emojis longer than limit when building pdf names then keeps 100 whole emojis`() {
        val title = "😀".repeat(101)

        val result = names(title)

        assertEquals(listOf("😀".repeat(100) + ".pdf"), result)
    }

    @Test
    fun `given space left at end after cutting when building pdf names then trims it`() {
        val title = "a".repeat(98) + "  " + "b".repeat(10)

        val result = names(title)

        assertEquals(listOf("a".repeat(98) + ".pdf"), result)
    }

    // RF-31: nombres repetidos

    @Test
    fun `given repeated titles when building pdf names then adds numbered suffixes in list order`() {
        val result = names("Compra", "Compra", "Compra")

        assertEquals(listOf("Compra.pdf", "Compra (2).pdf", "Compra (3).pdf"), result)
    }

    @Test
    fun `given titles differing only in case when building pdf names then treats them as duplicates`() {
        val result = names("Compra", "compra")

        assertEquals(listOf("Compra.pdf", "compra (2).pdf"), result)
    }

    @Test
    fun `given two untitled notes when building pdf names then second gets suffix`() {
        val result = names("", "  ")

        assertEquals(listOf("Sin título.pdf", "Sin título (2).pdf"), result)
    }

    @Test
    fun `given title equal to an existing suffixed name when building pdf names then all names are unique`() {
        val result = names("A", "a", "A (2)")

        assertEquals(3, result.size)
        assertEquals(result.size, result.map { it.lowercase() }.distinct().size)
        assertEquals("A.pdf", result[0])
        assertEquals("a (2).pdf", result[1])
    }

    private companion object {
        const val UNTITLED = "Sin título"
    }
}

