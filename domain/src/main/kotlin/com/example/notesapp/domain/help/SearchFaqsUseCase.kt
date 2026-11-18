package com.example.notesapp.domain.help

import javax.inject.Inject

/** Devuelve las preguntas frecuentes que coinciden con [query]; todas si está en blanco (RF-17 a RF-19). */
class SearchFaqsUseCase @Inject constructor(
    private val repository: FaqRepository,
) {
    suspend operator fun invoke(query: String): List<Faq> = FaqSearch.filter(repository.getFaqs(), query)
}
