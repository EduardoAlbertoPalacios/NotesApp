package com.example.notesapp.presentation.notes.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.notes.common.NoteDateLabel
import com.example.notesapp.presentation.theme.NotesAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteEditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<NoteEditorIntent>()

    private fun setContent(state: NoteEditorUiState) {
        composeRule.setContent {
            NotesAppTheme {
                NoteEditorScreen(state = state, onIntent = { intents += it })
            }
        }
    }

    @Test
    fun given_note_when_displayed_then_shows_title_content_date_and_saved_status() {
        setContent(
            NoteEditorUiState(
                title = "Reunión de equipo",
                content = "Menos ruido, más foco.",
                date = NoteDateLabel.Yesterday,
                saveStatus = SaveStatus.Saved,
            ),
        )

        composeRule.onNodeWithText("Reunión de equipo").assertIsDisplayed()
        composeRule.onNodeWithText("Menos ruido, más foco.").assertIsDisplayed()
        composeRule.onNodeWithText("Ayer").assertIsDisplayed()
        composeRule.onNodeWithText("Todos los cambios guardados").assertIsDisplayed()
    }

    @Test
    fun given_new_note_when_typing_then_sends_text_changes() {
        // Estado que se actualiza con los intents, como lo hace el ViewModel.
        composeRule.setContent {
            var state by remember { mutableStateOf(NoteEditorUiState()) }
            NotesAppTheme {
                NoteEditorScreen(
                    state = state,
                    onIntent = { intent ->
                        intents += intent
                        state = when (intent) {
                            is NoteEditorIntent.TitleChanged -> state.copy(title = intent.title)
                            is NoteEditorIntent.ContentChanged -> state.copy(content = intent.content)
                            else -> state
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithText("Título").assertIsDisplayed()
        composeRule.onNodeWithTag(NoteEditorTestTags.TITLE).performTextInput("Ideas")
        composeRule.onNodeWithTag(NoteEditorTestTags.CONTENT).performTextInput("Libreta")

        assertEquals(
            listOf(NoteEditorIntent.TitleChanged("Ideas"), NoteEditorIntent.ContentChanged("Libreta")),
            intents,
        )
    }

    @Test
    fun given_editor_when_back_and_pin_clicked_then_sends_intents() {
        setContent(NoteEditorUiState(title = "Ideas"))

        composeRule.onNodeWithTag(NoteEditorTestTags.PIN).performClick()
        composeRule.onNodeWithTag(NoteEditorTestTags.BACK).performClick()

        assertEquals(listOf(NoteEditorIntent.PinClicked, NoteEditorIntent.BackClicked), intents)
    }

    @Test
    fun given_more_options_when_delete_selected_then_sends_delete_clicked() {
        setContent(NoteEditorUiState(title = "Ideas"))

        composeRule.onNodeWithTag(NoteEditorTestTags.MORE).performClick()
        composeRule.onNodeWithTag(NoteEditorTestTags.DELETE).performClick()

        assertEquals(listOf<NoteEditorIntent>(NoteEditorIntent.DeleteClicked), intents)
    }

    @Test
    fun given_delete_dialog_when_confirmed_or_cancelled_then_sends_intents() {
        setContent(NoteEditorUiState(title = "Ideas", isDeleteDialogVisible = true))

        composeRule.onNodeWithText("¿Eliminar esta nota?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.onNodeWithText("Eliminar").performClick()

        assertEquals(listOf(NoteEditorIntent.DeleteDismissed, NoteEditorIntent.DeleteConfirmed), intents)
    }

    @Test
    fun given_not_found_when_displayed_then_shows_message_without_note_actions() {
        setContent(NoteEditorUiState(loadError = LoadError.NotFound))

        composeRule.onNodeWithTag(NoteEditorTestTags.LOAD_ERROR).assertIsDisplayed()
        composeRule.onNodeWithTag(NoteEditorTestTags.PIN).assertDoesNotExist()
    }

    @Test
    fun given_note_with_category_when_displayed_then_shows_category_tag() {
        setContent(NoteEditorUiState(title = "Ideas", category = NoteCategory.WORK, date = NoteDateLabel.Yesterday))

        composeRule.onNodeWithTag(NoteEditorTestTags.CATEGORY_TAG)
            .assertIsDisplayed()
            .assertTextEquals(string(R.string.note_category_work))
    }

    @Test
    fun given_note_with_personal_category_when_displayed_then_tag_shows_personal() {
        setContent(NoteEditorUiState(title = "Ideas", category = NoteCategory.PERSONAL))

        composeRule.onNodeWithTag(NoteEditorTestTags.CATEGORY_TAG).assertTextEquals(string(R.string.note_category_personal))
    }

    @Test
    fun given_note_without_category_when_displayed_then_hides_category_tag() {
        setContent(NoteEditorUiState(title = "Ideas", category = null, date = NoteDateLabel.Yesterday))

        composeRule.onNodeWithTag(NoteEditorTestTags.CATEGORY_TAG).assertDoesNotExist()
    }

    @Test
    fun given_category_options_when_clicked_then_sends_category_clicked() {
        setContent(NoteEditorUiState(title = "Ideas"))

        composeRule.onNodeWithTag(NoteEditorTestTags.CATEGORY_OPTION + NoteCategory.WORK.name).performClick()
        composeRule.onNodeWithTag(NoteEditorTestTags.CATEGORY_OPTION + NoteCategory.PERSONAL.name).performClick()

        assertEquals(
            listOf(
                NoteEditorIntent.CategoryClicked(NoteCategory.WORK),
                NoteEditorIntent.CategoryClicked(NoteCategory.PERSONAL),
            ),
            intents,
        )
    }

    @Test
    fun given_selected_category_when_displayed_then_only_that_option_is_selected() {
        setContent(NoteEditorUiState(title = "Ideas", category = NoteCategory.PERSONAL))

        composeRule.onNodeWithTag(NoteEditorTestTags.CATEGORY_OPTION + NoteCategory.PERSONAL.name).assertIsSelected()
        composeRule.onNodeWithTag(NoteEditorTestTags.CATEGORY_OPTION + NoteCategory.WORK.name).assertIsNotSelected()
    }

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
