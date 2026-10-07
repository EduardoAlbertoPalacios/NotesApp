package com.example.notesapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.presentation.notes.editor.NoteEditorRoute
import com.example.notesapp.presentation.notes.editor.NoteEditorViewModel
import com.example.notesapp.presentation.notes.list.NoteListRoute

private object NotesDestinations {
    const val NOTE_LIST = "notes"
    const val NOTE_EDITOR = "notes/editor?${NoteEditorViewModel.NOTE_ID_ARG}={${NoteEditorViewModel.NOTE_ID_ARG}}"

    fun noteEditor(noteId: Long?) = "notes/editor?${NoteEditorViewModel.NOTE_ID_ARG}=${noteId ?: Note.NEW_ID}"
}

@Composable
fun NotesNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = NotesDestinations.NOTE_LIST,
        modifier = modifier,
    ) {
        composable(NotesDestinations.NOTE_LIST) {
            NoteListRoute(
                onNavigateToNote = { noteId -> navController.navigate(NotesDestinations.noteEditor(noteId)) },
                // Ajustes aún no existe; se conecta cuando se implemente.
                onNavigateToSettings = {},
            )
        }
        composable(
            route = NotesDestinations.NOTE_EDITOR,
            arguments = listOf(
                navArgument(NoteEditorViewModel.NOTE_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = Note.NEW_ID
                },
            ),
        ) {
            NoteEditorRoute(onNavigateBack = { navController.popBackStack() })
        }
    }
}
