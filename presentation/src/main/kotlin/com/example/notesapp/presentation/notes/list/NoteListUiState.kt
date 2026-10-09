package com.example.notesapp.presentation.notes.list

import androidx.compose.runtime.Immutable
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.presentation.notes.common.NoteDateLabel

@Immutable
data class NoteListUiState(
    val isLoading: Boolean = true,
    val notes: List<NoteItemUi> = emptyList(),
    val hasError: Boolean = false,
    val selectedFilter: NoteFilter = NoteFilter.ALL,
)

@Immutable
data class NoteItemUi(
    val id: Long,
    val title: String,
    val content: String,
    val color: NoteColor,
    val isPinned: Boolean,
    val date: NoteDateLabel,
    /** Ruta de la primera imagen de la nota, para la miniatura; nula si no tiene. */
    val imagePath: String? = null,
)
