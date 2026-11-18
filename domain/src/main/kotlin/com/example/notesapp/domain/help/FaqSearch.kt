package com.example.notesapp.domain.help

import com.example.notesapp.domain.notes.NoteTextMatcher

/**
 * Filtra las preguntas frecuentes con las mismas reglas que la búsqueda de notas (spec 001, D2):
 * sin distinguir mayúsculas ni tildes, «ñ» distinta de «n» y cada palabra en la pregunta o en la respuesta.
 */
internal object FaqSearch {

    /** Las preguntas que contienen cada palabra de [query], en su orden; todas si [query] está en blanco (RF-17 a RF-19). */
    fun filter(faqs: List<Faq>, query: String): List<Faq> {
        val words = NoteTextMatcher.words(query)
        if (words.isEmpty()) return faqs
        return faqs.filter { faq ->
            val question = NoteTextMatcher.normalize(faq.question)
            val answer = NoteTextMatcher.normalize(faq.answer)
            words.all { word -> word in question || word in answer }
        }
    }
}
