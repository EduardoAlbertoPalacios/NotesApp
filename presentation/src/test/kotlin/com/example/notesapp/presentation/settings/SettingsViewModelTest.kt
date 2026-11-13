package com.example.notesapp.presentation.settings

import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.settings.GetAutoSaveEnabledUseCase
import com.example.notesapp.domain.settings.GetDefaultNoteColorUseCase
import com.example.notesapp.domain.settings.GetNotesViewUseCase
import com.example.notesapp.domain.settings.GetThemeModeUseCase
import com.example.notesapp.domain.settings.NotesView
import com.example.notesapp.domain.settings.SetAutoSaveEnabledUseCase
import com.example.notesapp.domain.settings.SetDefaultNoteColorUseCase
import com.example.notesapp.domain.settings.SetNotesViewUseCase
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
        GetNotesViewUseCase(repository),
        SetNotesViewUseCase(repository),
        GetAutoSaveEnabledUseCase(repository),
        SetAutoSaveEnabledUseCase(repository),
    )

    private suspend fun createViewModelWithTheme(mode: ThemeMode?): SettingsViewModel {
        repository.emitStored(mode)
        return createViewModel()
    }

    private suspend fun createViewModelWithDefaultColor(color: NoteColor?): SettingsViewModel {
        repository.emitStoredDefaultNoteColor(color)
        return createViewModel()
    }

    private suspend fun createViewModelWithNotesView(view: NotesView?): SettingsViewModel {
        repository.emitStoredNotesView(view)
        return createViewModel()
    }

    private suspend fun createViewModelWithAutoSave(enabled: Boolean?): SettingsViewModel {
        repository.emitStoredAutoSaveEnabled(enabled)
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
    fun `given settings when export row clicked then emits navigate to export without changing state`() = runTest {
        val viewModel = createViewModelWithTheme(ThemeMode.DARK)
        val stateBefore = viewModel.state.value

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.ExportRowClicked)

            assertEquals(SettingsEffect.NavigateToExport, awaitItem())
            expectNoEvents()
        }
        assertEquals(stateBefore, viewModel.state.value)
    }

    @Test
    fun `given export finished without missing images when received then emits only export done without changing state`() = runTest {
        val viewModel = createViewModelWithTheme(ThemeMode.DARK)
        val stateBefore = viewModel.state.value

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.ExportFinished(exportedCount = 3, missingImageCount = 0))

            assertEquals(SettingsEffect.ShowExportDone(3), awaitItem())
            expectNoEvents()
        }
        assertEquals(stateBefore, viewModel.state.value)
    }

    @Test
    fun `given export finished with missing images when received then emits export done then missing images without changing state`() = runTest {
        val viewModel = createViewModelWithTheme(ThemeMode.DARK)
        val stateBefore = viewModel.state.value

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.ExportFinished(exportedCount = 1, missingImageCount = 2))

            assertEquals(SettingsEffect.ShowExportDone(1), awaitItem())
            assertEquals(SettingsEffect.ShowExportMissingImages, awaitItem())
            expectNoEvents()
        }
        assertEquals(stateBefore, viewModel.state.value)
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

    @Test
    fun `given no notes view emission yet when created then state has grid and closed sheet`() {
        val viewModel = createViewModel()

        assertEquals(NotesView.GRID, viewModel.state.value.notesView)
        assertFalse(viewModel.state.value.isNotesViewSheetVisible)
    }

    @Test
    fun `given stored notes view when created then state shows it`() = runTest {
        val viewModel = createViewModelWithNotesView(NotesView.LIST)

        viewModel.state.test {
            assertEquals(NotesView.LIST, awaitItem().notesView)
        }
    }

    @Test
    fun `given repository without notes view when created then state shows grid`() = runTest {
        val viewModel = createViewModelWithNotesView(null)

        viewModel.state.test {
            assertEquals(NotesView.GRID, awaitItem().notesView)
        }
    }

    @Test
    fun `given stored notes view when it changes elsewhere then state follows it`() = runTest {
        val viewModel = createViewModelWithNotesView(NotesView.GRID)

        viewModel.state.test {
            assertEquals(NotesView.GRID, awaitItem().notesView)

            repository.emitStoredNotesView(NotesView.LIST)

            assertEquals(NotesView.LIST, awaitItem().notesView)
        }
    }

    @Test
    fun `given closed sheet when notes view row clicked then opens sheet without changing view`() = runTest {
        val viewModel = createViewModelWithNotesView(NotesView.LIST)

        viewModel.onIntent(SettingsIntent.NotesViewRowClicked)

        assertEquals(SettingsUiState(notesView = NotesView.LIST, isNotesViewSheetVisible = true), viewModel.state.value)
        assertTrue(repository.savedNotesViews.isEmpty())
    }

    @Test
    fun `given open notes view sheet when dismissed then closes it without changing or saving view`() = runTest {
        val viewModel = createViewModelWithNotesView(NotesView.LIST)
        viewModel.onIntent(SettingsIntent.NotesViewRowClicked)

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.NotesViewSheetDismissed)

            expectNoEvents()
        }

        assertEquals(SettingsUiState(notesView = NotesView.LIST, isNotesViewSheetVisible = false), viewModel.state.value)
        assertTrue(repository.savedNotesViews.isEmpty())
    }

    @Test
    fun `given other notes view when selected then saves it and state reflects it without effects`() = runTest {
        val viewModel = createViewModelWithNotesView(NotesView.GRID)

        viewModel.effects.test {
            viewModel.state.test {
                assertEquals(NotesView.GRID, awaitItem().notesView)

                viewModel.onIntent(SettingsIntent.NotesViewSelected(NotesView.LIST))

                assertEquals(NotesView.LIST, awaitItem().notesView)
            }
            expectNoEvents()
        }
        assertEquals(listOf(NotesView.LIST), repository.savedNotesViews)
    }

    @Test
    fun `given active notes view when selected again then does not save nor change anything`() = runTest {
        repository.notesViewSaveResult = AppResult.Error(SettingsError.Storage)
        val viewModel = createViewModelWithNotesView(NotesView.LIST)

        viewModel.effects.test {
            viewModel.state.test {
                val initial = awaitItem()

                viewModel.onIntent(SettingsIntent.NotesViewSelected(NotesView.LIST))

                assertEquals(initial, viewModel.state.value)
                expectNoEvents()
            }
            expectNoEvents()
        }
        assertTrue(repository.savedNotesViews.isEmpty())
    }

    @Test
    fun `given save fails when notes view selected then emits its error and keeps chosen view`() = runTest {
        repository.notesViewSaveResult = AppResult.Error(SettingsError.Storage)
        val viewModel = createViewModelWithNotesView(NotesView.GRID)

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.NotesViewSelected(NotesView.LIST))

            assertEquals(SettingsEffect.ShowNotesViewSaveError, awaitItem())
            expectNoEvents()
        }
        assertEquals(NotesView.LIST, viewModel.state.value.notesView)
    }

    @Test
    fun `given open sheet when other notes view selected then sheet stays open until dismissed`() = runTest {
        val viewModel = createViewModelWithNotesView(NotesView.GRID)
        viewModel.onIntent(SettingsIntent.NotesViewRowClicked)

        viewModel.onIntent(SettingsIntent.NotesViewSelected(NotesView.LIST))

        assertEquals(SettingsUiState(notesView = NotesView.LIST, isNotesViewSheetVisible = true), viewModel.state.value)

        viewModel.onIntent(SettingsIntent.NotesViewSheetDismissed)

        assertEquals(SettingsUiState(notesView = NotesView.LIST, isNotesViewSheetVisible = false), viewModel.state.value)
    }

    @Test
    fun `given open notes view sheet when state is collected again then sheet is still open with same view`() = runTest {
        val viewModel = createViewModelWithNotesView(NotesView.LIST)
        viewModel.onIntent(SettingsIntent.NotesViewRowClicked)
        viewModel.state.test {
            awaitItem()
        }

        viewModel.state.test {
            assertEquals(SettingsUiState(notesView = NotesView.LIST, isNotesViewSheetVisible = true), awaitItem())
        }
    }

    @Test
    fun `given notes view save fails when theme and color also change then only notes view error is emitted`() = runTest {
        repository.notesViewSaveResult = AppResult.Error(SettingsError.Storage)
        repository.emitStored(ThemeMode.DARK)
        repository.emitStoredDefaultNoteColor(NoteColor.LAVENDER)
        val viewModel = createViewModelWithNotesView(NotesView.GRID)

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.ThemeSelected(ThemeMode.LIGHT))
            viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)
            viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.ROSE))
            viewModel.onIntent(SettingsIntent.DefaultColorApplied)
            viewModel.onIntent(SettingsIntent.NotesViewSelected(NotesView.LIST))

            assertEquals(SettingsEffect.ShowNotesViewSaveError, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `given theme save fails when notes view also changes then only theme error is emitted`() = runTest {
        repository.saveResult = AppResult.Error(SettingsError.Storage)
        repository.emitStored(ThemeMode.DARK)
        val viewModel = createViewModelWithNotesView(NotesView.GRID)

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.NotesViewSelected(NotesView.LIST))
            viewModel.onIntent(SettingsIntent.ThemeSelected(ThemeMode.LIGHT))

            assertEquals(SettingsEffect.ShowThemeSaveError, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `given color save fails when notes view also changes then only color error is emitted`() = runTest {
        repository.defaultNoteColorSaveResult = AppResult.Error(SettingsError.Storage)
        repository.emitStoredDefaultNoteColor(NoteColor.LAVENDER)
        val viewModel = createViewModelWithNotesView(NotesView.GRID)

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.NotesViewSelected(NotesView.LIST))
            viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)
            viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.ROSE))
            viewModel.onIntent(SettingsIntent.DefaultColorApplied)

            assertEquals(SettingsEffect.ShowDefaultColorSaveError, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `given no auto save emission yet when created then state shows auto save enabled`() {
        val viewModel = createViewModel()

        assertTrue(viewModel.state.value.isAutoSaveEnabled)
    }

    @Test
    fun `given stored auto save off when created then state shows it off`() = runTest {
        val viewModel = createViewModelWithAutoSave(false)

        viewModel.state.test {
            assertFalse(awaitItem().isAutoSaveEnabled)
        }
    }

    @Test
    fun `given repository without auto save when created then state shows it on`() = runTest {
        val viewModel = createViewModelWithAutoSave(null)

        viewModel.state.test {
            assertTrue(awaitItem().isAutoSaveEnabled)
        }
    }

    @Test
    fun `given stored auto save when it changes elsewhere then state follows it`() = runTest {
        val viewModel = createViewModelWithAutoSave(true)

        viewModel.state.test {
            assertTrue(awaitItem().isAutoSaveEnabled)

            repository.emitStoredAutoSaveEnabled(false)

            assertFalse(awaitItem().isAutoSaveEnabled)
        }
    }

    @Test
    fun `given auto save on when toggled then saves it off and state reflects it without effects`() = runTest {
        val viewModel = createViewModelWithAutoSave(true)

        viewModel.effects.test {
            viewModel.state.test {
                assertTrue(awaitItem().isAutoSaveEnabled)

                viewModel.onIntent(SettingsIntent.AutoSaveToggled)

                assertFalse(awaitItem().isAutoSaveEnabled)
            }
            expectNoEvents()
        }
        assertEquals(listOf(false), repository.savedAutoSaveEnabled)
    }

    @Test
    fun `given auto save off when toggled then saves it on and state reflects it`() = runTest {
        val viewModel = createViewModelWithAutoSave(false)

        viewModel.onIntent(SettingsIntent.AutoSaveToggled)

        assertEquals(listOf(true), repository.savedAutoSaveEnabled)
        assertTrue(viewModel.state.value.isAutoSaveEnabled)
    }

    @Test
    fun `given auto save toggled twice when each save completes then saves both values in order`() = runTest {
        val viewModel = createViewModelWithAutoSave(true)

        viewModel.onIntent(SettingsIntent.AutoSaveToggled)
        viewModel.onIntent(SettingsIntent.AutoSaveToggled)

        assertEquals(listOf(false, true), repository.savedAutoSaveEnabled)
        assertTrue(viewModel.state.value.isAutoSaveEnabled)
    }

    @Test
    fun `given toggle does not change other settings when toggled then theme color and view stay`() = runTest {
        repository.emitStored(ThemeMode.LIGHT)
        repository.emitStoredDefaultNoteColor(NoteColor.MINT)
        repository.emitStoredNotesView(NotesView.LIST)
        val viewModel = createViewModelWithAutoSave(true)

        viewModel.onIntent(SettingsIntent.AutoSaveToggled)

        assertEquals(
            SettingsUiState(
                themeMode = ThemeMode.LIGHT,
                defaultNoteColor = NoteColor.MINT,
                notesView = NotesView.LIST,
                isAutoSaveEnabled = false,
            ),
            viewModel.state.value,
        )
        assertTrue(repository.savedModes.isEmpty())
        assertTrue(repository.savedDefaultNoteColors.isEmpty())
        assertTrue(repository.savedNotesViews.isEmpty())
    }

    @Test
    fun `given save fails when auto save toggled then emits its error and keeps chosen state`() = runTest {
        repository.autoSaveSaveResult = AppResult.Error(SettingsError.Storage)
        val viewModel = createViewModelWithAutoSave(true)

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.AutoSaveToggled)

            assertEquals(SettingsEffect.ShowAutoSaveSaveError, awaitItem())
            expectNoEvents()
        }
        assertFalse(viewModel.state.value.isAutoSaveEnabled)
    }

    @Test
    fun `given failed save when toggled again then uses state kept for the session`() = runTest {
        repository.autoSaveSaveResult = AppResult.Error(SettingsError.Storage)
        val viewModel = createViewModelWithAutoSave(true)
        viewModel.onIntent(SettingsIntent.AutoSaveToggled)

        viewModel.onIntent(SettingsIntent.AutoSaveToggled)

        assertEquals(listOf(false, true), repository.savedAutoSaveEnabled)
        assertTrue(viewModel.state.value.isAutoSaveEnabled)
    }

    @Test
    fun `given auto save save fails when other three settings change then only auto save error is emitted`() = runTest {
        repository.autoSaveSaveResult = AppResult.Error(SettingsError.Storage)
        repository.emitStored(ThemeMode.DARK)
        repository.emitStoredDefaultNoteColor(NoteColor.LAVENDER)
        repository.emitStoredNotesView(NotesView.GRID)
        val viewModel = createViewModelWithAutoSave(true)

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.ThemeSelected(ThemeMode.LIGHT))
            viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)
            viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.ROSE))
            viewModel.onIntent(SettingsIntent.DefaultColorApplied)
            viewModel.onIntent(SettingsIntent.NotesViewSelected(NotesView.LIST))
            viewModel.onIntent(SettingsIntent.AutoSaveToggled)

            assertEquals(SettingsEffect.ShowAutoSaveSaveError, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `given other three saves fail when auto save also toggles then auto save error is not emitted`() = runTest {
        repository.saveResult = AppResult.Error(SettingsError.Storage)
        repository.defaultNoteColorSaveResult = AppResult.Error(SettingsError.Storage)
        repository.notesViewSaveResult = AppResult.Error(SettingsError.Storage)
        repository.emitStored(ThemeMode.DARK)
        repository.emitStoredDefaultNoteColor(NoteColor.LAVENDER)
        repository.emitStoredNotesView(NotesView.GRID)
        val viewModel = createViewModelWithAutoSave(true)

        viewModel.effects.test {
            viewModel.onIntent(SettingsIntent.AutoSaveToggled)
            viewModel.onIntent(SettingsIntent.ThemeSelected(ThemeMode.LIGHT))
            viewModel.onIntent(SettingsIntent.DefaultColorRowClicked)
            viewModel.onIntent(SettingsIntent.DefaultColorOptionSelected(NoteColor.ROSE))
            viewModel.onIntent(SettingsIntent.DefaultColorApplied)
            viewModel.onIntent(SettingsIntent.NotesViewSelected(NotesView.LIST))

            assertEquals(SettingsEffect.ShowThemeSaveError, awaitItem())
            assertEquals(SettingsEffect.ShowDefaultColorSaveError, awaitItem())
            assertEquals(SettingsEffect.ShowNotesViewSaveError, awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf(false), repository.savedAutoSaveEnabled)
    }
}
