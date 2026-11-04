package com.example.notesapp.domain.settings

import app.cash.turbine.test
import com.example.notesapp.domain.notes.NoteColor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetDefaultNoteColorUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val getDefaultNoteColor = GetDefaultNoteColorUseCase(repository)

    @Test
    fun `given no available color when observing then emits aqua`() = runTest {
        repository.defaultNoteColor.value = null

        getDefaultNoteColor().test {
            assertEquals(NoteColor.AQUA, awaitItem())
        }
    }

    @Test
    fun `given each stored color when observing then emits that color`() = runTest {
        NoteColor.entries.forEach { color ->
            repository.defaultNoteColor.value = color

            getDefaultNoteColor().test {
                assertEquals(color, awaitItem())
            }
        }
    }

    @Test
    fun `given observed color when it changes then emits each change`() = runTest {
        getDefaultNoteColor().test {
            assertEquals(NoteColor.AQUA, awaitItem())

            repository.defaultNoteColor.value = NoteColor.MINT
            assertEquals(NoteColor.MINT, awaitItem())

            repository.defaultNoteColor.value = NoteColor.NONE
            assertEquals(NoteColor.NONE, awaitItem())

            repository.defaultNoteColor.value = null
            assertEquals(NoteColor.AQUA, awaitItem())
        }
    }
}
