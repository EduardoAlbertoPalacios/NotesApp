package com.example.notesapp.presentation.notes.trash

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.trash.TrashRetention
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.theme.NotesAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Pantalla de la papelera (spec 012), en ambos temas. */
@RunWith(Parameterized::class)
class TrashScreenTest(private val darkTheme: Boolean) {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<TrashIntent>()

    private fun setContent(state: TrashUiState) {
        composeRule.setContent {
            NotesAppTheme(darkTheme = darkTheme) {
                TrashScreen(state = state, onIntent = { intents += it })
            }
        }
    }

    private fun item(
        id: Long,
        title: String = "Nota $id",
        excerpt: String = "Extracto $id",
        color: NoteColor = NoteColor.MINT,
        daysLeft: Int? = 29,
    ) = TrashItemUi(id = id, title = title, excerpt = excerpt, color = color, daysLeft = daysLeft)

    private fun loadedState(
        notes: List<TrashItemUi> = listOf(
            item(id = 1, title = "La compra", excerpt = "Leche, pan y huevos", daysLeft = 29),
            item(id = 2, title = "", excerpt = "Llamar al fontanero", color = NoteColor.NONE, daysLeft = 1),
            item(id = 3, title = "Ideas para el viaje", excerpt = "", color = NoteColor.LAVENDER, daysLeft = 0),
        ),
        retention: TrashRetention = TrashRetention.DAYS_30,
    ) = TrashUiState(isLoading = false, notes = notes, retention = retention)

    @Test
    fun given_loading_when_displayed_then_shows_progress_without_list_header_or_messages() {
        setContent(TrashUiState(isLoading = true))

        node(TrashTestTags.LOADING).assertIsDisplayed()
        node(TrashTestTags.RETENTION).assertIsDisplayed()
        node(TrashTestTags.LIST).assertDoesNotExist()
        node(TrashTestTags.COUNT).assertDoesNotExist()
        node(TrashTestTags.EMPTY_ACTION).assertDoesNotExist()
        node(TrashTestTags.EMPTY).assertDoesNotExist()
        node(TrashTestTags.LOAD_ERROR).assertDoesNotExist()
    }

    @Test
    fun given_no_notes_when_displayed_then_shows_empty_message_without_list_or_empty_action() {
        setContent(loadedState(notes = emptyList()))

        node(TrashTestTags.EMPTY).assertIsDisplayed().assertTextEquals(string(R.string.trash_empty))
        node(TrashTestTags.LIST).assertDoesNotExist()
        node(TrashTestTags.COUNT).assertDoesNotExist()
        node(TrashTestTags.EMPTY_ACTION).assertDoesNotExist()
        node(TrashTestTags.LOADING).assertDoesNotExist()
        node(TrashTestTags.LOAD_ERROR).assertDoesNotExist()
    }

    @Test
    fun given_load_failed_when_displayed_then_shows_load_error_instead_of_empty_message() {
        setContent(TrashUiState(isLoading = false, loadFailed = true))

        node(TrashTestTags.LOAD_ERROR).assertIsDisplayed().assertTextEquals(string(R.string.trash_load_error))
        composeRule.onNodeWithText(string(R.string.trash_empty)).assertDoesNotExist()
        node(TrashTestTags.LIST).assertDoesNotExist()
        node(TrashTestTags.COUNT).assertDoesNotExist()
        node(TrashTestTags.EMPTY_ACTION).assertDoesNotExist()
        node(TrashTestTags.LOADING).assertDoesNotExist()
    }

    @Test
    fun given_notes_when_displayed_then_cards_show_title_or_untitled_and_excerpt() {
        setContent(loadedState())

        card(1).assertIsDisplayed()
            .assert(hasText("La compra"))
            .assert(hasText("Leche, pan y huevos"))
        card(2).assertIsDisplayed()
            .assert(hasText(string(R.string.trash_untitled)))
            .assert(hasText("Llamar al fontanero"))
        card(3).assertIsDisplayed().assert(hasText("Ideas para el viaje"))
        node(TrashTestTags.EMPTY).assertDoesNotExist()
    }

    @Test
    fun given_days_left_when_displayed_then_cards_show_days_tomorrow_or_today() {
        setContent(loadedState())

        card(1).assert(hasText(plural(R.plurals.trash_days_left, 29)))
        card(2).assert(hasText(string(R.string.trash_days_left_tomorrow)))
            .assert(!hasText(plural(R.plurals.trash_days_left, 1)))
        card(3).assert(hasText(string(R.string.trash_days_left_today)))
            .assert(!hasText(plural(R.plurals.trash_days_left, 0)))
    }

    @Test
    fun given_retention_never_when_displayed_then_cards_show_no_days_left() {
        setContent(loadedState(notes = listOf(item(id = 1, daysLeft = null)), retention = TrashRetention.NEVER))

        card(1).assertIsDisplayed()

        assertEquals(listOf("Nota 1", "Extracto 1"), card(1).texts())
    }

