package com.example.notesapp.domain.notes

sealed interface NoteError {
    data object Storage : NoteError
}
