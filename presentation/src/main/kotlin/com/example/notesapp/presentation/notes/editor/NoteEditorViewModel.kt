package com.example.notesapp.presentation.notes.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.AddNoteImagesUseCase
import com.example.notesapp.domain.notes.DeleteNoteUseCase
import com.example.notesapp.domain.notes.EditField
import com.example.notesapp.domain.notes.EditKind
import com.example.notesapp.domain.notes.GetNoteUseCase
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteChecklist
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteFormatting
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.NoteUndo
import com.example.notesapp.domain.notes.RemoveNoteImageUseCase
import com.example.notesapp.domain.notes.RestoreNoteUseCase
import com.example.notesapp.domain.notes.SaveNoteUseCase
import com.example.notesapp.domain.notes.TextFormat
import com.example.notesapp.domain.notes.UndoEdit
import com.example.notesapp.domain.notes.UndoHistory
import com.example.notesapp.domain.notes.UndoSnapshot
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
    private val restoreNote: RestoreNoteUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val addNoteImages: AddNoteImagesUseCase,
    private val removeNoteImage: RemoveNoteImageUseCase,
    private val dateFormatter: NoteDateFormatter,
) : ViewModel() {

    private val _state = MutableStateFlow(NoteEditorUiState())
    val state: StateFlow<NoteEditorUiState> = _state.asStateFlow()

    private val _effects = Channel<NoteEditorEffect>(Channel.BUFFERED)
    val effects: Flow<NoteEditorEffect> = _effects.receiveAsFlow()

    /** Última versión guardada (o la nota nueva aún sin guardar): conserva el id. */
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
    /** La nota tal como se abrió; nula en una nota nueva. Deshacer hasta ella recupera su fecha. */
    private var openedNote: Note? = null
    /** Si la última edición fue deshacer: solo entonces el guardado puede recuperar la fecha original. */
    private var lastEditWasUndo = false
    private var hasPendingChanges = false
    private var autosaveJob: Job? = null
    // Evita dos guardados simultáneos, que podrían insertar dos veces una nota nueva.
    private val saveMutex = Mutex()
    /** Pasos de edición del título y del contenido; vive mientras el editor está abierto. */
    private var history = UndoHistory()
    /** Cierra el paso abierto tras una pausa al escribir. */
    private var undoPauseJob: Job? = null
    /** Id de la última petición de foco; crece en cada deshacer. */
    private var focusRequestId = 0L

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
            is NoteEditorIntent.TitleChanged -> changeTitle(intent.title)
            is NoteEditorIntent.ContentChanged -> changeContent(intent.content)
            NoteEditorIntent.PinClicked -> saveNow { it.copy(isPinned = !it.isPinned) }
            is NoteEditorIntent.ContentFocusChanged -> _state.update { it.copy(isContentFocused = intent.isFocused) }
            NoteEditorIntent.FormatButtonClicked -> _state.update { it.copy(isFormatMenuVisible = true) }
            NoteEditorIntent.FormatMenuDismissed -> _state.update { it.copy(isFormatMenuVisible = false) }
            is NoteEditorIntent.FormatSelected -> selectFormat(intent.format)
            NoteEditorIntent.AddImageClicked -> send(NoteEditorEffect.OpenImagePicker)
            is NoteEditorIntent.ImagesPicked -> addImages(intent.uris)
            is NoteEditorIntent.RemoveImageClicked -> removeImage(intent.image)
            is NoteEditorIntent.CategoryClicked -> saveNow {
                it.copy(category = intent.category.takeIf { category -> category != it.category })
            }
            NoteEditorIntent.ChecklistButtonClicked -> toggleChecklist()
            is NoteEditorIntent.TaskToggled -> toggleTask(intent.line)
            NoteEditorIntent.ColorButtonClicked -> _state.update { it.copy(pickerColor = it.color) }
            is NoteEditorIntent.PickerColorSelected -> _state.update { it.copy(pickerColor = intent.color) }
            NoteEditorIntent.ColorApplied -> applyColor()
            NoteEditorIntent.ColorPickerDismissed -> _state.update { it.copy(pickerColor = null) }
            NoteEditorIntent.DeleteClicked -> _state.update { it.copy(isDeleteDialogVisible = true) }
            NoteEditorIntent.DeleteDismissed -> _state.update { it.copy(isDeleteDialogVisible = false) }
            NoteEditorIntent.DeleteConfirmed -> delete()
            NoteEditorIntent.BackClicked -> leave()
            NoteEditorIntent.UndoClicked -> undo()
            is NoteEditorIntent.FocusRequestHandled -> _state.update {
                if (it.focusRequest?.id == intent.id) it.copy(focusRequest = null) else it
            }
        }
    }

    private fun load(id: Long) {
        _state.update { it.copy(isNewNote = false, isLoading = true) }
        viewModelScope.launch {
            when (val result = getNote(id)) {
                is AppResult.Success -> {
                    note = result.data
                    openedNote = note
                    _state.update {
                        it.copy(
                            isLoading = false,
                            title = TextFieldValue(note.title),
                            content = TextFieldValue(note.content),
                            contentSpans = note.contentSpans,
                            contentTasks = note.contentTasks,
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
        lastEditWasUndo = false
        hasPendingChanges = true
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            // Una vez empezado, el guardado termina aunque llegue otra edición.
            withContext(NonCancellable) { save() }
        }
    }

    private fun changeTitle(title: TextFieldValue) {
        val current = _state.value
        if (title.text == current.title.text) {
            if (title.selection != current.title.selection) closeStep()
            _state.update { it.copy(title = title) }
        } else {
            edit { it.copy(title = title) }
            recordStep(current.snapshot(EditField.TITLE), NoteUndo.classify(current.title.text, title.text))
        }
    }

    private fun changeContent(content: TextFieldValue) {
        val current = _state.value
        if (content.text == current.content.text) {
            // Mover el cursor o cambiar la selección no es una edición: no se guarda.
            val activeFormats = if (content.selection == current.content.selection) {
                current.activeFormats
            } else {
                closeStep()
                NoteFormatting.activeFormats(current.contentSpans, content.selection.min, content.selection.max)
            }
            _state.update { it.copy(content = content, activeFormats = activeFormats) }
        } else {
            edit {
                // Primero las tareas: Intro en una tarea vacía o borrar al inicio de una tarea anulan la tecla.
                val checklist = NoteChecklist.onTextChanged(
                    old = it.content.text,
                    new = content.text,
                    cursor = content.selection.end,
                    tasks = it.contentTasks,
                )
                val newContent = checklist.cursor?.let { cursor -> TextFieldValue(checklist.text, TextRange(cursor)) }
                    ?: content
                // Después el formato, con el texto final: lo escrito o pegado toma los formatos activos
                // y el resto de los tramos se desplaza; una tecla anulada no los mueve.
                it.copy(
                    content = newContent,
                    contentTasks = checklist.tasks,
                    contentSpans = NoteFormatting.onTextChanged(
                        spans = it.contentSpans,
                        old = it.content.text,
                        new = newContent.text,
                        activeFormats = it.activeFormats,
                    ),
                )
            }
            // Se clasifica el cambio final, con las reglas de tareas ya aplicadas.
            val edited = _state.value
            if (edited.content.text != current.content.text || edited.contentTasks != current.contentTasks) {
                recordStep(current.snapshot(EditField.CONTENT), NoteUndo.classify(current.content.text, edited.content.text))
            }
        }
    }

    /** Convierte en tareas las líneas del cursor o de la selección, o las devuelve a texto si ya lo eran todas. */
    private fun toggleChecklist() {
        val current = _state.value
        edit {
            val selection = it.content.selection
            it.copy(contentTasks = NoteChecklist.toggleLines(it.content.text, it.contentTasks, selection.min, selection.max))
        }
        if (_state.value.contentTasks != current.contentTasks) recordStandalone(current)
    }

    /** Marcar o desmarcar es un cambio de un toque: se guarda al momento. */
    private fun toggleTask(line: Int) {
        val current = _state.value
        val task = current.contentTasks.firstOrNull { it.line == line } ?: return
        saveNow { it.copy(contentTasks = NoteChecklist.setDone(it.contentTasks, line, !task.isDone)) }
        recordStandalone(current)
    }

    private fun selectFormat(format: TextFormat) {
        val selection = _state.value.content.selection
        if (selection.collapsed) {
            // Sin selección solo cambia lo que se escribirá después: no es una edición.
            _state.update {
                val activeFormats = if (format in it.activeFormats) it.activeFormats - format else it.activeFormats + format
                it.copy(isFormatMenuVisible = false, activeFormats = activeFormats)
            }
        } else {
            recordStandalone(_state.value)
            edit {
                val spans = NoteFormatting.toggle(it.contentSpans, selection.min, selection.max, format)
                it.copy(
                    isFormatMenuVisible = false,
                    contentSpans = spans,
                    activeFormats = NoteFormatting.activeFormats(spans, selection.min, selection.max),
                )
            }
        }
    }

    /**
     * Registra un cambio en el historial de deshacer con la foto de antes ([before]). Si el paso queda abierto,
     * una pausa de [UNDO_PAUSE_MS] sin escribir lo cierra.
     */
    private fun recordStep(before: UndoSnapshot, edit: UndoEdit) {
        history = NoteUndo.record(history, before, edit)
        undoPauseJob?.cancel()
        if (history.open != null) {
            undoPauseJob = viewModelScope.launch {
                delay(UNDO_PAUSE_MS)
                history = NoteUndo.close(history)
            }
        }
        _state.update { it.copy(canUndo = history.canUndo) }
    }

    /** Paso propio del contenido (formato, tareas): su foto lleva la selección del contenido. */
    private fun recordStandalone(before: NoteEditorUiState) {
        recordStep(before.snapshot(EditField.CONTENT), UndoEdit(EditKind.STANDALONE))
    }

    /**
     * Vuelve a poner la foto del último paso: título, contenido, formato y tareas, con el cursor en el campo
     * y la posición de antes del paso. Color, categoría, fijación e imágenes no cambian. Se guarda como
     * cualquier edición.
     */
    private fun undo() {
        undoPauseJob?.cancel()
        val (remaining, snapshot) = NoteUndo.undo(history) ?: return
        history = remaining
        val selection = TextRange(snapshot.selectionStart, snapshot.selectionEnd)
        edit {
            val title = if (snapshot.field == EditField.TITLE) {
                TextFieldValue(snapshot.title, selection)
            } else {
                it.title.restoredTo(snapshot.title)
            }
            val content = if (snapshot.field == EditField.CONTENT) {
                TextFieldValue(snapshot.content, selection)
            } else {
                it.content.restoredTo(snapshot.content)
            }
            it.copy(
                title = title,
                content = content,
                contentSpans = snapshot.contentSpans,
                contentTasks = snapshot.contentTasks,
                // Como al mover el cursor: los formatos del carácter anterior al cursor restaurado.
                activeFormats = NoteFormatting.activeFormats(snapshot.contentSpans, content.selection.min, content.selection.max),
                canUndo = history.canUndo,
                focusRequest = FocusRequest(snapshot.field, id = ++focusRequestId),
            )
        }
        lastEditWasUndo = true
    }

    /** El campo que el paso no cambió se queda como está; si su texto difiere, el cursor va al final. */
    private fun TextFieldValue.restoredTo(text: String): TextFieldValue =
        if (this.text == text) this else TextFieldValue(text, TextRange(text.length))

    /** Mover el cursor cierra el paso abierto: lo siguiente que se escriba es otro paso. */
    private fun closeStep() {
        undoPauseJob?.cancel()
        history = NoteUndo.close(history)
    }

    /** Foto del estado para el historial, con la selección de [field], el campo que cambia el paso. */
    private fun NoteEditorUiState.snapshot(field: EditField): UndoSnapshot {
        val selection = if (field == EditField.TITLE) title.selection else content.selection
        return UndoSnapshot(
            title = title.text,
            content = content.text,
            contentSpans = contentSpans,
            contentTasks = contentTasks,
            field = field,
            selectionStart = selection.start,
            selectionEnd = selection.end,
        )
    }

    /**
     * Aplica la opción del selector y lo cierra. Solo guarda si el color cambió, así que aplicar el mismo
     * no cambia la fecha de edición. Una nota nueva vacía no se guarda: el color viaja en el siguiente guardado.
     */
    private fun applyColor() {
        val current = _state.value
        val picked = current.pickerColor ?: return
        if (picked == current.color) {
            _state.update { it.copy(pickerColor = null) }
        } else {
            saveNow { it.copy(color = picked, pickerColor = null) }
        }
    }

    /** Cambios de un toque (fijar, categoría, color, marcar tarea): se guardan al momento, sin esperar al autoguardado. */
    private fun saveNow(transform: (NoteEditorUiState) -> NoteEditorUiState) {
        _state.update(transform)
        lastEditWasUndo = false
        hasPendingChanges = true
        autosaveJob?.cancel()
        viewModelScope.launch { save() }
    }

    private suspend fun save() = saveMutex.withLock {
        if (!hasPendingChanges) return@withLock
        val current = _state.value
        val draft = note.copy(
            title = current.title.text,
            content = current.content.text,
            contentSpans = current.contentSpans,
            contentTasks = current.contentTasks,
            isPinned = current.isPinned,
            category = current.category,
            color = current.color,
        )
        _state.update { it.copy(saveStatus = SaveStatus.Saving) }
        // Deshacer hasta la nota tal como se abrió (en todo, también color e imágenes) recupera su fecha.
        val opened = openedNote?.takeIf { lastEditWasUndo && draft.copy(updatedAt = it.updatedAt) == it }
        when (val result = if (opened != null) restoreNote(opened) else saveNote(draft)) {
            is AppResult.Success -> {
                note = result.data
                // Si hubo ediciones durante el guardado, el autoguardado pendiente las guardará.
                hasPendingChanges = hasUnsavedChanges()
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

    /** Si el estado tiene cambios que la última versión guardada ([note]) todavía no incluye. */
    private fun hasUnsavedChanges(): Boolean = _state.value.run {
        title.text != note.title || content.text != note.content || isPinned != note.isPinned ||
            category != note.category || contentSpans != note.contentSpans || color != note.color ||
            contentTasks != note.contentTasks
    }

    private fun addImages(uris: List<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            saveMutex.withLock {
                val current = _state.value
                // El borrador lleva el texto actual, que se guarda junto con las imágenes.
                val draft = note.copy(
                    title = current.title.text,
                    content = current.content.text,
                    contentSpans = current.contentSpans,
                    contentTasks = current.contentTasks,
                    isPinned = current.isPinned,
                    category = current.category,
                    color = current.color,
                )
                when (val result = addNoteImages(draft, uris)) {
                    is AppResult.Success -> {
                        note = result.data.note
                        if (result.data.failedCount > 0) {
                            send(NoteEditorEffect.ShowMessage(R.string.note_editor_image_error))
                        }
                        // Si hubo ediciones mientras se copiaban las imágenes, el autoguardado las guardará.
                        hasPendingChanges = hasUnsavedChanges()
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
        /** Pausa sin escribir que cierra el paso de deshacer abierto. */
        internal const val UNDO_PAUSE_MS = 1_000L
    }
}
