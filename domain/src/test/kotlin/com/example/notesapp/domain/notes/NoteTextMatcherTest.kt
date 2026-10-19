package com.example.notesapp.domain.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteTextMatcherTest {

    @Test
    fun `given uppercase query when matching lowercase note then matches`() {
        val note = note(title = "reunión de equipo")

        assertTrue(matches(note, "REUNIÓN"))
    }

    @Test
    fun `given uppercase enye when matching lowercase enye then matches`() {
        val note = note(content = "Plan del año")

        assertTrue(matches(note, "AÑO"))
    }

    @Test
    fun `given query without accent when matching accented note then matches`() {
        val note = note(title = "Reunión")

        assertTrue(matches(note, "reunion"))
    }

    @Test
    fun `given accented query when matching note without accent then matches`() {
        val note = note(title = "Reunion")

        assertTrue(matches(note, "reunión"))
    }

    @Test
    fun `given query without dieresis when matching note with dieresis then matches`() {
        val note = note(content = "Foto del pingüino")

        assertTrue(matches(note, "pinguino"))
    }

    @Test
    fun `given n query when matching note with enye then does not match`() {
        val note = note(content = "Plan del año")

        assertFalse(matches(note, "ano"))
    }

    @Test
    fun `given enye query when matching note with n then does not match`() {
        val note = note(content = "Dolor de ano")

        assertFalse(matches(note, "año"))
    }

    @Test
    fun `given decomposed enye in note when matching precomposed enye query then matches`() {
        val note = note(content = "Plan del año")

        assertTrue(matches(note, "año"))
    }

    @Test
    fun `given part of a word when matching then matches`() {
        val note = note(title = "Reunión de equipo")

        assertTrue(matches(note, "reun"))
    }

    @Test
    fun `given several words in different order when matching then matches`() {
        val note = note(content = "Reunión de equipo el lunes")

        assertTrue(matches(note, "lunes equipo reunión"))
    }

    @Test
    fun `given words split between title and content when matching then matches`() {
        val note = note(title = "Compras", content = "Leche y pan")

        assertTrue(matches(note, "pan compras"))
    }

    @Test
    fun `given note missing one of the words when matching then does not match`() {
        val note = note(title = "Compras", content = "Leche y pan")

        assertFalse(matches(note, "compras huevos"))
    }

    @Test
    fun `given leading trailing and repeated spaces when splitting then returns only the words`() {
        val words = NoteTextMatcher.words("   reunión    de\t equipo  ")

        assertEquals(listOf("reunion", "de", "equipo"), words)
    }

    @Test
    fun `given query with extra spaces when matching then matches as without them`() {
        val note = note(title = "Reunión de equipo")

        assertTrue(matches(note, "  equipo   reunión  "))
    }

    @Test
    fun `given only spaces when splitting then returns no words`() {
        val words = NoteTextMatcher.words("    \t  ")

        assertTrue(words.isEmpty())
    }

    @Test
    fun `given punctuation in query when matching note with the same punctuation then matches`() {
        val note = note(content = "¡Hola! ¿Qué tal?")

        assertTrue(matches(note, "¡hola!"))
    }

    @Test
    fun `given punctuation in query when matching note without it then does not match`() {
        val note = note(content = "Hola, qué tal")

        assertFalse(matches(note, "¡hola!"))
    }

    @Test
    fun `given emoji query when matching note with that emoji then matches`() {
        val note = note(title = "Cumpleaños 😀")

        assertTrue(matches(note, "😀"))
    }

    @Test
    fun `given emoji query when matching note without it then does not match`() {
        val note = note(title = "Cumpleaños 🎂")

        assertFalse(matches(note, "😀"))
    }

    @Test
    fun `given very long pasted text when matching short note then does not match`() {
        val note = note(title = "Reunión", content = "Equipo")
        val longQuery = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(200)

        assertFalse(matches(note, longQuery))
    }

    @Test
    fun `given very long note when matching a word it contains then matches`() {
        val note = note(content = "Lorem ipsum dolor sit amet. ".repeat(2_000) + "pingüino")

        assertTrue(matches(note, "PINGUINO"))
    }

    @Test
    fun `given note without title when matching a word in its content then matches`() {
        val note = note(title = "", content = "Reunión de equipo")

        assertTrue(matches(note, "equipo"))
    }

    @Test
    fun `given note without content when matching a word in its title then matches`() {
        val note = note(title = "Reunión de equipo", content = "")

        assertTrue(matches(note, "equipo"))
    }

    @Test
    fun `given work note whose text lacks the category name when matching category name then does not match`() {
        val note = note(title = "Informe", content = "Enviar el lunes", category = NoteCategory.WORK)

        assertFalse(matches(note, "work"))
        assertFalse(matches(note, "trabajo"))
    }

    @Test
    fun `given note with only images when matching text in the image path then does not match`() {
        val note = note(
            title = "",
            content = "",
            images = listOf(NoteImage(id = 1, path = "/data/files/images/reunion.jpg")),
        )

        assertFalse(matches(note, "reunion"))
        assertFalse(matches(note, "jpg"))
    }

    private fun matches(note: Note, query: String): Boolean =
        NoteTextMatcher.matches(note, NoteTextMatcher.words(query))

    private fun note(
        title: String = "",
        content: String = "",
        category: NoteCategory? = null,
        images: List<NoteImage> = emptyList(),
    ) = Note(
        id = 1,
        title = title,
        content = content,
        color = NoteColor.AQUA,
        isPinned = false,
        category = category,
        updatedAt = 0,
        images = images,
    )
}
