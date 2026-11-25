package com.example.util

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** A calendar day without time (works on Android 7, no java.time needed). */
data class DayDate(val year: Int, val month: Int, val day: Int) {

    fun toKey(): String = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    fun monthYear(): MonthYear = MonthYear(year, month)

    private fun calendar(): Calendar = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day)
    }

    fun plusDays(n: Int): DayDate = from(calendar().apply { add(Calendar.DAY_OF_MONTH, n) })

    /** e.g. "السبت 22 نوفمبر" */
    fun toArabicDisplay(): String {
        val dow = calendar().get(Calendar.DAY_OF_WEEK)
        return "${ARABIC_DAYS[dow - 1]} $day ${monthYear().toArabicDisplay()}"
    }

    fun shortLabel(): String = "$day/$month"

    /** UTC midnight millis, what the Material date picker uses. */
    fun toUtcMillis(): Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month - 1, day)
    }.timeInMillis

    companion object {
        private val ARABIC_DAYS = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")

        fun today(): DayDate = from(Calendar.getInstance())

        fun from(c: Calendar) = DayDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))

        fun fromUtcMillis(millis: Long): DayDate =
            from(Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis })

        fun fromKey(key: String): DayDate? {
            val p = key.split("-")
            if (p.size != 3) return null
            return DayDate(p[0].toIntOrNull() ?: return null, p[1].toIntOrNull() ?: return null, p[2].toIntOrNull() ?: return null)
        }

        /** First and last key of a month, for "BETWEEN" queries. */
        fun monthRange(m: MonthYear): Pair<String, String> =
            String.format(Locale.US, "%04d-%02d-01", m.year, m.month) to
                String.format(Locale.US, "%04d-%02d-31", m.year, m.month)

        /** First day of [m], or today if today is inside [m]. */
        fun defaultFor(m: MonthYear): DayDate {
            val t = today()
            return if (t.monthYear() == m) t else DayDate(m.year, m.month, 1)
        }
    }
}
