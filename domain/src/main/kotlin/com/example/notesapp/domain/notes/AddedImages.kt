package com.example.notesapp.domain.notes

/** Resultado de agregar imágenes: la nota actualizada y cuántas imágenes no se pudieron copiar. */
data class AddedImages(
    val note: Note,
    val failedCount: Int,
)
