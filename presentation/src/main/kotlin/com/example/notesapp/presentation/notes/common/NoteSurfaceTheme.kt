package com.example.notesapp.presentation.notes.common

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.notesapp.domain.notes.NoteColor

/**
 * Tema para lo que se pinta sobre el color de la nota: fondo del color y textos e íconos oscuros.
 * Las superficies (tarjetas de categoría) pasan a un tinte translúcido del texto para seguir legibles.
 * Con [NoteColor.NONE] no cambia nada, así que el contenido conserva el tema oscuro de la app.
 */
@Composable
internal fun NoteSurfaceTheme(color: NoteColor, content: @Composable () -> Unit) {
    if (color == NoteColor.NONE) {
        content()
        return
    }
    val appColors = AppColors(MaterialTheme.colorScheme, LocalContentColor.current)
    val container = color.toContainerColor()
    val onContainer = color.toContentColor()
    val colorScheme = MaterialTheme.colorScheme.copy(
        background = container,
        onBackground = onContainer,
        onSurface = onContainer,
        onSurfaceVariant = color.toMetadataColor(),
        primary = onContainer,
        onPrimary = container,
        secondary = onContainer,
        surfaceContainer = onContainer.copy(alpha = SURFACE_TINT_ALPHA),
        outlineVariant = onContainer.copy(alpha = OUTLINE_TINT_ALPHA),
    )
    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(
            LocalAppColors provides appColors,
            // Los textos e íconos sin color explícito toman el del contenido, no el claro heredado.
            LocalContentColor provides onContainer,
            content = content,
        )
    }
}

/**
 * Restaura el tema oscuro de la app dentro de un [NoteSurfaceTheme], para lo que no va sobre el color
 * de la nota: barra inferior, menús y botones sobre imágenes. Fuera de él no cambia nada.
 */
@Composable
internal fun AppColorsTheme(content: @Composable () -> Unit) {
    val appColors = LocalAppColors.current ?: return content()
    MaterialTheme(colorScheme = appColors.colorScheme) {
        CompositionLocalProvider(LocalContentColor provides appColors.contentColor, content = content)
    }
}

private data class AppColors(val colorScheme: ColorScheme, val contentColor: Color)

private val LocalAppColors = staticCompositionLocalOf<AppColors?> { null }

private const val SURFACE_TINT_ALPHA = 0.06f
private const val OUTLINE_TINT_ALPHA = 0.16f
