package com.example.notesapp.presentation.notes.list

import androidx.compose.runtime.Immutable
import com.example.notesapp.domain.notes.NoteColor

@Immutable
data class NoteListUiState(
    val isLoading: Boolean = true,
    val notes: List<NoteItemUi> = emptyList(),
    val hasError: Boolean = false,
)

@Immutable
data class NoteItemUi(
    val id: Long,
    val title: String,
    val content: String,
    val color: NoteColor,
    val isPinned: Boolean,
    val date: NoteDateLabel,
)

sealed interface NoteDateLabel {
    data class Today(val time: String) : NoteDateLabel
    data object Yesterday : NoteDateLabel
    data class Date(val dayMonth: String) : NoteDateLabel
}
