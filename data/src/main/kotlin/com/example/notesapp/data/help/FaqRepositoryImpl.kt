package com.example.notesapp.data.help

import android.content.Context
import com.example.notesapp.common.dispatchers.IoDispatcher
import com.example.notesapp.data.R
import com.example.notesapp.domain.help.Faq
import com.example.notesapp.domain.help.FaqRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Lee las preguntas frecuentes de los recursos de `:data` (spec 013, D1). Los textos se leen en cada llamada,
 * así que un cambio de idioma del sistema se refleja al volver a abrir la ayuda.
 */
internal class FaqRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FaqRepository {

    override suspend fun getFaqs(): List<Faq> = withContext(ioDispatcher) {
        FAQS.mapIndexed { index, (question, answer) ->
            Faq(id = index + 1, question = context.getString(question), answer = context.getString(answer))
        }
    }

    private companion object {
        /** Pares de recursos (pregunta, respuesta) en el orden de la spec; el id de cada pregunta es su posición (1 a 12). */
        val FAQS: List<Pair<Int, Int>> = listOf(
            R.string.help_faq_1_question to R.string.help_faq_1_answer,
            R.string.help_faq_2_question to R.string.help_faq_2_answer,
            R.string.help_faq_3_question to R.string.help_faq_3_answer,
            R.string.help_faq_4_question to R.string.help_faq_4_answer,
            R.string.help_faq_5_question to R.string.help_faq_5_answer,
            R.string.help_faq_6_question to R.string.help_faq_6_answer,
            R.string.help_faq_7_question to R.string.help_faq_7_answer,
            R.string.help_faq_8_question to R.string.help_faq_8_answer,
            R.string.help_faq_9_question to R.string.help_faq_9_answer,
            R.string.help_faq_10_question to R.string.help_faq_10_answer,
            R.string.help_faq_11_question to R.string.help_faq_11_answer,
            R.string.help_faq_12_question to R.string.help_faq_12_answer,
        )
    }
}
