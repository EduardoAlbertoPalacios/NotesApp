package com.example.notesapp.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.notesapp.presentation.theme.NotesTheme

/** Asa de las hojas inferiores de la app. */
@Composable
internal fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = NotesTheme.spacing.sm, bottom = NotesTheme.spacing.sheetSection)
            .size(width = NotesTheme.sizes.sheetHandleWidth, height = NotesTheme.sizes.sheetHandleHeight)
            .background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(NotesTheme.radii.sheetHandle)),
    )
}
