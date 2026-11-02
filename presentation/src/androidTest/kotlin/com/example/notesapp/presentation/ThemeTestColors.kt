package com.example.notesapp.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.text.TextLayoutResult
import com.example.notesapp.domain.notes.NoteColor
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/** Valores de la sección 4.3 del plan 006, escritos aparte de los tokens para que cambiarlos por error se detecte. */
internal object ThemeTestColors {
    val LightBackground = Color(0xFFFAFBFC)
    val LightSurface = Color(0xFFEEF1F3)
    val LightSheet = Color(0xFFFFFFFF)
    val LightText = Color(0xFF202629)
    val LightSecondaryText = Color(0xFF626C71)
    val LightAccent = Color(0xFF17646D)
    val DarkBackground = Color(0xFF080808)
    val DarkSurface = Color(0xFF1C1C1E)
    val DarkSecondaryText = Color(0xFF929296)
    val NoteText = Color(0xFF152326)
    val NoteMetadata = Color(0xFF3B4C50)

    /** Los siete colores de nota distintos de «Sin color», iguales en ambos temas. */
    val NoteContainers = mapOf(
        NoteColor.AQUA to Color(0xFF94E3EB),
        NoteColor.ROSE to Color(0xFFF3A2C4),
        NoteColor.VANILLA to Color(0xFFF4E6A4),
        NoteColor.LAVENDER to Color(0xFFC7B8EF),
        NoteColor.MINT to Color(0xFFBFE4C7),
        NoteColor.PEACH to Color(0xFFF5C4A5),
        NoteColor.SAND to Color(0xFFE2D5C3),
    )
}

/**
 * El color opaco más frecuente de la imagen: el fondo, aunque encima haya textos o íconos.
 * Ignora lo translúcido porque la captura de una hoja modal incluye su velo.
 */
internal fun ImageBitmap.dominantColor(): Color {
    val counts = HashMap<Int, Int>()
    toPixelMap().buffer.forEach { pixel ->
        if (pixel ushr ALPHA_SHIFT == OPAQUE_ALPHA) counts[pixel] = (counts[pixel] ?: 0) + 1
    }
    return Color(counts.maxBy { it.value }.key)
}

internal fun ImageBitmap.containsColor(expected: Color): Boolean =
    toPixelMap().buffer.any { pixel -> Color(pixel).isCloseTo(expected) }

internal fun assertColorEquals(message: String, expected: Color, actual: Color) {
    assertTrue("$message: se esperaba $expected y se obtuvo $actual", actual.isCloseTo(expected))
}

/** Color con el que se dibuja el texto del nodo, sin depender de píxeles. */
internal fun SemanticsNodeInteraction.textColor(): Color {
    val results = mutableListOf<TextLayoutResult>()
    fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
    assertEquals(1, results.size)
    return results.single().layoutInput.style.color
}

private fun Color.isCloseTo(other: Color): Boolean =
    abs(red - other.red) <= COLOR_TOLERANCE &&
        abs(green - other.green) <= COLOR_TOLERANCE &&
        abs(blue - other.blue) <= COLOR_TOLERANCE &&
        abs(alpha - other.alpha) <= COLOR_TOLERANCE

/** Dos niveles de 255, por el redondeo de la captura. */
private const val COLOR_TOLERANCE = 2f / 255f
private const val ALPHA_SHIFT = 24
private const val OPAQUE_ALPHA = 0xFF
