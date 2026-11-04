package com.example.notesapp.presentation.settings

import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.settings.GetDefaultNoteColorUseCase
import com.example.notesapp.domain.settings.GetThemeModeUseCase
import com.example.notesapp.domain.settings.SetDefaultNoteColorUseCase
import com.example.notesapp.domain.settings.SetThemeModeUseCase
import com.example.notesapp.domain.settings.SettingsError
import com.example.notesapp.domain.settings.ThemeMode
import com.example.notesapp.presentation.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        GetDefaultNoteColorUseCase(repository),
        SetDefaultNoteColorUseCase(repository),
    )

    private suspend fun createViewModelWithTheme(mode: ThemeMode?): SettingsViewModel {
        repository.emitStored(mode)
        return createViewModel()
    }

    private suspend fun createViewModelWithDefaultColor(color: NoteColor?): SettingsViewModel {
        repository.emitStoredDefaultNoteColor(color)
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

    @Test
    fun `given stored default color when created then state shows it with closed picker`() = runTest {
        val viewModel = createViewModelWithDefaultColor(NoteColor.MINT)

        assertEquals(NoteColor.MINT, viewModel.state.value.defaultNoteColor)
        assertNull(viewModel.state.value.defaultColorPicker)
    }

    @Test
    fun `given stored no color when created then state shows no color`() = runTest {
        val viewModel = createViewModelWithDefaultColor(NoteColor.NONE)

        assertEquals(NoteColor.NONE, viewModel.state.value.defaultNoteColor)
    }

    @Test
    fun `given repository without default color when created then state shows aqua`() = runTest {
        val viewModel = createViewModelWithDefaultColor(null)

        assertEquals(NoteColor.AQUA, viewModel.state.value.defaultNoteColor)
    }

    @Test
    fun `given stored default color when it changes then state follows it`() = runTest {
        val viewModel = createViewModelWithDefaultColor(NoteColor.MINT)

        viewModel.state.test {
            assertEquals(NoteColor.MINT, awaitItem().defaultNoteColor)

            repository.emitStoredDefaultNoteColor(NoteColor.SAND)

            assertEquals(NoteColor.SAND, awaitItem().defaultNoteColor)
        }
    }

    @Test
    fun `given default color when row clicked then opens picker with it`() = runTest {
        val viewModel = createViewModelWithDefaultColor(NoteColor.LAVENDER)

        viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)

        assertEquals(NoteColor.LAVENDER, viewModel.state.value.defaultColorPicker)
        assertEquals(NoteColor.LAVENDER, viewModel.state.value.defaultNoteColor)
    }

    @Test
    fun `given open picker when other options selected then marks the last without changing default or saving`() = runTest {
        val viewModel = createViewModelWithDefaultColor(NoteColor.LAVENDER)
        viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)

        viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.ROSE))
        viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.PEACH))

        assertEquals(NoteColor.PEACH, viewModel.state.value.defaultColorPicker)
        assertEquals(NoteColor.LAVENDER, viewModel.state.value.defaultNoteColor)
        assertTrue(repository.savedDefaultNoteColors.isEmpty())
    }

    @Test
    fun `given changed option when picker dismissed then closes keeping default and without saving`() = runTest {
        val viewModel = createViewModelWithDefaultColor(NoteColor.LAVENDER)
        viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)
        viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.ROSE))

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.DefaultColorPickerDismissed)

            expectNoEvents()
        }

        assertNull(viewModel.state.value.defaultColorPicker)
        assertEquals(NoteColor.LAVENDER, viewModel.state.value.defaultNoteColor)
        assertTrue(repository.savedDefaultNoteColors.isEmpty())
    }

    @Test
    fun `given picker dismissed with changed option when reopened then marks current default`() = runTest {
        val viewModel = createViewModelWithDefaultColor(NoteColor.LAVENDER)
        viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)
        viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.ROSE))
        viewModel.onIntent(SettingsIntent.DefaultColorPickerDismissed)

        viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)

        assertEquals(NoteColor.LAVENDER, viewModel.state.value.defaultColorPicker)
    }

    @Test
    fun `given other option when applied then saves it, closes picker and updates default without effects`() = runTest {
        val viewModel = createViewModelWithDefaultColor(NoteColor.LAVENDER)
        viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)
        viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.ROSE))

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.DefaultColorApplied)

            expectNoEvents()
        }

        assertEquals(listOf(NoteColor.ROSE), repository.savedDefaultNoteColors)
        assertNull(viewModel.state.value.defaultColorPicker)
        assertEquals(NoteColor.ROSE, viewModel.state.value.defaultNoteColor)
    }

    @Test
    fun `given no color option when applied then saves no color as default`() = runTest {
        val viewModel = createViewModelWithDefaultColor(NoteColor.AQUA)
        viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)
        viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.NONE))

        viewModel.onIntent(SettingsIntent.DefaultColorApplied)

        assertEquals(listOf(NoteColor.NONE), repository.savedDefaultNoteColors)
        assertEquals(NoteColor.NONE, viewModel.state.value.defaultNoteColor)
    }

    @Test
    fun `given same color when applied then closes picker without saving nor warning`() = runTest {
        repository.defaultNoteColorSaveResult = AppResult.Error(SettingsError.Storage)
        val viewModel = createViewModelWithDefaultColor(NoteColor.LAVENDER)
        viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.DefaultColorApplied)

            expectNoEvents()
        }

        assertTrue(repository.savedDefaultNoteColors.isEmpty())
        assertNull(viewModel.state.value.defaultColorPicker)
        assertEquals(NoteColor.LAVENDER, viewModel.state.value.defaultNoteColor)
    }

    @Test
    fun `given option changed and returned to default when applied then does not save`() = runTest {
        val viewModel = createViewModelWithDefaultColor(NoteColor.LAVENDER)
        viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)
        viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.ROSE))
        viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.LAVENDER))

        viewModel.onIntent(SettingsIntent.DefaultColorApplied)

        assertTrue(repository.savedDefaultNoteColors.isEmpty())
        assertNull(viewModel.state.value.defaultColorPicker)
    }

    @Test
    fun `given closed picker when applied then does not save`() = runTest {
        val viewModel = createViewModelWithDefaultColor(NoteColor.LAVENDER)

        viewModel.onIntent(SettingsIntent.DefaultColorApplied)

        assertTrue(repository.savedDefaultNoteColors.isEmpty())
        assertEquals(NoteColor.LAVENDER, viewModel.state.value.defaultNoteColor)
    }

    @Test
    fun `given save fails when color applied then emits its error and color stays current`() = runTest {
        repository.defaultNoteColorSaveResult = AppResult.Error(SettingsError.Storage)
        val viewModel = createViewModelWithDefaultColor(NoteColor.LAVENDER)
        viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)
        viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.ROSE))

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.DefaultColorApplied)

            assertEquals(SettingsEffect.ShowDefaultColorSaveError, awaitItem())
            expectNoEvents()
        }

        assertNull(viewModel.state.value.defaultColorPicker)
        assertEquals(NoteColor.ROSE, viewModel.state.value.defaultNoteColor)
    }

    @Test
    fun `given failed save when picker reopened then marks color kept for the session`() = runTest {
        repository.defaultNoteColorSaveResult = AppResult.Error(SettingsError.Storage)
        val viewModel = createViewModelWithDefaultColor(NoteColor.LAVENDER)
        viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)
        viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.ROSE))
        viewModel.onIntent(SettingsIntent.DefaultColorApplied)

        viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)

        assertEquals(NoteColor.ROSE, viewModel.state.value.defaultColorPicker)
    }

    @Test
    fun `given theme saves fine and color save fails when both change then only color error is emitted`() = runTest {
        repository.defaultNoteColorSaveResult = AppResult.Error(SettingsError.Storage)
        repository.emitStored(ThemeMode.DARK)
        val viewModel = createViewModelWithDefaultColor(NoteColor.LAVENDER)

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.ThemeSelected(ThemeMode.LIGHT))
            viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)
            viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.ROSE))
            viewModel.onIntent(SettingsIntent.DefaultColorApplied)

            assertEquals(SettingsEffect.ShowDefaultColorSaveError, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `given color saves fine and theme save fails when both change then only theme error is emitted`() = runTest {
        repository.saveResult = AppResult.Error(SettingsError.Storage)
        repository.emitStored(ThemeMode.DARK)
        val viewModel = createViewModelWithDefaultColor(NoteColor.LAVENDER)

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)
            viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.ROSE))
            viewModel.onIntent(SettingsIntent.DefaultColorApplied)
            viewModel.onIntent(SettingsIntent.ThemeSelected(ThemeMode.LIGHT))

            assertEquals(SettingsEffect.ShowThemeSaveError, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `given open picker when state is collected again then keeps same option open`() = runTest {
        val viewModel = createViewModelWithDefaultColor(NoteColor.LAVENDER)
        viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)
        viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.ROSE))
        viewModel.state.test {
            awaitItem()
        }

        viewModel.state.test {
            assertEquals(NoteColor.ROSE, awaitItem().defaultColorPicker)
        }
    }

    @Test
    fun `given open picker when theme changes then picker stays with same option`() = runTest {
        repository.emitStored(ThemeMode.DARK)
        val viewModel = createViewModelWithDefaultColor(NoteColor.LAVENDER)
        viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)
        viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.ROSE))

        viewModel.onIntent(SettingsIntent.ThemeSelected(ThemeMode.LIGHT))

        assertEquals(NoteColor.ROSE, viewModel.state.value.defaultColorPicker)
        assertEquals(ThemeMode.LIGHT, viewModel.state.value.themeMode)
    }
}