    @Test
    fun given_notes_when_displayed_then_cards_have_no_pinned_indicator() {
        setContent(loadedState())

        composeRule.onNodeWithContentDescription(string(R.string.note_list_pinned)).assertDoesNotExist()
    }

    @Test
    fun given_three_notes_when_displayed_then_header_shows_plural_count_and_empty_action() {
        setContent(loadedState())

        node(TrashTestTags.COUNT).assertIsDisplayed().assertTextEquals(plural(R.plurals.trash_count, 3))
        node(TrashTestTags.EMPTY_ACTION).assertIsDisplayed().assert(hasText(string(R.string.trash_empty_action)))
    }

    @Test
    fun given_one_note_when_displayed_then_header_shows_singular_count() {
        setContent(loadedState(notes = listOf(item(id = 1))))

        node(TrashTestTags.COUNT).assertTextEquals(plural(R.plurals.trash_count, 1))
    }

    @Test
    fun given_notes_when_empty_action_clicked_then_sends_empty_clicked() {
        setContent(loadedState())

        node(TrashTestTags.EMPTY_ACTION)
            .assert(hasRole(Role.Button))
            .performClick()

        assertEquals(listOf<TrashIntent>(TrashIntent.EmptyClicked), intents)
    }

    @Test
    fun given_notes_when_card_clicked_then_sends_note_clicked_with_its_id() {
        setContent(loadedState())

        card(3).assert(hasRole(Role.Button)).assertHasClickAction().performClick()

        assertEquals(listOf<TrashIntent>(TrashIntent.NoteClicked(3)), intents)
    }

    @Test
    fun given_notes_when_displayed_then_card_click_action_is_labelled_view_actions() {
        setContent(loadedState())

        val onClick = card(1).fetchSemanticsNode().config[SemanticsActions.OnClick]

        assertEquals(string(R.string.trash_note_actions), onClick.label)
    }

    @Test
    fun given_actions_note_when_displayed_then_sheet_shows_title_and_both_actions() {
        setContent(loadedState().copy(actionsNoteId = 1))

        node(TrashTestTags.ACTIONS_SHEET).assertIsDisplayed()
        node(TrashTestTags.ACTIONS_SHEET).assert(hasAnyDescendant(hasText("La compra")))
        node(TrashTestTags.RESTORE).assertIsDisplayed()
            .assert(hasText(string(R.string.trash_restore)))
            .assert(hasRole(Role.Button))
        node(TrashTestTags.DELETE_FOREVER).assertIsDisplayed()
            .assert(hasText(string(R.string.trash_delete_forever)))
            .assert(hasRole(Role.Button))
    }

    @Test
    fun given_actions_for_untitled_note_when_displayed_then_sheet_shows_untitled() {
        setContent(loadedState().copy(actionsNoteId = 2))

        node(TrashTestTags.ACTIONS_SHEET).assert(hasAnyDescendant(hasText(string(R.string.trash_untitled))))
    }

    @Test
    fun given_no_actions_note_when_displayed_then_sheet_does_not_exist() {
        setContent(loadedState())

        node(TrashTestTags.ACTIONS_SHEET).assertDoesNotExist()
        node(TrashTestTags.RESTORE).assertDoesNotExist()
        node(TrashTestTags.DELETE_FOREVER).assertDoesNotExist()
    }

    @Test
    fun given_actions_for_missing_note_when_displayed_then_sheet_does_not_exist() {
        setContent(loadedState().copy(actionsNoteId = 99))

        node(TrashTestTags.ACTIONS_SHEET).assertDoesNotExist()
    }

    @Test
    fun given_actions_sheet_when_restore_clicked_then_sends_restore_clicked() {
        setContent(loadedState().copy(actionsNoteId = 1))

        node(TrashTestTags.RESTORE).performClick()

        assertEquals(listOf<TrashIntent>(TrashIntent.RestoreClicked), intents)
    }

    @Test
    fun given_actions_sheet_when_delete_forever_clicked_then_sends_delete_forever_clicked() {
        setContent(loadedState().copy(actionsNoteId = 1))

        node(TrashTestTags.DELETE_FOREVER).performClick()

        assertEquals(listOf<TrashIntent>(TrashIntent.DeleteForeverClicked), intents)
    }

