package com.example.notesapp.common.time

/** Fuente de la hora actual, inyectable para poder fijarla en pruebas. */
fun interface TimeProvider {
    /** Milisegundos desde epoch. */
    fun now(): Long
}
