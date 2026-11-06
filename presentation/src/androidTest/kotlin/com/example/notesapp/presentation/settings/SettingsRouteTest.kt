package com.example.notesapp.presentation.settings

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.settings.GetDefaultNoteColorUseCase
import com.example.notesapp.domain.settings.GetNotesViewUseCase
import com.example.notesapp.domain.settings.GetThemeModeUseCase
import com.example.notesapp.domain.settings.NotesView
import com.example.notesapp.domain.settings.SetDefaultNoteColorUseCase
import com.example.notesapp.domain.settings.SetNotesViewUseCase
import com.example.notesapp.domain.settings.SetThemeModeUseCase
import com.example.notesapp.domain.settings.SettingsError
import com.example.notesapp.domain.settings.SettingsRepository
import com.example.notesapp.domain.settings.ThemeMode
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.notes.common.ColorPickerTestTags
import com.example.notesapp.presentation.theme.NotesAppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SettingsRouteTest(private val darkTheme: Boolean) {

    @get:Rule
    val composeRule = createComposeRule()

    private val activeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT
    private val otherMode = if (darkTheme) ThemeMode.LIGHT else ThemeMode.DARK

    @Volatile
    private var navigateBackCalls = 0

    private fun setFailingSaveContent() {
        val repository = FailingSaveSettingsRepository(initialMode = activeMode)
        // En el hilo principal: así el ViewModel ya tiene el tema guardado antes del primer toque.
        val viewModel = composeRule.runOnUiThread {
            SettingsViewModel(
                GetThemeModeUseCase(repository),
                SetThemeModeUseCase(repository),
                GetDefaultNoteColorUseCase(repository),
                SetDefaultNoteColorUseCase(repository),
                GetNotesViewUseCase(repository),
                SetNotesViewUseCase(repository),
            )
        }
        composeRule.setContent {
            NotesAppTheme(darkTheme = darkTheme) {
                SettingsRoute(
                    appVersion = APP_VERSION,
                    onNavigateBack = { navigateBackCalls++ },
                    viewModel = viewModel,
                )
            }
        }
    }

    private fun chooseOtherThemeInSheet() {
        row(SettingsTestTags.THEME_ROW).assert(hasText(string(activeMode.labelRes()))).performClick()
        row(SettingsTestTags.THEME_OPTION + otherMode.name).performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            val sheetClosed = composeRule.onAllNodesWithTag(SettingsTestTags.THEME_SHEET)
                .fetchSemanticsNodes().isEmpty()
            val errorShown = composeRule.onAllNodesWithText(string(R.string.settings_theme_save_error))
                .fetchSemanticsNodes().isNotEmpty()
            sheetClosed && errorShown
        }
    }

    private fun applyOtherDefaultColor() {
        row(SettingsTestTags.DEFAULT_COLOR_ROW).assert(hasText(string(R.string.note_color_aqua))).performClick()
        row(ColorPickerTestTags.OPTION + NoteColor.ROSE.name).performClick()
        row(ColorPickerTestTags.APPLY).performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            val sheetClosed = composeRule.onAllNodesWithTag(ColorPickerTestTags.SHEET)
                .fetchSemanticsNodes().isEmpty()
            val errorShown = composeRule.onAllNodesWithText(string(R.string.settings_default_color_save_error))
                .fetchSemanticsNodes().isNotEmpty()
            sheetClosed && errorShown
        }
    }

    private fun chooseListViewInSheet() {
        row(SettingsTestTags.NOTES_VIEW_ROW).assert(hasText(string(R.string.notes_view_grid))).performClick()
        row(SettingsTestTags.NOTES_VIEW_OPTION + NotesView.LIST.name).performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            val sheetClosed = composeRule.onAllNodesWithTag(SettingsTestTags.NOTES_VIEW_SHEET)
                .fetchSemanticsNodes().isEmpty()
            val errorShown = composeRule.onAllNodesWithText(string(R.string.notes_view_save_error))
                .fetchSemanticsNodes().isNotEmpty()
            sheetClosed && errorShown
        }
    }

    @Test
    fun given_failing_save_when_notes_view_chosen_in_sheet_then_shows_its_error_and_keeps_chosen_view() {
        setFailingSaveContent()

        chooseListViewInSheet()

        composeRule.onNodeWithText(string(R.string.notes_view_save_error)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_theme_save_error)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.settings_default_color_save_error)).assertDoesNotExist()
        row(SettingsTestTags.NOTES_VIEW_ROW).assert(hasText(string(R.string.notes_view_list)))
    }

    @Test
    fun given_visible_notes_view_save_error_when_back_clicked_then_navigates_without_waiting_for_error_to_close() {
        setFailingSaveContent()
        chooseListViewInSheet()
        composeRule.mainClock.autoAdvance = false

        row(SettingsTestTags.BACK).performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) { navigateBackCalls > 0 }

        assertEquals(1, navigateBackCalls)
        composeRule.onNodeWithText(string(R.string.notes_view_save_error)).assertIsDisplayed()
    }

    @Test
    fun given_failing_save_when_theme_chosen_in_sheet_then_shows_error_and_keeps_chosen_theme() {
        setFailingSaveContent()

        chooseOtherThemeInSheet()

        composeRule.onNodeWithText(string(R.string.settings_theme_save_error)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_default_color_save_error)).assertDoesNotExist()
        row(SettingsTestTags.THEME_ROW).assert(hasText(string(otherMode.labelRes())))
    }

    @Test
    fun given_failing_save_when_default_color_applied_then_shows_its_error_and_keeps_chosen_color() {
        setFailingSaveContent()

        applyOtherDefaultColor()

        composeRule.onNodeWithText(string(R.string.settings_default_color_save_error)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_theme_save_error)).assertDoesNotExist()
        row(SettingsTestTags.DEFAULT_COLOR_ROW).assert(hasText(string(R.string.note_color_rose)))
    }

    @Test
    fun given_visible_color_save_error_when_back_clicked_then_navigates_without_waiting_for_error_to_close() {
        setFailingSaveContent()
        applyOtherDefaultColor()
        composeRule.mainClock.autoAdvance = false

        row(SettingsTestTags.BACK).performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) { navigateBackCalls > 0 }

        assertEquals(1, navigateBackCalls)
        composeRule.onNodeWithText(string(R.string.settings_default_color_save_error)).assertIsDisplayed()
    }

    @Test
    fun given_visible_save_error_when_back_clicked_then_navigates_without_waiting_for_error_to_close() {
        setFailingSaveContent()
        chooseOtherThemeInSheet()
        // Con el reloj parado el aviso no puede cerrarse: si el efecto lo esperara, nunca se navegaría.
        composeRule.mainClock.autoAdvance = false

        row(SettingsTestTags.BACK).performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) { navigateBackCalls > 0 }

        assertEquals(1, navigateBackCalls)
        composeRule.onNodeWithText(string(R.string.settings_theme_save_error)).assertIsDisplayed()
    }

    private fun row(tag: String): SemanticsNodeInteraction = composeRule.onNodeWithTag(tag)

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "darkTheme={0}")
        fun themes(): List<Boolean> = listOf(true, false)

        private const val APP_VERSION = "9.8.7-test"
        private const val TIMEOUT_MS = 5_000L
    }
}

/** Como el repositorio real, un fallo al guardar mantiene el tema, el color o la vista elegidos durante la sesión. */
private class FailingSaveSettingsRepository(initialMode: ThemeMode) : SettingsRepository {

    private val themeMode = MutableStateFlow<ThemeMode?>(initialMode)
    private val defaultNoteColor = MutableStateFlow<NoteColor?>(null)
    private val notesView = MutableStateFlow<NotesView?>(null)

    override fun observeThemeMode(): Flow<ThemeMode?> = themeMode

    override suspend fun setThemeMode(mode: ThemeMode): AppResult<Unit, SettingsError> {
        themeMode.value = mode
        return AppResult.Error(SettingsError.Storage)
    }

    override fun observeDefaultNoteColor(): Flow<NoteColor?> = defaultNoteColor

    override suspend fun setDefaultNoteColor(color: NoteColor): AppResult<Unit, SettingsError> {
        defaultNoteColor.value = color
        return AppResult.Error(SettingsError.Storage)
    }

    override fun observeNotesView(): Flow<NotesView?> = notesView

    override suspend fun setNotesView(view: NotesView): AppResult<Unit, SettingsError> {
        notesView.value = view
        return AppResult.Error(SettingsError.Storage)
    }
}
