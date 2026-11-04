package com.example.notesapp.domain.settings

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetNotesViewUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val getNotesView = GetNotesViewUseCase(repository)

    @Test
    fun `given notes view enum when reading its default then is grid`() {
        assertEquals(NotesView.GRID, NotesView.DEFAULT)
    }

    @Test
    fun `given no available view when observing then emits grid`() = runTest {
        repository.notesView.value = null

        getNotesView().test {
            assertEquals(NotesView.GRID, awaitItem())
        }
    }

    @Test
    fun `given each stored view when observing then emits that view`() = runTest {
        NotesView.entries.forEach { view ->
            repository.notesView.value = view

            getNotesView().test {
                assertEquals(view, awaitItem())
            }
        }
    }

    @Test
    fun `given observed view when it changes then emits each change`() = runTest {
        getNotesView().test {
            assertEquals(NotesView.GRID, awaitItem())

            repository.notesView.value = NotesView.LIST
            assertEquals(NotesView.LIST, awaitItem())

            repository.notesView.value = null
            assertEquals(NotesView.GRID, awaitItem())
        }
    }
}
