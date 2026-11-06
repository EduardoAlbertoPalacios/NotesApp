package com.example.notesapp.domain.settings

import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SetAutoSaveEnabledUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val setAutoSaveEnabled = SetAutoSaveEnabledUseCase(repository)

    @Test
    fun `given each value when setting then saves it in the repository`() = runTest {
        val results = listOf(false, true).map { setAutoSaveEnabled(it) }

        assertEquals(listOf(AppResult.Success(Unit), AppResult.Success(Unit)), results)
        assertEquals(listOf(false, true), repository.savedAutoSaveEnabled)
    }

    @Test
    fun `given disabled saved when observing the setting then emits disabled`() = runTest {
        setAutoSaveEnabled(false)

        GetAutoSaveEnabledUseCase(repository)().test {
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `given storage failure when setting then returns the error`() = runTest {
        repository.failure = SettingsError.Storage

        assertEquals(AppResult.Error(SettingsError.Storage), setAutoSaveEnabled(false))
    }
}
