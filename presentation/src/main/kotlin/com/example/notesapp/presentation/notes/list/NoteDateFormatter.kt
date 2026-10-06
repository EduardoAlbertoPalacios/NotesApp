package com.example.notesapp.presentation.notes.list

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/** Convierte la fecha de última edición en la etiqueta relativa que muestra la tarjeta. */
class NoteDateFormatter internal constructor(
    private val now: () -> Long,
    private val locale: Locale,
    private val timeZone: TimeZone,
) {
    @Inject
    constructor() : this(System::currentTimeMillis, Locale.getDefault(), TimeZone.getDefault())

    fun format(epochMillis: Long): NoteDateLabel {
        val today = startOfDay(now())
        val date = startOfDay(epochMillis)
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        return when (date) {
            today -> NoteDateLabel.Today(timeFormat().format(epochMillis))
            yesterday -> NoteDateLabel.Yesterday
            else -> NoteDateLabel.Date(SimpleDateFormat(DAY_MONTH_PATTERN, locale).withZone().format(epochMillis))
        }
    }

    private fun startOfDay(epochMillis: Long): Calendar = Calendar.getInstance(timeZone, locale).apply {
        timeInMillis = epochMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun timeFormat(): DateFormat = DateFormat.getTimeInstance(DateFormat.SHORT, locale).withZone()

    private fun DateFormat.withZone(): DateFormat = apply { timeZone = this@NoteDateFormatter.timeZone }

    private companion object {
        const val DAY_MONTH_PATTERN = "d MMM"
    }
}
