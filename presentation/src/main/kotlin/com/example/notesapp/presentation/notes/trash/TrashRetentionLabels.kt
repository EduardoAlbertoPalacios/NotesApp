package com.example.notesapp.presentation.notes.trash

import androidx.annotation.StringRes
import com.example.notesapp.domain.notes.trash.TrashRetention
import com.example.notesapp.presentation.R

/** Nombre visible del plazo de la papelera. */
@StringRes
internal fun TrashRetention.labelRes(): Int = when (this) {
    TrashRetention.DAYS_7 -> R.string.trash_retention_7_days
    TrashRetention.DAYS_30 -> R.string.trash_retention_30_days
    TrashRetention.DAYS_90 -> R.string.trash_retention_90_days
    TrashRetention.NEVER -> R.string.trash_retention_never
}
