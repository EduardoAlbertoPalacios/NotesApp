package com.example.notesapp.presentation.notes.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.notes.common.NoteDateLabel
import com.example.notesapp.presentation.theme.NotesAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val note = NoteItemUi(
        id = 42,
        title = "Reunión de equipo",
        content = "Prototipo el viernes",
        color = NoteColor.AQUA,
        isPinned = true,
        date = NoteDateLabel.Yesterday,
    )
    private val intents = mutableListOf<NoteListIntent>()

    private fun setContent(state: NoteListUiState) {
        composeRule.setContent {
            NotesAppTheme {
                NoteListScreen(state = state, onIntent = { intents += it })
            }
        }
    }

    @Test
    fun given_notes_when_displayed_then_shows_title_content_and_pin() {
        setContent(NoteListUiState(isLoading = false, notes = listOf(note)))

        composeRule.onNodeWithText(note.title).assertIsDisplayed()
        composeRule.onNodeWithText(note.content).assertIsDisplayed()
        composeRule.onNodeWithText("1 nota · un espacio para tus ideas").assertIsDisplayed()
    }

    @Test
    fun given_note_when_clicked_then_sends_note_clicked() {
        setContent(NoteListUiState(isLoading = false, notes = listOf(note)))

        composeRule.onNodeWithTag(NoteListTestTags.NOTE_CARD + note.id).performClick()

        assertEquals(listOf<NoteListIntent>(NoteListIntent.NoteClicked(note.id)), intents)
    }

    @Test
    fun given_screen_when_create_and_settings_clicked_then_sends_intents() {
        setContent(NoteListUiState(isLoading = false, notes = listOf(note)))

        composeRule.onNodeWithTag(NoteListTestTags.CREATE_NOTE).performClick()
        composeRule.onNodeWithTag(NoteListTestTags.SETTINGS).performClick()

        assertEquals(listOf(NoteListIntent.CreateNoteClicked, NoteListIntent.SettingsClicked), intents)
    }

    @Test
    fun given_loading_state_when_displayed_then_shows_progress() {
        setContent(NoteListUiState(isLoading = true))

        composeRule.onNodeWithTag(NoteListTestTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun given_no_notes_when_displayed_then_shows_empty_message() {
        setContent(NoteListUiState(isLoading = false))

        composeRule.onNodeWithTag(NoteListTestTags.EMPTY).assertIsDisplayed()
    }

    @Test
    fun given_error_when_displayed_then_shows_friendly_error() {
        setContent(NoteListUiState(isLoading = false, hasError = true))

        composeRule.onNodeWithTag(NoteListTestTags.ERROR).assertIsDisplayed()
    }

    @Test
    fun given_filter_chips_when_clicked_then_sends_filter_selected() {
        setContent(NoteListUiState(isLoading = false, notes = listOf(note)))

        composeRule.onNodeWithTag(NoteListTestTags.FILTER + NoteFilter.WORK.name).performClick()
        composeRule.onNodeWithTag(NoteListTestTags.FILTER + NoteFilter.PERSONAL.name).performClick()
        composeRule.onNodeWithTag(NoteListTestTags.FILTER + NoteFilter.ALL.name).performClick()

        assertEquals(
            listOf(
                NoteListIntent.FilterSelected(NoteFilter.WORK),
                NoteListIntent.FilterSelected(NoteFilter.PERSONAL),
                NoteListIntent.FilterSelected(NoteFilter.ALL),
            ),
            intents,
        )
    }

    @Test
    fun given_selected_filter_when_displayed_then_only_that_chip_is_selected() {
        setContent(NoteListUiState(isLoading = false, notes = listOf(note), selectedFilter = NoteFilter.WORK))

        composeRule.onNodeWithTag(NoteListTestTags.FILTER + NoteFilter.WORK.name).assertIsSelected()
        composeRule.onNodeWithTag(NoteListTestTags.FILTER + NoteFilter.ALL.name).assertIsNotSelected()
        composeRule.onNodeWithTag(NoteListTestTags.FILTER + NoteFilter.PERSONAL.name).assertIsNotSelected()
    }

    @Test
    fun given_no_notes_and_all_filter_when_displayed_then_shows_general_empty_message() {
        setContent(NoteListUiState(isLoading = false, selectedFilter = NoteFilter.ALL))

        composeRule.onNodeWithText(string(R.string.note_list_empty)).assertIsDisplayed()
    }

    @Test
    fun given_no_notes_and_work_filter_when_displayed_then_shows_work_empty_message() {
        setContent(NoteListUiState(isLoading = false, selectedFilter = NoteFilter.WORK))

        composeRule.onNodeWithText(string(R.string.note_list_empty_work)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.note_list_empty)).assertDoesNotExist()
    }

    @Test
    fun given_no_notes_and_personal_filter_when_displayed_then_shows_personal_empty_message() {
        setContent(NoteListUiState(isLoading = false, selectedFilter = NoteFilter.PERSONAL))

        composeRule.onNodeWithText(string(R.string.note_list_empty_personal)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.note_list_empty)).assertDoesNotExist()
    }

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
