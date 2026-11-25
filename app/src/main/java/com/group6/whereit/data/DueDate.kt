package com.group6.whereit.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Helpers for SpotItem.dueDate (epoch millis), which can hold two kinds of value:
 *
 *  - DATE ONLY: midnight UTC of the chosen day. This is what the DatePicker returns and
 *    what older items already have saved, so they keep working unchanged.
 *  - DATE + TIME: the real moment in the phone's time zone. If that happens to land exactly
 *    on midnight UTC, 1 ms is added so it can't be mistaken for a date-only value.
 */
object DueDate {
    private const val DAY_MS = 86_400_000L

    fun hasTime(millis: Long): Boolean = millis % DAY_MS != 0L

    /** Combine a DatePicker value (midnight UTC) with a local hour and minute. */
    fun withTime(dateUtcMidnight: Long, hour: Int, minute: Int): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateUtcMidnight }
        val local = Calendar.getInstance().apply {
            clear()
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), hour, minute)
        }
        val result = local.timeInMillis
        return if (result % DAY_MS == 0L) result + 1 else result
    }

    /** The value to show as selected in the DatePicker (always midnight UTC of the due day). */
    fun pickerDate(millis: Long): Long {
        if (!hasTime(millis)) return millis
        val local = Calendar.getInstance().apply { timeInMillis = millis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    /** Hour and minute of a date+time value, or null for a date-only value. */
    fun timeOf(millis: Long): Pair<Int, Int>? {
        if (!hasTime(millis)) return null
        val local = Calendar.getInstance().apply { timeInMillis = millis }
        return local.get(Calendar.HOUR_OF_DAY) to local.get(Calendar.MINUTE)
    }

    /** "Nov 26, 2026" for date-only values, "Nov 26, 2026, 3:30 PM" when a time is set. */
    fun format(millis: Long): String =
        if (hasTime(millis)) {
            SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault()).format(Date(millis))
        } else {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC") // date-only values are midnight UTC
            }.format(Date(millis))
        }
}
