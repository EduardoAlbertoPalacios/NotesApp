package com.example.notesapp.presentation.notes.editor

import androidx.lifecycle.SavedStateHandle
import com.example.notesapp.domain.notes.ChecklistItem
import com.example.notesapp.domain.notes.FormatSpan
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.TextFormat

/**
 * Cambios sin guardar de una nota con el guardado automático apagado. Viaja en el [SavedStateHandle]: Android lo
 * conserva si cierra la app en segundo plano y lo descarta si el usuario la cierra desde las apps recientes.
 */
internal data class EditorDraft(
    val title: String,
    val content: String,
    val contentSpans: List<FormatSpan>,
    val contentTasks: List<ChecklistItem>,
    val isPinned: Boolean,
    val category: NoteCategory?,
    val color: NoteColor,
    /** Copias de imágenes agregadas y aún sin asociar a la nota. */
    val pendingImagePaths: List<String>,
    /** Imágenes guardadas que el usuario quitó. */
    val removedImageIds: List<Long>,
)

/** Guarda [EditorDraft] con tipos que admite un `Bundle`: los modelos de `:domain` no son `Parcelable`. */
internal object EditorDraftCodec {
    private const val PREFIX = "draft_"
    private const val TITLE = PREFIX + "title"
    private const val CONTENT = PREFIX + "content"
    private const val SPANS = PREFIX + "spans"
    private const val TASKS = PREFIX + "tasks"
    private const val PINNED = PREFIX + "pinned"
    private const val CATEGORY = PREFIX + "category"
    private const val COLOR = PREFIX + "color"
    private const val PENDING_IMAGES = PREFIX + "pending_images"
    private const val REMOVED_IMAGES = PREFIX + "removed_images"
    private val KEYS = listOf(TITLE, CONTENT, SPANS, TASKS, PINNED, CATEGORY, COLOR, PENDING_IMAGES, REMOVED_IMAGES)

    fun write(handle: SavedStateHandle, draft: EditorDraft) {
        handle[TITLE] = draft.title
        handle[CONTENT] = draft.content
        // Tres enteros por tramo: inicio, fin y formato.
        handle[SPANS] = draft.contentSpans.flatMap { listOf(it.start, it.end, it.format.ordinal) }.toIntArray()
        // Dos enteros por tarea: línea y si está hecha.
        handle[TASKS] = draft.contentTasks.flatMap { listOf(it.line, if (it.isDone) 1 else 0) }.toIntArray()
        handle[PINNED] = draft.isPinned
        handle[CATEGORY] = draft.category?.name
        handle[COLOR] = draft.color.name
        handle[PENDING_IMAGES] = ArrayList(draft.pendingImagePaths)
        handle[REMOVED_IMAGES] = draft.removedImageIds.toLongArray()
    }

    /** `null` si no hay borrador o está incompleto. */
    fun read(handle: SavedStateHandle): EditorDraft? {
        val title = handle.get<String>(TITLE) ?: return null
        val content = handle.get<String>(CONTENT) ?: return null
        val spans = handle.get<IntArray>(SPANS)?.takeIf { it.size % 3 == 0 } ?: return null
        val tasks = handle.get<IntArray>(TASKS)?.takeIf { it.size % 2 == 0 } ?: return null
        val color = handle.get<String>(COLOR)?.let { name -> NoteColor.entries.firstOrNull { it.name == name } }
            ?: return null
        return EditorDraft(
            title = title,
            content = content,
            contentSpans = spans.toList().chunked(3).mapNotNull { (start, end, format) ->
                TextFormat.entries.getOrNull(format)?.let { FormatSpan(start, end, it) }
            },
            contentTasks = tasks.toList().chunked(2).map { (line, done) -> ChecklistItem(line, done == 1) },
            isPinned = handle.get<Boolean>(PINNED) ?: false,
            category = handle.get<String>(CATEGORY)?.let { name -> NoteCategory.entries.firstOrNull { it.name == name } },
            color = color,
            pendingImagePaths = handle.get<ArrayList<String>>(PENDING_IMAGES).orEmpty(),
            removedImageIds = handle.get<LongArray>(REMOVED_IMAGES)?.toList().orEmpty(),
        )
    }

    fun clear(handle: SavedStateHandle) {
        KEYS.forEach { handle.remove<Any>(it) }
    }
}
