package com.example.notesapp.domain.help

import org.junit.Assert.assertEquals
import org.junit.Test

class FaqSearchTest {

    private val createNote = faq(
        id = 1,
        question = "¿Cómo creo una nota?",
        answer = "Toca el botón de añadir en la lista de notas.",
    )
    private val recoverNote = faq(
        id = 2,
        question = "¿Cómo recupero una nota eliminada?",
        answer = "Abre la papelera desde el menú y restaura la nota.",
    )
    private val changeTheme = faq(
        id = 3,
        question = "¿Cómo cambio el tema?",
        answer = "Ve a Ajustes y elige claro, oscuro o el del sistema.",
    )
    private val allFaqs = listOf(createNote, recoverNote, changeTheme)

    @Test
    fun `given empty query when filtering then returns all faqs in order`() {
        val result = FaqSearch.filter(allFaqs, "")

        assertEquals(allFaqs, result)
    }

    @Test
    fun `given whitespace only query when filtering then returns all faqs in order`() {
        val result = FaqSearch.filter(allFaqs, "   \t ")

        assertEquals(allFaqs, result)
    }

    @Test
    fun `given word in question when filtering then returns that faq`() {
        val result = FaqSearch.filter(allFaqs, "tema")

        assertEquals(listOf(changeTheme), result)
    }

    @Test
    fun `given word only in answer when filtering then returns that faq`() {
        val result = FaqSearch.filter(allFaqs, "papelera")

        assertEquals(listOf(recoverNote), result)
    }

    @Test
    fun `given words split between question and answer when filtering then returns that faq`() {
        val result = FaqSearch.filter(allFaqs, "recupero papelera")

        assertEquals(listOf(recoverNote), result)
    }

    @Test
    fun `given one word missing when filtering then excludes the faq`() {
        val result = FaqSearch.filter(allFaqs, "papelera tema")

        assertEquals(emptyList<Faq>(), result)
    }

    @Test
    fun `given uppercase query when filtering lowercase text then matches`() {
        val result = FaqSearch.filter(allFaqs, "PAPELERA")

        assertEquals(listOf(recoverNote), result)
    }

    @Test
    fun `given query without accent when filtering accented text then matches`() {
        val result = FaqSearch.filter(allFaqs, "como cambio")

        assertEquals(listOf(changeTheme), result)
    }

    @Test
    fun `given accented query when filtering text without accent then matches`() {
        val faqs = listOf(faq(id = 1, question = "Menu principal", answer = "Sin tildes"))

        val result = FaqSearch.filter(faqs, "menú")

        assertEquals(faqs, result)
    }

    @Test
    fun `given n query when filtering text with enye then does not match`() {
        val faqs = listOf(faq(id = 1, question = "¿Qué pasa al cambiar de año?", answer = "Nada."))

        val result = FaqSearch.filter(faqs, "ano")

        assertEquals(emptyList<Faq>(), result)
    }

    @Test
    fun `given enye query when filtering text with enye then matches`() {
        val faqs = listOf(faq(id = 1, question = "¿Qué pasa al cambiar de año?", answer = "Nada."))

        val result = FaqSearch.filter(faqs, "AÑO")

        assertEquals(faqs, result)
    }

    @Test
    fun `given query with leading and trailing spaces when filtering then ignores them`() {
        val result = FaqSearch.filter(allFaqs, "   papelera  ")

        assertEquals(listOf(recoverNote), result)
    }

    @Test
    fun `given query without matches when filtering then returns empty list`() {
        val result = FaqSearch.filter(allFaqs, "exportar")

        assertEquals(emptyList<Faq>(), result)
    }

    @Test
    fun `given several matches when filtering then keeps original order`() {
        val result = FaqSearch.filter(allFaqs, "nota")

        assertEquals(listOf(createNote, recoverNote), result)
    }

    @Test
    fun `given several matches in unsorted ids when filtering then keeps list order`() {
        val faqs = listOf(changeTheme, recoverNote, createNote)

        val result = FaqSearch.filter(faqs, "como")

        assertEquals(faqs, result)
    }
}

internal fun faq(
    id: Int = 1,
    question: String = "¿Pregunta?",
    answer: String = "Respuesta.",
) = Faq(id = id, question = question, answer = answer)
