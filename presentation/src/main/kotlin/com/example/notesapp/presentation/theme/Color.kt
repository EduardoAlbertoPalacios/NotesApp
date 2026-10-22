package com.example.notesapp.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

internal val Black = Color(0xFF080808)
internal val Graphite = Color(0xFF1C1C1E)
internal val Iron = Color(0xFF2A2A2E)
internal val Snow = Color(0xFFF7F7F8)
internal val Ash = Color(0xFF929296)
internal val Aqua = Color(0xFF94E3EB)
internal val Ink = Color(0xFF152326)
// Más oscuro que el #405357 del diseño para llegar a 4,5:1 sobre Rosa y Lavanda (plan 003, D5).
internal val Slate = Color(0xFF3B4C50)
internal val Vanilla = Color(0xFFF4E6A4)
internal val Sand = Color(0xFFE2D5C3)
internal val Mint = Color(0xFFBFE4C7)
internal val Rose = Color(0xFFF3A2C4)
internal val Lavender = Color(0xFFC7B8EF)
internal val Peach = Color(0xFFF5C4A5)
internal val Swatch = Color(0xFF262628)
internal val SwatchBorder = Color(0xFF323234)

/** Colores de las tarjetas de nota, que no encajan en los roles de Material. */
@Immutable
data class NoteColors(
    /** «Sin color»: el gris de las superficies. */
    val none: Color = Graphite,
    val aqua: Color = Aqua,
    val vanilla: Color = Vanilla,
    val sand: Color = Sand,
    val mint: Color = Mint,
    val rose: Color = Rose,
    val lavender: Color = Lavender,
    val peach: Color = Peach,
    val content: Color = Ink,
    val metadata: Color = Slate,
    /** Muestra de «Sin color» en el selector. */
    val swatch: Color = Swatch,
    val swatchBorder: Color = SwatchBorder,
)
