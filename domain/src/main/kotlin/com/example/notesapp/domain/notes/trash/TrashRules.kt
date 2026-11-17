package com.example.notesapp.domain.notes.trash

/**
 * Reglas del plazo de la papelera. Los días se cuentan en bloques de 24 h desde que la nota entró en la
 * papelera, no en días de calendario (decisión D3 del plan 012): así no dependen de la zona horaria.
 */
object TrashRules {

    const val DAY_MILLIS = 24L * 60 * 60 * 1000

    /** Si la nota que entró en [trashedAt] ya cumplió el plazo [retention] en [now]. Con `NEVER`, nunca. */
    fun isExpired(trashedAt: Long, retention: TrashRetention, now: Long): Boolean {
        val days = retention.days ?: return false
        return now - trashedAt >= days * DAY_MILLIS
    }

    /**
     * Días enteros que le quedan a la nota antes de borrarse: 0 significa que se borra hoy (en menos de 24 h).
     * Siempre entre 0 y el plazo, aunque el reloj del dispositivo se haya atrasado. Nulo con `NEVER`.
     */
    fun daysLeft(trashedAt: Long, retention: TrashRetention, now: Long): Int? {
        val days = retention.days ?: return null
        val remaining = trashedAt + days * DAY_MILLIS - now
        return (remaining / DAY_MILLIS).coerceIn(0L, days.toLong()).toInt()
    }

    /** Las notas de [notes] que habrán cumplido el plazo [retention] en [now], en el mismo orden. */
    fun expiredBy(notes: List<TrashedNote>, retention: TrashRetention, now: Long): List<TrashedNote> =
        notes.filter { isExpired(it.trashedAt, retention, now) }

    /** Si [new] es más corto que [current]. `NEVER` cuenta como el plazo más largo. */
    fun isShorter(new: TrashRetention, current: TrashRetention): Boolean {
        val newDays = new.days ?: return false
        val currentDays = current.days ?: return true
        return newDays < currentDays
    }
}
