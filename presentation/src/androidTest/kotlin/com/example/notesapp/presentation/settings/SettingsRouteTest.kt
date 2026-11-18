package com.example.notesapp.presentation.settings

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.NoteRepository
import com.example.notesapp.domain.notes.trash.GetTrashRetentionUseCase
import com.example.notesapp.domain.notes.trash.GetTrashUseCase
import com.example.notesapp.domain.notes.trash.TrashedNote
import com.example.notesapp.domain.notes.trash.TrashRetention
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
import com.example.notesapp.domain.settings.SettingsRepository
import com.example.notesapp.domain.settings.ThemeMode
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.notes.common.ColorPickerTestTags
import com.example.notesapp.presentation.theme.NotesAppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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

    @Volatile
    private var exportResultShownCalls = 0

    @Volatile
    private var navigateToTrashCalls = 0

    @Volatile
    private var navigateToHelpCalls = 0

    private fun setFailingSaveContent() = setRouteContent()

    private fun setRouteContent(
        exportedCount: Int? = null,
        missingImageCount: Int = 0,
        trashedNotes: List<TrashedNote> = emptyList(),
    ) {
        val repository = FailingSaveSettingsRepository(initialMode = activeMode)
        val notesRepository = TrashOnlyNoteRepository(trashedNotes)
        // En el hilo principal: así el ViewModel ya tiene el tema guardado antes del primer toque.
        val viewModel = composeRule.runOnUiThread {
            SettingsViewModel(
                GetThemeModeUseCase(repository),
                SetThemeModeUseCase(repository),
                GetDefaultNoteColorUseCase(repository),
                SetDefaultNoteColorUseCase(repository),
                GetNotesViewUseCase(repository),
                SetNotesViewUseCase(repository),
                GetAutoSaveEnabledUseCase(repository),
                SetAutoSaveEnabledUseCase(repository),
                GetTrashUseCase(notesRepository, GetTrashRetentionUseCase(repository)) { NOW },
            )
        }
        composeRule.setContent {
            NotesAppTheme(darkTheme = darkTheme) {
                SettingsRoute(
                    appVersion = APP_VERSION,
                    onNavigateBack = { navigateBackCalls++ },
                    onNavigateToExport = {},
                    onNavigateToTrash = { navigateToTrashCalls++ },
                    onNavigateToHelp = { navigateToHelpCalls++ },
                    exportedCount = exportedCount,
                    missingImageCount = missingImageCount,
                    onExportResultShown = { exportResultShownCalls++ },
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

    private fun toggleAutoSave() {
        row(SettingsTestTags.AUTO_SAVE_ROW).assertIsOn().performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText(string(R.string.settings_auto_save_error))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun given_failing_save_when_auto_save_toggled_then_shows_its_error_and_keeps_switch_off() {
        setFailingSaveContent()

        toggleAutoSave()

        composeRule.onNodeWithText(string(R.string.settings_auto_save_error)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_theme_save_error)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.settings_default_color_save_error)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.notes_view_save_error)).assertDoesNotExist()
        row(SettingsTestTags.AUTO_SAVE_ROW).assertIsOff()
    }

    @Test
    fun given_visible_auto_save_error_when_back_clicked_then_navigates_without_waiting_for_error_to_close() {
        setFailingSaveContent()
        toggleAutoSave()
        composeRule.mainClock.autoAdvance = false

        row(SettingsTestTags.BACK).performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) { navigateBackCalls > 0 }

        assertEquals(1, navigateBackCalls)
        composeRule.onNodeWithText(string(R.string.settings_auto_save_error)).assertIsDisplayed()
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

    @Test
    fun given_one_exported_note_when_displayed_then_shows_singular_export_done() {
        setRouteContent(exportedCount = 1)

        waitForText(exportDone(1))

        composeRule.onNodeWithText(exportDone(1)).assertIsDisplayed()
    }

    @Test
    fun given_three_exported_notes_when_displayed_then_shows_plural_export_done() {
        setRouteContent(exportedCount = 3)

        waitForText(exportDone(3))

        composeRule.onNodeWithText(exportDone(3)).assertIsDisplayed()
    }

    @Test
    fun given_missing_images_when_displayed_then_shows_missing_images_after_export_done() {
        setRouteContent(exportedCount = 2, missingImageCount = 2)

        waitForText(exportDone(2))
        composeRule.onNodeWithText(string(R.string.export_missing_images)).assertDoesNotExist()
        waitForText(string(R.string.export_missing_images), timeoutMillis = SNACKBAR_QUEUE_TIMEOUT_MS)

        composeRule.onNodeWithText(string(R.string.export_missing_images)).assertIsDisplayed()
        composeRule.onNodeWithText(exportDone(2)).assertDoesNotExist()
    }

    @Test
    fun given_no_missing_images_when_export_done_dismissed_then_missing_images_never_shows() {
        setRouteContent(exportedCount = 2, missingImageCount = 0)
        waitForText(exportDone(2))

        composeRule.waitUntil(timeoutMillis = SNACKBAR_QUEUE_TIMEOUT_MS) {
            composeRule.onAllNodesWithText(exportDone(2)).fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNodeWithText(string(R.string.export_missing_images)).assertDoesNotExist()
    }

    @Test
    fun given_export_result_when_displayed_then_reports_it_shown_exactly_once() {
        setRouteContent(exportedCount = 1, missingImageCount = 2)

        waitForText(string(R.string.export_missing_images), timeoutMillis = SNACKBAR_QUEUE_TIMEOUT_MS)
        composeRule.waitForIdle()

        assertEquals(1, exportResultShownCalls)
    }

    @Test
    fun given_no_export_result_when_displayed_then_shows_no_export_notice() {
        setRouteContent(exportedCount = null, missingImageCount = 2)

        composeRule.waitForIdle()

        composeRule.onNodeWithText(exportDone(1)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.export_missing_images)).assertDoesNotExist()
        assertEquals(0, exportResultShownCalls)
    }

    @Test
    fun given_trashed_notes_when_displayed_then_trash_row_shows_their_count() {
        setRouteContent(trashedNotes = listOf(trashedNote(id = 1), trashedNote(id = 2)))

        waitForText(trashCount(2))

        row(SettingsTestTags.TRASH_ROW).assert(hasText(trashCount(2)))
    }

    @Test
    fun given_empty_trash_when_displayed_then_trash_row_shows_empty() {
        setRouteContent()

        waitForText(string(R.string.settings_trash_empty))

        row(SettingsTestTags.TRASH_ROW).assert(hasText(string(R.string.settings_trash_empty)))
    }

    @Test
    fun given_settings_when_trash_row_clicked_then_navigates_to_trash() {
        setRouteContent(trashedNotes = listOf(trashedNote(id = 1)))

        row(SettingsTestTags.TRASH_ROW).performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) { navigateToTrashCalls > 0 }

        assertEquals(1, navigateToTrashCalls)
    }

    @Test
    fun given_settings_when_help_row_clicked_then_navigates_to_help() {
        setRouteContent()

        row(SettingsTestTags.HELP_ROW).performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) { navigateToHelpCalls > 0 }

        assertEquals(1, navigateToHelpCalls)
        assertEquals(0, navigateToTrashCalls)
        assertEquals(0, navigateBackCalls)
    }

    private fun trashCount(count: Int): String = InstrumentationRegistry.getInstrumentation().targetContext
        .resources.getQuantityString(R.plurals.settings_trash_count, count, count)

    private fun trashedNote(id: Long) = TrashedNote(
        note = Note(
            id = id,
            title = "Nota $id",
            content = "",
            color = NoteColor.NONE,
            isPinned = false,
            category = null,
            updatedAt = NOW,
        ),
        trashedAt = NOW,
    )

    private fun waitForText(text: String, timeoutMillis: Long = TIMEOUT_MS) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun exportDone(count: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.resources
            .getQuantityString(R.plurals.export_done, count, count)

    private fun row(tag: String): SemanticsNodeInteraction = composeRule.onNodeWithTag(tag)

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "darkTheme={0}")
        fun themes(): List<Boolean> = listOf(true, false)

        private const val APP_VERSION = "9.8.7-test"
        private const val TIMEOUT_MS = 5_000L
        private const val NOW = 1_700_000_000_000L

        /** Un aviso corto dura 4 s: el siguiente de la cola aparece después. */
        private const val SNACKBAR_QUEUE_TIMEOUT_MS = 10_000L
    }
}

