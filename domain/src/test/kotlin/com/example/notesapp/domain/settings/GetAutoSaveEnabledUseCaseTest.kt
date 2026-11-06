package com.example.notesapp.domain.settings

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAutoSaveEnabledUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val getAutoSaveEnabled = GetAutoSaveEnabledUseCase(repository)

    @Test
    fun `given use case when reading its default then is enabled`() {
        assertTrue(GetAutoSaveEnabledUseCase.DEFAULT)
    }

    @Test
    fun `given no available value when observing then emits enabled`() = runTest {
        repository.autoSaveEnabled.value = null

        getAutoSaveEnabled().test {
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `given each stored value when observing then emits that value`() = runTest {
        listOf(true, false).forEach { enabled ->
            repository.autoSaveEnabled.value = enabled

            getAutoSaveEnabled().test {
                assertEquals(enabled, awaitItem())
            }
        }
    }

    @Test
    fun `given observed value when it changes then emits each change`() = runTest {
        getAutoSaveEnabled().test {
            assertEquals(true, awaitItem())

            repository.autoSaveEnabled.value = false
            assertEquals(false, awaitItem())

            repository.autoSaveEnabled.value = null
            assertEquals(true, awaitItem())
        }
    }
}
