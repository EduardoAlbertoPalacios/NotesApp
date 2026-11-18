package com.example.notesapp.presentation.help

/** Estado de la pantalla Ayuda y comentarios (spec 013). */
data class HelpUiState(
    val query: String = "",
    val faqs: List<FaqUi> = emptyList(),
    val isLoading: Boolean = true,
) {
    /** Hay texto buscado y ninguna pregunta coincide (RF-20). */
    val showNoResults: Boolean get() = !isLoading && query.isNotBlank() && faqs.isEmpty()

    /** Texto buscado tal como se cita en el aviso sin resultados, sin los espacios exteriores. */
    val noResultsQuery: String get() = query.trim()
}

/** Una pregunta frecuente tal como se pinta: abierta muestra su respuesta (RF-12, RF-13). */
data class FaqUi(
    val id: Int,
    val question: String,
    val answer: String,
    val isExpanded: Boolean,
)
