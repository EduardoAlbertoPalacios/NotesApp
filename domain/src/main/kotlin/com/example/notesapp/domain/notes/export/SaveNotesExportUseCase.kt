package com.example.notesapp.domain.notes.export

import com.example.notesapp.common.result.AppResult
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Copia la exportación al documento que eligió el usuario y, salga bien o mal, borra el temporal. Si falla, el
 * destino no queda con un archivo incompleto.
 */
class SaveNotesExportUseCase @Inject constructor(
    private val exporter: NotesExporter,
) {
    suspend operator fun invoke(export: PreparedExport, destination: String): AppResult<Unit, ExportError> =
        try {
            exporter.save(export, destination)
        } finally {
            // También al cancelar (el usuario salió de la pantalla): el temporal no debe quedarse.
            withContext(NonCancellable) { exporter.discard(export) }
        }
}
