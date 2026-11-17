package com.example.notesapp.presentation.notes.trash

import com.example.notesapp.domain.notes.trash.TrashRetention

sealed interface TrashIntent {
    /** Abre la hoja de acciones de la nota: en la papelera no se puede abrir ni editar (RF-20, RF-21). */
    data class NoteClicked(val id: Long) : TrashIntent
    data object ActionsDismissed : TrashIntent
    data object RestoreClicked : TrashIntent
    data object DeleteForeverClicked : TrashIntent
    data object DeleteForeverConfirmed : TrashIntent
    data object DeleteForeverDismissed : TrashIntent
    data object EmptyClicked : TrashIntent
    data object EmptyConfirmed : TrashIntent
    data object EmptyDismissed : TrashIntent
    data object RetentionClicked : TrashIntent
    data object RetentionSheetDismissed : TrashIntent
    data class RetentionSelected(val retention: TrashRetention) : TrashIntent
    data object RetentionConfirmed : TrashIntent
    data object RetentionDismissed : TrashIntent
    data object BackClicked : TrashIntent
}
