package com.example.notesapp.presentation.notes.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.DeleteNoteUseCase
import com.example.notesapp.domain.notes.GetNoteUseCase
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.SaveNoteUseCase
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.notes.common.NoteDateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getNote: GetNoteUseCase,
    private val saveNote: SaveNoteUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val dateFormatter: NoteDateFormatter,
) : ViewModel() {

    private val _state = MutableStateFlow(NoteEditorUiState())
    val state: StateFlow<NoteEditorUiState> = _state.asStateFlow()

    private val _effects = Channel<NoteEditorEffect>(Channel.BUFFERED)
    val effects: Flow<NoteEditorEffect> = _effects.receiveAsFlow()

    /** Última versión guardada (o la nota nueva aún sin guardar): conserva id y color. */
    private var note = Note(
        id = Note.NEW_ID,
        title = "",
        content = "",
        color = NoteColor.AQUA,
        isPinned = false,
        updatedAt = 0,
    )
    private var hasPendingChanges = false
    private var autosaveJob: Job? = null
    // Evita dos guardados simultáneos, que podrían insertar dos veces una nota nueva.
    private val saveMutex = Mutex()

    init {
        val noteId = savedStateHandle.get<Long>(NOTE_ID_ARG) ?: Note.NEW_ID
        if (noteId != Note.NEW_ID) load(noteId)
    }

    fun onIntent(intent: NoteEditorIntent) {
        when (intent) {
            is NoteEditorIntent.TitleChanged -> edit { it.copy(title = intent.title) }
            is NoteEditorIntent.ContentChanged -> edit { it.copy(content = intent.content) }
            NoteEditorIntent.PinClicked -> togglePin()
            NoteEditorIntent.DeleteClicked -> _state.update { it.copy(isDeleteDialogVisible = true) }
            NoteEditorIntent.DeleteDismissed -> _state.update { it.copy(isDeleteDialogVisible = false) }
            NoteEditorIntent.DeleteConfirmed -> delete()
            NoteEditorIntent.BackClicked -> leave()
        }
    }

    private fun load(id: Long) {
        _state.update { it.copy(isNewNote = false, isLoading = true) }
        viewModelScope.launch {
            when (val result = getNote(id)) {
                is AppResult.Success -> {
                    note = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            title = note.title,
                            content = note.content,
                            isPinned = note.isPinned,
                            date = dateFormatter.format(note.updatedAt),
                        )
                    }
                }
                is AppResult.Error -> _state.update {
                    it.copy(isLoading = false, loadError = result.error.toLoadError())
                }
            }
        }
    }

    private fun edit(transform: (NoteEditorUiState) -> NoteEditorUiState) {
        _state.update(transform)
        hasPendingChanges = true
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            // Una vez empezado, el guardado termina aunque llegue otra edición.
            withContext(NonCancellable) { save() }
        }
    }

    private fun togglePin() {
        _state.update { it.copy(isPinned = !it.isPinned) }
        hasPendingChanges = true
        autosaveJob?.cancel()
        viewModelScope.launch { save() }
    }

    private suspend fun save() = saveMutex.withLock {
        if (!hasPendingChanges) return@withLock
        val current = _state.value
        val draft =
            note.copy(title = current.title, content = current.content, isPinned = current.isPinned)
        _state.update { it.copy(saveStatus = SaveStatus.Saving) }
        when (val result = saveNote(draft)) {
            is AppResult.Success -> {
                note = result.data
                // Si hubo ediciones durante el guardado, el autoguardado pendiente las guardará.
                hasPendingChanges = _state.value.run {
                    title != note.title || content != note.content || isPinned != note.isPinned
                }
                _state.update {
                    it.copy(saveStatus = SaveStatus.Saved, date = dateFormatter.format(note.updatedAt))
                }
            }
            is AppResult.Error -> {
                _state.update { it.copy(saveStatus = SaveStatus.Idle) }
                if (result.error == NoteError.EmptyNote) {
                    hasPendingChanges = false
                } else {
                    send(NoteEditorEffect.ShowMessage(R.string.note_editor_save_error))
                }
            }
        }
    }

    private fun leave() {
        viewModelScope.launch {
            autosaveJob?.cancel()
            autosaveJob?.join()
            save()
            if (!hasPendingChanges) send(NoteEditorEffect.NavigateBack)
        }
    }

    private fun delete() {
        _state.update { it.copy(isDeleteDialogVisible = false) }
        autosaveJob?.cancel()
        hasPendingChanges = false
        viewModelScope.launch {
            autosaveJob?.join()
            if (note.id == Note.NEW_ID) {
                send(NoteEditorEffect.NavigateBack)
                return@launch
            }
            when (deleteNote(note.id)) {
                is AppResult.Success -> send(NoteEditorEffect.NavigateBack)
                is AppResult.Error -> send(NoteEditorEffect.ShowMessage(R.string.note_editor_delete_error))
            }
        }
    }

    private fun NoteError.toLoadError(): LoadError = when (this) {
        NoteError.NotFound -> LoadError.NotFound
        NoteError.Storage, NoteError.EmptyNote -> LoadError.Storage
    }

    private fun send(effect: NoteEditorEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    companion object {
        /** Argumento de navegación con el id de la nota; [Note.NEW_ID] para crear una nueva. */
        const val NOTE_ID_ARG = "noteId"
        internal const val AUTOSAVE_DELAY_MS = 500L
    }
}
