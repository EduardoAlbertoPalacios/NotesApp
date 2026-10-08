package com.example.notesapp.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.notesapp.presentation.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

private val Regular = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
private val SemiBold = Regular.copy(fontWeight = FontWeight.SemiBold)

val Typography = Typography(
    // Título de página.
    headlineLarge = SemiBold.copy(fontSize = 34.sp),
    // Título de la nota en el editor.
    headlineMedium = SemiBold.copy(fontSize = 32.sp, lineHeight = 1.15.em),
    // Título de sección en el editor ("Categoría").
    titleLarge = SemiBold.copy(fontSize = 18.sp),
    // Título de tarjeta de nota.
    titleMedium = SemiBold.copy(fontSize = 17.sp, lineHeight = 1.2.em),
    // Acción principal (botón "Nueva nota").
    titleSmall = SemiBold.copy(fontSize = 15.sp),
    // Placeholder de búsqueda.
    bodyLarge = Regular.copy(fontSize = 14.sp),
    // Contenido de tarjeta de nota.
    bodyMedium = Regular.copy(fontSize = 13.sp, lineHeight = 1.5.em),
    // Resumen bajo el título.
    bodySmall = Regular.copy(fontSize = 13.sp),
    // Filtros.
    labelLarge = Regular.copy(fontSize = 13.sp),
    // Orden activo.
    labelMedium = Regular.copy(fontSize = 12.sp),
    // Fecha de la nota.
    labelSmall = Regular.copy(fontSize = 10.sp),
)

/** Estilos del diseño que no encajan en los roles de Material. */
@Immutable
data class NotesTypography(
    val editorBody: TextStyle = Regular.copy(fontSize = 16.sp, lineHeight = 1.65.em),
    val categoryTitle: TextStyle = SemiBold.copy(fontSize = 14.sp),
    val categoryDescription: TextStyle = Regular.copy(fontSize = 13.sp, lineHeight = 1.4.em),
    val tag: TextStyle = SemiBold.copy(fontSize = 12.sp),
)
