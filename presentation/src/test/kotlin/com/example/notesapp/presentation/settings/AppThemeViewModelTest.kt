package com.example.notesapp.presentation.settings

import app.cash.turbine.test
import com.example.notesapp.domain.settings.GetThemeModeUseCase
import com.example.notesapp.domain.settings.ThemeMode
import com.example.notesapp.presentation.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AppThemeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSettingsRepository()

    private fun createViewModel() = AppThemeViewModel(GetThemeModeUseCase(repository))

    @Test
    fun `given no emission yet when created then theme mode is null`() = runTest {
        val viewModel = createViewModel()

        viewModel.themeMode.test {
            assertNull(awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `given stored theme when read then exposes it`() = runTest {
        val viewModel = createViewModel()

        viewModel.themeMode.test {
            assertNull(awaitItem())

            repository.emitStored(ThemeMode.LIGHT)

            assertEquals(ThemeMode.LIGHT, awaitItem())
        }
    }

    @Test
    fun `given stored theme when it changes then follows each theme`() = runTest {
        repository.emitStored(ThemeMode.LIGHT)
        val viewModel = createViewModel()

        viewModel.themeMode.test {
            assertEquals(ThemeMode.LIGHT, awaitItem())

            repository.emitStored(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem())

            repository.emitStored(ThemeMode.LIGHT)
            assertEquals(ThemeMode.LIGHT, awaitItem())
        }
    }

    @Test
    fun `given repository without theme when read then exposes dark`() = runTest {
        val viewModel = createViewModel()

        viewModel.themeMode.test {
            assertNull(awaitItem())

            repository.emitStored(null)

            assertEquals(ThemeMode.DARK, awaitItem())
        }
    }

    @Test
    fun `given no collector when theme is read then value is already updated`() = runTest {
        val viewModel = createViewModel()

        repository.emitStored(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
    }
}
