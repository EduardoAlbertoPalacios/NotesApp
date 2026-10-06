package com.example.notesapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
internal data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val color: String,
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
