package com.example.notesapp.domain.notes

/** Imagen adjunta a una nota; [path] apunta a la copia guardada en el almacenamiento de la app. */
data class NoteImage(
    val id: Long,
    val path: String,
)
