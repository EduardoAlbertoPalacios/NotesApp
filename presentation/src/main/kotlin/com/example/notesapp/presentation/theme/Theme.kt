package com.example.notesapp.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val DarkColorScheme = darkColorScheme(
    primary = Aqua,
    onPrimary = Ink,
    secondary = Sand,
    tertiary = Mint,
    background = Black,
    onBackground = Snow,
    surface = Black,
    onSurface = Snow,
    surfaceContainer = Graphite,
    surfaceVariant = Graphite,
    onSurfaceVariant = Ash,
    outlineVariant = Iron,
)

private val LocalNoteColors = staticCompositionLocalOf { NoteColors() }
private val LocalSpacing = staticCompositionLocalOf { Spacing() }
private val LocalSizes = staticCompositionLocalOf { Sizes() }
private val LocalRadii = staticCompositionLocalOf { Radii() }
private val LocalNotesTypography = staticCompositionLocalOf { NotesTypography() }

/** El diseño solo define modo oscuro. */
@Composable
fun NotesAppTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNoteColors provides NoteColors(),
        LocalSpacing provides Spacing(),
        LocalSizes provides Sizes(),
        LocalRadii provides Radii(),
        LocalNotesTypography provides NotesTypography(),
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content,
        )
    }
}

object NotesTheme {
    val noteColors: NoteColors
        @Composable @ReadOnlyComposable get() = LocalNoteColors.current
    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
    val sizes: Sizes
        @Composable @ReadOnlyComposable get() = LocalSizes.current
    val radii: Radii
        @Composable @ReadOnlyComposable get() = LocalRadii.current
    val typography: NotesTypography
        @Composable @ReadOnlyComposable get() = LocalNotesTypography.current
}
