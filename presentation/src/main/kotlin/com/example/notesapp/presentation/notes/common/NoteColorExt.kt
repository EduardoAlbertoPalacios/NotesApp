package com.example.notesapp.presentation.notes.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.presentation.theme.NotesTheme

/** Color de fondo de la nota según el tema. */
@Composable
internal fun NoteColor.toContainerColor(): Color {
    val colors = NotesTheme.noteColors
    return when (this) {
        NoteColor.AQUA -> colors.aqua
        NoteColor.SAND -> colors.sand
        NoteColor.MINT -> colors.mint
        NoteColor.ROSE -> colors.rose
        NoteColor.LAVENDER -> colors.lavender
        NoteColor.PEACH -> colors.peach
    }
}
