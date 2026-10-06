package com.example.notesapp.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.notesapp.domain.notes.NoteColor
import java.util.concurrent.TimeUnit

/** Inserta notas de ejemplo la primera vez que se crea la base, para que la app no arranque vacía. */
internal class SeedNotesCallback(
    private val now: () -> Long = System::currentTimeMillis,
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        val createdAt = now()
        seedNotes(createdAt).forEach { note ->
            db.insert("notes", SQLiteDatabase.CONFLICT_ABORT, note.toContentValues())
        }
    }

    private fun NoteEntity.toContentValues() = ContentValues().apply {
        put("title", title)
        put("content", content)
        put("color", color)
        put("is_pinned", isPinned)
        put("updated_at", updatedAt)
    }

    companion object {
        fun seedNotes(now: Long): List<NoteEntity> = listOf(
            seed(
                title = "Reunión de equipo",
                content = "Nuevo lanzamiento:\n• Inicio más sencillo\n• Textos claros\n• Prototipo el viernes",
                color = NoteColor.AQUA,
                isPinned = true,
                updatedAt = now - TimeUnit.MINUTES.toMillis(3),
            ),
            seed(
                title = "La compra",
                content = "☐  Tomates\n☐  Leche de avena\n☑  Pan integral\n☐  Café",
                color = NoteColor.ROSE,
                updatedAt = now - TimeUnit.MINUTES.toMillis(85),
            ),
            seed(
                title = "Ideas sueltas",
                content = "Un café, una libreta y tiempo para crear.",
                color = NoteColor.SAND,
                updatedAt = now - TimeUnit.DAYS.toMillis(1),
            ),
            seed(
                title = "Escapada",
                content = "La sierra nos espera.\n\nLlevar la cámara y desconectar.",
                color = NoteColor.LAVENDER,
                updatedAt = now - TimeUnit.DAYS.toMillis(1) - TimeUnit.HOURS.toMillis(1),
            ),
            seed(
                title = "Para leer",
                content = "Hábitos atómicos",
                color = NoteColor.MINT,
                updatedAt = now - TimeUnit.DAYS.toMillis(2),
            ),
            seed(
                title = "Una buena idea",
                content = "Hacer más de lo que me hace bien.",
                color = NoteColor.PEACH,
                updatedAt = now - TimeUnit.DAYS.toMillis(3),
            ),
        )

        private fun seed(
            title: String,
            content: String,
            color: NoteColor,
            updatedAt: Long,
            isPinned: Boolean = false,
        ) = NoteEntity(
            title = title,
            content = content,
            color = color.name,
            isPinned = isPinned,
            updatedAt = updatedAt,
        )
    }
}
