package com.example.notesapp.data.notes

import android.database.sqlite.SQLiteException
import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.data.local.NoteEntity
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteRepositoryImplTest {

    private val dao = FakeNoteDao()
    private val repository = NoteRepositoryImpl(dao)

    private val entity = NoteEntity(
        id = 1,
        title = "Para leer",
        content = "Hábitos atómicos",
        color = "MINT",
        isPinned = false,
        updatedAt = 10,
    )

    @Test
    fun `given stored notes when observing then emits them as domain models`() = runTest {
        dao.insertAll(listOf(entity))

        repository.observeNotes().test {
            assertEquals(AppResult.Success(listOf(entity.toDomain())), awaitItem())
        }
    }

    @Test
    fun `given database failure when observing then emits storage error`() = runTest {
        dao.failure = SQLiteException("disk I/O error")

        repository.observeNotes().test {
            assertEquals(AppResult.Error(NoteError.Storage), awaitItem())
            awaitComplete()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `given unexpected failure when observing then rethrows it`() = runTest {
        dao.failure = IllegalStateException("bug")

        repository.observeNotes().collect {}
    }

    @Test
    fun `given stored note when getting by id then returns it`() = runTest {
        dao.insertAll(listOf(entity))

        assertEquals(AppResult.Success(entity.toDomain()), repository.getNote(1))
    }

    @Test
    fun `given missing note when getting by id then returns not found`() = runTest {
        assertEquals(AppResult.Error(NoteError.NotFound), repository.getNote(42))
    }

    @Test
    fun `given new note when saving then inserts it and returns generated id`() = runTest {
        val result = repository.saveNote(note(id = Note.NEW_ID, title = "Nueva"))

        assertEquals(AppResult.Success(1L), result)
        assertEquals("Nueva", dao.stored.single().title)
    }

    @Test
    fun `given existing note when saving then updates it`() = runTest {
        dao.insertAll(listOf(entity))

        val result = repository.saveNote(entity.toDomain().copy(title = "Editada"))

        assertEquals(AppResult.Success(1L), result)
        assertEquals("Editada", dao.stored.single().title)
    }

    @Test
    fun `given deleted note when saving then returns not found`() = runTest {
        assertEquals(AppResult.Error(NoteError.NotFound), repository.saveNote(note(id = 9, title = "Fantasma")))
    }

    @Test
    fun `given stored note when deleting then removes it`() = runTest {
        dao.insertAll(listOf(entity))

        assertEquals(AppResult.Success(Unit), repository.deleteNote(1))
        assertTrue(dao.stored.isEmpty())
    }

    @Test
    fun `given database failure when saving or deleting then returns storage error`() = runTest {
        dao.failure = SQLiteException("disk full")

        assertEquals(AppResult.Error(NoteError.Storage), repository.saveNote(note(id = Note.NEW_ID, title = "x")))
        assertEquals(AppResult.Error(NoteError.Storage), repository.deleteNote(1))
        assertEquals(AppResult.Error(NoteError.Storage), repository.getNote(1))
    }

    @Test
    fun `given new note with category when saving then stores category name`() = runTest {
        repository.saveNote(note(id = Note.NEW_ID, title = "Sprint", category = NoteCategory.WORK))

        assertEquals("WORK", dao.stored.single().category)
    }

    @Test
    fun `given existing note when changing category then updates stored category`() = runTest {
        dao.insertAll(listOf(entity.copy(category = "WORK")))

        repository.saveNote(entity.toDomain().copy(category = NoteCategory.PERSONAL))

        assertEquals("PERSONAL", dao.stored.single().category)
    }

    @Test
    fun `given existing note when removing category then stores null`() = runTest {
        dao.insertAll(listOf(entity.copy(category = "WORK")))

        repository.saveNote(entity.toDomain().copy(category = null))

        assertEquals(null, dao.stored.single().category)
    }

    @Test
    fun `given stored notes with categories when reading then exposes domain categories`() = runTest {
        dao.insertAll(listOf(entity.copy(category = "PERSONAL"), entity.copy(title = "Sin categoría")))

        assertEquals(AppResult.Success(entity.toDomain().copy(category = NoteCategory.PERSONAL)), repository.getNote(1))
        repository.observeNotes().test {
            val notes = (awaitItem() as AppResult.Success).data
            assertEquals(listOf(NoteCategory.PERSONAL, null), notes.map { it.category })
        }
    }

    private fun note(id: Long, title: String, category: NoteCategory? = null) = Note(
        id = id,
        title = title,
        content = "",
        color = NoteColor.AQUA,
        isPinned = false,
        category = category,
        updatedAt = 5,
    )
}
