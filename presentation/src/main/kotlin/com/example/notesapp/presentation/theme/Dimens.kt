package com.example.notesapp.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    val xxxs: Dp = 2.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 5.dp,
    val xsm: Dp = 6.dp,
    val s: Dp = 8.dp,
    val sm: Dp = 10.dp,
    val m: Dp = 12.dp,
    val ml: Dp = 14.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 22.dp,
    val screen: Dp = 24.dp,
    /** Separación entre secciones de la hoja de color. */
    val sheetSection: Dp = 18.dp,
    /** Separación entre las dos líneas de la vista previa de color. */
    val previewLines: Dp = 7.dp,
)

@Immutable
data class Sizes(
    val iconAction: Dp = 44.dp,
    val searchHeight: Dp = 50.dp,
    val chipHeight: Dp = 34.dp,
    val fabHeight: Dp = 60.dp,
    val topBarHeight: Dp = 64.dp,
    val border: Dp = 1.dp,
    val toolbarHeight: Dp = 64.dp,
    val colorDot: Dp = 24.dp,
    val colorDotIcon: Dp = 12.dp,
    val imageAction: Dp = 36.dp,
    val listImageHeight: Dp = 120.dp,
    val sheetHandleWidth: Dp = 36.dp,
    val sheetHandleHeight: Dp = 4.dp,
    val sheetButtonHeight: Dp = 50.dp,
    val colorOptionWidth: Dp = 76.dp,
    val colorSwatch: Dp = 50.dp,
    val colorSwatchSelectedBorder: Dp = 2.dp,
    val colorSwatchIcon: Dp = 20.dp,
    val colorCheckIcon: Dp = 22.dp,
    val taskCheckbox: Dp = 20.dp,
    val taskCheckboxBorder: Dp = 1.5.dp,
    val taskCheckIcon: Dp = 14.dp,
    /** Separación entre la casilla de una tarea y su texto. */
    val taskGap: Dp = 12.dp,
    /** Alto de cada fila de tarea: la casilla y la separación con la siguiente, como en el diseño. */
    val taskRow: Dp = 36.dp,
    /** Casilla de tarea en la tarjeta de Mis notas, más pequeña que la del editor. */
    val cardTaskCheckbox: Dp = 13.dp,
    val cardTaskGap: Dp = 6.dp,
)

@Immutable
data class Radii(
    val tag: Dp = 8.dp,
    val card: Dp = 16.dp,
    val fab: Dp = 20.dp,
    val pill: Dp = 28.dp,
    val toolbar: Dp = 24.dp,
    val sheetHandle: Dp = 2.dp,
    val taskCheckbox: Dp = 5.dp,
    val cardTaskCheckbox: Dp = 3.dp,
)
