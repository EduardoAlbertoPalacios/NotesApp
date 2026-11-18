package com.example.notesapp.domain.help

/** Repositorio en memoria para las pruebas de la ayuda. */
class FakeFaqRepository(var faqs: List<Faq> = emptyList()) : FaqRepository {

    var getFaqsCalls = 0
        private set

    override suspend fun getFaqs(): List<Faq> {
        getFaqsCalls++
        return faqs
    }
}
