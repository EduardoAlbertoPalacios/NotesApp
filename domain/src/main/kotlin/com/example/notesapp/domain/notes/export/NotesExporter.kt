package com.example.notesapp.domain.notes.export

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.Note

/** Genera la exportación de notas como un .zip con un PDF por nota y la guarda donde elija el usuario. */
interface NotesExporter {

    /** Genera el .zip temporal con un PDF por cada nota de [notes], en ese orden. */
    suspend fun prepare(notes: List<Note>): AppResult<PreparedExport, ExportError>

    /**
     * Copia [export] al documento [destination] (URI que creó el selector del sistema). Si falla o se cancela,
     * borra [destination] para no dejar un archivo incompleto. No borra el temporal.
     */
    suspend fun save(export: PreparedExport, destination: String): AppResult<Unit, ExportError>

    /** Borra el temporal de [export]. No falla si ya no existe. */
    suspend fun discard(export: PreparedExport)

    /** Borra el documento [destination] cuando no hay exportación que copiar en él. No falla si no se puede. */
    suspend fun discardDestination(destination: String)
}
