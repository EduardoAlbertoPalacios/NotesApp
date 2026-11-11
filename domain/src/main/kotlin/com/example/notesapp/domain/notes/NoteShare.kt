package com.example.notesapp.domain.notes

/**
 * Lo que se comparte de una nota. [subject] y [text] son `null` si no hay nada que poner; [imagePaths]
 * solo incluye las imágenes que existen, y [missingImageCount] cuenta las que se omitieron.
 */
data class NoteShare(
    val subject: String?,
    val text: String?,
    val imagePaths: List<String>,
    val missingImageCount: Int,
)
