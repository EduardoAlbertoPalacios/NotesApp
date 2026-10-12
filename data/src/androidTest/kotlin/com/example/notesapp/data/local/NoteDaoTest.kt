package com.example.notesapp.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {

    private lateinit var database: NotesDatabase
    private lateinit var dao: NoteDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NotesDatabase::class.java,
        ).build()
        dao = database.noteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun inserted_notes_are_observed_with_generated_ids() = runTest {
        val note = NoteEntity(title = "Ideas", content = "Libreta", color = "SAND", isPinned = false, updatedAt = 5)

        dao.insertAll(listOf(note))

        dao.observeAll().test {
            assertEquals(listOf(NoteWithImages(note.copy(id = 1), emptyList())), awaitItem())
        }
    }

    @Test
    fun insert_update_get_and_delete_note() = runTest {
        val id = dao.insert(NoteEntity(title = "Ideas", content = "", color = "AQUA", isPinned = false, updatedAt = 1))

        val updated = NoteEntity(id = id, title = "Ideas", content = "Libreta", color = "AQUA", isPinned = true, updatedAt = 2)
        assertEquals(1, dao.update(updated))
        assertEquals(updated, dao.getById(id)?.note)

        dao.deleteById(id)
        assertEquals(null, dao.getById(id))
        assertEquals(0, dao.update(updated))
    }

    @Test
    fun seed_callback_inserts_sample_notes_on_create() = runTest {
        val seeded = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NotesDatabase::class.java,
        ).addCallback(SeedNotesCallback(now = { 1_000_000_000L })).build()

        seeded.noteDao().observeAll().test {
            assertEquals(SeedNotesCallback.seedNotes(1_000_000_000L).size, awaitItem().size)
        }
        seeded.close()
    }

    @Test
    fun given_note_with_category_when_inserted_then_persists_category() = runTest {
        val id = dao.insert(
            NoteEntity(title = "Sprint", content = "", color = "AQUA", isPinned = false, updatedAt = 1, category = "WORK"),
        )

        assertEquals("WORK", dao.getById(id)?.note?.category)
    }

    @Test
    fun given_note_with_category_when_category_changed_or_removed_then_persists_change() = runTest {
        val note = NoteEntity(title = "Compra", content = "", color = "ROSE", isPinned = false, updatedAt = 1, category = "WORK")
        val id = dao.insert(note)

        dao.update(note.copy(id = id, category = "PERSONAL"))
        assertEquals("PERSONAL", dao.getById(id)?.note?.category)

        dao.update(note.copy(id = id, category = null))
        assertEquals(null, dao.getById(id)?.note?.category)
    }

    @Test
    fun given_seed_callback_when_created_then_seeded_notes_keep_their_categories() = runTest {
        val seeded = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NotesDatabase::class.java,
        ).addCallback(SeedNotesCallback(now = { 1_000_000_000L })).build()

        seeded.noteDao().observeAll().test {
            val byTitle = awaitItem().associate { it.note.title to it.note.category }
            assertEquals(
                mapOf(
                    "Reunión de equipo" to "WORK",
                    "La compra" to "PERSONAL",
                    "Ideas sueltas" to "PERSONAL",
                    "Escapada" to "PERSONAL",
                    "Para leer" to "PERSONAL",
                    "Una buena idea" to null,
                ),
                byTitle,
            )
        }
        seeded.close()
    }

    @Test
    fun given_note_when_images_inserted_then_returns_ids_and_reads_them_with_the_note() = runTest {
        val noteId = dao.insert(noteEntity())

        val ids = dao.insertImages(listOf(image(noteId, "/a.jpg"), image(noteId, "/b.jpg")))

        assertEquals(2, ids.size)
        assertEquals(
            listOf(NoteImageEntity(ids[0], noteId, "/a.jpg"), NoteImageEntity(ids[1], noteId, "/b.jpg")),
            dao.getById(noteId)?.images,
        )
    }

    @Test
    fun given_two_notes_with_images_when_observing_then_each_note_gets_only_its_images() = runTest {
        val first = dao.insert(noteEntity(title = "Primera"))
        val second = dao.insert(noteEntity(title = "Segunda"))
        dao.insertImages(listOf(image(first, "/a.jpg"), image(second, "/b.jpg")))

        dao.observeAll().test {
            val pathsByTitle = awaitItem().associate { it.note.title to it.images.map(NoteImageEntity::path) }
            assertEquals(mapOf("Primera" to listOf("/a.jpg"), "Segunda" to listOf("/b.jpg")), pathsByTitle)
        }
    }

    @Test
    fun given_observed_note_when_image_inserted_then_emits_note_with_image() = runTest {
        val noteId = dao.insert(noteEntity())

        dao.observeAll().test {
            assertTrue(awaitItem().single().images.isEmpty())

            dao.insertImages(listOf(image(noteId, "/a.jpg")))

            assertEquals(listOf("/a.jpg"), awaitItem().single().images.map { it.path })
        }
    }

    @Test
    fun given_note_with_images_when_note_deleted_then_its_images_are_deleted_in_cascade() = runTest {
        val deleted = dao.insert(noteEntity(title = "Borrar"))
        val kept = dao.insert(noteEntity(title = "Conservar"))
        dao.insertImages(listOf(image(deleted, "/a.jpg"), image(kept, "/b.jpg")))

        dao.deleteById(deleted)

        assertEquals(listOf(kept to "/b.jpg"), imagesInTable())
    }

    @Test
    fun given_note_with_images_when_one_image_deleted_then_keeps_the_others() = runTest {
        val noteId = dao.insert(noteEntity())
        val ids = dao.insertImages(listOf(image(noteId, "/a.jpg"), image(noteId, "/b.jpg")))

        dao.deleteImage(ids[0])

        assertEquals(listOf("/b.jpg"), dao.getById(noteId)?.images?.map { it.path })
    }

    @Test
    fun given_image_for_missing_note_when_inserted_then_foreign_key_rejects_it() = runTest {
        val result = runCatching { dao.insertImages(listOf(image(noteId = 99, path = "/a.jpg"))) }

        assertTrue(result.exceptionOrNull() is SQLiteConstraintException)
    }

    /** Lee la tabla directamente para comprobar que no quedan filas huérfanas. */
    private fun imagesInTable(): List<Pair<Long, String>> =
        database.query("SELECT note_id, path FROM note_images", null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getLong(0) to cursor.getString(1))
            }
        }

    private fun noteEntity(title: String = "Ideas") =
        NoteEntity(title = title, content = "", color = "AQUA", isPinned = false, updatedAt = 1)

    private fun image(noteId: Long, path: String) = NoteImageEntity(noteId = noteId, path = path)
}
