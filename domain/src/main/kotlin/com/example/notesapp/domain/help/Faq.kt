package com.example.notesapp.domain.help

/**
 * Una pregunta frecuente de la ayuda (spec 013).
 *
 * @property id posición fija de la pregunta (1 a 12); identifica qué preguntas están abiertas aunque cambie el texto.
 */
data class Faq(
    val id: Int,
    val question: String,
    val answer: String,
)
