package com.example.notesapp.presentation.notes.export

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.theme.NotesAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Pantalla de selección de notas para exportar (spec 011), en ambos temas. */
@RunWith(Parameterized::class)
class ExportNotesScreenTest(private val darkTheme: Boolean) {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<ExportNotesIntent>()

    private fun setContent(state: ExportNotesUiState) {
        composeRule.setContent {
            NotesAppTheme(darkTheme = darkTheme) {
                ExportNotesScreen(state = state, onIntent = { intents += it })
            }
        }
    }

    private fun loadedState(
        notes: List<ExportNoteItem> = listOf(
            ExportNoteItem(id = 1, title = "La compra"),
            ExportNoteItem(id = 2, title = ""),
            ExportNoteItem(id = 3, title = "Ideas para el viaje"),
        ),
        selectedIds: Set<Long> = emptySet(),
        isExporting: Boolean = false,
    ) = ExportNotesUiState(isLoading = false, notes = notes, selectedIds = selectedIds, isExporting = isExporting)

    @Test
    fun given_loading_when_displayed_then_shows_neither_list_nor_empty_message() {
        setContent(ExportNotesUiState(isLoading = true))

        node(ExportNotesTestTags.BACK).assertIsDisplayed()
        node(ExportNotesTestTags.EMPTY).assertDoesNotExist()
        node(ExportNotesTestTags.LIST).assertDoesNotExist()
        node(ExportNotesTestTags.EXPORT).assertDoesNotExist()
    }

    @Test
    fun given_no_notes_when_displayed_then_shows_empty_message_without_list_or_export() {
        setContent(loadedState(notes = emptyList()))

        node(ExportNotesTestTags.EMPTY).assertIsDisplayed().assertTextEquals(string(R.string.export_empty))
        node(ExportNotesTestTags.LIST).assertDoesNotExist()
        node(ExportNotesTestTags.SELECT_ALL).assertDoesNotExist()
        node(ExportNotesTestTags.EXPORT).assertDoesNotExist()
    }

    @Test
    fun given_load_failed_when_displayed_then_shows_load_error_without_list_export_or_empty_message() {
        setContent(ExportNotesUiState(isLoading = false, loadFailed = true))

        node(ExportNotesTestTags.EMPTY).assertIsDisplayed().assertTextEquals(string(R.string.export_load_error))
        composeRule.onNodeWithText(string(R.string.export_empty)).assertDoesNotExist()
        node(ExportNotesTestTags.LIST).assertDoesNotExist()
        node(ExportNotesTestTags.SELECTED_COUNT).assertDoesNotExist()
        node(ExportNotesTestTags.SELECT_ALL).assertDoesNotExist()
        node(ExportNotesTestTags.EXPORT).assertDoesNotExist()
    }

    @Test
    fun given_load_failed_when_back_clicked_then_sends_back_clicked() {
        setContent(ExportNotesUiState(isLoading = false, loadFailed = true))

        node(ExportNotesTestTags.BACK).performClick()

        assertEquals(listOf<ExportNotesIntent>(ExportNotesIntent.BackClicked), intents)
    }

    @Test
    fun given_notes_when_displayed_then_rows_show_title_or_untitled() {
        setContent(loadedState())

        row(1).assertIsDisplayed().assert(hasText("La compra"))
        row(2).assertIsDisplayed().assert(hasText(string(R.string.export_untitled)))
        row(3).assertIsDisplayed().assert(hasText("Ideas para el viaje"))
        node(ExportNotesTestTags.EMPTY).assertDoesNotExist()
    }

    @Test
    fun given_notes_when_displayed_then_rows_are_checkboxes_with_their_toggle_state() {
        setContent(loadedState(selectedIds = setOf(1)))

        row(1)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            .assertIsOn()
            .assertHasClickAction()
            .assertIsEnabled()
        row(2)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsOff()
    }

    @Test
    fun given_nothing_selected_when_displayed_then_count_reads_zero_selected() {
        setContent(loadedState())

        node(ExportNotesTestTags.SELECTED_COUNT).assertTextEquals(plural(R.plurals.export_selected_count, 0))
    }

    @Test
    fun given_one_selected_when_displayed_then_count_uses_singular() {
        setContent(loadedState(selectedIds = setOf(1)))

        node(ExportNotesTestTags.SELECTED_COUNT).assertTextEquals(plural(R.plurals.export_selected_count, 1))
    }

    @Test
    fun given_two_selected_when_displayed_then_count_uses_plural() {
        setContent(loadedState(selectedIds = setOf(1, 3)))

        node(ExportNotesTestTags.SELECTED_COUNT).assertTextEquals(plural(R.plurals.export_selected_count, 2))
    }

