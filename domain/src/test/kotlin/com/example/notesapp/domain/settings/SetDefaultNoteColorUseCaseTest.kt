package com.example.notesapp.domain.settings

import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.NoteColor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SetDefaultNoteColorUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val setDefaultNoteColor = SetDefaultNoteColorUseCase(repository)

    @Test
    fun `given each color when setting then saves it in the repository`() = runTest {
        val results = NoteColor.entries.map { setDefaultNoteColor(it) }

        assertEquals(NoteColor.entries.map { AppResult.Success(Unit) }, results)
        assertEquals(NoteColor.entries, repository.savedDefaultNoteColors)
    }

    @Test
    fun `given saved color when observing the default then emits it`() = runTest {
        setDefaultNoteColor(NoteColor.NONE)

        GetDefaultNoteColorUseCase(repository)().test {
            assertEquals(NoteColor.NONE, awaitItem())
        }
    }

    @Test
    fun `given storage failure when setting then returns the error`() = runTest {
        repository.failure = SettingsError.Storage

        assertEquals(AppResult.Error(SettingsError.Storage), setDefaultNoteColor(NoteColor.MINT))
    }
}
