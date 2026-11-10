package com.example.notesapp.presentation.notes.editor

import androidx.lifecycle.SavedStateHandle
import com.example.notesapp.domain.notes.ChecklistItem
import com.example.notesapp.domain.notes.FormatSpan
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.TextFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditorDraftCodecTest {

    private fun draft(
        title: String = "Viaje",
        content: String = "Billetes\nHotel\nMaleta",
        contentSpans: List<FormatSpan> = listOf(
            FormatSpan(0, 8, TextFormat.BOLD),
            FormatSpan(9, 14, TextFormat.UNDERLINE),
            FormatSpan(15, 21, TextFormat.STRIKETHROUGH),
        ),
        contentTasks: List<ChecklistItem> = listOf(ChecklistItem(1, isDone = true), ChecklistItem(2, isDone = false)),
        isPinned: Boolean = true,
        category: NoteCategory? = NoteCategory.PERSONAL,
        color: NoteColor = NoteColor.LAVENDER,
        pendingImagePaths: List<String> = listOf("/files/note_images/a.jpg", "/files/note_images/b.jpg"),
        removedImageIds: List<Long> = listOf(3L, 9L),
    ) = EditorDraft(title, content, contentSpans, contentTasks, isPinned, category, color, pendingImagePaths, removedImageIds)

    @Test
    fun `given draft with every field when written and read then is unchanged`() {
        val handle = SavedStateHandle()
        val draft = draft()

        EditorDraftCodec.write(handle, draft)

        assertEquals(draft, EditorDraftCodec.read(handle))
    }

    @Test
    fun `given draft without category spans tasks nor images when written and read then is unchanged`() {
        val handle = SavedStateHandle()
        val draft = draft(
            title = "",
            content = "",
            contentSpans = emptyList(),
            contentTasks = emptyList(),
            isPinned = false,
            category = null,
            color = NoteColor.NONE,
            pendingImagePaths = emptyList(),
            removedImageIds = emptyList(),
        )

        EditorDraftCodec.write(handle, draft)

        assertEquals(draft, EditorDraftCodec.read(handle))
    }

    @Test
    fun `given each color and category when written and read then keeps them`() {
        NoteColor.entries.forEach { color ->
            NoteCategory.entries.forEach { category ->
                val handle = SavedStateHandle()
                val draft = draft(color = color, category = category)

                EditorDraftCodec.write(handle, draft)

                assertEquals(draft, EditorDraftCodec.read(handle))
            }
        }
    }

    @Test
    fun `given newer draft when written over older one then reads the newer`() {
        val handle = SavedStateHandle()
        EditorDraftCodec.write(handle, draft())
        val newer = draft(title = "Otro", category = null, pendingImagePaths = emptyList())

        EditorDraftCodec.write(handle, newer)

        assertEquals(newer, EditorDraftCodec.read(handle))
    }

    @Test
    fun `given empty saved state when read then returns null`() {
        assertNull(EditorDraftCodec.read(SavedStateHandle()))
    }

    @Test
    fun `given draft missing a required field when read then returns null`() {
        listOf("draft_title", "draft_content", "draft_spans", "draft_tasks", "draft_color").forEach { key ->
            val handle = SavedStateHandle()
            EditorDraftCodec.write(handle, draft())
            handle.remove<Any>(key)

            assertNull("Sin $key", EditorDraftCodec.read(handle))
        }
    }

    @Test
    fun `given draft with unknown color when read then returns null`() {
        val handle = SavedStateHandle()
        EditorDraftCodec.write(handle, draft())
        handle["draft_color"] = "TURQUOISE"

        assertNull(EditorDraftCodec.read(handle))
    }

    @Test
    fun `given spans array whose size is not a multiple of three when read then returns null`() {
        listOf(intArrayOf(0), intArrayOf(0, 8), intArrayOf(0, 8, 0, 9)).forEach { spans ->
            val handle = SavedStateHandle()
            EditorDraftCodec.write(handle, draft())
            handle["draft_spans"] = spans

            assertNull("Tramos ${spans.toList()}", EditorDraftCodec.read(handle))
        }
    }

    @Test
    fun `given tasks array whose size is not a multiple of two when read then returns null`() {
        listOf(intArrayOf(1), intArrayOf(1, 1, 2)).forEach { tasks ->
            val handle = SavedStateHandle()
            EditorDraftCodec.write(handle, draft())
            handle["draft_tasks"] = tasks

            assertNull("Tareas ${tasks.toList()}", EditorDraftCodec.read(handle))
        }
    }

    @Test
    fun `given written draft when cleared then reads null and keeps navigation arguments`() {
        val handle = SavedStateHandle(mapOf(NoteEditorViewModel.NOTE_ID_ARG to 7L, NoteEditorViewModel.CATEGORY_ARG to "WORK"))
        EditorDraftCodec.write(handle, draft())

        EditorDraftCodec.clear(handle)

        assertNull(EditorDraftCodec.read(handle))
        assertEquals(setOf(NoteEditorViewModel.NOTE_ID_ARG, NoteEditorViewModel.CATEGORY_ARG), handle.keys())
    }
}
