package com.example.notesapp.data.notes

import com.example.notesapp.data.local.NoteEntity
import com.example.notesapp.data.local.NoteImageEntity
import com.example.notesapp.data.local.NoteWithImages
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteImage

internal fun NoteWithImages.toDomain() = note.toDomain(images.map { it.toDomain() })

internal fun NoteImageEntity.toDomain() = NoteImage(id = id, path = path)

internal fun NoteEntity.toDomain(images: List<NoteImage> = emptyList()) = Note(
    id = id,
    title = title,
    content = content,
    color = NoteColor.entries.firstOrNull { it.name == color } ?: NoteColor.AQUA,
    isPinned = isPinned,
    updatedAt = updatedAt,
    category = NoteCategory.entries.firstOrNull { it.name == category },
    images = images,
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
