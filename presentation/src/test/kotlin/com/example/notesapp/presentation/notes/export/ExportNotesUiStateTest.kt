package com.example.notesapp.presentation.notes.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportNotesUiStateTest {

    private fun item(id: Long) = ExportNoteItem(id = id, title = "Nota $id")

    private fun loaded(
        notes: List<ExportNoteItem> = listOf(item(1), item(2)),
        selectedIds: Set<Long> = emptySet(),
        isExporting: Boolean = false,
    ) = ExportNotesUiState(isLoading = false, notes = notes, selectedIds = selectedIds, isExporting = isExporting)

    @Test
    fun `given loading when checked then is not empty`() {
        assertFalse(ExportNotesUiState(isLoading = true).isEmpty)
    }

    @Test
    fun `given loaded without notes when checked then is empty`() {
        assertTrue(loaded(notes = emptyList()).isEmpty)
    }

    @Test
    fun `given loaded with notes when checked then is not empty`() {
        assertFalse(loaded().isEmpty)
    }

    @Test
    fun `given initial state when checked then load has not failed`() {
        assertFalse(ExportNotesUiState().loadFailed)
    }

    @Test
    fun `given load failed without notes when checked then is not empty`() {
        assertFalse(ExportNotesUiState(isLoading = false, loadFailed = true).isEmpty)
    }

    @Test
    fun `given load failed when checked then cannot export`() {
        assertFalse(ExportNotesUiState(isLoading = false, loadFailed = true).canExport)
    }

    @Test
    fun `given selected ids of notes no longer listed when counting then ignores them`() {
        val state = loaded(selectedIds = setOf(1, 99))

        assertEquals(1, state.selectedCount)
    }

    @Test
    fun `given nothing selected when counting then count is zero`() {
        assertEquals(0, loaded().selectedCount)
    }

    @Test
    fun `given every note selected when checked then all are selected`() {
        assertTrue(loaded(selectedIds = setOf(1, 2)).allSelected)
    }

    @Test
    fun `given every note selected plus a stale id when checked then all are selected`() {
        assertTrue(loaded(selectedIds = setOf(1, 2, 99)).allSelected)
    }

    @Test
    fun `given some notes selected when checked then not all are selected`() {
        assertFalse(loaded(selectedIds = setOf(1)).allSelected)
    }

    @Test
    fun `given no notes when checked then not all are selected`() {
        assertFalse(loaded(notes = emptyList(), selectedIds = setOf(1)).allSelected)
    }

    @Test
    fun `given a selected note when not exporting then can export`() {
        assertTrue(loaded(selectedIds = setOf(1)).canExport)
    }

    @Test
    fun `given nothing selected when checked then cannot export`() {
        assertFalse(loaded().canExport)
    }

    @Test
    fun `given only stale ids selected when checked then cannot export`() {
        assertFalse(loaded(selectedIds = setOf(99)).canExport)
    }

    @Test
    fun `given a selected note while exporting when checked then cannot export`() {
        assertFalse(loaded(selectedIds = setOf(1), isExporting = true).canExport)
    }
}
