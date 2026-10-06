package com.example.notesapp.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

internal val Black = Color(0xFF080808)
internal val Graphite = Color(0xFF1C1C1E)
internal val Snow = Color(0xFFF7F7F8)
internal val Ash = Color(0xFF929296)
internal val Aqua = Color(0xFF94E3EB)
internal val Ink = Color(0xFF152326)
internal val Slate = Color(0xFF405357)
internal val Sand = Color(0xFFF4E6A4)
internal val Mint = Color(0xFFBFE4C7)
internal val Rose = Color(0xFFF3A2C4)
internal val Lavender = Color(0xFFC7B8EF)
internal val Peach = Color(0xFFF5C4A5)

/** Colores de las tarjetas de nota, que no encajan en los roles de Material. */
@Immutable
data class NoteColors(
    val aqua: Color = Aqua,
    val sand: Color = Sand,
    val mint: Color = Mint,
    val rose: Color = Rose,
    val lavender: Color = Lavender,
    val peach: Color = Peach,
    val content: Color = Ink,
    val metadata: Color = Slate,
)
