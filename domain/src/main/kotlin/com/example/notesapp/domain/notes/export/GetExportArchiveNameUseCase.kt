package com.example.notesapp.domain.notes.export

import com.example.notesapp.common.time.TimeProvider
import java.time.ZoneId
import javax.inject.Inject

/** Nombre que se propone al guardar el .zip, con la fecha de hoy: «Notas AAAA-MM-DD.zip». */
class GetExportArchiveNameUseCase internal constructor(
    private val timeProvider: TimeProvider,
    private val zone: () -> ZoneId,
) {
    @Inject
    constructor(timeProvider: TimeProvider) : this(timeProvider, ZoneId::systemDefault)

    operator fun invoke(): String = NoteExportNaming.archiveName(timeProvider.now(), zone())
}
