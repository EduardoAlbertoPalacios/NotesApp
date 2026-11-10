package com.example.notesapp.presentation.notes.editor

import android.os.Bundle
import android.os.Parcel
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.notesapp.domain.notes.ChecklistItem
import com.example.notesapp.domain.notes.FormatSpan
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.TextFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Riesgo del plan 009 (sección 9): el borrador viaja en el `Bundle` del estado guardado, que Android limita a
 * alrededor de 1 MB. Se mide una nota de 100 000 caracteres con 500 tramos de formato y 500 tareas.
 */
@RunWith(AndroidJUnit4::class)
class EditorDraftSizeTest {

    @Test
    fun given_draft_of_100k_chars_with_500_spans_and_500_tasks_when_parcelled_then_fits_in_saved_state_limit() {
        val handle = SavedStateHandle(mapOf(NoteEditorViewModel.NOTE_ID_ARG to 7L))
        val draft = largeDraft()
        EditorDraftCodec.write(handle, draft)

        val bundle: Bundle = handle.savedStateProvider().saveState()
        val bytes = parcelledSize(bundle)
        Log.i(TAG, "Borrador de ${draft.content.length} caracteres, ${draft.contentSpans.size} tramos y " +
            "${draft.contentTasks.size} tareas: $bytes bytes")

        assertEquals(CONTENT_LENGTH, draft.content.length)
        assertTrue("El borrador ocupa $bytes bytes; el límite es $SAVED_STATE_LIMIT_BYTES", bytes < SAVED_STATE_LIMIT_BYTES)
    }

    @Test
    fun given_large_draft_when_saved_state_restored_from_parcel_then_reads_the_same_draft() {
        val handle = SavedStateHandle()
        val draft = largeDraft()
        EditorDraftCodec.write(handle, draft)
        val parcel = Parcel.obtain()

        val restored = try {
            parcel.writeBundle(handle.savedStateProvider().saveState())
            parcel.setDataPosition(0)
            val bundle = requireNotNull(parcel.readBundle(javaClass.classLoader))
            SavedStateHandle.createHandle(bundle, null)
        } finally {
            parcel.recycle()
        }

        assertEquals(draft, EditorDraftCodec.read(restored))
    }

    private fun parcelledSize(bundle: Bundle): Int {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(bundle)
            parcel.dataSize()
        } finally {
            parcel.recycle()
        }
    }

    /** 500 líneas de 199 letras más su salto: 100 000 caracteres, un tramo y una tarea por línea. */
    private fun largeDraft(): EditorDraft {
        val lineLength = CONTENT_LENGTH / LINES - 1
        val content = (0 until LINES).joinToString(separator = "\n", postfix = "\n") { line ->
            "${line % 10}".repeat(lineLength)
        }
        val spans = (0 until LINES).map { line ->
            val start = line * (lineLength + 1)
            FormatSpan(start, start + lineLength, TextFormat.entries[line % TextFormat.entries.size])
        }
        val tasks = (0 until LINES).map { line -> ChecklistItem(line, isDone = line % 2 == 0) }
        return EditorDraft(
            title = "Nota muy larga",
            content = content,
            contentSpans = spans,
            contentTasks = tasks,
            isPinned = true,
            category = NoteCategory.WORK,
            color = NoteColor.LAVENDER,
            pendingImagePaths = emptyList(),
            removedImageIds = emptyList(),
        )
    }

    private companion object {
        const val TAG = "EditorDraftSize"
        const val CONTENT_LENGTH = 100_000
        const val LINES = 500
        /** Límite del búfer de transacciones de Binder, que comparte todo el estado guardado de la app. */
        const val SAVED_STATE_LIMIT_BYTES = 1_048_576
    }
}
