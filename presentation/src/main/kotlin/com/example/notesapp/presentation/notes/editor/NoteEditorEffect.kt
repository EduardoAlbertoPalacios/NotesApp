package com.example.notesapp.presentation.notes.editor

import androidx.annotation.StringRes

sealed interface NoteEditorEffect {
    data object NavigateBack : NoteEditorEffect
    data class ShowMessage(@StringRes val messageRes: Int) : NoteEditorEffect
}
