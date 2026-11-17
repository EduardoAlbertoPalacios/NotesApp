package com.example.notesapp.presentation.notes.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.DeleteNoteUseCase
import com.example.notesapp.domain.notes.trash.ChangeTrashRetentionUseCase
import com.example.notesapp.domain.notes.trash.EmptyTrashUseCase
import com.example.notesapp.domain.notes.trash.GetTrashRetentionUseCase
import com.example.notesapp.domain.notes.trash.GetTrashUseCase
import com.example.notesapp.domain.notes.trash.RestoreFromTrashUseCase
import com.example.notesapp.domain.notes.trash.TrashEntry
import com.example.notesapp.domain.notes.trash.TrashRetention
import com.example.notesapp.domain.notes.trash.TrashRetentionChange
import com.example.notesapp.presentation.R
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

/** Papelera: lista las notas eliminadas, las restaura o borra, vacía la papelera y cambia el plazo. */
@HiltViewModel
class TrashViewModel @Inject constructor(
    getTrash: GetTrashUseCase,
    getTrashRetention: GetTrashRetentionUseCase,
    private val restoreFromTrash: RestoreFromTrashUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val emptyTrash: EmptyTrashUseCase,
    private val changeTrashRetention: ChangeTrashRetentionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TrashUiState())
    val state: StateFlow<TrashUiState> = _state.asStateFlow()

    private val _effects = Channel<TrashEffect>(Channel.BUFFERED)
    val effects: Flow<TrashEffect> = _effects.receiveAsFlow()

    init {
        getTrash()
            .onEach { result ->
                val notes = (result as? AppResult.Success)?.data.orEmpty().map { it.toUi() }
                val ids = notes.mapTo(mutableSetOf()) { it.id }
                _state.update { state ->
                    state.copy(
                        isLoading = false,
                        loadFailed = result is AppResult.Error,
                        notes = notes,
                        // Si la nota desaparece (otra pantalla, la purga o un plazo más corto), se cierran su hoja
                        // y su diálogo; si no queda ninguna, también la confirmación de vaciar.
                        actionsNoteId = state.actionsNoteId?.takeIf { it in ids },
                        pendingDeleteId = state.pendingDeleteId?.takeIf { it in ids },
                        isEmptyConfirmVisible = state.isEmptyConfirmVisible && notes.isNotEmpty(),
                    )
                }
            }
            .launchIn(viewModelScope)

        getTrashRetention()
            .onEach { retention -> _state.update { it.copy(retention = retention) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: TrashIntent) {
        when (intent) {
            is TrashIntent.NoteClicked -> openActions(intent.id)
            TrashIntent.ActionsDismissed -> _state.update { it.copy(actionsNoteId = null) }
            TrashIntent.RestoreClicked -> restore()
            TrashIntent.DeleteForeverClicked -> _state.update {
                it.copy(actionsNoteId = null, pendingDeleteId = it.actionsNoteId)
            }
            TrashIntent.DeleteForeverConfirmed -> deleteForever()
            TrashIntent.DeleteForeverDismissed -> _state.update { it.copy(pendingDeleteId = null) }
            TrashIntent.EmptyClicked -> _state.update { it.copy(isEmptyConfirmVisible = it.canEmpty) }
            TrashIntent.EmptyConfirmed -> empty()
            TrashIntent.EmptyDismissed -> _state.update { it.copy(isEmptyConfirmVisible = false) }
            TrashIntent.RetentionClicked -> _state.update { it.copy(isRetentionSheetVisible = true) }
            TrashIntent.RetentionSheetDismissed -> _state.update { it.copy(isRetentionSheetVisible = false) }
            is TrashIntent.RetentionSelected -> selectRetention(intent.retention)
            TrashIntent.RetentionConfirmed -> confirmRetention()
            TrashIntent.RetentionDismissed -> _state.update { it.copy(pendingRetention = null) }
            TrashIntent.BackClicked -> viewModelScope.launch { _effects.send(TrashEffect.NavigateBack) }
        }
    }

    private fun openActions(id: Long) {
        if (_state.value.notes.none { it.id == id }) return
        _state.update { it.copy(actionsNoteId = id) }
    }

    private fun restore() {
        val id = _state.value.actionsNoteId ?: return
        _state.update { it.copy(actionsNoteId = null) }
        viewModelScope.launch {
            val message = when (restoreFromTrash(id)) {
                is AppResult.Success -> R.string.trash_restored
                is AppResult.Error -> R.string.trash_restore_error
            }
            _effects.send(TrashEffect.ShowMessage(message))
        }
    }

    private fun deleteForever() {
        val id = _state.value.pendingDeleteId ?: return
        _state.update { it.copy(pendingDeleteId = null) }
        viewModelScope.launch {
            if (deleteNote(id) is AppResult.Error) _effects.send(TrashEffect.ShowMessage(R.string.trash_delete_error))
        }
    }

    private fun empty() {
        if (!_state.value.isEmptyConfirmVisible) return
        _state.update { it.copy(isEmptyConfirmVisible = false) }
        viewModelScope.launch {
            if (emptyTrash() is AppResult.Error) _effects.send(TrashEffect.ShowMessage(R.string.trash_empty_error))
        }
    }

    private fun selectRetention(retention: TrashRetention) {
        _state.update { it.copy(isRetentionSheetVisible = false) }
        if (retention == _state.value.retention) return
        changeRetention(retention, confirmed = false)
    }

    private fun confirmRetention() {
        val pending = _state.value.pendingRetention ?: return
        _state.update { it.copy(pendingRetention = null) }
        changeRetention(pending.retention, confirmed = true)
    }

    private fun changeRetention(retention: TrashRetention, confirmed: Boolean) {
        viewModelScope.launch {
            when (val change = changeTrashRetention(retention, confirmed)) {
                TrashRetentionChange.Applied -> Unit
                is TrashRetentionChange.NeedsConfirmation -> _state.update {
                    it.copy(pendingRetention = PendingRetention(retention, change.count))
                }
                TrashRetentionChange.Failed ->
                    _effects.send(TrashEffect.ShowMessage(R.string.trash_retention_save_error))
            }
        }
    }
}

private fun TrashEntry.toUi(): TrashItemUi {
    val note = trashed.note
    return TrashItemUi(
        id = note.id,
        title = note.title.trim(),
        excerpt = note.content.trim(),
        color = note.color,
        daysLeft = daysLeft,
    )
}
