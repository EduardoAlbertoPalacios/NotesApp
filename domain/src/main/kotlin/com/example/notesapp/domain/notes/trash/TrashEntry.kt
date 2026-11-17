package com.example.notesapp.domain.notes.trash

/** Una nota de la papelera tal como se muestra: con los días que le quedan, o `null` si el plazo es «Nunca». */
data class TrashEntry(
    val trashed: TrashedNote,
    val daysLeft: Int?,
)
