package com.example.notesapp.presentation.notes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.GetNotesUseCase
import com.example.notesapp.domain.notes.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val getNotes: GetNotesUseCase,
    private val dateFormatter: NoteDateFormatter,
) : ViewModel() {

    private val _state = MutableStateFlow(NoteListUiState())
    val state: StateFlow<NoteListUiState> = _state.asStateFlow()

    private val _effects = Channel<NoteListEffect>(Channel.BUFFERED)
    val effects: Flow<NoteListEffect> = _effects.receiveAsFlow()

    init {
        observeNotes()
    }

    fun onIntent(intent: NoteListIntent) {
        when (intent) {
            is NoteListIntent.NoteClicked -> send(NoteListEffect.NavigateToNote(intent.id))
            NoteListIntent.CreateNoteClicked -> send(NoteListEffect.NavigateToNote(null))
            NoteListIntent.SettingsClicked -> send(NoteListEffect.NavigateToSettings)
        }
    }

    private fun observeNotes() {
        getNotes()
            .onEach { result ->
                when (result) {
                    is AppResult.Success -> _state.update {
                        it.copy(isLoading = false, hasError = false, notes = result.data.map { note -> note.toUi() })
                    }
                    is AppResult.Error -> _state.update { it.copy(isLoading = false, hasError = true) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun Note.toUi() = NoteItemUi(
        id = id,
        title = title,
        content = content,
        color = color,
        isPinned = isPinned,
        date = dateFormatter.format(updatedAt),
    )

    private fun send(effect: NoteListEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
