package com.example.notesapp.presentation.notes.editor

import androidx.compose.ui.text.input.TextFieldValue
import com.example.notesapp.domain.notes.NoteImage
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

    @Test
    fun `given empty note when reading share availability then cannot share`() {
        val state = NoteEditorUiState()

        assertFalse(state.canShare)
    }

    @Test
    fun `given title and content with only spaces when reading share availability then cannot share`() {
        val state = NoteEditorUiState(title = TextFieldValue("   "), content = TextFieldValue(" \n\t "))

        assertFalse(state.canShare)
    }

    @Test
    fun `given only title when reading share availability then can share`() {
        val state = NoteEditorUiState(title = TextFieldValue("Ideas"))

        assertTrue(state.canShare)
    }

    @Test
    fun `given only content when reading share availability then can share`() {
        val state = NoteEditorUiState(content = TextFieldValue("Libreta"))

        assertTrue(state.canShare)
    }

    @Test
    fun `given only images when reading share availability then can share`() {
        val state = NoteEditorUiState(images = listOf(NoteImage(id = 1, path = "/files/note_images/a.jpg")))

        assertTrue(state.canShare)
    }
}
