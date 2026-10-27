package com.example.notesapp.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.notesapp.data.notes.decodeSpans
import com.example.notesapp.data.notes.encode
import com.example.notesapp.domain.notes.NoteChecklist
import com.example.notesapp.domain.notes.NoteFormatting

/**
 * v5: el amarillo que se guardaba como `SAND` pasa a llamarse `VANILLA`, y `SAND` queda para el beige «Arena».
 * Solo cambian datos: las notas amarillas conservan su color visible.
 */
internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE notes SET color = 'VANILLA' WHERE color = 'SAND'")
    }
}

/**
 * v6: columna `content_tasks`, vacía para las notas existentes. Además, las líneas que empezaban por «☐»
 * o «☑» escritos como texto pasan a ser tareas reales: se quita el carácter con sus espacios y el formato
 * se recoloca sobre las mismas palabras. La fecha de edición no cambia: no es una edición del usuario.
 */
internal val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN content_tasks TEXT NOT NULL DEFAULT ''")

        // Se leen todas antes de escribir, para no actualizar filas con el cursor abierto.
        val notesWithMarkers = buildList {
            db.query("SELECT id, content, content_spans FROM notes WHERE content LIKE '%☐%' OR content LIKE '%☑%'")
                .use { cursor ->
                    while (cursor.moveToNext()) {
                        add(LegacyNote(cursor.getLong(0), cursor.getString(1), cursor.getString(2)))
                    }
                }
        }
        notesWithMarkers.forEach { note ->
            val conversion = NoteChecklist.fromLegacyMarkers(note.content)
            if (conversion.tasks.isEmpty()) return@forEach
            // De abajo arriba, para que los rangos pendientes sigan apuntando al mismo texto.
            var text = note.content
            var spans = note.spans.decodeSpans(note.content.length)
            conversion.removedRanges.asReversed().forEach { range ->
                val withoutMarker = text.removeRange(range)
                spans = NoteFormatting.onTextChanged(spans, text, withoutMarker, activeFormats = emptySet())
                text = withoutMarker
            }
            db.execSQL(
                "UPDATE notes SET content = ?, content_spans = ?, content_tasks = ? WHERE id = ?",
                arrayOf<Any>(text, spans.encode(), conversion.tasks.encode(), note.id),
            )
        }
    }
}

private class LegacyNote(val id: Long, val content: String, val spans: String)

internal val ALL_MIGRATIONS = arrayOf(MIGRATION_4_5, MIGRATION_5_6)
