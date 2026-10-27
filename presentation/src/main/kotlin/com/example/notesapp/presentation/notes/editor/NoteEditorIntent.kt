package com.example.notesapp.presentation.notes.editor

import androidx.compose.ui.text.input.TextFieldValue
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.TextFormat

sealed interface NoteEditorIntent {
    data class TitleChanged(val title: String) : NoteEditorIntent
    /** Cambio de texto o solo de selección. */
    data class ContentChanged(val content: TextFieldValue) : NoteEditorIntent
    data object PinClicked : NoteEditorIntent
    data class ContentFocusChanged(val isFocused: Boolean) : NoteEditorIntent
    data object FormatButtonClicked : NoteEditorIntent
    data object FormatMenuDismissed : NoteEditorIntent
    /** Con selección alterna el formato en lo seleccionado; sin selección, para lo próximo que se escriba. */
    data class FormatSelected(val format: TextFormat) : NoteEditorIntent
    /** Elegir la categoría ya seleccionada la quita. */
    data class CategoryClicked(val category: NoteCategory) : NoteEditorIntent
    /** Convierte en tareas las líneas del cursor o de la selección, o las devuelve a texto si ya lo eran. */
    data object ChecklistButtonClicked : NoteEditorIntent
    /** Marca o desmarca la tarea de la línea [line] del contenido. */
    data class TaskToggled(val line: Int) : NoteEditorIntent
    data object ColorButtonClicked : NoteEditorIntent
    /** Elegir una opción en el selector no cambia el color de la nota hasta aplicarla. */
    data class PickerColorSelected(val color: NoteColor) : NoteEditorIntent
    data object ColorApplied : NoteEditorIntent
    /** Cerrar el selector sin aplicar descarta la opción elegida. */
    data object ColorPickerDismissed : NoteEditorIntent
    data object DeleteClicked : NoteEditorIntent
    data object DeleteConfirmed : NoteEditorIntent
    data object DeleteDismissed : NoteEditorIntent
    data object BackClicked : NoteEditorIntent
    data object AddImageClicked : NoteEditorIntent
    /** Uris elegidas en el selector de fotos; vacía si el usuario lo cerró sin elegir. */
    data class ImagesPicked(val uris: List<String>) : NoteEditorIntent
    data class RemoveImageClicked(val image: NoteImage) : NoteEditorIntent
}
