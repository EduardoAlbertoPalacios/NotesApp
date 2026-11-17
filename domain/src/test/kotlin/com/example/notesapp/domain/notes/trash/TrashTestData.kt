package com.example.notesapp.domain.notes.trash

import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteImage

internal const val NOW = 1_700_000_000_000L
internal const val DAY = TrashRules.DAY_MILLIS

internal fun trashedNote(
    id: Long = 1,
    trashedAt: Long = NOW,
    images: List<NoteImage> = emptyList(),
) = TrashedNote(
    note = Note(
        id = id,
        title = "Nota $id",
        content = "",
        color = NoteColor.LAVENDER,
        isPinned = false,
        category = null,
        updatedAt = 10,
        images = images,
    ),
    trashedAt = trashedAt,
)
