package com.example.notesapp.domain.help

/** Origen de las preguntas frecuentes. El contenido no cambia mientras la app está abierta. */
interface FaqRepository {

    /** Todas las preguntas, en el orden en que se muestran (RF-7). */
    suspend fun getFaqs(): List<Faq>
}
