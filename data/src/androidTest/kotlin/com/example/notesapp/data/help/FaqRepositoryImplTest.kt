package com.example.notesapp.data.help

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FaqRepositoryImplTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun given_repository_when_getting_faqs_then_returns_twelve_faqs() = runTest {
        val repository = createRepository()

        val faqs = repository.getFaqs()

        assertEquals(12, faqs.size)
    }

    @Test
    fun given_repository_when_getting_faqs_then_ids_go_from_one_to_twelve_in_order() = runTest {
        val repository = createRepository()

        val faqs = repository.getFaqs()

        assertEquals((1..12).toList(), faqs.map { it.id })
    }

    @Test
    fun given_repository_when_getting_faqs_then_no_question_or_answer_is_blank() = runTest {
        val repository = createRepository()

        val faqs = repository.getFaqs()

        faqs.forEach { faq ->
            assertTrue("Pregunta ${faq.id} vacía", faq.question.isNotBlank())
            assertTrue("Respuesta ${faq.id} vacía", faq.answer.isNotBlank())
        }
    }

    @Test
    fun given_repository_when_getting_faqs_then_first_faq_matches_the_design() = runTest {
        val repository = createRepository()

        val first = repository.getFaqs().first()

        assertEquals("¿Dónde se guardan mis notas?", first.question)
        assertEquals(
            "Tus notas se guardan en este dispositivo. Para tener una copia, ve a Ajustes → Exportar notas.",
            first.answer,
        )
    }

    @Test
    fun given_repository_when_getting_faqs_then_questions_follow_the_spec_order() = runTest {
        val repository = createRepository()

        val faqs = repository.getFaqs()

        assertEquals(SPEC_QUESTIONS, faqs.map { it.question })
    }

    private fun TestScope.createRepository() = FaqRepositoryImpl(context, StandardTestDispatcher(testScheduler))

    private companion object {
        /** Preguntas de la tabla «Preguntas frecuentes» de la spec 013, en su orden. */
        val SPEC_QUESTIONS = listOf(
            "¿Dónde se guardan mis notas?",
            "¿Cómo recupero una nota eliminada?",
            "¿Cómo cambio el color de mis notas?",
            "¿Cómo organizo mis notas por categoría?",
            "¿Cómo fijo una nota?",
            "¿Cómo creo una lista de tareas?",
            "¿Cómo doy formato al texto?",
            "¿Puedo deshacer un cambio?",
            "¿Se guardan mis cambios automáticamente?",
            "¿Cómo comparto una nota?",
            "¿Cómo exporto mis notas?",
            "¿Cómo cambio entre tema claro y oscuro?",
        )
    }
}
