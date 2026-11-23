package com.example.notesapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.presentation.help.HelpRoute
import com.example.notesapp.presentation.notes.editor.NoteEditorRoute
import com.example.notesapp.presentation.notes.editor.NoteEditorViewModel
import com.example.notesapp.presentation.notes.export.ExportNotesRoute
import com.example.notesapp.presentation.notes.list.NoteListRoute
import com.example.notesapp.presentation.notes.trash.TrashRoute
import com.example.notesapp.presentation.settings.SettingsRoute

private object NotesDestinations {
    const val NOTE_LIST = "notes"
    const val SETTINGS = "settings"
    const val EXPORT_NOTES = "settings/export"
    const val TRASH = "settings/trash"
    const val HELP = "settings/help"
    const val NOTE_EDITOR = "notes/editor?" +
        "${NoteEditorViewModel.NOTE_ID_ARG}={${NoteEditorViewModel.NOTE_ID_ARG}}&" +
        "${NoteEditorViewModel.CATEGORY_ARG}={${NoteEditorViewModel.CATEGORY_ARG}}"

    fun noteEditor(noteId: Long?, category: NoteCategory?): String {
        val route = "notes/editor?${NoteEditorViewModel.NOTE_ID_ARG}=${noteId ?: Note.NEW_ID}"
        return if (category == null) route else "$route&${NoteEditorViewModel.CATEGORY_ARG}=${category.name}"
    }
}

@Composable
fun NotesNavHost(appVersion: String, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = NotesDestinations.NOTE_LIST,
        modifier = modifier,
        // Mismas transiciones para todas las pantallas (spec 015, D1).
        enterTransition = NavTransitions.enter,
        exitTransition = NavTransitions.exit,
        popEnterTransition = NavTransitions.popEnter,
        popExitTransition = NavTransitions.popExit,
        predictivePopEnterTransition = NavTransitions.predictivePopEnter,
        predictivePopExitTransition = NavTransitions.predictivePopExit,
    ) {
        composable(NotesDestinations.NOTE_LIST) { entry ->
            // Nota que el editor acaba de mover a la papelera (spec 012, RF-3).
            val results = entry.savedStateHandle
            val movedToTrashNoteId by results.getStateFlow<Long?>(MOVED_TO_TRASH_NOTE_ID_KEY, null)
                .collectAsStateWithLifecycle()
            NoteListRoute(
                onNavigateToNote = { noteId, category ->
                    entry.ifResumed { navController.navigate(NotesDestinations.noteEditor(noteId, category)) }
                },
                onNavigateToSettings = { entry.ifResumed { navController.navigate(NotesDestinations.SETTINGS) } },
                movedToTrashNoteId = movedToTrashNoteId,
                onMovedToTrashShown = { results[MOVED_TO_TRASH_NOTE_ID_KEY] = null },
                modifier = Modifier.blockTouchesWhileExiting(this),
            )
        }
        composable(
            route = NotesDestinations.NOTE_EDITOR,
            arguments = listOf(
                navArgument(NoteEditorViewModel.NOTE_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = Note.NEW_ID
                },
                navArgument(NoteEditorViewModel.CATEGORY_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            NoteEditorRoute(
                onNavigateBack = { navController.popBackStack() },
                onMovedToTrash = { noteId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(MOVED_TO_TRASH_NOTE_ID_KEY, noteId)
                    navController.popBackStack()
                },
                modifier = Modifier.blockTouchesWhileExiting(this),
            )
        }
        composable(NotesDestinations.SETTINGS) { entry ->
            // Resultado que deja Exportar notas al volver (RF-20, RF-45).
            val results = entry.savedStateHandle
            val exportedCount by results.getStateFlow<Int?>(EXPORTED_COUNT_KEY, null).collectAsStateWithLifecycle()
            SettingsRoute(
                appVersion = appVersion,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExport = { entry.ifResumed { navController.navigate(NotesDestinations.EXPORT_NOTES) } },
                onNavigateToTrash = { entry.ifResumed { navController.navigate(NotesDestinations.TRASH) } },
                onNavigateToHelp = { entry.ifResumed { navController.navigate(NotesDestinations.HELP) } },
                exportedCount = exportedCount,
                missingImageCount = results[MISSING_IMAGE_COUNT_KEY] ?: 0,
                onExportResultShown = {
                    results[EXPORTED_COUNT_KEY] = null
                    results.remove<Int>(MISSING_IMAGE_COUNT_KEY)
                },
                modifier = Modifier.blockTouchesWhileExiting(this),
            )
        }
        composable(NotesDestinations.EXPORT_NOTES) {
            ExportNotesRoute(
                onNavigateBack = { navController.popBackStack() },
                onExported = { exportedCount, missingImageCount ->
                    navController.previousBackStackEntry?.savedStateHandle?.let { results ->
                        results[MISSING_IMAGE_COUNT_KEY] = missingImageCount
                        results[EXPORTED_COUNT_KEY] = exportedCount
                    }
                    navController.popBackStack()
                },
                modifier = Modifier.blockTouchesWhileExiting(this),
            )
        }
        composable(NotesDestinations.TRASH) {
            TrashRoute(
                onNavigateBack = { navController.popBackStack() },
                modifier = Modifier.blockTouchesWhileExiting(this),
            )
        }
        composable(NotesDestinations.HELP) {
            HelpRoute(
                onNavigateBack = { navController.popBackStack() },
                modifier = Modifier.blockTouchesWhileExiting(this),
            )
        }
    }
}

private const val MOVED_TO_TRASH_NOTE_ID_KEY = "trash_moved_note_id"
private const val EXPORTED_COUNT_KEY = "export_exported_count"
private const val MISSING_IMAGE_COUNT_KEY = "export_missing_image_count"
