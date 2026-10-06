package com.example.notesapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.notesapp.presentation.notes.list.NoteListRoute

private object NotesDestinations {
    const val NOTE_LIST = "notes"
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
                // El editor y los ajustes aún no existen; se conectan cuando se implementen.
                onNavigateToNote = {},
                onNavigateToSettings = {},
            )
        }
    }
}
