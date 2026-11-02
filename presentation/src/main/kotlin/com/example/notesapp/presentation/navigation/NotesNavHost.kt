package com.example.notesapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.presentation.notes.editor.NoteEditorRoute
import com.example.notesapp.presentation.notes.editor.NoteEditorViewModel
import com.example.notesapp.presentation.notes.list.NoteListRoute
import com.example.notesapp.presentation.settings.SettingsRoute

private object NotesDestinations {
    const val NOTE_LIST = "notes"
    const val SETTINGS = "settings"
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
    ) {
        composable(NotesDestinations.NOTE_LIST) {
            NoteListRoute(
                onNavigateToNote = { noteId, category ->
                    navController.navigate(NotesDestinations.noteEditor(noteId, category))
                },
                onNavigateToSettings = { navController.navigate(NotesDestinations.SETTINGS) },
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
            NoteEditorRoute(onNavigateBack = { navController.popBackStack() })
        }
        composable(NotesDestinations.SETTINGS) {
            SettingsRoute(appVersion = appVersion, onNavigateBack = { navController.popBackStack() })
        }
    }
}
