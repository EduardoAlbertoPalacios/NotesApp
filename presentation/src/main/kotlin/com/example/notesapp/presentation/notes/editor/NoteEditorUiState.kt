package com.example.notesapp.presentation.notes.editor

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue
import com.example.notesapp.domain.notes.ChecklistItem
import com.example.notesapp.domain.notes.FormatSpan
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.TextFormat
import com.example.notesapp.presentation.notes.common.NoteDateLabel

@Immutable
data class NoteEditorUiState(
    /** Nota que todavía no existe: el título recibe el foco al abrir el editor. */
    val isNewNote: Boolean = true,
    val isLoading: Boolean = false,
    val title: String = "",
    /** Contenido con su selección: el formato se aplica sobre lo seleccionado o en la posición del cursor. */
    val content: TextFieldValue = TextFieldValue(),
    val contentSpans: List<FormatSpan> = emptyList(),
    /** Líneas del contenido que son tareas, con su estado. */
    val contentTasks: List<ChecklistItem> = emptyList(),
    /** Formatos que tendrá lo próximo que se escriba; no se guardan con la nota. */
    val activeFormats: Set<TextFormat> = emptySet(),
    val isFormatMenuVisible: Boolean = false,
    /** El botón de formato solo se habilita con el foco en el contenido. */
    val isContentFocused: Boolean = false,
    val isPinned: Boolean = false,
    val category: NoteCategory? = null,
    val color: NoteColor = NoteColor.AQUA,
    /** Opción elegida en el selector de color, aún sin aplicar; nula con el selector cerrado. */
    val pickerColor: NoteColor? = null,
    val images: List<NoteImage> = emptyList(),
    /** Fecha de la última edición; nula mientras la nota no se ha guardado. */
    val date: NoteDateLabel? = null,
    val saveStatus: SaveStatus = SaveStatus.Idle,
    val loadError: LoadError? = null,
    val isDeleteDialogVisible: Boolean = false,
) {
    /** La nota ya cargó sin error: solo entonces se pinta su color y se muestran sus acciones. */
    val isNoteVisible: Boolean get() = !isLoading && loadError == null
}

enum class SaveStatus { Idle, Saving, Saved }

enum class LoadError { NotFound, Storage }