    @Test
    fun given_stale_selected_id_when_displayed_then_count_ignores_it() {
        setContent(loadedState(selectedIds = setOf(1, 99)))

        node(ExportNotesTestTags.SELECTED_COUNT).assertTextEquals(plural(R.plurals.export_selected_count, 1))
    }

    @Test
    fun given_unselected_row_when_clicked_then_sends_note_toggled() {
        setContent(loadedState())

        row(3).performClick()

        assertEquals(listOf<ExportNotesIntent>(ExportNotesIntent.NoteToggled(3)), intents)
    }

    @Test
    fun given_selected_row_when_clicked_then_sends_note_toggled() {
        setContent(loadedState(selectedIds = setOf(2)))

        row(2).performClick()

        assertEquals(listOf<ExportNotesIntent>(ExportNotesIntent.NoteToggled(2)), intents)
    }

    @Test
    fun given_not_all_selected_when_select_all_clicked_then_sends_select_all() {
        setContent(loadedState(selectedIds = setOf(1)))

        selectAll()
            .assert(hasText(string(R.string.export_select_all)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
            .performClick()

        assertEquals(listOf<ExportNotesIntent>(ExportNotesIntent.SelectAllClicked), intents)
    }

    @Test
    fun given_all_selected_when_clear_selection_clicked_then_sends_clear_selection() {
        setContent(loadedState(selectedIds = setOf(1, 2, 3)))

        selectAll()
            .assert(hasText(string(R.string.export_clear_selection)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        assertEquals(listOf<ExportNotesIntent>(ExportNotesIntent.ClearSelectionClicked), intents)
    }

    @Test
    fun given_nothing_selected_when_export_clicked_then_button_is_disabled_and_sends_nothing() {
        setContent(loadedState())

        export()
            .assertIsDisplayed()
            .assertIsNotEnabled()
            .assert(hasText(string(R.string.export_action)))
            .performClick()
        composeRule.waitForIdle()

        assertTrue(intents.isEmpty())
    }

    @Test
    fun given_selection_when_export_clicked_then_sends_export_clicked() {
        setContent(loadedState(selectedIds = setOf(1)))

        export()
            .assertIsEnabled()
            .assert(hasText(string(R.string.export_action)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        assertEquals(listOf<ExportNotesIntent>(ExportNotesIntent.ExportClicked), intents)
    }

    @Test
    fun given_exporting_when_displayed_then_shows_labelled_progress_in_disabled_button() {
        setContent(loadedState(selectedIds = setOf(1), isExporting = true))

        node(ExportNotesTestTags.PROGRESS)
            .assertIsDisplayed()
            .assertContentDescriptionEquals(string(R.string.export_in_progress))
        export().assertIsNotEnabled()
    }

    @Test
    fun given_not_exporting_when_displayed_then_progress_does_not_exist() {
        setContent(loadedState(selectedIds = setOf(1)))

        node(ExportNotesTestTags.PROGRESS).assertDoesNotExist()
    }

    @Test
    fun given_exporting_when_rows_and_select_all_clicked_then_they_are_disabled_and_send_nothing() {
        setContent(loadedState(selectedIds = setOf(1), isExporting = true))

        row(1).assertIsNotEnabled().assertIsOn().performClick()
        row(2).assertIsNotEnabled().performClick()
        selectAll().assertIsNotEnabled().performClick()
        export().performClick()
        composeRule.waitForIdle()

        assertTrue(intents.isEmpty())
    }

    @Test
    fun given_screen_when_back_clicked_then_sends_back_clicked() {
        setContent(loadedState())

        node(ExportNotesTestTags.BACK)
            .assertContentDescriptionEquals(string(R.string.export_back))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        assertEquals(listOf<ExportNotesIntent>(ExportNotesIntent.BackClicked), intents)
    }

    @Test
    fun given_exporting_when_back_clicked_then_sends_back_clicked() {
        setContent(loadedState(selectedIds = setOf(1), isExporting = true))

        node(ExportNotesTestTags.BACK).assertIsEnabled().performClick()

        assertEquals(listOf<ExportNotesIntent>(ExportNotesIntent.BackClicked), intents)
    }

    private fun row(id: Long): SemanticsNodeInteraction = node(ExportNotesTestTags.NOTE + id)

    private fun selectAll(): SemanticsNodeInteraction = node(ExportNotesTestTags.SELECT_ALL)

    private fun export(): SemanticsNodeInteraction = node(ExportNotesTestTags.EXPORT)

    private fun node(tag: String): SemanticsNodeInteraction = composeRule.onNodeWithTag(tag)

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun plural(id: Int, count: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getQuantityString(id, count, count)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "darkTheme={0}")
        fun themes(): List<Boolean> = listOf(true, false)
    }
}
