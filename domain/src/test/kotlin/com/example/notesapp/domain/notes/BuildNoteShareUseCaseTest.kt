package com.example.notesapp.domain.notes

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildNoteShareUseCaseTest {

    private val imageStorage = FakeImageStorage()

    private fun createBuildNoteShare() = BuildNoteShareUseCase(imageStorage)

    private suspend fun share(
        title: String = "",
        content: String = "",
        tasks: List<ChecklistItem> = emptyList(),
        imagePaths: List<String> = emptyList(),
    ): NoteShare = createBuildNoteShare()(title, content, tasks, imagePaths)

    @Test
    fun `given only title when building share then text and subject are the title`() = runTest {
        val result = share(title = "Compras")

        assertEquals("Compras", result.text)
        assertEquals("Compras", result.subject)
    }

    @Test
    fun `given only content when building share then text is the content and subject is null`() = runTest {
        val result = share(content = "Leche y pan")

        assertEquals("Leche y pan", result.text)
        assertNull(result.subject)
    }

    @Test
    fun `given title and content when building share then text is title blank line and content`() = runTest {
        val result = share(title = "Compras", content = "Leche y pan")

        assertEquals("Compras\n\nLeche y pan", result.text)
        assertEquals("Compras", result.subject)
    }

    @Test
    fun `given title and content with only spaces when building share then text and subject are null`() = runTest {
        val result = share(title = "   ", content = " \n  \n ")

        assertNull(result.text)
        assertNull(result.subject)
    }

    @Test
    fun `given title with only spaces and content when building share then text is only the content`() = runTest {
        val result = share(title = "   ", content = "Leche")

        assertEquals("Leche", result.text)
        assertNull(result.subject)
    }

    @Test
    fun `given content with only spaces and title when building share then text is only the title`() = runTest {
        val result = share(title = "Compras", content = "  \n   ")

        assertEquals("Compras", result.text)
    }

    @Test
    fun `given title surrounded by spaces when building share then subject and text use the trimmed title`() = runTest {
        val result = share(title = "  Compras \t")

        assertEquals("Compras", result.subject)
        assertEquals("Compras", result.text)
    }

    @Test
    fun `given content with spaces at edges when building share then trims only the edges`() = runTest {
        val result = share(content = "   Leche\n  pan  \nhuevos   ")

        assertEquals("Leche\n  pan  \nhuevos", result.text)
    }

    @Test
    fun `given content with leading and trailing blank lines when building share then removes them`() = runTest {
        val result = share(title = "Compras", content = "\n  \nLeche\n\npan\n \n")

        assertEquals("Compras\n\nLeche\n\npan", result.text)
    }

    @Test
    fun `given pending and done tasks when building share then prefixes each task line with its marker`() = runTest {
        val result = share(
            content = "Lista\nLeche\nPan\nNota final",
            tasks = listOf(ChecklistItem(line = 1, isDone = false), ChecklistItem(line = 2, isDone = true)),
        )

        assertEquals("Lista\n☐ Leche\n☑ Pan\nNota final", result.text)
    }

    @Test
    fun `given tasks on first and last lines when building share then prefixes them`() = runTest {
        val result = share(
            content = "Leche\nNota\nPan",
            tasks = listOf(ChecklistItem(line = 0, isDone = true), ChecklistItem(line = 2, isDone = false)),
        )

        assertEquals("☑ Leche\nNota\n☐ Pan", result.text)
    }

    @Test
    fun `given empty task in the middle when building share then shares it as pending marker`() = runTest {
        val result = share(
            content = "Leche\n\nPan",
            tasks = listOf(ChecklistItem(line = 1, isDone = false)),
        )

        assertEquals("Leche\n☐ \nPan", result.text)
    }

    @Test
    fun `given empty task on first line when building share then keeps the marker untrimmed`() = runTest {
        val result = share(
            content = "\nLeche",
            tasks = listOf(ChecklistItem(line = 0, isDone = false)),
        )

        assertEquals("☐ \nLeche", result.text)
    }

    @Test
    fun `given empty task on last line when building share then keeps the marker untrimmed`() = runTest {
        val result = share(
            title = "Compras",
            content = "Leche\n",
            tasks = listOf(ChecklistItem(line = 1, isDone = false)),
        )

        assertEquals("Compras\n\nLeche\n☐ ", result.text)
    }

    @Test
    fun `given only an empty task when building share then text is the pending marker`() = runTest {
        val result = share(content = "", tasks = listOf(ChecklistItem(line = 0, isDone = false)))

        assertEquals("☐ ", result.text)
    }

    @Test
    fun `given task line with spaces at edges when building share then does not trim it`() = runTest {
        val result = share(
            content = "  Leche  ",
            tasks = listOf(ChecklistItem(line = 0, isDone = true)),
        )

        assertEquals("☑   Leche  ", result.text)
    }

    @Test
    fun `given leading blank lines before a task when building share then removes them and marks the right line`() = runTest {
        val result = share(
            content = "\n \nLeche\nPan",
            tasks = listOf(ChecklistItem(line = 3, isDone = true)),
        )

        assertEquals("Leche\n☑ Pan", result.text)
    }

    @Test
    fun `given only images when building share then text and subject are null and images are shared`() = runTest {
        imageStorage.storedPaths += listOf(PATH_A, PATH_B)

        val result = share(imagePaths = listOf(PATH_A, PATH_B))

        assertNull(result.text)
        assertNull(result.subject)
        assertEquals(listOf(PATH_A, PATH_B), result.imagePaths)
        assertEquals(0, result.missingImageCount)
    }

    @Test
    fun `given missing images when building share then omits and counts them keeping order of existing`() = runTest {
        imageStorage.storedPaths += listOf(PATH_A, PATH_C)

        val result = share(title = "Compras", imagePaths = listOf(PATH_C, PATH_MISSING_1, PATH_A, PATH_MISSING_2))

        assertEquals(listOf(PATH_C, PATH_A), result.imagePaths)
        assertEquals(2, result.missingImageCount)
        assertEquals("Compras", result.text)
    }

    @Test
    fun `given all images missing when building share then shares no images and counts all`() = runTest {
        val result = share(content = "Leche", imagePaths = listOf(PATH_A, PATH_B))

        assertTrue(result.imagePaths.isEmpty())
        assertEquals(2, result.missingImageCount)
        assertEquals("Leche", result.text)
    }

    @Test
    fun `given empty note when building share then nothing is shared`() = runTest {
        val result = share()

        assertEquals(NoteShare(subject = null, text = null, imagePaths = emptyList(), missingImageCount = 0), result)
    }

    private companion object {
        const val PATH_A = "/files/note_images/a.jpg"
        const val PATH_B = "/files/note_images/b.jpg"
        const val PATH_C = "/files/note_images/c.jpg"
        const val PATH_MISSING_1 = "/files/note_images/missing1.jpg"
        const val PATH_MISSING_2 = "/files/note_images/missing2.jpg"
    }
}
