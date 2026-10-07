package com.example.notesapp.presentation.notes.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.notesapp.domain.notes.NoteColor
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
}
