package com.example.notesapp.domain.notes

sealed interface NoteError {
    data object Storage : NoteError
    data object NotFound : NoteError
    data object EmptyNote : NoteError
}
