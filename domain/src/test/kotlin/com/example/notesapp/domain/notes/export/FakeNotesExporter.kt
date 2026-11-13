package com.example.notesapp.domain.notes.export

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.Note
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.yield

class FakeNotesExporter : NotesExporter {

    /** Resultado que devuelve [prepare]. */
    var prepareResult: AppResult<PreparedExport, ExportError> = AppResult.Success(PREPARED)

    /** Resultado que devuelve [save] si no lanza ni queda suspendido. */
    var saveResult: AppResult<Unit, ExportError> = AppResult.Success(Unit)

    /** Excepción que lanza [save], si no es nula. */
    var saveFailure: Throwable? = null

    /** Si no es nulo, [save] se queda suspendido hasta que se complete; sirve para cancelar a mitad. */
    var saveGate: CompletableDeferred<Unit>? = null

    val preparedNotes = mutableListOf<List<Note>>()
    val saves = mutableListOf<Pair<PreparedExport, String>>()
    val discarded = mutableListOf<PreparedExport>()
    val discardedDestinations = mutableListOf<String>()

    override suspend fun prepare(notes: List<Note>): AppResult<PreparedExport, ExportError> {
        preparedNotes += notes
        return prepareResult
    }

    override suspend fun save(export: PreparedExport, destination: String): AppResult<Unit, ExportError> {
        saves += export to destination
        saveGate?.await()
        saveFailure?.let { throw it }
        return saveResult
    }

    override suspend fun discard(export: PreparedExport) {
        // Punto de suspensión que comprueba la cancelación: solo se registra si se llama fuera de un job cancelado
        // o dentro de NonCancellable.
        yield()
        discarded += export
    }

    override suspend fun discardDestination(destination: String) {
        discardedDestinations += destination
    }

    companion object {
        val PREPARED = PreparedExport(id = "export-1", noteCount = 2, missingImageCount = 0)
    }
}
