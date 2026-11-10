package com.example.notesapp.domain.notes

/** Imágenes copiadas pero aún sin asociar a una nota, y cuántas no se pudieron copiar. */
data class ImportedImages(
    val paths: List<String>,
    val failedCount: Int,
)
