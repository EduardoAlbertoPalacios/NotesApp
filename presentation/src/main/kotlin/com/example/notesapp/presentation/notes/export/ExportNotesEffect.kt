package com.example.notesapp.presentation.notes.export

import androidx.annotation.StringRes

sealed interface ExportNotesEffect {
    /** Abrir el selector de documentos del sistema para crear el .zip, con [suggestedName] como nombre. */
    data class OpenDocumentPicker(val suggestedName: String) : ExportNotesEffect

    /** El .zip se guardó: volver a Ajustes, que muestra los avisos. */
    data class Finished(val exportedCount: Int, val missingImageCount: Int) : ExportNotesEffect
    data class ShowMessage(@StringRes val messageRes: Int) : ExportNotesEffect
    data object NavigateBack : ExportNotesEffect
}
