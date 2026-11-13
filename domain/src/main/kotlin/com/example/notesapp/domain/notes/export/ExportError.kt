package com.example.notesapp.domain.notes.export

sealed interface ExportError {
    /** No había ninguna nota que exportar. */
    data object NothingToExport : ExportError

    /** No se pudo generar el .zip temporal o escribirlo en el destino. */
    data object WriteFailed : ExportError
}
