package com.example.notesapp.domain.notes

/**
 * Resultado de convertir los marcadores antiguos «☐»/«☑» en tareas: el texto sin ellos, las tareas y los
 * rangos quitados del texto original (de `first` a `last`, ambos incluidos, en orden), para recolocar
 * el formato.
 */
data class LegacyConversion(
    val text: String,
    val tasks: List<ChecklistItem>,
    val removedRanges: List<IntRange>,
)
