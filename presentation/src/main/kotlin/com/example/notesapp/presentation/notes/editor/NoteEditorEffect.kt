package com.example.notesapp.presentation.notes.editor

import androidx.annotation.StringRes

sealed interface NoteEditorEffect {
    data object NavigateBack : NoteEditorEffect
    /** La nota [noteId] se movió a la papelera: cerrar el editor y avisar en Mis notas (RF-2, RF-3). */
    data class MovedToTrash(val noteId: Long) : NoteEditorEffect
    data object OpenImagePicker : NoteEditorEffect
    data class ShowMessage(@StringRes val messageRes: Int) : NoteEditorEffect
    /** Abre el menú de compartir del sistema. [imagePaths] son copias propias que existen. */
    data class ShareNote(
        val subject: String?,
        val text: String?,
        val imagePaths: List<String>,
    ) : NoteEditorEffect
}
