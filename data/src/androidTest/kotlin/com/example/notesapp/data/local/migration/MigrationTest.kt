package com.example.notesapp.data.local.migration

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notesapp.data.local.NoteImageEntity
import com.example.notesapp.data.local.NotesDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NotesDatabase::class.java,
    )

    @Test
    fun given_v1_database_with_note_when_migrating_to_v2_then_keeps_note_with_null_category() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(INSERT_V1_NOTE)
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true)

        db.query("SELECT id, title, content, color, is_pinned, updated_at, category FROM notes").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(1L, cursor.getLong(0))
            assertEquals("Ideas", cursor.getString(1))
            assertEquals("Libreta", cursor.getString(2))
            assertEquals("SAND", cursor.getString(3))
            assertEquals(1, cursor.getInt(4))
            assertEquals(5L, cursor.getLong(5))
            assertTrue(cursor.isNull(6))
        }
        db.close()
    }

    @Test
    fun given_v1_database_when_opened_with_room_then_reads_migrated_note() = runTest {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(INSERT_V1_NOTE)
            close()
        }

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NotesDatabase::class.java,
            TEST_DB,
        ).build()
        val note = database.noteDao().getById(1)
        database.close()

        assertEquals("Ideas", note?.note?.title)
        assertEquals(null, note?.note?.category)
        assertEquals(emptyList<Any>(), note?.images)
    }

    @Test
    fun given_v2_database_with_note_when_migrating_to_v3_then_keeps_note_and_creates_empty_images_table() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(INSERT_V2_NOTE)
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true)

        db.query("SELECT id, title, content, color, is_pinned, updated_at, category FROM notes").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(1L, cursor.getLong(0))
            assertEquals("Ideas", cursor.getString(1))
            assertEquals("Libreta", cursor.getString(2))
            assertEquals("SAND", cursor.getString(3))
            assertEquals(1, cursor.getInt(4))
            assertEquals(5L, cursor.getLong(5))
            assertEquals("WORK", cursor.getString(6))
        }
        db.query("SELECT COUNT(*) FROM note_images").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun given_v1_database_when_opened_with_room_then_reaches_v3_and_accepts_images() = runTest {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(INSERT_V1_NOTE)
            close()
        }

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NotesDatabase::class.java,
            TEST_DB,
        ).build()
        val dao = database.noteDao()
        dao.insertImages(listOf(NoteImageEntity(noteId = 1, path = "/a.jpg")))
        val note = dao.getById(1)
        val version = database.openHelper.readableDatabase.version
        database.close()

        assertEquals(3, version)
        assertEquals("Ideas", note?.note?.title)
        assertEquals(listOf("/a.jpg"), note?.images?.map { it.path })
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
        const val INSERT_V1_NOTE =
            "INSERT INTO notes (id, title, content, color, is_pinned, updated_at) " +
                "VALUES (1, 'Ideas', 'Libreta', 'SAND', 1, 5)"
        const val INSERT_V2_NOTE =
            "INSERT INTO notes (id, title, content, color, is_pinned, updated_at, category) " +
                "VALUES (1, 'Ideas', 'Libreta', 'SAND', 1, 5, 'WORK')"
    }
}