    @Test
    fun given_pending_delete_when_displayed_then_dialog_shows_its_texts() {
        setContent(loadedState().copy(pendingDeleteId = 1))

        composeRule.onNode(isDialog()).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.trash_delete_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.trash_delete_message)).assertIsDisplayed()
        node(TrashTestTags.DELETE_CONFIRM).assertTextEquals(string(R.string.trash_delete_confirm))
        node(TrashTestTags.DELETE_DISMISS).assertTextEquals(string(R.string.trash_cancel))
    }

    @Test
    fun given_delete_dialog_when_confirmed_then_sends_delete_forever_confirmed() {
        setContent(loadedState().copy(pendingDeleteId = 1))

        node(TrashTestTags.DELETE_CONFIRM).performClick()

        assertEquals(listOf<TrashIntent>(TrashIntent.DeleteForeverConfirmed), intents)
    }

    @Test
    fun given_delete_dialog_when_cancelled_then_sends_delete_forever_dismissed() {
        setContent(loadedState().copy(pendingDeleteId = 1))

        node(TrashTestTags.DELETE_DISMISS).performClick()

        assertEquals(listOf<TrashIntent>(TrashIntent.DeleteForeverDismissed), intents)
    }

    @Test
    fun given_no_pending_confirmation_when_displayed_then_no_dialog_exists() {
        setContent(loadedState())

        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun given_empty_confirm_with_three_notes_when_displayed_then_dialog_shows_plural_message() {
        setContent(loadedState().copy(isEmptyConfirmVisible = true))

        composeRule.onNodeWithText(string(R.string.trash_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithText(plural(R.plurals.trash_empty_message, 3)).assertIsDisplayed()
        node(TrashTestTags.EMPTY_CONFIRM).assertTextEquals(string(R.string.trash_empty_confirm))
        node(TrashTestTags.EMPTY_DISMISS).assertTextEquals(string(R.string.trash_cancel))
    }

    @Test
    fun given_empty_confirm_with_one_note_when_displayed_then_dialog_shows_singular_message() {
        setContent(loadedState(notes = listOf(item(id = 1))).copy(isEmptyConfirmVisible = true))

        composeRule.onNodeWithText(plural(R.plurals.trash_empty_message, 1)).assertIsDisplayed()
    }

    @Test
    fun given_empty_dialog_when_confirmed_then_sends_empty_confirmed() {
        setContent(loadedState().copy(isEmptyConfirmVisible = true))

        node(TrashTestTags.EMPTY_CONFIRM).performClick()

        assertEquals(listOf<TrashIntent>(TrashIntent.EmptyConfirmed), intents)
    }

    @Test
    fun given_empty_dialog_when_cancelled_then_sends_empty_dismissed() {
        setContent(loadedState().copy(isEmptyConfirmVisible = true))

        node(TrashTestTags.EMPTY_DISMISS).performClick()

        assertEquals(listOf<TrashIntent>(TrashIntent.EmptyDismissed), intents)
    }

    @Test
    fun given_pending_shorter_retention_for_two_notes_when_displayed_then_dialog_shows_plural_message() {
        setContent(loadedState().copy(pendingRetention = PendingRetention(TrashRetention.DAYS_7, count = 2)))

        composeRule.onNodeWithText(string(R.string.trash_retention_confirm_title)).assertIsDisplayed()
        composeRule.onNodeWithText(retentionMessage(count = 2, labelRes = R.string.trash_retention_7_days))
            .assertIsDisplayed()
        node(TrashTestTags.RETENTION_CONFIRM).assertTextEquals(string(R.string.trash_retention_confirm))
        node(TrashTestTags.RETENTION_DISMISS).assertTextEquals(string(R.string.trash_cancel))
    }

    @Test
    fun given_pending_shorter_retention_for_one_note_when_displayed_then_dialog_shows_singular_message() {
        setContent(loadedState().copy(pendingRetention = PendingRetention(TrashRetention.DAYS_30, count = 1)))

        composeRule.onNodeWithText(retentionMessage(count = 1, labelRes = R.string.trash_retention_30_days))
            .assertIsDisplayed()
    }

    @Test
    fun given_retention_dialog_when_confirmed_then_sends_retention_confirmed() {
        setContent(loadedState().copy(pendingRetention = PendingRetention(TrashRetention.DAYS_7, count = 2)))

        node(TrashTestTags.RETENTION_CONFIRM).performClick()

        assertEquals(listOf<TrashIntent>(TrashIntent.RetentionConfirmed), intents)
    }

    @Test
    fun given_retention_dialog_when_cancelled_then_sends_retention_dismissed() {
        setContent(loadedState().copy(pendingRetention = PendingRetention(TrashRetention.DAYS_7, count = 2)))

        node(TrashTestTags.RETENTION_DISMISS).performClick()

        assertEquals(listOf<TrashIntent>(TrashIntent.RetentionDismissed), intents)
    }

    @Test
    fun given_notes_when_retention_button_clicked_then_sends_retention_clicked() {
        setContent(loadedState())

        node(TrashTestTags.RETENTION)
            .assertIsDisplayed()
            .assertContentDescriptionEquals(string(R.string.trash_retention))
            .assert(hasRole(Role.Button))
            .performClick()

        assertEquals(listOf<TrashIntent>(TrashIntent.RetentionClicked), intents)
    }

    @Test
    fun given_empty_trash_when_retention_button_clicked_then_sends_retention_clicked() {
        setContent(loadedState(notes = emptyList()))

        node(TrashTestTags.RETENTION).assertIsDisplayed().performClick()

        assertEquals(listOf<TrashIntent>(TrashIntent.RetentionClicked), intents)
    }

    @Test
    fun given_load_failed_when_displayed_then_retention_button_is_visible() {
        setContent(TrashUiState(isLoading = false, loadFailed = true))

        node(TrashTestTags.RETENTION).assertIsDisplayed()
    }

    @Test
    fun given_retention_sheet_when_displayed_then_shows_title_and_four_options_with_current_selected() {
        setContent(loadedState(retention = TrashRetention.DAYS_30).copy(isRetentionSheetVisible = true))

        node(TrashTestTags.RETENTION_SHEET).assertIsDisplayed()
            .assert(hasAnyDescendant(hasText(string(R.string.trash_retention))))
        retentionLabels.forEach { (retention, labelRes) ->
            retentionOption(retention).assertIsDisplayed()
                .assert(hasText(string(labelRes)))
                .assert(hasRole(Role.RadioButton))
        }
        retentionOption(TrashRetention.DAYS_30).assertIsSelected()
        retentionOption(TrashRetention.DAYS_7).assertIsNotSelected()
        retentionOption(TrashRetention.DAYS_90).assertIsNotSelected()
        retentionOption(TrashRetention.NEVER).assertIsNotSelected()
    }

    @Test
    fun given_retention_never_when_sheet_displayed_then_never_is_selected() {
        setContent(loadedState(retention = TrashRetention.NEVER).copy(isRetentionSheetVisible = true))

        retentionOption(TrashRetention.NEVER).assertIsSelected()
        retentionOption(TrashRetention.DAYS_30).assertIsNotSelected()
    }

    @Test
    fun given_retention_sheet_hidden_when_displayed_then_sheet_does_not_exist() {
        setContent(loadedState())

        node(TrashTestTags.RETENTION_SHEET).assertDoesNotExist()
    }

    @Test
    fun given_retention_sheet_when_option_clicked_then_sends_retention_selected_and_closes() {
        setContent(loadedState(retention = TrashRetention.DAYS_30).copy(isRetentionSheetVisible = true))

        retentionOption(TrashRetention.DAYS_7).performClick()
        composeRule.waitUntil(timeoutMillis = SHEET_TIMEOUT_MS) { TrashIntent.RetentionSheetDismissed in intents }

        assertEquals(
            listOf(TrashIntent.RetentionSelected(TrashRetention.DAYS_7), TrashIntent.RetentionSheetDismissed),
            intents,
        )
    }

    @Test
    fun given_trash_when_back_clicked_then_sends_back_clicked() {
        setContent(loadedState())

        node(TrashTestTags.BACK)
            .assertContentDescriptionEquals(string(R.string.trash_back))
            .assert(hasRole(Role.Button))
            .performClick()

        assertEquals(listOf<TrashIntent>(TrashIntent.BackClicked), intents)
    }

    @Test
    fun given_trash_when_displayed_then_shows_title() {
        setContent(loadedState())

        composeRule.onNodeWithText(string(R.string.trash_title)).assertIsDisplayed()
    }

    private val retentionLabels = linkedMapOf(
        TrashRetention.DAYS_7 to R.string.trash_retention_7_days,
        TrashRetention.DAYS_30 to R.string.trash_retention_30_days,
        TrashRetention.DAYS_90 to R.string.trash_retention_90_days,
        TrashRetention.NEVER to R.string.trash_retention_never,
    )

    private fun card(id: Long): SemanticsNodeInteraction = node(TrashTestTags.NOTE + id)

    /** Textos visibles de la tarjeta, en orden, tal como los une el lector de pantalla. */
    private fun SemanticsNodeInteraction.texts(): List<String> =
        fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }

    private fun retentionOption(retention: TrashRetention): SemanticsNodeInteraction =
        node(TrashTestTags.RETENTION_OPTION + retention.name)

    private fun node(tag: String): SemanticsNodeInteraction = composeRule.onNodeWithTag(tag)

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private fun retentionMessage(count: Int, labelRes: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.resources
            .getQuantityString(R.plurals.trash_retention_confirm_message, count, count, string(labelRes))

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun plural(id: Int, count: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getQuantityString(id, count, count)

    companion object {
        private const val SHEET_TIMEOUT_MS = 5_000L

        @JvmStatic
        @Parameterized.Parameters(name = "darkTheme={0}")
        fun themes(): List<Boolean> = listOf(true, false)
    }
}
