package com.example.notesapp.domain.notes.export

import javax.inject.Inject

/** Descarta una exportación que no se va a guardar, por ejemplo al cerrar el selector sin elegir destino. */
class DiscardNotesExportUseCase @Inject constructor(
    private val exporter: NotesExporter,
) {
    suspend operator fun invoke(export: PreparedExport) = exporter.discard(export)
}
