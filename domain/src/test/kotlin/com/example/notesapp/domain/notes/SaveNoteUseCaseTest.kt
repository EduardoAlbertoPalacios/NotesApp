package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveNoteUseCaseTest {

    private val repository = FakeNoteRepository()
    private val saveNote = SaveNoteUseCase(repository, TimeProvider { NOW })

    @Test
    fun `given blank title and content when saving then returns empty note error without saving`() = runTest {
        val result = saveNote(note(title = "  ", content = "\n"))

        assertEquals(AppResult.Error(NoteError.EmptyNote), result)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `given new note when saving then stores it with current time and returns generated id`() = runTest {
        val result = saveNote(note(id = Note.NEW_ID, title = "Ideas"))

        assertEquals(note(id = Note.NEW_ID, title = "Ideas", updatedAt = NOW), repository.saved.single())
        assertEquals(AppResult.Success(note(id = 100, title = "Ideas", updatedAt = NOW)), result)
    }

    @Test
    fun `given existing note with only content when saving then keeps its id`() = runTest {
        val result = saveNote(note(id = 7, title = "", content = "Solo cuerpo"))

        assertEquals(AppResult.Success(note(id = 7, title = "", content = "Solo cuerpo", updatedAt = NOW)), result)
    }

    @Test
    fun `given note without text but with images when saving then stores it`() = runTest {
        val withImage = note(id = 7, title = "", content = "").copy(images = listOf(NoteImage(id = 1, path = "/a.jpg")))

        val result = saveNote(withImage)

        assertEquals(AppResult.Success(withImage.copy(updatedAt = NOW)), result)
        assertEquals(listOf(withImage.copy(updatedAt = NOW)), repository.saved)
    }

    @Test
    fun `given note with content spans when saving then stores and returns them with current time`() = runTest {
        val spans = listOf(
            FormatSpan(start = 0, end = 3, format = TextFormat.BOLD),
            FormatSpan(start = 2, end = 7, format = TextFormat.UNDERLINE),
            FormatSpan(start = 4, end = 7, format = TextFormat.STRIKETHROUGH),
        )
        val formatted = note(id = 7, content = "Reunión", contentSpans = spans, updatedAt = 500)

        val result = saveNote(formatted)

        val expected = formatted.copy(updatedAt = NOW)
        assertEquals(listOf(expected), repository.saved)
        assertEquals(AppResult.Success(expected), result)
    }

    @Test
    fun `given note where only spans changed when saving then updates its edit time`() = runTest {
        val previous = note(id = 7, content = "Reunión", updatedAt = 500)
        val reformatted = previous.copy(contentSpans = listOf(FormatSpan(start = 0, end = 3, format = TextFormat.BOLD)))

        val result = saveNote(reformatted)

        assertEquals(AppResult.Success(reformatted.copy(updatedAt = NOW)), result)
        assertEquals(listOf(reformatted.copy(updatedAt = NOW)), repository.saved)
    }

    @Test
    fun `given note without color when saving then keeps none color and updates its edit time`() = runTest {
        val uncolored = note(id = 7, color = NoteColor.NONE, updatedAt = 500)

        val result = saveNote(uncolored)

        val expected = uncolored.copy(updatedAt = NOW)
        assertEquals(listOf(expected), repository.saved)
        assertEquals(AppResult.Success(expected), result)
    }

    @Test
    fun `given note with vanilla color when saving then keeps its color and updates its edit time`() = runTest {
        val vanilla = note(id = 7, color = NoteColor.VANILLA, updatedAt = 500)

        val result = saveNote(vanilla)

        val expected = vanilla.copy(updatedAt = NOW)
        assertEquals(listOf(expected), repository.saved)
        assertEquals(AppResult.Success(expected), result)
    }

    @Test
    fun `given note with sand color when saving then keeps its color and updates its edit time`() = runTest {
        val sand = note(id = 7, color = NoteColor.SAND, updatedAt = 500)

        val result = saveNote(sand)

        val expected = sand.copy(updatedAt = NOW)
        assertEquals(listOf(expected), repository.saved)
        assertEquals(AppResult.Success(expected), result)
    }

    @Test
    fun `given note with content tasks when saving then keeps them and updates its edit time`() = runTest {
        val tasks = listOf(ChecklistItem(line = 0, isDone = true), ChecklistItem(line = 1, isDone = false))
        val withTasks = note(id = 7, content = "Tomates\nCafé", contentTasks = tasks, updatedAt = 500)

        val result = saveNote(withTasks)

        val expected = withTasks.copy(updatedAt = NOW)
        assertEquals(listOf(expected), repository.saved)
        assertEquals(AppResult.Success(expected), result)
        assertEquals(tasks, repository.saved.single().contentTasks)
    }

    @Test
    fun `given storage failure when saving then returns the error`() = runTest {
        repository.failure = NoteError.Storage

        assertEquals(AppResult.Error(NoteError.Storage), saveNote(note(title = "Ideas")))
    }

    private fun note(
        id: Long = 1,
        title: String = "Título",
        content: String = "",
        updatedAt: Long = 0,
        contentSpans: List<FormatSpan> = emptyList(),
        color: NoteColor = NoteColor.AQUA,
        contentTasks: List<ChecklistItem> = emptyList(),
    ) = Note(
        id = id,
        title = title,
        content = content,
        color = color,
        isPinned = false,
        category = null,
        updatedAt = updatedAt,
        contentSpans = contentSpans,
        contentTasks = contentTasks,
    )

    private companion object {
        const val NOW = 1_000L
    }
}
