package io.github.khaledsabry255.ncmpermission.data

import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Dates are stored as dd/mm/yyyy text, not ISO, so they are parsed by hand.
 * Nothing is handed to a locale-dependent parser, and impossible dates such as
 * 31/02 are rejected instead of silently rolling into March.
 */
object Dates {

    private val DMY = Regex("""^(\d{1,2})[/.\-](\d{1,2})[/.\-](\d{4})$""")
    private val YMD = Regex("""^(\d{4})[/.\-](\d{1,2})[/.\-](\d{1,2})""")

    /** Returns the date as yyyy, mm, dd, or null when the text is not a date. */
    fun parse(value: String?): Triple<Int, Int, Int>? {
        val s = value?.trim().orEmpty()
        if (s.isEmpty()) return null

        val (y, m, d) = DMY.find(s)?.let {
            Triple(it.groupValues[3].toInt(), it.groupValues[2].toInt(), it.groupValues[1].toInt())
        } ?: YMD.find(s)?.let {
            Triple(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt())
        } ?: return null

        if (y < 1900 || y > 2200 || m < 1 || m > 12 || d < 1 || d > 31) return null

        val cal = Calendar.getInstance().apply {
            isLenient = false
            clear()
            set(y, m - 1, d)
        }
        return try {
            cal.time                                   // throws on 31/02 and friends
            Triple(y, m, d)
        } catch (e: Exception) {
            null
        }
    }

    fun format(value: String?): String? {
        val (y, m, d) = parse(value) ?: return null
        return "%02d/%02d/%04d".format(d, m, y)
    }

    /**
     * Whole-day difference from calendar values, so daylight-saving shifts
     * cannot move a permit by a day.
     */
    fun daysLeft(value: String?): Int? {
        val (y, m, d) = parse(value) ?: return null
        val target = Calendar.getInstance().apply { clear(); set(y, m - 1, d) }
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val diff = target.timeInMillis - today.timeInMillis
        return Math.round(diff.toDouble() / TimeUnit.DAYS.toMillis(1)).toInt()
    }
}
