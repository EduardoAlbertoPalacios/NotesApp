package com.example.notesapp.presentation.notes.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import com.example.notesapp.domain.notes.NoteColor
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.notesapp.domain.notes.ChecklistItem
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.TextFormat
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.notes.common.NoteDateLabel
import com.example.notesapp.presentation.theme.NotesAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteEditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<NoteEditorIntent>()

    /** El campo de contenido avisa de su foco incluso al componerse: se ignora salvo en las pruebas de foco. */
    private val userIntents: List<NoteEditorIntent>
        get() = intents.filterNot { it is NoteEditorIntent.ContentFocusChanged }

    private fun setContent(state: NoteEditorUiState) {
        composeRule.setContent {
            val currentState by remember { mutableStateOf(state) }
            NotesAppTheme {
                NoteEditorScreen(state = currentState, onIntent = { intents += it })
            }
        }
    }

    @Test
    fun given_note_when_displayed_then_shows_title_content_date_and_saved_status() {
        setContent(
            NoteEditorUiState(
                title = "Reunión de equipo",
                content = TextFieldValue("Menos ruido, más foco."),
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
            listOf(NoteEditorIntent.TitleChanged("Ideas"), NoteEditorIntent.ContentChanged(TextFieldValue("Libreta"))),
            userIntents.withoutSelectionOnlyContentChanges(),
        )
    }

    /**
     * La selección depende del cursor: solo se compara el texto. Además, el campo de contenido puede emitir
     * cambios solo de selección (mismo texto) según el momento; se descartan los `ContentChanged` que repiten
     * el texto del `ContentChanged` inmediatamente anterior.
     */
    private fun List<NoteEditorIntent>.withoutSelectionOnlyContentChanges(): List<NoteEditorIntent> =
        map { intent ->
            if (intent is NoteEditorIntent.ContentChanged) {
                NoteEditorIntent.ContentChanged(TextFieldValue(intent.content.text))
            } else {
                intent
            }
        }.fold(emptyList()) { acc, intent ->
            val previous = acc.lastOrNull()
            if (intent is NoteEditorIntent.ContentChanged && previous == intent) acc else acc + intent
        }

    @Test
    fun given_editor_when_back_and_pin_clicked_then_sends_intents() {
        setContent(NoteEditorUiState(title = "Ideas"))

        composeRule.onNodeWithTag(NoteEditorTestTags.PIN).performClick()
        composeRule.onNodeWithTag(NoteEditorTestTags.BACK).performClick()

        assertEquals(listOf(NoteEditorIntent.PinClicked, NoteEditorIntent.BackClicked), userIntents)
    }

    @Test
    fun given_more_options_when_delete_selected_then_sends_delete_clicked() {
        setContent(NoteEditorUiState(title = "Ideas"))

        composeRule.onNodeWithTag(NoteEditorTestTags.MORE).performClick()
        composeRule.onNodeWithTag(NoteEditorTestTags.DELETE).performClick()

        assertEquals(listOf<NoteEditorIntent>(NoteEditorIntent.DeleteClicked), userIntents)
    }

    @Test
    fun given_delete_dialog_when_confirmed_or_cancelled_then_sends_intents() {
        setContent(NoteEditorUiState(title = "Ideas", isDeleteDialogVisible = true))

        composeRule.onNodeWithText("¿Eliminar esta nota?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.onNodeWithText("Eliminar").performClick()

        assertEquals(listOf(NoteEditorIntent.DeleteDismissed, NoteEditorIntent.DeleteConfirmed), userIntents)
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
            userIntents,
        )
    }

    @Test
    fun given_selected_category_when_displayed_then_only_that_option_is_selected() {
        setContent(NoteEditorUiState(title = "Ideas", category = NoteCategory.PERSONAL))

        composeRule.onNodeWithTag(NoteEditorTestTags.CATEGORY_OPTION + NoteCategory.PERSONAL.name).assertIsSelected()
        composeRule.onNodeWithTag(NoteEditorTestTags.CATEGORY_OPTION + NoteCategory.WORK.name).assertIsNotSelected()
    }

    @Test
    fun given_editor_when_image_button_clicked_then_sends_add_image_clicked() {
        setContent(NoteEditorUiState(title = "Ideas"))

        composeRule.onNodeWithTag(NoteEditorTestTags.TOOLBAR_IMAGE).assertIsEnabled().performClick()

        assertEquals(listOf<NoteEditorIntent>(NoteEditorIntent.AddImageClicked), userIntents)
    }

    @Test
    fun given_editor_when_disabled_toolbar_buttons_clicked_then_sends_nothing() {
        setContent(NoteEditorUiState(title = "Ideas"))

        listOf(
            R.string.note_editor_toolbar_text,
            R.string.note_editor_toolbar_checklist,
            R.string.note_editor_toolbar_undo,
        ).forEach { labelRes ->
            composeRule.onNodeWithContentDescription(string(labelRes)).assertIsNotEnabled().performClick()
        }

        assertTrue(userIntents.isEmpty())
    }

    @Test
    fun given_note_with_images_when_displayed_then_shows_each_image() {
        setContent(NoteEditorUiState(title = "Escapada", images = listOf(firstImage, secondImage)))

        composeRule.onNodeWithTag(NoteEditorTestTags.IMAGE + firstImage.id).assertExists()
        composeRule.onNodeWithTag(NoteEditorTestTags.IMAGE + secondImage.id).assertExists()
    }

    @Test
    fun given_note_without_images_when_displayed_then_shows_no_image() {
        setContent(NoteEditorUiState(title = "Escapada"))

        composeRule.onNodeWithTag(NoteEditorTestTags.IMAGE + firstImage.id).assertDoesNotExist()
        composeRule.onNodeWithTag(NoteEditorTestTags.REMOVE_IMAGE + firstImage.id).assertDoesNotExist()
    }

    @Test
    fun given_note_with_images_when_remove_clicked_then_sends_remove_image_clicked_for_that_image() {
        setContent(NoteEditorUiState(title = "Escapada", images = listOf(firstImage, secondImage)))

        composeRule.onNodeWithTag(NoteEditorTestTags.REMOVE_IMAGE + secondImage.id).performScrollTo().performClick()

        assertEquals(listOf<NoteEditorIntent>(NoteEditorIntent.RemoveImageClicked(secondImage)), userIntents)
    }

    @Test
    fun given_loading_note_when_displayed_then_hides_toolbar() {
        setContent(NoteEditorUiState(isNewNote = false, isLoading = true))

        composeRule.onNodeWithTag(NoteEditorTestTags.TOOLBAR_IMAGE).assertDoesNotExist()
    }

    @Test
    fun given_content_not_focused_when_displayed_then_format_button_is_disabled() {
        setContent(NoteEditorUiState(title = "Ideas", isContentFocused = false))

        composeRule.onNodeWithTag(NoteEditorTestTags.TOOLBAR_FORMAT).assertIsNotEnabled().performClick()

        assertTrue(userIntents.isEmpty())
    }

    @Test
    fun given_content_focused_when_format_button_clicked_then_sends_format_button_clicked() {
        setContent(NoteEditorUiState(title = "Ideas", isContentFocused = true))

        composeRule.onNodeWithTag(NoteEditorTestTags.TOOLBAR_FORMAT).assertIsEnabled().performClick()

        assertEquals(listOf<NoteEditorIntent>(NoteEditorIntent.FormatButtonClicked), userIntents)
    }

    @Test
    fun given_focus_moves_between_content_and_title_when_tapped_then_format_button_follows_content_focus() {
        composeRule.setContent {
            var state by remember { mutableStateOf(NoteEditorUiState(isNewNote = false, title = "Ideas")) }
            NotesAppTheme {
                NoteEditorScreen(
                    state = state,
                    onIntent = { intent ->
                        intents += intent
                        state = when (intent) {
                            is NoteEditorIntent.TitleChanged -> state.copy(title = intent.title)
                            is NoteEditorIntent.ContentChanged -> state.copy(content = intent.content)
                            is NoteEditorIntent.ContentFocusChanged -> state.copy(isContentFocused = intent.isFocused)
                            else -> state
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithTag(NoteEditorTestTags.CONTENT).performClick()
        composeRule.onNodeWithTag(NoteEditorTestTags.TOOLBAR_FORMAT).assertIsEnabled()
        assertEquals(NoteEditorIntent.ContentFocusChanged(isFocused = true), intents.last { it is NoteEditorIntent.ContentFocusChanged })

        composeRule.onNodeWithTag(NoteEditorTestTags.TITLE).performClick()
        composeRule.onNodeWithTag(NoteEditorTestTags.TOOLBAR_FORMAT).assertIsNotEnabled()
        assertEquals(NoteEditorIntent.ContentFocusChanged(isFocused = false), intents.last { it is NoteEditorIntent.ContentFocusChanged })
    }

    @Test
    fun given_format_menu_hidden_when_displayed_then_shows_no_options() {
        setContent(NoteEditorUiState(title = "Ideas", isContentFocused = true))

        TextFormat.entries.forEach { format ->
            composeRule.onNodeWithTag(NoteEditorTestTags.FORMAT_OPTION + format.name).assertDoesNotExist()
        }
    }

    @Test
    fun given_format_menu_visible_when_displayed_then_shows_three_options_with_their_labels() {
        setContent(NoteEditorUiState(title = "Ideas", isContentFocused = true, isFormatMenuVisible = true))

        mapOf(
            TextFormat.BOLD to R.string.note_editor_format_bold,
            TextFormat.UNDERLINE to R.string.note_editor_format_underline,
            TextFormat.STRIKETHROUGH to R.string.note_editor_format_strikethrough,
        ).forEach { (format, labelRes) ->
            composeRule.onNodeWithTag(NoteEditorTestTags.FORMAT_OPTION + format.name)
                .assertIsDisplayed()
                .assertTextEquals(string(labelRes))
        }
    }

    @Test
    fun given_format_menu_visible_when_option_clicked_then_sends_format_selected() {
        setContent(NoteEditorUiState(title = "Ideas", isContentFocused = true, isFormatMenuVisible = true))

        composeRule.onNodeWithTag(NoteEditorTestTags.FORMAT_OPTION + TextFormat.UNDERLINE.name).performClick()

        assertEquals(listOf<NoteEditorIntent>(NoteEditorIntent.FormatSelected(TextFormat.UNDERLINE)), userIntents)
    }

    @Test
    fun given_active_formats_when_menu_visible_then_options_describe_active_or_inactive_state() {
        setContent(
            NoteEditorUiState(
                title = "Ideas",
                isContentFocused = true,
                isFormatMenuVisible = true,
                activeFormats = setOf(TextFormat.BOLD, TextFormat.STRIKETHROUGH),
            ),
        )

        val active = string(R.string.note_editor_format_active)
        val inactive = string(R.string.note_editor_format_inactive)
        composeRule.onNodeWithTag(NoteEditorTestTags.FORMAT_OPTION + TextFormat.BOLD.name).assert(hasStateDescription(active))
        composeRule.onNodeWithTag(NoteEditorTestTags.FORMAT_OPTION + TextFormat.UNDERLINE.name).assert(hasStateDescription(inactive))
        composeRule.onNodeWithTag(NoteEditorTestTags.FORMAT_OPTION + TextFormat.STRIKETHROUGH.name)
            .assert(hasStateDescription(active))
    }

    @Test
    fun given_editor_when_color_button_clicked_then_sends_color_button_clicked() {
        setContent(NoteEditorUiState(title = "Ideas"))

        composeRule.onNodeWithTag(NoteEditorTestTags.TOOLBAR_COLOR)
            .assertContentDescriptionEquals(string(R.string.note_editor_toolbar_color))
            .assertIsEnabled()
            .performClick()

        assertEquals(listOf<NoteEditorIntent>(NoteEditorIntent.ColorButtonClicked), userIntents)
    }

    @Test
    fun given_picker_color_null_when_displayed_then_color_sheet_is_hidden() {
        setContent(NoteEditorUiState(title = "Ideas", pickerColor = null))

        composeRule.onNodeWithTag(NoteEditorTestTags.COLOR_SHEET).assertDoesNotExist()
    }

    @Test
    fun given_picker_color_when_displayed_then_sheet_shows_title_description_and_eight_options_with_labels() {
        setContent(NoteEditorUiState(title = "Ideas", pickerColor = NoteColor.AQUA))

        composeRule.onNodeWithTag(NoteEditorTestTags.COLOR_SHEET).assertIsDisplayed()
        composeRule.onNode(hasText(string(R.string.note_editor_toolbar_color)) and inColorSheet).assertIsDisplayed()
        composeRule.onNode(hasText(string(R.string.note_color_sheet_description)) and inColorSheet).assertIsDisplayed()
        composeRule.onAllNodes(hasRole(Role.RadioButton) and inColorSheet).assertCountEquals(NoteColor.entries.size)
        colorLabels.forEach { (color, labelRes) ->
            colorOption(color).assertIsDisplayed().assertTextEquals(string(labelRes))
        }
    }

    @Test
    fun given_picker_color_when_displayed_then_options_follow_palette_order_in_two_rows_of_four() {
        setContent(NoteEditorUiState(title = "Ideas", pickerColor = NoteColor.AQUA))

        val bounds = NoteColor.entries.map { colorOption(it).getUnclippedBoundsInRoot() }

        val (firstRow, secondRow) = bounds.chunked(4)
        listOf(firstRow, secondRow).forEach { row ->
            assertTrue(row.all { it.top == row.first().top })
            assertTrue(row.zipWithNext().all { (left, right) -> left.right <= right.left })
        }
        assertTrue(firstRow.first().bottom <= secondRow.first().top)
    }

    @Test
    fun given_picker_color_when_displayed_then_only_that_option_is_selected() {
        setContent(NoteEditorUiState(title = "Ideas", color = NoteColor.AQUA, pickerColor = NoteColor.LAVENDER))

        colorOption(NoteColor.LAVENDER).assertIsSelected()
        NoteColor.entries.filter { it != NoteColor.LAVENDER }.forEach { color ->
            colorOption(color).assertIsNotSelected()
        }
    }

    @Test
    fun given_picker_color_none_when_displayed_then_none_option_is_selected() {
        setContent(NoteEditorUiState(title = "Ideas", pickerColor = NoteColor.NONE))

        colorOption(NoteColor.NONE).assertIsSelected()
        colorOption(NoteColor.AQUA).assertIsNotSelected()
    }

    @Test
    fun given_color_sheet_when_option_clicked_then_sends_picker_color_selected() {
        setContent(NoteEditorUiState(title = "Ideas", pickerColor = NoteColor.AQUA))

        colorOption(NoteColor.ROSE).performClick()
        colorOption(NoteColor.NONE).performClick()

        assertEquals(
            listOf(
                NoteEditorIntent.PickerColorSelected(NoteColor.ROSE),
                NoteEditorIntent.PickerColorSelected(NoteColor.NONE),
            ),
            userIntents,
        )
    }

    @Test
    fun given_note_with_title_when_sheet_displayed_then_preview_shows_title_and_color_caption() {
        setContent(NoteEditorUiState(title = "Reunión de equipo", pickerColor = NoteColor.PEACH))

        composeRule.onNode(hasText("Reunión de equipo") and inColorPreview).assertIsDisplayed()
        composeRule.onNode(hasText(string(R.string.note_color_preview_untitled)) and inColorPreview).assertDoesNotExist()
        composeRule.onNode(
            hasText(string(R.string.note_color_preview_caption, string(R.string.note_color_peach))) and inColorPreview,
        ).assertIsDisplayed()
    }

    @Test
    fun given_note_without_title_when_sheet_displayed_then_preview_shows_untitled_and_color_caption() {
        setContent(NoteEditorUiState(title = "", pickerColor = NoteColor.NONE))

        composeRule.onNode(hasText(string(R.string.note_color_preview_untitled)) and inColorPreview).assertIsDisplayed()
        composeRule.onNode(
            hasText(string(R.string.note_color_preview_caption, string(R.string.note_color_none))) and inColorPreview,
        ).assertIsDisplayed()
    }

    @Test
    fun given_color_sheet_when_apply_clicked_then_sends_color_applied_after_closing() {
        setContent(NoteEditorUiState(title = "Ideas", pickerColor = NoteColor.MINT))

        composeRule.onNodeWithTag(NoteEditorTestTags.COLOR_APPLY)
            .assertTextEquals(string(R.string.note_color_apply))
            .performClick()
        composeRule.waitUntil(timeoutMillis = SHEET_TIMEOUT_MS) { userIntents.isNotEmpty() }
        composeRule.waitForIdle()

        assertEquals(listOf<NoteEditorIntent>(NoteEditorIntent.ColorApplied), userIntents)
    }

    @Test
    fun given_color_sheet_when_close_clicked_then_sends_color_picker_dismissed_after_closing() {
        setContent(NoteEditorUiState(title = "Ideas", pickerColor = NoteColor.MINT))

        composeRule.onNodeWithTag(NoteEditorTestTags.COLOR_CLOSE).performClick()
        composeRule.waitUntil(timeoutMillis = SHEET_TIMEOUT_MS) { userIntents.isNotEmpty() }
        composeRule.waitForIdle()

        assertEquals(listOf<NoteEditorIntent>(NoteEditorIntent.ColorPickerDismissed), userIntents)
    }

    @Test
    fun given_color_sheet_when_displayed_then_options_and_close_have_accessible_labels() {
        setContent(NoteEditorUiState(title = "Ideas", pickerColor = NoteColor.AQUA))

        colorLabels.forEach { (color, labelRes) ->
            colorOption(color)
                .assert(hasRole(Role.RadioButton))
                .assert(hasText(string(labelRes)))
                .assertHasClickAction()
        }
        composeRule.onNodeWithTag(NoteEditorTestTags.COLOR_CLOSE)
            .assertContentDescriptionEquals(string(R.string.note_color_close))
            .assertHasClickAction()
    }

    @Test
    fun given_content_focused_when_checklist_button_clicked_then_sends_checklist_button_clicked() {
        setContent(NoteEditorUiState(title = "Ideas", isContentFocused = true))

        composeRule.onNodeWithTag(NoteEditorTestTags.TOOLBAR_CHECKLIST)
            .assertContentDescriptionEquals(string(R.string.note_editor_toolbar_checklist))
            .assertIsEnabled()
            .performClick()

        assertEquals(listOf<NoteEditorIntent>(NoteEditorIntent.ChecklistButtonClicked), userIntents)
    }

    @Test
    fun given_content_not_focused_when_checklist_button_clicked_then_is_disabled_and_sends_nothing() {
        setContent(NoteEditorUiState(title = "Ideas", isContentFocused = false))

        composeRule.onNodeWithTag(NoteEditorTestTags.TOOLBAR_CHECKLIST).assertIsNotEnabled().performClick()

        assertTrue(userIntents.isEmpty())
    }

    @Test
    fun given_tasks_when_displayed_then_each_task_has_a_checkbox_with_its_label_and_state() {
        setContent(checklistState)

        taskCheckbox(0)
            .assert(hasRole(Role.Checkbox))
            .assertContentDescriptionEquals("Tomates")
            .assert(hasStateDescription(string(R.string.note_editor_task_pending)))
            .assertIsOff()
        taskCheckbox(1)
            .assert(hasRole(Role.Checkbox))
            .assertContentDescriptionEquals("Pan")
            .assert(hasStateDescription(string(R.string.note_editor_task_done)))
            .assertIsOn()
    }

    @Test
    fun given_empty_task_when_displayed_then_checkbox_is_labelled_as_empty_task() {
        setContent(
            NoteEditorUiState(
                title = "Compra",
                content = TextFieldValue("Tomates\n"),
                contentTasks = listOf(ChecklistItem(line = 0, isDone = false), ChecklistItem(line = 1, isDone = false)),
            ),
        )

        taskCheckbox(1).assertContentDescriptionEquals(string(R.string.note_editor_task_empty))
    }

    @Test
    fun given_tasks_when_checkboxes_clicked_then_sends_task_toggled_for_each_line() {
        setContent(checklistState)

        taskCheckbox(1).performClick()
        taskCheckbox(0).performClick()

        assertEquals(
            listOf(NoteEditorIntent.TaskToggled(line = 1), NoteEditorIntent.TaskToggled(line = 0)),
            userIntents,
        )
    }

    @Test
    fun given_tasks_when_displayed_then_each_checkbox_touch_target_is_at_least_44_dp() {
        setContent(checklistState)

        listOf(0, 1).forEach { line ->
            taskCheckbox(line).assertWidthIsAtLeast(MIN_TOUCH_TARGET).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        }
    }

    private val checklistState = NoteEditorUiState(
        title = "Compra",
        content = TextFieldValue("Tomates\nPan"),
        contentTasks = listOf(ChecklistItem(line = 0, isDone = false), ChecklistItem(line = 1, isDone = true)),
    )

    private fun taskCheckbox(line: Int) = composeRule.onNodeWithTag(NoteEditorTestTags.TASK_CHECKBOX + line)

    private fun colorOption(color: NoteColor) = composeRule.onNodeWithTag(NoteEditorTestTags.COLOR_OPTION + color.name)

    private val inColorSheet = hasAnyAncestor(hasTestTag(NoteEditorTestTags.COLOR_SHEET))

    private val inColorPreview = hasAnyAncestor(hasTestTag(NoteEditorTestTags.COLOR_PREVIEW))

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private val colorLabels = mapOf(
        NoteColor.NONE to R.string.note_color_none,
        NoteColor.AQUA to R.string.note_color_aqua,
        NoteColor.ROSE to R.string.note_color_rose,
        NoteColor.VANILLA to R.string.note_color_vanilla,
        NoteColor.LAVENDER to R.string.note_color_lavender,
        NoteColor.MINT to R.string.note_color_mint,
        NoteColor.PEACH to R.string.note_color_peach,
        NoteColor.SAND to R.string.note_color_sand,
    )

    private fun hasStateDescription(value: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    private val firstImage = NoteImage(id = 1, path = "/no/existe/a.jpg")
    private val secondImage = NoteImage(id = 2, path = "/no/existe/b.jpg")

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun string(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private companion object {
        const val SHEET_TIMEOUT_MS = 5_000L
        val MIN_TOUCH_TARGET = 44.dp
    }
}
