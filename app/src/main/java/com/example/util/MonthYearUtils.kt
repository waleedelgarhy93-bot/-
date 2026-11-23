package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MonthYear(
    val year: Int,
    val month: Int // 1..12
) {
    fun toStorageKey(): String = String.format(Locale.US, "%04d-%02d", year, month)

    fun toArabicDisplay(): String {
        val monthName = ARABIC_MONTHS.getOrElse(month - 1) { month.toString() }
        return "$monthName $year"
    }

    fun previous(): MonthYear = if (month == 1) MonthYear(year - 1, 12) else MonthYear(year, month - 1)

    fun next(): MonthYear = if (month == 12) MonthYear(year + 1, 1) else MonthYear(year, month + 1)

    companion object {
        private val ARABIC_MONTHS = listOf(
            "يناير",
            "فبراير",
            "مارس",
            "أبريل",
            "مايو",
            "يونيو",
            "يوليو",
            "أغسطس",
            "سبتمبر",
            "أكتوبر",
            "نوفمبر",
            "ديسمبر"
        )

        fun current(): MonthYear {
            val cal = Calendar.getInstance()
            return MonthYear(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1
            )
        }
    }
}

object Formatters {
    private val numberFormat = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))

    fun formatAmount(amount: Double): String = numberFormat.format(amount)

    fun formatEgp(amount: Double): String = "${numberFormat.format(amount)} ج.م"

    fun formatShortDate(millis: Long?): String {
        if (millis == null) return ""
        val sdf = SimpleDateFormat("d/M", Locale.US)
        return sdf.format(Date(millis))
    }

    fun formatFullDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return sdf.format(Date(millis))
    }
}
