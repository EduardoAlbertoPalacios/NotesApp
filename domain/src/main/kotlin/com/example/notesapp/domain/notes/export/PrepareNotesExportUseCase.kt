package com.example.notesapp.domain.notes.export

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.Note
import javax.inject.Inject

/** Genera el .zip temporal con un PDF por cada nota de [notes]. Exportar no modifica ninguna nota. */
class PrepareNotesExportUseCase @Inject constructor(
    private val exporter: NotesExporter,
) {
    suspend operator fun invoke(notes: List<Note>): AppResult<PreparedExport, ExportError> =
        if (notes.isEmpty()) AppResult.Error(ExportError.NothingToExport) else exporter.prepare(notes)
}
