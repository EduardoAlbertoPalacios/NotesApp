package com.example.notesapp.domain.settings

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetThemeModeUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val getThemeMode = GetThemeModeUseCase(repository)

    @Test
    fun `given no available theme when observing then emits dark`() = runTest {
        repository.themeMode.value = null

        getThemeMode().test {
            assertEquals(ThemeMode.DARK, awaitItem())
        }
    }

    @Test
    fun `given stored light theme when observing then emits light`() = runTest {
        repository.themeMode.value = ThemeMode.LIGHT

        getThemeMode().test {
            assertEquals(ThemeMode.LIGHT, awaitItem())
        }
    }

    @Test
    fun `given stored dark theme when observing then emits dark`() = runTest {
        repository.themeMode.value = ThemeMode.DARK

        getThemeMode().test {
            assertEquals(ThemeMode.DARK, awaitItem())
        }
    }

    @Test
    fun `given observed theme when it changes then emits each change`() = runTest {
        getThemeMode().test {
            assertEquals(ThemeMode.DARK, awaitItem())

            repository.themeMode.value = ThemeMode.LIGHT
            assertEquals(ThemeMode.LIGHT, awaitItem())

            repository.themeMode.value = null
            assertEquals(ThemeMode.DARK, awaitItem())
        }
    }
}
