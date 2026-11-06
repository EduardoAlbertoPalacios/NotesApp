package com.example.notesapp.presentation.notes.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.GetNotesUseCase
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.settings.GetNotesViewUseCase
import com.example.notesapp.domain.settings.NotesView
import com.example.notesapp.domain.settings.SetNotesViewUseCase
import com.example.notesapp.presentation.notes.common.NoteDateFormatter
import com.example.notesapp.presentation.notes.common.buildCardContent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getNotes: GetNotesUseCase,
    private val dateFormatter: NoteDateFormatter,
    private val getNotesView: GetNotesViewUseCase,
    private val setNotesView: SetNotesViewUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NoteListUiState(query = savedStateHandle[QUERY_KEY] ?: ""))
    val state: StateFlow<NoteListUiState> = _state.asStateFlow()

    private val selectedFilter = MutableStateFlow(NoteFilter.ALL)
    // Sobrevive a la muerte del proceso; Android lo descarta cuando el usuario cierra la app.
    // Es la copia que dispara la búsqueda; `_state.query` es la que pinta el campo. Cambia ambas solo con updateQuery().
    private val query = savedStateHandle.getStateFlow(QUERY_KEY, "")

    private val _effects = Channel<NoteListEffect>(Channel.BUFFERED)
    val effects: Flow<NoteListEffect> = _effects.receiveAsFlow()

    init {
        observeNotes()
    }

    fun onIntent(intent: NoteListIntent) {
        when (intent) {
            is NoteListIntent.NoteClicked -> send(NoteListEffect.NavigateToNote(intent.id))
            NoteListIntent.CreateNoteClicked ->
                send(NoteListEffect.NavigateToNote(id = null, category = selectedFilter.value.category))
            NoteListIntent.SettingsClicked -> send(NoteListEffect.NavigateToSettings)
            is NoteListIntent.FilterSelected -> {
                // Tocar un chip durante una búsqueda la limpia y aplica esa categoría.
                if (_state.value.isSearching) updateQuery("")
                selectedFilter.value = intent.filter
                _state.update { it.copy(selectedFilter = intent.filter) }
            }
            is NoteListIntent.QueryChanged -> updateQuery(intent.query)
            NoteListIntent.ClearQueryClicked -> updateQuery("")
            is NoteListIntent.NotesViewSelected -> selectNotesView(intent.view)
        }
    }

    private fun selectNotesView(view: NotesView) {
        if (view == _state.value.notesView) return
        viewModelScope.launch {
            if (setNotesView(view) is AppResult.Error) _effects.send(NoteListEffect.ShowNotesViewSaveError)
        }
    }

    /** Único punto que cambia el texto buscado: mantiene iguales `_state.query` y el `SavedStateHandle`. */
    private fun updateQuery(text: String) {
        // Primero el estado, de forma síncrona: el campo de texto nunca recibe un valor viejo.
        _state.update { it.copy(query = text) }
        savedStateHandle[QUERY_KEY] = text
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeNotes() {
        // Cada cambio de filtro o de texto cancela el cálculo anterior.
        val notes = combine(selectedFilter, query) { filter, text -> filter to text }
            .flatMapLatest { (filter, text) -> getNotes(filter.category, text) }
            // Antes de combinar: cambiar de vista no tiene por qué volver a construir las tarjetas.
            .map { result -> result to (result as? AppResult.Success)?.data?.notes?.map { note -> note.toUi() }.orEmpty() }
        // Junto con la vista: la carga no termina hasta conocer ambas, para no mostrar las notas en la otra vista.
        combine(notes, getNotesView()) { (result, items), view -> Triple(result, items, view) }
            .onEach { (result, items, view) ->
                when (result) {
                    is AppResult.Success -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                hasError = false,
                                notes = items,
                                totalCount = result.data.totalCount,
                                notesView = view,
                            )
                        }
                        // Sin notas el buscador se deshabilita: no puede quedar una búsqueda activa.
                        if (result.data.totalCount == 0 && _state.value.query.isNotEmpty()) updateQuery("")
                    }
                    is AppResult.Error -> _state.update { it.copy(isLoading = false, hasError = true, notesView = view) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun Note.toUi(): NoteItemUi {
        val cardContent = buildCardContent(content, contentSpans, contentTasks)
        return NoteItemUi(
            id = id,
            title = title,
            content = cardContent,
            color = color,
            isPinned = isPinned,
            date = dateFormatter.format(updatedAt),
            imagePath = images.firstOrNull()?.path,
            category = category,
            listContent = if (contentTasks.isEmpty()) cardContent else buildCardContent(content, contentSpans, contentTasks, flowTasks = true),
        )
    }

    private fun send(effect: NoteListEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private companion object {
        const val QUERY_KEY = "query"
    }
}
