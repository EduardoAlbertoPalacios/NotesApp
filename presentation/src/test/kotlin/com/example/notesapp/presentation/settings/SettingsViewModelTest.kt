package com.example.notesapp.presentation.settings

import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.settings.GetThemeModeUseCase
import com.example.notesapp.domain.settings.SetThemeModeUseCase
import com.example.notesapp.domain.settings.SettingsError
import com.example.notesapp.domain.settings.ThemeMode
import com.example.notesapp.presentation.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSettingsRepository()

    private fun createViewModel() = SettingsViewModel(
        GetThemeModeUseCase(repository),
        SetThemeModeUseCase(repository),
    )

    private suspend fun createViewModelWithTheme(mode: ThemeMode?): SettingsViewModel {
        repository.emitStored(mode)
        return createViewModel()
    }

    @Test
    fun `given no emission yet when created then state has default theme and closed sheet`() {
        val viewModel = createViewModel()

        assertEquals(SettingsUiState(themeMode = ThemeMode.DARK, isThemeSheetVisible = false), viewModel.state.value)
    }

    @Test
    fun `given stored theme when created then state shows it`() = runTest {
        val viewModel = createViewModelWithTheme(ThemeMode.LIGHT)

        viewModel.state.test {
            assertEquals(SettingsUiState(themeMode = ThemeMode.LIGHT), awaitItem())
        }
    }

    @Test
    fun `given repository without theme when created then state shows dark`() = runTest {
        val viewModel = createViewModelWithTheme(null)

        viewModel.state.test {
            assertEquals(SettingsUiState(themeMode = ThemeMode.DARK), awaitItem())
        }
    }

    @Test
    fun `given stored theme when it changes then state follows it`() = runTest {
        val viewModel = createViewModelWithTheme(ThemeMode.DARK)

        viewModel.state.test {
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)

            repository.emitStored(ThemeMode.LIGHT)

            assertEquals(ThemeMode.LIGHT, awaitItem().themeMode)
        }
    }

    @Test
    fun `given settings when back clicked then emits navigate back`() = runTest {
        val viewModel = createViewModelWithTheme(ThemeMode.DARK)

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.BackClicked)

            assertEquals(SettingsEffect.NavigateBack, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `given closed sheet when theme row clicked then opens sheet without changing theme`() = runTest {
        val viewModel = createViewModelWithTheme(ThemeMode.LIGHT)

        viewModel.state.test {
            assertFalse(awaitItem().isThemeSheetVisible)

            viewModel.onIntent(SettingsIntent.ThemeRowClicked)

            assertEquals(SettingsUiState(themeMode = ThemeMode.LIGHT, isThemeSheetVisible = true), awaitItem())
        }
    }

    @Test
    fun `given open sheet when dismissed then closes it without changing or saving theme`() = runTest {
        val viewModel = createViewModelWithTheme(ThemeMode.LIGHT)
        viewModel.onIntent(SettingsIntent.ThemeRowClicked)

        viewModel.state.test {
            assertTrue(awaitItem().isThemeSheetVisible)

            viewModel.onIntent(SettingsIntent.ThemeSheetDismissed)

            assertEquals(SettingsUiState(themeMode = ThemeMode.LIGHT, isThemeSheetVisible = false), awaitItem())
        }
        assertTrue(repository.savedModes.isEmpty())
    }

    @Test
    fun `given other theme when selected then saves it and state reflects it without effects`() = runTest {
        val viewModel = createViewModelWithTheme(ThemeMode.DARK)

        viewModel.effects.test {
            viewModel.state.test {
                assertEquals(ThemeMode.DARK, awaitItem().themeMode)

                viewModel.onIntent(SettingsIntent.ThemeSelected(ThemeMode.LIGHT))

                assertEquals(ThemeMode.LIGHT, awaitItem().themeMode)
            }
            expectNoEvents()
        }
        assertEquals(listOf(ThemeMode.LIGHT), repository.savedModes)
    }

    @Test
    fun `given active theme when selected again then does not save nor change anything`() = runTest {
        val viewModel = createViewModelWithTheme(ThemeMode.DARK)

        viewModel.effects.test {
            viewModel.state.test {
                val initial = awaitItem()

                viewModel.onIntent(SettingsIntent.ThemeSelected(ThemeMode.DARK))

                assertEquals(initial, viewModel.state.value)
                expectNoEvents()
            }
            expectNoEvents()
        }
        assertTrue(repository.savedModes.isEmpty())
    }

    @Test
    fun `given save fails when theme selected then emits save error and keeps chosen theme`() = runTest {
        repository.saveResult = AppResult.Error(SettingsError.Storage)
        val viewModel = createViewModelWithTheme(ThemeMode.DARK)

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.ThemeSelected(ThemeMode.LIGHT))

            assertEquals(SettingsEffect.ShowThemeSaveError, awaitItem())
            expectNoEvents()
        }
        assertEquals(ThemeMode.LIGHT, viewModel.state.value.themeMode)
    }

    @Test
    fun `given open sheet when other theme selected then sheet stays open until dismissed`() = runTest {
        val viewModel = createViewModelWithTheme(ThemeMode.DARK)
        viewModel.onIntent(SettingsIntent.ThemeRowClicked)

        viewModel.onIntent(SettingsIntent.ThemeSelected(ThemeMode.LIGHT))

        assertEquals(SettingsUiState(themeMode = ThemeMode.LIGHT, isThemeSheetVisible = true), viewModel.state.value)

        viewModel.onIntent(SettingsIntent.ThemeSheetDismissed)

        assertEquals(SettingsUiState(themeMode = ThemeMode.LIGHT, isThemeSheetVisible = false), viewModel.state.value)
    }

    @Test
    fun `given open sheet when active theme selected then sheet stays open until dismissed`() = runTest {
        val viewModel = createViewModelWithTheme(ThemeMode.DARK)
        viewModel.onIntent(SettingsIntent.ThemeRowClicked)

        viewModel.onIntent(SettingsIntent.ThemeSelected(ThemeMode.DARK))

        assertEquals(SettingsUiState(themeMode = ThemeMode.DARK, isThemeSheetVisible = true), viewModel.state.value)
    }

    @Test
    fun `given open sheet when save fails then sheet stays open until dismissed`() = runTest {
        repository.saveResult = AppResult.Error(SettingsError.Storage)
        val viewModel = createViewModelWithTheme(ThemeMode.DARK)
        viewModel.onIntent(SettingsIntent.ThemeRowClicked)

        viewModel.onIntent(SettingsIntent.ThemeSelected(ThemeMode.LIGHT))

        assertTrue(viewModel.state.value.isThemeSheetVisible)
    }

    @Test
    fun `given open sheet when state is collected again then sheet is still open with same theme`() = runTest {
        val viewModel = createViewModelWithTheme(ThemeMode.LIGHT)
        viewModel.onIntent(SettingsIntent.ThemeRowClicked)
        viewModel.state.test {
            awaitItem()
        }

        viewModel.state.test {
            assertEquals(SettingsUiState(themeMode = ThemeMode.LIGHT, isThemeSheetVisible = true), awaitItem())
        }
    }
}
