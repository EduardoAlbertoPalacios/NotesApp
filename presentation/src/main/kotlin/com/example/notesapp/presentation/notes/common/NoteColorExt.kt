package com.example.notesapp.presentation.notes.common

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.theme.NotesTheme

/** Color de fondo de la nota según el tema. */
@Composable
internal fun NoteColor.toContainerColor(): Color {
    val colors = NotesTheme.noteColors
    return when (this) {
        NoteColor.NONE -> colors.none
        NoteColor.AQUA -> colors.aqua
        NoteColor.ROSE -> colors.rose
        NoteColor.VANILLA -> colors.vanilla
        NoteColor.LAVENDER -> colors.lavender
        NoteColor.MINT -> colors.mint
        NoteColor.PEACH -> colors.peach
        NoteColor.SAND -> colors.sand
    }
}

/** Color del texto principal sobre la nota: oscuro sobre un color, claro sobre «Sin color». */
@Composable
internal fun NoteColor.toContentColor(): Color =
    if (this == NoteColor.NONE) MaterialTheme.colorScheme.onBackground else NotesTheme.noteColors.content

/** Color del texto secundario (fecha, descripción) sobre la nota. */
@Composable
internal fun NoteColor.toMetadataColor(): Color =
    if (this == NoteColor.NONE) MaterialTheme.colorScheme.onSurfaceVariant else NotesTheme.noteColors.metadata

/** Nombre visible del color. */
@StringRes
internal fun NoteColor.labelRes(): Int = when (this) {
    NoteColor.NONE -> R.string.note_color_none
    NoteColor.AQUA -> R.string.note_color_aqua
    NoteColor.ROSE -> R.string.note_color_rose
    NoteColor.VANILLA -> R.string.note_color_vanilla
    NoteColor.LAVENDER -> R.string.note_color_lavender
    NoteColor.MINT -> R.string.note_color_mint
    NoteColor.PEACH -> R.string.note_color_peach
    NoteColor.SAND -> R.string.note_color_sand
}
