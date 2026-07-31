package com.goyimatica.synaxismobile.ui.screens

import com.goyimatica.synaxismobile.core.CalStyle
import com.goyimatica.synaxismobile.core.FIXED
import com.goyimatica.synaxismobile.core.FastRule
import com.goyimatica.synaxismobile.core.Feast
import com.goyimatica.synaxismobile.core.MOVABLE
import com.goyimatica.synaxismobile.core.Pascha
import com.goyimatica.synaxismobile.core.fastFor
import java.time.LocalDate

val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

val DAY_NAMES = listOf(
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
)

/** Monday first, as the church week is counted. */
val DAY_INITIALS = listOf("M", "T", "W", "T", "F", "S", "S")

fun monthName(month: Int): String = MONTH_NAMES[month - 1]

fun dayName(date: LocalDate): String = DAY_NAMES[date.dayOfWeek.value - 1]

/** "Friday, 31 July 2026" */
fun longDate(date: LocalDate): String =
    dayName(date) + ", " + date.dayOfMonth + " " + monthName(date.monthValue) + " " + date.year

/** "31 July" */
fun shortDate(date: LocalDate): String =
    date.dayOfMonth.toString() + " " + monthName(date.monthValue)

/**
 * Everything the app ever needs to know about one day.
 *
 * `churchKey` is the MM-dd of the *church* date, which is what the saints
 * index is keyed on. Under the old reckoning the church date runs thirteen
 * days behind the civil one, which is why the twenty-fifth of December falls
 * on the seventh of January for those who keep it.
 */
data class DayFacts(
    val date: LocalDate,
    val churchDate: LocalDate,
    val churchKey: String,
    val civilLine: String,
    val churchLine: String,
    val season: String,
    val paschaOffset: Long,
    val feasts: List<Feast>,
    val rule: FastRule,
) {
    val great: Boolean get() = feasts.any { it.great }
}

fun factsFor(date: LocalDate, style: CalStyle): DayFacts {
    val church = date.minusDays(style.shift)
    val key = pad(church.monthValue) + "-" + pad(church.dayOfMonth)

    val offset = Pascha.offset(date)
    val movable = MOVABLE[offset]
    val fixed = FIXED[key]

    val feasts = listOfNotNull(movable, fixed)
        .sortedByDescending { it.great }

    return DayFacts(
        date = date,
        churchDate = church,
        churchKey = key,
        civilLine = longDate(date),
        churchLine = shortDate(church) + " " + churchWord(style),
        season = Pascha.weekLabel(offset),
        paschaOffset = offset,
        feasts = feasts,
        rule = fastFor(date, style),
    )
}

private fun churchWord(style: CalStyle): String =
    if (style.shift > 0L) "(old reckoning)" else "(revised reckoning)"

private fun pad(n: Int): String = if (n < 10) "0" + n else n.toString()