package com.example.notesapp.domain.notes.trash

import app.cash.turbine.test
import com.example.notesapp.domain.settings.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetTrashRetentionUseCaseTest {

    private val repository = FakeSettingsRepository()

    private fun createGetTrashRetention() = GetTrashRetentionUseCase(repository)

    @Test
    fun `given no stored retention when observing then emits default`() = runTest {
        repository.trashRetention.value = null

        createGetTrashRetention()().test {
            assertEquals(TrashRetention.DEFAULT, awaitItem())
        }
    }

    @Test
    fun `given stored never when observing then emits never`() = runTest {
        repository.trashRetention.value = TrashRetention.NEVER

        createGetTrashRetention()().test {
            assertEquals(TrashRetention.NEVER, awaitItem())
        }
    }

    @Test
    fun `given observed retention when it changes then emits each change`() = runTest {
        createGetTrashRetention()().test {
            assertEquals(TrashRetention.DAYS_30, awaitItem())

            repository.trashRetention.value = TrashRetention.DAYS_7
            assertEquals(TrashRetention.DAYS_7, awaitItem())

            repository.trashRetention.value = null
            assertEquals(TrashRetention.DAYS_30, awaitItem())
        }
    }
}
