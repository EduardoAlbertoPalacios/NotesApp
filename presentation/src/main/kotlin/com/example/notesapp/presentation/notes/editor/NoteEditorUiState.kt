package com.example.notesapp.presentation.notes.editor

import androidx.compose.runtime.Immutable
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.presentation.notes.common.NoteDateLabel

@Immutable
data class NoteEditorUiState(
    /** Nota que todavía no existe: el título recibe el foco al abrir el editor. */
    val isNewNote: Boolean = true,
    val isLoading: Boolean = false,
    val title: String = "",
    val content: String = "",
    val isPinned: Boolean = false,
    val category: NoteCategory? = null,
    val color: NoteColor = NoteColor.AQUA,
    val images: List<NoteImage> = emptyList(),
    /** Fecha de la última edición; nula mientras la nota no se ha guardado. */
    val date: NoteDateLabel? = null,
    val saveStatus: SaveStatus = SaveStatus.Idle,
    val loadError: LoadError? = null,
    val isDeleteDialogVisible: Boolean = false,
)

enum class SaveStatus { Idle, Saving, Saved }

enum class LoadError { NotFound, Storage }
