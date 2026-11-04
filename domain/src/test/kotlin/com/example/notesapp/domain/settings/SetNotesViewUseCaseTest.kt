package com.example.notesapp.domain.settings

import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SetNotesViewUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val setNotesView = SetNotesViewUseCase(repository)

    @Test
    fun `given each view when setting then saves it in the repository`() = runTest {
        val results = NotesView.entries.map { setNotesView(it) }

        assertEquals(NotesView.entries.map { AppResult.Success(Unit) }, results)
        assertEquals(NotesView.entries, repository.savedNotesViews)
    }

    @Test
    fun `given saved view when observing the view then emits it`() = runTest {
        setNotesView(NotesView.LIST)

        GetNotesViewUseCase(repository)().test {
            assertEquals(NotesView.LIST, awaitItem())
        }
    }

    @Test
    fun `given storage failure when setting then returns the error`() = runTest {
        repository.failure = SettingsError.Storage

        assertEquals(AppResult.Error(SettingsError.Storage), setNotesView(NotesView.LIST))
    }
}
