package com.example.notesapp.domain.settings

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Observa si el guardado automático está encendido; sin valor disponible, lo está. */
class GetAutoSaveEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<Boolean> =
        repository.observeAutoSaveEnabled().map { it ?: DEFAULT }

    companion object {
        /** Encendido: es como guardaba la app antes de existir el ajuste. */
        const val DEFAULT = true
    }
}
