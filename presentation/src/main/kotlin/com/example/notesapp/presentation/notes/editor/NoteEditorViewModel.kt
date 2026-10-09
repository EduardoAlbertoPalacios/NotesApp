package com.example.notesapp.presentation.notes.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.AddNoteImagesUseCase
import com.example.notesapp.domain.notes.DeleteNoteUseCase
import com.example.notesapp.domain.notes.GetNoteUseCase
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.RemoveNoteImageUseCase
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
    private val addNoteImages: AddNoteImagesUseCase,
    private val removeNoteImage: RemoveNoteImageUseCase,
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
        // Una nota nueva nace en la categoría del filtro activo del listado, si lo hay.
        category = savedStateHandle.get<String>(CATEGORY_ARG)
            ?.let { name -> NoteCategory.entries.firstOrNull { it.name == name } },
        updatedAt = 0,
    )
    private var hasPendingChanges = false
    private var autosaveJob: Job? = null
    // Evita dos guardados simultáneos, que podrían insertar dos veces una nota nueva.
    private val saveMutex = Mutex()

    init {
        val noteId = savedStateHandle.get<Long>(NOTE_ID_ARG) ?: Note.NEW_ID
        if (noteId == Note.NEW_ID) {
            _state.update { it.copy(category = note.category) }
        } else {
            load(noteId)
        }
    }

    fun onIntent(intent: NoteEditorIntent) {
        when (intent) {
            is NoteEditorIntent.TitleChanged -> edit { it.copy(title = intent.title) }
            is NoteEditorIntent.ContentChanged -> edit { it.copy(content = intent.content) }
            NoteEditorIntent.PinClicked -> saveNow { it.copy(isPinned = !it.isPinned) }
            NoteEditorIntent.AddImageClicked -> send(NoteEditorEffect.OpenImagePicker)
            is NoteEditorIntent.ImagesPicked -> addImages(intent.uris)
            is NoteEditorIntent.RemoveImageClicked -> removeImage(intent.image)
            is NoteEditorIntent.CategoryClicked -> saveNow {
                it.copy(category = intent.category.takeIf { category -> category != it.category })
            }
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
                            category = note.category,
                            color = note.color,
                            images = note.images,
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

    /** Cambios de un toque (fijar, categoría): se guardan al momento, sin esperar al autoguardado. */
    private fun saveNow(transform: (NoteEditorUiState) -> NoteEditorUiState) {
        _state.update(transform)
        hasPendingChanges = true
        autosaveJob?.cancel()
        viewModelScope.launch { save() }
    }

    private suspend fun save() = saveMutex.withLock {
        if (!hasPendingChanges) return@withLock
        val current = _state.value
        val draft = note.copy(
            title = current.title,
            content = current.content,
            isPinned = current.isPinned,
            category = current.category,
        )
        _state.update { it.copy(saveStatus = SaveStatus.Saving) }
        when (val result = saveNote(draft)) {
            is AppResult.Success -> {
                note = result.data
                // Si hubo ediciones durante el guardado, el autoguardado pendiente las guardará.
                hasPendingChanges = _state.value.run {
                    title != note.title || content != note.content || isPinned != note.isPinned ||
                        category != note.category
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

    private fun addImages(uris: List<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            saveMutex.withLock {
                val current = _state.value
                // El borrador lleva el texto actual, que se guarda junto con las imágenes.
                val draft = note.copy(
                    title = current.title,
                    content = current.content,
                    isPinned = current.isPinned,
                    category = current.category,
                )
                when (val result = addNoteImages(draft, uris)) {
                    is AppResult.Success -> {
                        note = result.data.note
                        if (result.data.failedCount > 0) {
                            send(NoteEditorEffect.ShowMessage(R.string.note_editor_image_error))
                        }
                        // Si hubo ediciones mientras se copiaban las imágenes, el autoguardado las guardará.
                        hasPendingChanges = _state.value.run {
                            title != note.title || content != note.content || isPinned != note.isPinned ||
                                category != note.category
                        }
                        _state.update {
                            it.copy(
                                images = note.images,
                                date = dateFormatter.format(note.updatedAt),
                                saveStatus = SaveStatus.Saved,
                            )
                        }
                    }
                    is AppResult.Error -> send(NoteEditorEffect.ShowMessage(R.string.note_editor_image_error))
                }
            }
        }
    }

    private fun removeImage(image: NoteImage) {
        viewModelScope.launch {
            // Mismo candado que los guardados: evita pisar `note` con un guardado o una carga de imágenes en curso.
            saveMutex.withLock {
                when (removeNoteImage(image)) {
                    is AppResult.Success -> {
                        note = note.copy(images = note.images - image)
                        _state.update { it.copy(images = note.images) }
                    }
                    is AppResult.Error -> send(NoteEditorEffect.ShowMessage(R.string.note_editor_image_remove_error))
                }
            }
        }
    }

    private fun leave() {
        viewModelScope.launch {
            autosaveJob?.cancel()
            autosaveJob?.join()
            save()
            if (hasPendingChanges) return@launch
            // Una nota creada al agregar una imagen queda vacía si luego se quitan todas: no se conserva.
            if (note.id != Note.NEW_ID && note.isBlank) deleteNote(note.id)
            send(NoteEditorEffect.NavigateBack)
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
        NoteError.Storage, NoteError.EmptyNote, NoteError.ImageUnavailable -> LoadError.Storage
    }

    private fun send(effect: NoteEditorEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    companion object {
        /** Argumento de navegación con el id de la nota; [Note.NEW_ID] para crear una nueva. */
        const val NOTE_ID_ARG = "noteId"
        /** Argumento opcional con el nombre de la `NoteCategory` inicial de una nota nueva. */
        const val CATEGORY_ARG = "category"
        internal const val AUTOSAVE_DELAY_MS = 500L
    }
}
