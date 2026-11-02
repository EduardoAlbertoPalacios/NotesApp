package com.example.notesapp.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Aqua,
    onPrimary = Ink,
    secondary = Vanilla,
    tertiary = Mint,
    background = Black,
    onBackground = Snow,
    surface = Black,
    onSurface = Snow,
    surfaceContainer = Graphite,
    surfaceContainerHigh = Graphite,
    surfaceVariant = Graphite,
    onSurfaceVariant = Ash,
    outlineVariant = Iron,
)

private val LightColorScheme = lightColorScheme(
    primary = Aqua,
    onPrimary = Ink,
    secondary = Vanilla,
    tertiary = Aqua,
    background = Mist,
    onBackground = Charcoal,
    surface = Mist,
    onSurface = Charcoal,
    surfaceContainer = Frost,
    surfaceContainerHigh = Color.White,
    surfaceVariant = Frost,
    onSurfaceVariant = Steel,
    outlineVariant = Haze,
)

private val LocalNoteColors = staticCompositionLocalOf { NoteColors() }
internal val LocalAccentColors = staticCompositionLocalOf { AccentColors() }
private val LocalSpacing = staticCompositionLocalOf { Spacing() }
private val LocalSizes = staticCompositionLocalOf { Sizes() }
private val LocalRadii = staticCompositionLocalOf { Radii() }
private val LocalNotesTypography = staticCompositionLocalOf { NotesTypography() }

@Composable
fun NotesAppTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNoteColors provides if (darkTheme) NoteColors() else LightNoteColors,
        LocalAccentColors provides if (darkTheme) AccentColors() else LightAccentColors,
        LocalSpacing provides Spacing(),
        LocalSizes provides Sizes(),
        LocalRadii provides Radii(),
        LocalNotesTypography provides if (darkTheme) NotesTypography() else LightNotesTypography,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content,
        )
    }
}

object NotesTheme {
    val noteColors: NoteColors
        @Composable @ReadOnlyComposable get() = LocalNoteColors.current
    val accent: AccentColors
        @Composable @ReadOnlyComposable get() = LocalAccentColors.current
    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
    val sizes: Sizes
        @Composable @ReadOnlyComposable get() = LocalSizes.current
    val radii: Radii
        @Composable @ReadOnlyComposable get() = LocalRadii.current
    val typography: NotesTypography
        @Composable @ReadOnlyComposable get() = LocalNotesTypography.current
}
