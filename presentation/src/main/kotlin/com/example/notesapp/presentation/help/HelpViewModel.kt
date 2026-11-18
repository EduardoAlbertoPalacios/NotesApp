package com.example.notesapp.presentation.help

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.domain.help.SearchFaqsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Ayuda y comentarios: busca entre las preguntas frecuentes y abre o cierra cada una.
 *
 * El texto buscado y las preguntas abiertas viven en el [SavedStateHandle] (D5): sobreviven al giro y
 * se olvidan al salir, porque el ViewModel pertenece a la entrada de navegación (RF-29, RF-30).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HelpViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    searchFaqs: SearchFaqsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HelpUiState(query = savedStateHandle[QUERY_KEY] ?: ""))
    val state: StateFlow<HelpUiState> = _state.asStateFlow()

    private val _effects = Channel<HelpEffect>(Channel.BUFFERED)
    val effects: Flow<HelpEffect> = _effects.receiveAsFlow()

    // `_state.query` pinta el campo al instante; esta copia dispara la búsqueda. Ambas cambian en updateQuery().
    private val query = savedStateHandle.getStateFlow(QUERY_KEY, "")
    private val expandedIds = savedStateHandle.getStateFlow(EXPANDED_IDS_KEY, IntArray(0))

    init {
        // Sin manejo de errores a propósito: las preguntas son recursos empaquetados con la app (D1),
        // así que leerlas no tiene un fallo esperable que mostrar al usuario.
        query
            .mapLatest { text -> searchFaqs(text) }
            .combine(expandedIds) { faqs, expanded ->
                faqs.map { faq ->
                    FaqUi(
                        id = faq.id,
                        question = faq.question,
                        answer = faq.answer,
                        isExpanded = faq.id in expanded,
                    )
                }
            }
            .onEach { faqs -> _state.update { it.copy(faqs = faqs, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: HelpIntent) {
        when (intent) {
            is HelpIntent.QueryChanged -> updateQuery(intent.query)
            HelpIntent.ClearQueryClicked -> updateQuery("")
            is HelpIntent.FaqClicked -> toggle(intent.id)
            HelpIntent.BackClicked -> viewModelScope.launch { _effects.send(HelpEffect.NavigateBack) }
        }
    }

    private fun updateQuery(text: String) {
        savedStateHandle[QUERY_KEY] = text
        _state.update { it.copy(query = text) }
    }

    /** Abre o cierra la pregunta sin tocar las demás; el conjunto no depende del filtro (RF-14, RF-23). */
    private fun toggle(id: Int) {
        val expanded = expandedIds.value
        savedStateHandle[EXPANDED_IDS_KEY] = if (id in expanded) expanded.filter { it != id }.toIntArray() else expanded + id
    }

    private companion object {
        const val QUERY_KEY = "help_query"
        const val EXPANDED_IDS_KEY = "help_expanded_ids"
    }
}
