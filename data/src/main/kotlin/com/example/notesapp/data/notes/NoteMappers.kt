package com.example.notesapp.data.notes

import com.example.notesapp.data.local.NoteEntity
import com.example.notesapp.data.local.NoteImageEntity
import com.example.notesapp.data.local.NoteWithImages
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.trash.TrashedNote

internal fun NoteWithImages.toDomain() = note.toDomain(images.map { it.toDomain() })

/**
 * Nota de la papelera, o `null` si la fila no está en ella: inventarle una fecha la daría por vencida y la purga
 * la borraría.
 */
internal fun NoteWithImages.toTrashedNote(): TrashedNote? =
    note.trashedAt?.let { TrashedNote(note = toDomain(), trashedAt = it) }

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
    contentSpans = contentSpans.decodeSpans(content.length),
    contentTasks = contentTasks.decodeTasks(content),
)

internal fun Note.toEntity() = NoteEntity(
    id = id,
    title = title,
    content = content,
    color = color.name,
    isPinned = isPinned,
    updatedAt = updatedAt,
    category = category?.name,
    contentSpans = contentSpans.encode(),
    contentTasks = contentTasks.encode(),
)
