package com.example.notesapp.presentation.notes.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.theme.NotesAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Botón Compartir de la barra superior del editor (spec 010), en ambos temas. */
@RunWith(Parameterized::class)
class NoteEditorShareScreenTest(private val darkTheme: Boolean) {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<NoteEditorIntent>()

    /** El campo de contenido avisa de su foco al componerse: no es una acción del usuario. */
    private val userIntents: List<NoteEditorIntent>
        get() = intents.filterNot { it is NoteEditorIntent.ContentFocusChanged }

    private fun setContent(state: NoteEditorUiState) {
        composeRule.setContent {
            NotesAppTheme(darkTheme = darkTheme) {
                NoteEditorScreen(state = state, onIntent = { intents += it })
            }
        }
    }

    private fun noteState(
        title: String = "Reunión de equipo",
        content: String = "Menos ruido, más foco.",
        images: List<NoteImage> = emptyList(),
        isManualSave: Boolean = false,
        color: NoteColor = NoteColor.NONE,
    ) = NoteEditorUiState(
        isNewNote = false,
        title = TextFieldValue(title),
        content = TextFieldValue(content),
        images = images,
        color = color,
        isManualSave = isManualSave,
    )

    @Test
    fun given_visible_note_when_displayed_then_share_button_is_labelled_button_between_pin_and_more() {
        setContent(noteState())

        val shareBounds = share()
            .assertIsDisplayed()
            .assertContentDescriptionEquals(string(R.string.note_editor_share))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .getUnclippedBoundsInRoot()
        val pinBounds = node(NoteEditorTestTags.PIN).getUnclippedBoundsInRoot()
        val moreBounds = node(NoteEditorTestTags.MORE).getUnclippedBoundsInRoot()
        assertTrue("Compartir va a la derecha de Fijar", pinBounds.right <= shareBounds.left)
        assertTrue("Compartir va a la izquierda de Más opciones", shareBounds.right <= moreBounds.left)
        assertTrue(
            "Compartir comparte la barra con Fijar",
            shareBounds.top < pinBounds.bottom && pinBounds.top < shareBounds.bottom,
        )
    }

    @Test
    fun given_auto_save_on_when_displayed_then_share_button_is_shown_and_enabled() {
        setContent(noteState(isManualSave = false))

        share().assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun given_manual_save_when_displayed_then_share_button_is_shown_and_enabled() {
        setContent(noteState(isManualSave = true))

        share().assertIsDisplayed().assertIsEnabled()
        node(NoteEditorTestTags.SAVE).assertIsDisplayed()
    }

    @Test
    fun given_note_loading_when_displayed_then_share_button_does_not_exist() {
        setContent(NoteEditorUiState(isNewNote = false, isLoading = true))

        share().assertDoesNotExist()
    }

    @Test
    fun given_note_not_found_when_displayed_then_share_button_does_not_exist() {
        setContent(NoteEditorUiState(isNewNote = false, loadError = LoadError.NotFound))

        share().assertDoesNotExist()
    }

    @Test
    fun given_storage_error_when_displayed_then_share_button_does_not_exist() {
        setContent(NoteEditorUiState(isNewNote = false, loadError = LoadError.Storage))

        share().assertDoesNotExist()
    }

    @Test
    fun given_empty_note_when_share_clicked_then_button_is_disabled_and_sends_nothing() {
        setContent(NoteEditorUiState())

        share()
            .assertIsDisplayed()
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
            .assertContentDescriptionEquals(string(R.string.note_editor_share))
            .performClick()
        composeRule.waitForIdle()

        assertTrue(userIntents.isEmpty())
    }

    @Test
    fun given_title_and_content_with_only_spaces_when_displayed_then_share_button_is_disabled() {
        setContent(noteState(title = "   ", content = " \n "))

        share().assertIsNotEnabled()
    }

    @Test
    fun given_empty_note_in_manual_save_when_displayed_then_share_button_is_disabled() {
        setContent(noteState(title = "", content = "", isManualSave = true))

        share().assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun given_only_title_when_displayed_then_share_button_is_enabled() {
        setContent(noteState(title = "Ideas", content = ""))

        share().assertIsEnabled()
    }

    @Test
    fun given_only_content_when_displayed_then_share_button_is_enabled() {
        setContent(noteState(title = "", content = "Libreta"))

        share().assertIsEnabled()
    }

    @Test
    fun given_only_images_when_displayed_then_share_button_is_enabled() {
        setContent(noteState(title = "", content = "", images = listOf(NoteImage(id = 1, path = "/missing/a.jpg"))))

        share().assertIsEnabled()
    }

    @Test
    fun given_empty_note_when_text_appears_then_share_button_becomes_enabled() {
        var state by mutableStateOf(NoteEditorUiState())
        composeRule.setContent {
            NotesAppTheme(darkTheme = darkTheme) {
                NoteEditorScreen(state = state, onIntent = { intents += it })
            }
        }
        share().assertIsNotEnabled()

        state = state.copy(title = TextFieldValue("Ideas"))

        share().assertIsEnabled()
    }

    @Test
    fun given_note_with_content_when_share_clicked_then_sends_share_clicked() {
        setContent(noteState())

        share().performClick()

        assertEquals(listOf<NoteEditorIntent>(NoteEditorIntent.ShareClicked), userIntents)
    }

    @Test
    fun given_manual_save_with_note_color_when_share_clicked_then_sends_share_clicked() {
        setContent(noteState(isManualSave = true, color = NoteColor.ROSE))

        share().performClick()

        assertEquals(listOf<NoteEditorIntent>(NoteEditorIntent.ShareClicked), userIntents)
    }

    @Test
    fun given_visible_note_when_displayed_then_share_button_touch_target_is_at_least_44_dp() {
        setContent(noteState())

        share().assertWidthIsAtLeast(MIN_TOUCH_TARGET).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
    }

    private fun share(): SemanticsNodeInteraction = node(NoteEditorTestTags.SHARE)

    private fun node(tag: String): SemanticsNodeInteraction = composeRule.onNodeWithTag(tag)

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "darkTheme={0}")
        fun themes(): List<Boolean> = listOf(true, false)

        private val MIN_TOUCH_TARGET = 44.dp
    }
}
