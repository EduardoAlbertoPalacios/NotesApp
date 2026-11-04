package com.example.notesapp.domain.settings

import com.example.notesapp.domain.notes.NoteColor
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Observa el color de las notas nuevas, con [NoteColor.DEFAULT] si no hay ninguno disponible. */
class GetDefaultNoteColorUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<NoteColor> =
        repository.observeDefaultNoteColor().map { it ?: NoteColor.DEFAULT }
}
