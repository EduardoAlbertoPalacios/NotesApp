package com.example.notesapp.domain.notes.export

/**
 * Un .zip ya generado en el almacenamiento temporal de la app, a la espera de copiarse al destino que elija el
 * usuario.
 *
 * @property id identifica el archivo temporal para quien lo generó; no es una ruta que deba interpretarse.
 * @property noteCount número de PDF que contiene.
 * @property missingImageCount imágenes que no se pudieron leer y se omitieron.
 */
data class PreparedExport(
    val id: String,
    val noteCount: Int,
    val missingImageCount: Int,
)
