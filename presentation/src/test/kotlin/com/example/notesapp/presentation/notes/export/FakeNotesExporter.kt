package com.example.notesapp.presentation.notes.export

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.export.ExportError
import com.example.notesapp.domain.notes.export.NotesExporter
import com.example.notesapp.domain.notes.export.PreparedExport
import kotlinx.coroutines.CompletableDeferred

class FakeNotesExporter : NotesExporter {

    var prepareResult: AppResult<PreparedExport, ExportError> = AppResult.Success(PREPARED)
    var saveResult: AppResult<Unit, ExportError> = AppResult.Success(Unit)

    /** Si no es nulo, [prepare] se queda suspendido hasta que se complete: simula la generación en curso. */
    var prepareGate: CompletableDeferred<Unit>? = null

    /** Si no es nulo, [save] se queda suspendido hasta que se complete: simula la copia en curso. */
    var saveGate: CompletableDeferred<Unit>? = null

    val preparedNotes = mutableListOf<List<Note>>()
    val saves = mutableListOf<Pair<PreparedExport, String>>()
    val discarded = mutableListOf<PreparedExport>()
    val discardedDestinations = mutableListOf<String>()

    override suspend fun prepare(notes: List<Note>): AppResult<PreparedExport, ExportError> {
        preparedNotes += notes
        prepareGate?.await()
        return prepareResult
    }

    override suspend fun save(export: PreparedExport, destination: String): AppResult<Unit, ExportError> {
        saves += export to destination
        saveGate?.await()
        return saveResult
    }

    override suspend fun discard(export: PreparedExport) {
        discarded += export
    }

    override suspend fun discardDestination(destination: String) {
        discardedDestinations += destination
    }

    companion object {
        val PREPARED = PreparedExport(id = "export-1", noteCount = 2, missingImageCount = 1)
    }
}
