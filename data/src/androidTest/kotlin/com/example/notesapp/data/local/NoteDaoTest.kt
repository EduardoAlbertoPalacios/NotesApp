package com.example.notesapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
            assertEquals(listOf(note.copy(id = 1)), awaitItem())
        }
    }

    @Test
    fun insert_update_get_and_delete_note() = runTest {
        val id = dao.insert(NoteEntity(title = "Ideas", content = "", color = "AQUA", isPinned = false, updatedAt = 1))

        val updated = NoteEntity(id = id, title = "Ideas", content = "Libreta", color = "AQUA", isPinned = true, updatedAt = 2)
        assertEquals(1, dao.update(updated))
        assertEquals(updated, dao.getById(id))

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

        assertEquals("WORK", dao.getById(id)?.category)
    }

    @Test
    fun given_note_with_category_when_category_changed_or_removed_then_persists_change() = runTest {
        val note = NoteEntity(title = "Compra", content = "", color = "ROSE", isPinned = false, updatedAt = 1, category = "WORK")
        val id = dao.insert(note)

        dao.update(note.copy(id = id, category = "PERSONAL"))
        assertEquals("PERSONAL", dao.getById(id)?.category)

        dao.update(note.copy(id = id, category = null))
        assertEquals(null, dao.getById(id)?.category)
    }

    @Test
    fun given_seed_callback_when_created_then_seeded_notes_keep_their_categories() = runTest {
        val seeded = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NotesDatabase::class.java,
        ).addCallback(SeedNotesCallback(now = { 1_000_000_000L })).build()

        seeded.noteDao().observeAll().test {
            val byTitle = awaitItem().associate { it.title to it.category }
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
}
