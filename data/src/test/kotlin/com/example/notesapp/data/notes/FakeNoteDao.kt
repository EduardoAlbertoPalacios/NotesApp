package com.example.notesapp.data.notes

import com.example.notesapp.data.local.NoteContentUpdate
import com.example.notesapp.data.local.NoteDao
import com.example.notesapp.data.local.NoteEntity
import com.example.notesapp.data.local.NoteImageEntity
import com.example.notesapp.data.local.NoteWithImages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

internal class FakeNoteDao : NoteDao {
    private val notes = MutableStateFlow<List<NoteEntity>>(emptyList())
    private val images = MutableStateFlow<List<NoteImageEntity>>(emptyList())
    private var nextId = 1L
    private var nextImageId = 1L
    var failure: Throwable? = null
    var deleteByIdsCalls = 0
        private set

    /** Tamaño de cada sentencia `deleteByIds`, en orden. */
    val deleteByIdsBatchSizes = mutableListOf<Int>()

    /** Simula una consulta de papelera que también devuelve filas con `trashed_at` nulo. */
    var trashQueryIncludesActive = false

    val stored: List<NoteEntity> get() = notes.value
    val storedImages: List<NoteImageEntity> get() = images.value

    override fun observeAll(): Flow<List<NoteWithImages>> =
        failure?.let { error -> flow { throw error } }
            ?: combine(notes, images) { notes, images ->
                notes.filter { it.trashedAt == null }.map { it.withImages(images) }
            }

    override fun observeTrashed(): Flow<List<NoteWithImages>> =
        failure?.let { error -> flow { throw error } }
            ?: combine(notes, images) { notes, images ->
                notes.filter { trashQueryIncludesActive || it.trashedAt != null }.map { it.withImages(images) }
            }

    override suspend fun getById(id: Long): NoteWithImages? {
        failure?.let { throw it }
        return notes.value.firstOrNull { it.id == id }?.withImages(images.value)
    }

    override suspend fun insert(note: NoteEntity): Long {
        failure?.let { throw it }
        val id = nextId++
        notes.update { it + note.copy(id = id) }
        return id
    }

    override suspend fun insertAll(notes: List<NoteEntity>) {
        notes.forEach { insert(it) }
    }

    /** Como `@Update(entity = NoteEntity::class)`: cambia todas las columnas menos `trashed_at`. */
    override suspend fun updateContent(note: NoteContentUpdate): Int {
        failure?.let { throw it }
        if (notes.value.none { it.id == note.id }) return 0
        notes.update { list -> list.map { if (it.id == note.id) note.toEntity(trashedAt = it.trashedAt) else it } }
        return 1
    }

    override suspend fun setTrashedAt(id: Long, trashedAt: Long?): Int {
        failure?.let { throw it }
        if (notes.value.none { it.id == id }) return 0
        notes.update { list -> list.map { if (it.id == id) it.copy(trashedAt = trashedAt) else it } }
        return 1
    }

    override suspend fun deleteById(id: Long) {
        failure?.let { throw it }
        notes.update { list -> list.filterNot { it.id == id } }
        // Como la clave foránea con ON DELETE CASCADE.
        images.update { list -> list.filterNot { it.noteId == id } }
    }

    override suspend fun deleteByIds(ids: List<Long>) {
        failure?.let { throw it }
        deleteByIdsCalls++
        deleteByIdsBatchSizes += ids.size
        notes.update { list -> list.filterNot { it.id in ids } }
        images.update { list -> list.filterNot { it.noteId in ids } }
    }

    override suspend fun insertImages(images: List<NoteImageEntity>): List<Long> {
        failure?.let { throw it }
        val inserted = images.map { it.copy(id = nextImageId++) }
        this.images.update { it + inserted }
        return inserted.map { it.id }
    }

    override suspend fun deleteImage(id: Long) {
        failure?.let { throw it }
        images.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun getImagePaths(): List<String> {
        failure?.let { throw it }
        return images.value.map { it.path }
    }

    private fun NoteContentUpdate.toEntity(trashedAt: Long?) = NoteEntity(
        id = id,
        title = title,
        content = content,
        color = color,
        isPinned = isPinned,
        updatedAt = updatedAt,
        category = category,
        contentSpans = contentSpans,
        contentTasks = contentTasks,
        trashedAt = trashedAt,
    )

    private fun NoteEntity.withImages(all: List<NoteImageEntity>) =
        NoteWithImages(note = this, images = all.filter { it.noteId == id })
}
