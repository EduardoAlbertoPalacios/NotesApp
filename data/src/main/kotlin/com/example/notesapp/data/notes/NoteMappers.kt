package com.example.notesapp.data.notes

import com.example.notesapp.data.local.NoteEntity
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor

internal fun NoteEntity.toDomain() = Note(
    id = id,
    title = title,
    content = content,
    color = NoteColor.entries.firstOrNull { it.name == color } ?: NoteColor.AQUA,
    isPinned = isPinned,
    updatedAt = updatedAt,
    category = NoteCategory.entries.firstOrNull { it.name == category },
)

internal fun Note.toEntity() = NoteEntity(
    id = id,
    title = title,
    content = content,
    color = color.name,
    isPinned = isPinned,
    updatedAt = updatedAt,
    category = category?.name,
)
