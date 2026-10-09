package com.example.notesapp.data.notes

import com.example.notesapp.data.local.NoteEntity
import com.example.notesapp.data.local.NoteImageEntity
import com.example.notesapp.data.local.NoteWithImages
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteImage
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
        category = "WORK",
    )
    private val note = Note(
        id = 7,
        title = "La compra",
        content = "Café",
        color = NoteColor.ROSE,
        isPinned = true,
        category = NoteCategory.WORK,
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

    @Test
    fun `given personal category when mapping both ways then keeps it`() {
        val personal = note.copy(category = NoteCategory.PERSONAL)

        assertEquals("PERSONAL", personal.toEntity().category)
        assertEquals(personal, personal.toEntity().toDomain())
    }

    @Test
    fun `given entity without category when mapping to domain then category is null`() {
        assertEquals(null, entity.copy(category = null).toDomain().category)
    }

    @Test
    fun `given note without category when mapping to entity then category is null`() {
        assertEquals(null, note.copy(category = null).toEntity().category)
    }

    @Test
    fun `given unknown category name when mapping to domain then category is null`() {
        assertEquals(null, entity.copy(category = "HOBBY").toDomain().category)
    }

    @Test
    fun `given image entity when mapping to domain then copies id and path`() {
        assertEquals(NoteImage(id = 3, path = "/a.jpg"), NoteImageEntity(id = 3, noteId = 7, path = "/a.jpg").toDomain())
    }

    @Test
    fun `given note with images when mapping to domain then includes them in order`() {
        val withImages = NoteWithImages(
            note = entity,
            images = listOf(
                NoteImageEntity(id = 3, noteId = 7, path = "/a.jpg"),
                NoteImageEntity(id = 4, noteId = 7, path = "/b.jpg"),
            ),
        )

        assertEquals(
            note.copy(images = listOf(NoteImage(3, "/a.jpg"), NoteImage(4, "/b.jpg"))),
            withImages.toDomain(),
        )
    }

    @Test
    fun `given note without images when mapping to domain then images are empty`() {
        assertEquals(note, NoteWithImages(note = entity, images = emptyList()).toDomain())
    }

    @Test
    fun `given note with images when mapping to entity then ignores images`() {
        assertEquals(entity, note.copy(images = listOf(NoteImage(3, "/a.jpg"))).toEntity())
    }
}
