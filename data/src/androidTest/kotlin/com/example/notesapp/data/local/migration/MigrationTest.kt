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
        ).addMigrations(*ALL_MIGRATIONS).build()
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
    fun given_v1_database_when_opened_with_room_then_reaches_latest_version_and_accepts_images() = runTest {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(INSERT_V1_NOTE)
            close()
        }

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NotesDatabase::class.java,
            TEST_DB,
        ).addMigrations(*ALL_MIGRATIONS).build()
        val dao = database.noteDao()
        dao.insertImages(listOf(NoteImageEntity(noteId = 1, path = "/a.jpg")))
        val note = dao.getById(1)
        val version = database.openHelper.readableDatabase.version
        database.close()

        assertEquals(6, version)
        assertEquals("Ideas", note?.note?.title)
        assertEquals("", note?.note?.contentSpans)
        assertEquals("", note?.note?.contentTasks)
        assertEquals(listOf("/a.jpg"), note?.images?.map { it.path })
    }

    @Test
    fun given_v3_database_with_note_and_image_when_migrating_to_v4_then_keeps_them_with_empty_spans() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(INSERT_V2_NOTE)
            execSQL(INSERT_V3_IMAGE)
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true)

        db.query("SELECT id, title, content, color, is_pinned, updated_at, category, content_spans FROM notes").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(1L, cursor.getLong(0))
            assertEquals("Ideas", cursor.getString(1))
            assertEquals("Libreta", cursor.getString(2))
            assertEquals("SAND", cursor.getString(3))
            assertEquals(1, cursor.getInt(4))
            assertEquals(5L, cursor.getLong(5))
            assertEquals("WORK", cursor.getString(6))
            assertEquals("", cursor.getString(7))
        }
        db.query("SELECT id, note_id, path FROM note_images").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(1L, cursor.getLong(0))
            assertEquals(1L, cursor.getLong(1))
            assertEquals("/a.jpg", cursor.getString(2))
        }
        db.close()
    }

    @Test
    fun given_v3_database_when_opened_with_room_then_reads_note_without_spans() = runTest {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(INSERT_V2_NOTE)
            close()
        }

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NotesDatabase::class.java,
            TEST_DB,
        ).addMigrations(*ALL_MIGRATIONS).build()
        val note = database.noteDao().getById(1)
        database.close()

        assertEquals("Libreta", note?.note?.content)
        assertEquals("", note?.note?.contentSpans)
    }

    @Test
    fun given_v4_database_with_notes_when_migrating_to_v5_then_sand_becomes_vanilla_and_the_rest_is_kept() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(INSERT_V4_SAND_NOTE)
            execSQL(INSERT_V4_AQUA_NOTE)
            execSQL(INSERT_V4_ROSE_NOTE)
            execSQL(INSERT_V3_IMAGE)
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        db.query(
            "SELECT id, title, content, color, is_pinned, updated_at, category, content_spans FROM notes ORDER BY id",
        ).use { cursor ->
            assertEquals(3, cursor.count)
            cursor.moveToNext()
            assertEquals(1L, cursor.getLong(0))
            assertEquals("Ideas", cursor.getString(1))
            assertEquals("Libreta", cursor.getString(2))
            assertEquals("VANILLA", cursor.getString(3))
            assertEquals(1, cursor.getInt(4))
            assertEquals(5L, cursor.getLong(5))
            assertEquals("WORK", cursor.getString(6))
            assertEquals("0,3,B", cursor.getString(7))
            cursor.moveToNext()
            assertEquals(2L, cursor.getLong(0))
            assertEquals("Compra", cursor.getString(1))
            assertEquals("Pan", cursor.getString(2))
            assertEquals("AQUA", cursor.getString(3))
            assertEquals(0, cursor.getInt(4))
            assertEquals(6L, cursor.getLong(5))
            assertTrue(cursor.isNull(6))
            assertEquals("", cursor.getString(7))
            cursor.moveToNext()
            assertEquals(3L, cursor.getLong(0))
            assertEquals("Viaje", cursor.getString(1))
            assertEquals("Maleta", cursor.getString(2))
            assertEquals("ROSE", cursor.getString(3))
            assertEquals(0, cursor.getInt(4))
            assertEquals(7L, cursor.getLong(5))
            assertEquals("PERSONAL", cursor.getString(6))
            assertEquals("", cursor.getString(7))
        }
        db.query("SELECT id, note_id, path FROM note_images").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(1L, cursor.getLong(0))
            assertEquals(1L, cursor.getLong(1))
            assertEquals("/a.jpg", cursor.getString(2))
        }
        db.close()
    }

    @Test
    fun given_v5_database_with_legacy_markers_when_migrating_to_v6_then_converts_them_to_tasks_and_keeps_the_rest() {
        val panStart = LEGACY_CONTENT.indexOf("Pan")
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                "INSERT INTO notes (id, title, content, color, is_pinned, updated_at, category, content_spans) " +
                    "VALUES (1, 'Compra', ?, 'VANILLA', 0, 42, 'PERSONAL', ?)",
                arrayOf<Any>(LEGACY_CONTENT, "$panStart,${panStart + 3},B"),
            )
            execSQL(INSERT_V5_PLAIN_NOTE)
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        db.query("SELECT id, content, content_spans, content_tasks, updated_at, title, color FROM notes ORDER BY id").use { cursor ->
            assertEquals(2, cursor.count)
            cursor.moveToNext()
            val content = cursor.getString(1)
            val (start, end, format) = cursor.getString(2).split(",")
            assertEquals(1L, cursor.getLong(0))
            assertEquals("Tomates\nPan integral\nTexto normal ☐ en medio", content)
            assertEquals("Pan", content.substring(start.toInt(), end.toInt()))
            assertEquals("B", format)
            assertEquals("0,0;1,1", cursor.getString(3))
            assertEquals(42L, cursor.getLong(4))
            assertEquals("Compra", cursor.getString(5))
            assertEquals("VANILLA", cursor.getString(6))
            cursor.moveToNext()
            assertEquals(2L, cursor.getLong(0))
            assertEquals("Libreta de ideas", cursor.getString(1))
            assertEquals("0,7,B", cursor.getString(2))
            assertEquals("", cursor.getString(3))
            assertEquals(7L, cursor.getLong(4))
            assertEquals("Ideas", cursor.getString(5))
            assertEquals("ROSE", cursor.getString(6))
        }
        db.close()
    }

    private companion object {
        const val LEGACY_CONTENT = "☐  Tomates\n☑  Pan integral\nTexto normal ☐ en medio"
        const val INSERT_V5_PLAIN_NOTE =
            "INSERT INTO notes (id, title, content, color, is_pinned, updated_at, category, content_spans) " +
                "VALUES (2, 'Ideas', 'Libreta de ideas', 'ROSE', 1, 7, NULL, '0,7,B')"
        const val TEST_DB = "migration-test.db"
        const val INSERT_V1_NOTE =
            "INSERT INTO notes (id, title, content, color, is_pinned, updated_at) " +
                "VALUES (1, 'Ideas', 'Libreta', 'SAND', 1, 5)"
        const val INSERT_V2_NOTE =
            "INSERT INTO notes (id, title, content, color, is_pinned, updated_at, category) " +
                "VALUES (1, 'Ideas', 'Libreta', 'SAND', 1, 5, 'WORK')"
        const val INSERT_V3_IMAGE =
            "INSERT INTO note_images (id, note_id, path) VALUES (1, 1, '/a.jpg')"
        const val INSERT_V4_SAND_NOTE =
            "INSERT INTO notes (id, title, content, color, is_pinned, updated_at, category, content_spans) " +
                "VALUES (1, 'Ideas', 'Libreta', 'SAND', 1, 5, 'WORK', '0,3,B')"
        const val INSERT_V4_AQUA_NOTE =
            "INSERT INTO notes (id, title, content, color, is_pinned, updated_at, category, content_spans) " +
                "VALUES (2, 'Compra', 'Pan', 'AQUA', 0, 6, NULL, '')"
        const val INSERT_V4_ROSE_NOTE =
            "INSERT INTO notes (id, title, content, color, is_pinned, updated_at, category, content_spans) " +
                "VALUES (3, 'Viaje', 'Maleta', 'ROSE', 0, 7, 'PERSONAL', '')"
    }
}
