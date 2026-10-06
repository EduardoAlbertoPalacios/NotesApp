package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.result.map
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Notas ordenadas: primero las fijadas y, dentro de cada grupo, la última edición más reciente. */
class GetNotesUseCase @Inject constructor(
    private val repository: NoteRepository,
) {
    operator fun invoke(): Flow<AppResult<List<Note>, NoteError>> =
        repository.observeNotes().map { result -> result.map { notes -> notes.sortedWith(NoteOrder) } }

    private companion object {
        val NoteOrder = compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt }
    }
}
