package com.example.notesapp.presentation.notes.editor

import androidx.annotation.StringRes

sealed interface NoteEditorEffect {
    data object NavigateBack : NoteEditorEffect
    data object OpenImagePicker : NoteEditorEffect
    data class ShowMessage(@StringRes val messageRes: Int) : NoteEditorEffect
    /** Abre el menú de compartir del sistema. [imagePaths] son copias propias que existen. */
    data class ShareNote(
        val subject: String?,
        val text: String?,
        val imagePaths: List<String>,
    ) : NoteEditorEffect
}
