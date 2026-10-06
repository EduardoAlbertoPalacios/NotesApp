package com.example.notesapp.data.notes

import android.database.sqlite.SQLiteException
import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.data.local.NoteEntity
import com.example.notesapp.domain.notes.NoteError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteRepositoryImplTest {

    private val dao = FakeNoteDao()
    private val repository = NoteRepositoryImpl(dao)

    @Test
    fun `given stored notes when observing then emits them as domain models`() = runTest {
        val entity = NoteEntity(
            id = 1,
            title = "Para leer",
            content = "Hábitos atómicos",
            color = "MINT",
            isPinned = false,
            updatedAt = 10,
        )
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
}
