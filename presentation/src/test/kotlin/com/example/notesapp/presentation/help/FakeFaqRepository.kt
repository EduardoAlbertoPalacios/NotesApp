package com.example.notesapp.presentation.help

import com.example.notesapp.domain.help.Faq
import com.example.notesapp.domain.help.FaqRepository

class FakeFaqRepository(
    private val faqs: List<Faq> = defaultFaqs,
) : FaqRepository {

    override suspend fun getFaqs(): List<Faq> = faqs

    companion object {
        val defaultFaqs = listOf(
            Faq(
                id = 1,
                question = "¿Dónde se guardan mis notas?",
                answer = "Tus notas se guardan en este dispositivo.",
            ),
            Faq(
                id = 2,
                question = "¿Cómo recupero una nota eliminada?",
                answer = "Abre la papelera desde Ajustes.",
            ),
            Faq(
                id = 3,
                question = "¿Cómo cambio el color?",
                answer = "Mantén pulsada la nota y elige un color.",
            ),
        )
    }
}
