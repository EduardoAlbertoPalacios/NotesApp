package com.example.notesapp.presentation.notes.export

sealed interface ExportNotesIntent {
    data class NoteToggled(val id: Long) : ExportNotesIntent
    data object SelectAllClicked : ExportNotesIntent
    data object ClearSelectionClicked : ExportNotesIntent
    data object ExportClicked : ExportNotesIntent

    /** Resultado del selector de documentos: la URI creada, o nula si se cerró sin confirmar. */
    data class DestinationPicked(val uri: String?) : ExportNotesIntent
    data object BackClicked : ExportNotesIntent
}
