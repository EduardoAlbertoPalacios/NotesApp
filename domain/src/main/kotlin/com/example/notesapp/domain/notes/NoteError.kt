package com.example.notesapp.domain.notes

sealed interface NoteError {
    data object Storage : NoteError
    data object NotFound : NoteError
    data object EmptyNote : NoteError
    /** No se pudo leer o copiar la imagen elegida. */
    data object ImageUnavailable : NoteError
}
