package com.example.notesapp.domain.help

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchFaqsUseCaseTest {

    private val createNote = faq(id = 1, question = "¿Cómo creo una nota?", answer = "Toca el botón de añadir.")
    private val recoverNote = faq(
        id = 2,
        question = "¿Cómo recupero una nota eliminada?",
        answer = "Abre la papelera y restáurala.",
    )
    private val allFaqs = listOf(createNote, recoverNote)

    private lateinit var repository: FakeFaqRepository

    private fun createUseCase(faqs: List<Faq> = allFaqs): SearchFaqsUseCase {
        repository = FakeFaqRepository(faqs)
        return SearchFaqsUseCase(repository)
    }

    @Test
    fun `given query when invoked then reads repository and returns matching faqs`() = runTest {
        val searchFaqs = createUseCase()

        val result = searchFaqs("papelera")

        assertEquals(listOf(recoverNote), result)
        assertEquals(1, repository.getFaqsCalls)
    }

    @Test
    fun `given blank query when invoked then returns all faqs in order`() = runTest {
        val searchFaqs = createUseCase()

        val result = searchFaqs("  ")

        assertEquals(allFaqs, result)
    }

    @Test
    fun `given repository content changed when invoked again then reflects new content`() = runTest {
        val searchFaqs = createUseCase()
        val newFaq = faq(id = 3, question = "¿Dónde está la papelera?")

        repository.faqs = listOf(newFaq)
        val result = searchFaqs("papelera")

        assertEquals(listOf(newFaq), result)
    }
}
