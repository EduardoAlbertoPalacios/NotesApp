package com.example.notesapp.presentation.notes.trash

import com.example.notesapp.domain.notes.NoteColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrashUiStateTest {

    private fun item(id: Long) =
        TrashItemUi(id = id, title = "Nota $id", excerpt = "Extracto $id", color = NoteColor.MINT, daysLeft = 29)

    private fun loaded(
        notes: List<TrashItemUi> = listOf(item(1), item(2)),
        actionsNoteId: Long? = null,
    ) = TrashUiState(isLoading = false, notes = notes, actionsNoteId = actionsNoteId)

    @Test
    fun `given no actions note id when checked then actions note is null`() {
        assertNull(loaded().actionsNote)
    }

    @Test
    fun `given actions note id in the list when checked then actions note is that note`() {
        assertEquals(item(2), loaded(actionsNoteId = 2).actionsNote)
    }

    @Test
    fun `given actions note id not in the list when checked then actions note is null`() {
        assertNull(loaded(actionsNoteId = 99).actionsNote)
    }

    @Test
    fun `given actions note id and empty list when checked then actions note is null`() {
        assertNull(loaded(notes = emptyList(), actionsNoteId = 1).actionsNote)
    }
}