/** Como el repositorio real, un fallo al guardar mantiene el tema, el color o la vista elegidos durante la sesión. */
private class FailingSaveSettingsRepository(initialMode: ThemeMode) : SettingsRepository {

    private val themeMode = MutableStateFlow<ThemeMode?>(initialMode)
    private val defaultNoteColor = MutableStateFlow<NoteColor?>(null)
    private val notesView = MutableStateFlow<NotesView?>(null)
    private val autoSaveEnabled = MutableStateFlow<Boolean?>(null)
    private val trashRetention = MutableStateFlow<TrashRetention?>(null)

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

    override fun observeAutoSaveEnabled(): Flow<Boolean?> = autoSaveEnabled

    override suspend fun setAutoSaveEnabled(enabled: Boolean): AppResult<Unit, SettingsError> {
        autoSaveEnabled.value = enabled
        return AppResult.Error(SettingsError.Storage)
    }

    override fun observeTrashRetention(): Flow<TrashRetention?> = trashRetention

    /** A diferencia de los demás ajustes, si falla el plazo observado no cambia. */
    override suspend fun setTrashRetention(retention: TrashRetention): AppResult<Unit, SettingsError> =
        AppResult.Error(SettingsError.Storage)
}

/** Solo sirve la papelera, que es lo único que Ajustes lee de las notas. */
private class TrashOnlyNoteRepository(private val trashed: List<TrashedNote>) : NoteRepository {

    override fun observeTrash(): Flow<AppResult<List<TrashedNote>, NoteError>> = flowOf(AppResult.Success(trashed))

    override fun observeNotes(): Flow<AppResult<List<Note>, NoteError>> = flowOf(AppResult.Success(emptyList()))

    override suspend fun getNote(id: Long): AppResult<Note, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun saveNote(note: Note): AppResult<Long, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun deleteNote(id: Long): AppResult<Unit, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun addImages(noteId: Long, paths: List<String>): AppResult<List<NoteImage>, NoteError> =
        AppResult.Error(NoteError.Storage)

    override suspend fun removeImage(imageId: Long): AppResult<Unit, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun saveNoteChanges(
        note: Note,
        addedPaths: List<String>,
        removedImageIds: List<Long>,
    ): AppResult<Note, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun getImagePaths(): AppResult<Set<String>, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun moveToTrash(id: Long, trashedAt: Long): AppResult<Unit, NoteError> =
        AppResult.Error(NoteError.Storage)

    override suspend fun restoreFromTrash(id: Long): AppResult<Unit, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun deleteNotes(ids: List<Long>): AppResult<Unit, NoteError> = AppResult.Error(NoteError.Storage)
}
