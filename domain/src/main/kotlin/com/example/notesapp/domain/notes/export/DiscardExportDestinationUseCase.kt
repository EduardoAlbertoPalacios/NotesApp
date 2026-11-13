package com.example.notesapp.domain.notes.export

import javax.inject.Inject

/**
 * Borra un destino que el selector ya creó, vacío, cuando no hay exportación que copiar en él; por ejemplo, si
 * el proceso murió con el selector abierto y el temporal se perdió.
 */
class DiscardExportDestinationUseCase @Inject constructor(
    private val exporter: NotesExporter,
) {
    suspend operator fun invoke(destination: String) = exporter.discardDestination(destination)
}
