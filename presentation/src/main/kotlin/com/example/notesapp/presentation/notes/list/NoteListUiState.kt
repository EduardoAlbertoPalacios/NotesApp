package com.example.notesapp.presentation.notes.list

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.presentation.notes.common.NoteDateLabel

@Immutable
data class NoteListUiState(
    val isLoading: Boolean = true,
    val notes: List<NoteItemUi> = emptyList(),
    val hasError: Boolean = false,
    val selectedFilter: NoteFilter = NoteFilter.ALL,
    val query: String = "",
    val totalCount: Int = 0,
) {
    val isSearching: Boolean get() = query.isNotBlank()
    val isSearchEnabled: Boolean get() = totalCount > 0
    val showNoResults: Boolean get() = isSearching && notes.isEmpty() && totalCount > 0
    val displayQuery: String get() = query.trim()
}

@Immutable
data class NoteItemUi(
    val id: Long,
    val title: String,
    /** Contenido con su formato (negrita, subrayado, tachado) para la vista previa de la tarjeta. */
    val content: AnnotatedString,
    val color: NoteColor,
    val isPinned: Boolean,
    val date: NoteDateLabel,
    val imagePath: String? = null,
)
