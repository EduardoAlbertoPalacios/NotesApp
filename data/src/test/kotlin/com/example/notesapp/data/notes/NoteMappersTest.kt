package com.example.notesapp.data.notes

import com.example.notesapp.data.local.NoteEntity
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteColor
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteMappersTest {

    private val entity = NoteEntity(
        id = 7,
        title = "La compra",
        content = "Café",
        color = "ROSE",
        isPinned = true,
        updatedAt = 1_000,
    )
    private val note = Note(
        id = 7,
        title = "La compra",
        content = "Café",
        color = NoteColor.ROSE,
        isPinned = true,
        updatedAt = 1_000,
    )

    @Test
    fun `given entity when mapping to domain then copies every field`() {
        assertEquals(note, entity.toDomain())
    }

    @Test
    fun `given note when mapping to entity then copies every field`() {
        assertEquals(entity, note.toEntity())
    }

    @Test
    fun `given unknown color when mapping to domain then falls back to aqua`() {
        assertEquals(NoteColor.AQUA, entity.copy(color = "MAGENTA").toDomain().color)
    }
}
