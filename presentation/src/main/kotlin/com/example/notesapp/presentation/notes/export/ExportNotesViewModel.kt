package com.example.notesapp.presentation.notes.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.GetNotesUseCase
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.export.DiscardExportDestinationUseCase
import com.example.notesapp.domain.notes.export.DiscardNotesExportUseCase
import com.example.notesapp.domain.notes.export.GetExportArchiveNameUseCase
import com.example.notesapp.domain.notes.export.PrepareNotesExportUseCase
import com.example.notesapp.domain.notes.export.PreparedExport
import com.example.notesapp.domain.notes.export.SaveNotesExportUseCase
import com.example.notesapp.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
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
import kotlinx.coroutines.withContext

/**
 * Selección de notas y exportación. Primero se generan los PDF en un temporal y después se abre el selector de
 * documentos (D1 del plan 011): así el destino solo existe mientras se copia el .zip.
 */
@HiltViewModel
class ExportNotesViewModel @Inject constructor(
    getNotes: GetNotesUseCase,
    private val prepareExport: PrepareNotesExportUseCase,
    private val saveExport: SaveNotesExportUseCase,
    private val discardExport: DiscardNotesExportUseCase,
    private val discardDestination: DiscardExportDestinationUseCase,
    private val getArchiveName: GetExportArchiveNameUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ExportNotesUiState())
    val state: StateFlow<ExportNotesUiState> = _state.asStateFlow()

    private val _effects = Channel<ExportNotesEffect>(Channel.BUFFERED)
    val effects: Flow<ExportNotesEffect> = _effects.receiveAsFlow()

    /** Notas completas de la lista, para exportarlas sin volver a leer la base. */
    private var notes: List<Note> = emptyList()

    /** El .zip ya generado mientras el selector de documentos está abierto. */
    private var prepared: PreparedExport? = null

    private var exportJob: Job? = null

    init {
        getNotes()
            .onEach { result ->
                val loaded = (result as? AppResult.Success)?.data?.notes.orEmpty()
                notes = loaded
                val ids = loaded.mapTo(mutableSetOf()) { it.id }
                _state.update { state ->
                    state.copy(
                        isLoading = false,
                        loadFailed = result is AppResult.Error,
                        notes = loaded.map { ExportNoteItem(id = it.id, title = it.title.trim()) },
                        // Una nota que desaparece deja de estar seleccionada.
                        selectedIds = state.selectedIds intersect ids,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: ExportNotesIntent) {
        when (intent) {
            is ExportNotesIntent.NoteToggled -> updateSelection { selected ->
                if (intent.id in selected) selected - intent.id else selected + intent.id
            }
            ExportNotesIntent.SelectAllClicked -> updateSelection { _state.value.notes.mapTo(mutableSetOf()) { it.id } }
            ExportNotesIntent.ClearSelectionClicked -> updateSelection { emptySet() }
            ExportNotesIntent.ExportClicked -> export()
            is ExportNotesIntent.DestinationPicked -> onDestinationPicked(intent.uri)
            ExportNotesIntent.BackClicked -> leave()
        }
    }

    private fun updateSelection(transform: (Set<Long>) -> Set<Long>) {
        if (_state.value.isExporting) return
        _state.update { it.copy(selectedIds = transform(it.selectedIds)) }
    }

    private fun export() {
        if (!_state.value.canExport) return
        val selectedIds = _state.value.selectedIds
        val selected = notes.filter { it.id in selectedIds }
        _state.update { it.copy(isExporting = true) }
        exportJob = viewModelScope.launch {
            when (val result = prepareExport(selected)) {
                is AppResult.Success -> {
                    prepared = result.data
                    _effects.send(ExportNotesEffect.OpenDocumentPicker(getArchiveName()))
                }
                is AppResult.Error -> fail()
            }
        }
    }

    private fun onDestinationPicked(uri: String?) {
        val export = prepared
        prepared = null
        exportJob = viewModelScope.launch {
            when {
                // Selector cerrado sin confirmar: no se crea nada y se conserva la selección.
                uri == null -> {
                    export?.let { discardExport(it) }
                    _state.update { it.copy(isExporting = false) }
                }
                // El proceso murió con el selector abierto y el temporal se perdió: el destino quedó vacío.
                export == null -> {
                    discardDestination(uri)
                    fail()
                }
                else -> when (saveExport(export, uri)) {
                    is AppResult.Success -> {
                        _state.update { it.copy(isExporting = false) }
                        _effects.send(ExportNotesEffect.Finished(export.noteCount, export.missingImageCount))
                    }
                    is AppResult.Error -> fail()
                }
            }
        }
    }

    /** Volver cancela la exportación en curso; al cancelar, el exportador borra lo que hubiera escrito. */
    private fun leave() {
        exportJob?.cancel()
        val export = prepared
        prepared = null
        viewModelScope.launch {
            export?.let { withContext(NonCancellable) { discardExport(it) } }
            _effects.send(ExportNotesEffect.NavigateBack)
        }
    }

    private suspend fun fail() {
        _state.update { it.copy(isExporting = false) }
        _effects.send(ExportNotesEffect.ShowMessage(R.string.export_error))
    }
}
