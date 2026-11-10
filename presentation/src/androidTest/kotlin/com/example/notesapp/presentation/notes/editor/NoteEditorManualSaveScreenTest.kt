package com.example.notesapp.presentation.notes.editor

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.ThemeTestColors
import com.example.notesapp.presentation.assertColorEquals
import com.example.notesapp.presentation.dominantColor
import com.example.notesapp.presentation.theme.NotesAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Botón Guardar, línea de estado y diálogo de salida con el guardado automático apagado (spec 009), en ambos temas. */
@RunWith(Parameterized::class)
class NoteEditorManualSaveScreenTest(private val darkTheme: Boolean) {

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
        isManualSave: Boolean = true,
        hasUnsavedChanges: Boolean = false,
        saveStatus: SaveStatus = SaveStatus.Idle,
        isLeaveDialogVisible: Boolean = false,
        color: NoteColor = NoteColor.NONE,
    ) = NoteEditorUiState(
        isNewNote = false,
        title = TextFieldValue("Reunión de equipo"),
        content = TextFieldValue("Menos ruido, más foco."),
        color = color,
        saveStatus = saveStatus,
        isManualSave = isManualSave,
        hasUnsavedChanges = hasUnsavedChanges,
        isLeaveDialogVisible = isLeaveDialogVisible,
    )

    @Test
    fun given_auto_save_on_when_displayed_then_save_button_does_not_exist() {
        setContent(noteState(isManualSave = false, hasUnsavedChanges = true))

        node(NoteEditorTestTags.SAVE).assertDoesNotExist()
        node(NoteEditorTestTags.PIN).assertIsDisplayed()
    }

    @Test
    fun given_manual_save_when_displayed_then_save_button_is_labelled_button_next_to_pin() {
        setContent(noteState(hasUnsavedChanges = true))

        val save = node(NoteEditorTestTags.SAVE)
            .assertIsDisplayed()
            .assertTextEquals(string(R.string.note_editor_save))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
        val saveBounds = save.getUnclippedBoundsInRoot()
        val pinBounds = node(NoteEditorTestTags.PIN).getUnclippedBoundsInRoot()
        assertTrue("Guardar va a la izquierda de Fijar", saveBounds.right <= pinBounds.left)
        assertTrue("Guardar y Fijar comparten la barra", saveBounds.top < pinBounds.bottom && pinBounds.top < saveBounds.bottom)
    }

    @Test
    fun given_manual_save_without_changes_when_displayed_then_save_button_is_disabled_and_sends_nothing() {
        setContent(noteState(hasUnsavedChanges = false))

        node(NoteEditorTestTags.SAVE)
            .assertIsDisplayed()
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
            .performClick()
        composeRule.waitForIdle()

        assertTrue(userIntents.isEmpty())
    }

    @Test
    fun given_manual_save_with_changes_when_save_clicked_then_sends_save_clicked() {
        setContent(noteState(hasUnsavedChanges = true, saveStatus = SaveStatus.Unsaved))

        node(NoteEditorTestTags.SAVE)
            .assertIsEnabled()
            .assert(!SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
            .performClick()

        assertEquals(listOf<NoteEditorIntent>(NoteEditorIntent.SaveClicked), userIntents)
    }

    @Test
    fun given_changes_saved_when_state_changes_then_save_button_becomes_disabled() {
        var state by mutableStateOf(noteState(hasUnsavedChanges = true, saveStatus = SaveStatus.Unsaved))
        composeRule.setContent {
            NotesAppTheme(darkTheme = darkTheme) {
                NoteEditorScreen(state = state, onIntent = { intents += it })
            }
        }
        node(NoteEditorTestTags.SAVE).assertIsEnabled()

        state = state.copy(hasUnsavedChanges = false, saveStatus = SaveStatus.Saved)

        node(NoteEditorTestTags.SAVE).assertIsNotEnabled()
        statusText(R.string.note_editor_saved).assertIsDisplayed()
    }

    @Test
    fun given_manual_save_when_displayed_then_save_button_touch_target_is_at_least_44_dp() {
        setContent(noteState(hasUnsavedChanges = true))

        node(NoteEditorTestTags.SAVE).assertWidthIsAtLeast(MIN_TOUCH_TARGET).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
    }

    @Test
    fun given_manual_save_while_loading_when_displayed_then_save_button_does_not_exist() {
        setContent(NoteEditorUiState(isNewNote = false, isLoading = true, isManualSave = true))

        node(NoteEditorTestTags.SAVE).assertDoesNotExist()
    }

    @Test
    fun given_note_not_found_in_manual_save_when_displayed_then_save_button_does_not_exist() {
        setContent(NoteEditorUiState(isNewNote = false, loadError = LoadError.NotFound, isManualSave = true))

        node(NoteEditorTestTags.SAVE).assertDoesNotExist()
    }

    @Test
    fun given_unsaved_status_when_displayed_then_status_line_shows_unsaved_changes() {
        setContent(noteState(hasUnsavedChanges = true, saveStatus = SaveStatus.Unsaved))

        node(NoteEditorTestTags.SAVE_STATUS).assertIsDisplayed()
        statusText(R.string.note_editor_unsaved).assertIsDisplayed()
        statusText(R.string.note_editor_saved).assertDoesNotExist()
        statusText(R.string.note_editor_saving).assertDoesNotExist()
    }

    @Test
    fun given_saved_status_in_manual_save_when_displayed_then_status_line_shows_all_changes_saved() {
        setContent(noteState(saveStatus = SaveStatus.Saved))

        statusText(R.string.note_editor_saved).assertIsDisplayed()
        statusText(R.string.note_editor_unsaved).assertDoesNotExist()
    }

    @Test
    fun given_idle_status_in_manual_save_when_displayed_then_status_line_is_hidden() {
        setContent(noteState(saveStatus = SaveStatus.Idle))

        node(NoteEditorTestTags.SAVE_STATUS).assertDoesNotExist()
    }

    @Test
    fun given_leave_dialog_hidden_when_displayed_then_dialog_does_not_exist() {
        setContent(noteState(hasUnsavedChanges = true, saveStatus = SaveStatus.Unsaved))

        node(NoteEditorTestTags.LEAVE_DIALOG).assertDoesNotExist()
    }

    @Test
    fun given_leave_dialog_visible_when_displayed_then_shows_title_message_and_three_options() {
        setContent(noteState(hasUnsavedChanges = true, saveStatus = SaveStatus.Unsaved, isLeaveDialogVisible = true))

        node(NoteEditorTestTags.LEAVE_DIALOG).assertIsDisplayed()
        composeRule.onNode(hasText(string(R.string.note_editor_leave_title)) and inLeaveDialog).assertIsDisplayed()
        composeRule.onNode(hasText(string(R.string.note_editor_leave_message)) and inLeaveDialog).assertIsDisplayed()
        node(NoteEditorTestTags.LEAVE_SAVE).assertIsDisplayed().assertTextEquals(string(R.string.note_editor_leave_save))
        node(NoteEditorTestTags.LEAVE_DISCARD).assertIsDisplayed().assertTextEquals(string(R.string.note_editor_leave_discard))
        node(NoteEditorTestTags.LEAVE_CANCEL).assertIsDisplayed().assertTextEquals(string(R.string.note_editor_leave_cancel))
    }

    @Test
    fun given_leave_dialog_when_save_clicked_then_sends_leave_save_clicked() {
        setContent(noteState(hasUnsavedChanges = true, isLeaveDialogVisible = true))

        node(NoteEditorTestTags.LEAVE_SAVE).performClick()

        assertEquals(listOf<NoteEditorIntent>(NoteEditorIntent.LeaveSaveClicked), userIntents)
    }

    @Test
    fun given_leave_dialog_when_discard_clicked_then_sends_leave_discard_clicked() {
        setContent(noteState(hasUnsavedChanges = true, isLeaveDialogVisible = true))

        node(NoteEditorTestTags.LEAVE_DISCARD).performClick()

        assertEquals(listOf<NoteEditorIntent>(NoteEditorIntent.LeaveDiscardClicked), userIntents)
    }

    @Test
    fun given_leave_dialog_when_cancel_clicked_then_sends_leave_dialog_dismissed() {
        setContent(noteState(hasUnsavedChanges = true, isLeaveDialogVisible = true))

        node(NoteEditorTestTags.LEAVE_CANCEL).performClick()

        assertEquals(listOf<NoteEditorIntent>(NoteEditorIntent.LeaveDialogDismissed), userIntents)
    }

    @Test
    fun given_leave_dialog_when_closed_with_system_back_then_sends_leave_dialog_dismissed() {
        setContent(noteState(hasUnsavedChanges = true, isLeaveDialogVisible = true))
        node(NoteEditorTestTags.LEAVE_DIALOG).assertIsDisplayed()

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()

        assertEquals(listOf<NoteEditorIntent>(NoteEditorIntent.LeaveDialogDismissed), userIntents)
    }

    @Test
    @SdkSuppress(minSdkVersion = CAPTURE_MIN_SDK)
    fun given_leave_dialog_when_displayed_then_uses_theme_surface_like_delete_dialog() {
        setContent(noteState(hasUnsavedChanges = true, isLeaveDialogVisible = true))

        val dialog = composeRule.onNode(isDialog()).captureToImage()

        assertColorEquals(
            "Fondo del diálogo de salida",
            if (darkTheme) ThemeTestColors.DarkSurface else ThemeTestColors.LightSurface,
            dialog.dominantColor(),
        )
    }

    @Test
    fun given_manual_save_with_note_color_when_displayed_then_save_button_is_shown_and_enabled() {
        setContent(noteState(hasUnsavedChanges = true, color = NoteColor.ROSE))

        node(NoteEditorTestTags.SAVE).assertIsDisplayed().assertIsEnabled()
    }

    /** La línea de estado no fusiona a sus hijos: el texto es un nodo dentro de ella. */
    private fun statusText(id: Int) = composeRule.onNode(
        hasText(string(id)) and hasAnyAncestor(hasTestTag(NoteEditorTestTags.SAVE_STATUS)),
        useUnmergedTree = true,
    )

    private val inLeaveDialog = hasAnyAncestor(hasTestTag(NoteEditorTestTags.LEAVE_DIALOG))

    private fun node(tag: String): SemanticsNodeInteraction = composeRule.onNodeWithTag(tag)

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "darkTheme={0}")
        fun themes(): List<Boolean> = listOf(true, false)

        private const val CAPTURE_MIN_SDK = 26
        private val MIN_TOUCH_TARGET = 44.dp
    }
}
