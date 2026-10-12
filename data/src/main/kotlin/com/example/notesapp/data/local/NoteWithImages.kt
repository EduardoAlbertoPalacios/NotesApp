package com.example.notesapp.data.local

import androidx.room.Embedded
import androidx.room.Relation

internal data class NoteWithImages(
    @Embedded val note: NoteEntity,
    @Relation(parentColumn = "id", entityColumn = "note_id")
    val images: List<NoteImageEntity>,
)
