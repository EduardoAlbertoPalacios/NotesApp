package com.example.notesapp.presentation.notes.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteEditorUiStateTest {

    @Test
    fun `given note loading when reading visibility then note is not visible`() {
        val state = NoteEditorUiState(isLoading = true)

        assertFalse(state.isNoteVisible)
    }

    @Test
    fun `given not found error when reading visibility then note is not visible`() {
        val state = NoteEditorUiState(loadError = LoadError.NotFound)

        assertFalse(state.isNoteVisible)
    }

    @Test
    fun `given storage error when reading visibility then note is not visible`() {
        val state = NoteEditorUiState(loadError = LoadError.Storage)

        assertFalse(state.isNoteVisible)
    }

    @Test
    fun `given loading with error when reading visibility then note is not visible`() {
        val state = NoteEditorUiState(isLoading = true, loadError = LoadError.Storage)

        assertFalse(state.isNoteVisible)
    }

    @Test
    fun `given note loaded without error when reading visibility then note is visible`() {
        val state = NoteEditorUiState(isLoading = false, loadError = null)

        assertTrue(state.isNoteVisible)
    }
}
