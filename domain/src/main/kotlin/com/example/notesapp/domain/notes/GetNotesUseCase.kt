package com.example.notesapp.domain.notes

import com.example.notesapp.common.dispatchers.DefaultDispatcher
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.result.map
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Notas ordenadas: primero las fijadas y, dentro de cada grupo, la última edición más reciente.
 * Sin [query], devuelve las de [category] (todas si es nula). Con [query], busca en todas las notas
 * e ignora [category]. El resultado incluye siempre el total de notas del usuario.
 */
class GetNotesUseCase @Inject constructor(
    private val repository: NoteRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) {
    operator fun invoke(
        category: NoteCategory? = null,
        query: String = "",
    ): Flow<AppResult<FilteredNotes, NoteError>> {
        val words = NoteTextMatcher.words(query)
        return repository.observeNotes()
            .map { result ->
                result.map { notes ->
                    val filtered = if (words.isEmpty()) {
                        notes.filter { category == null || it.category == category }
                    } else {
                        notes.filter { NoteTextMatcher.matches(it, words) }
                    }
                    FilteredNotes(notes = filtered.sortedWith(NoteOrder), totalCount = notes.size)
                }
            }
            // Normalizar el texto de cada nota no debe ocupar el hilo principal.
            .flowOn(dispatcher)
    }

    private companion object {
        val NoteOrder = compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt }
    }
}
