package com.goyimatica.synaxismobile.core

import java.time.LocalDate

/* Which reckoning the user keeps. The Paschalion is identical for both — only
   the fixed feasts move, which is why this is a thirteen-day offset and not a
   second calendar. */
enum class CalStyle(val shift: Long, val display: String) {
    JULIAN(13L, "Julian · Old Calendar"),
    REVISED(0L, "Revised Julian · New Calendar");

    companion object {
        fun from(raw: String?): CalStyle =
            if (raw == "revised") REVISED else JULIAN

        val DEFAULT = JULIAN
    }

    val storageKey: String get() = if (this == REVISED) "revised" else "julian"
}

/* A commemoration. `great` marks the Twelve Feasts and the Great Days, which
   the fasting rules consult — a Great Feast on a Wednesday allows fish. */
data class Feast(
    val name: String,
    val note: String = "",
    val great: Boolean = false,
    val movable: Boolean = false,
)

/* Six degrees, exactly as in FOOD. Ordered from lightest to strictest so that
   comparisons like `level >= FastLevel.XEROPHAGY` mean what they look like. */
enum class FastLevel {
    NONE, DAIRY, FISH, OIL, XEROPHAGY, STRICT;

    val isFast: Boolean get() = this != NONE
    val isStrict: Boolean get() = this == XEROPHAGY || this == STRICT
    val isLight: Boolean get() = this == FISH || this == OIL
}

data class FastRule(
    val level: FastLevel,
    val label: String = "",
    val detail: String = "",
    val eat: List<String> = emptyList(),
    val avoid: List<String> = emptyList(),
)

/* One square in the month grid. Null entries pad the start of the first week. */
data class DayCell(
    val date: LocalDate,
    val day: Int,
    val churchDay: Int,
    val churchKey: String,
    val great: Boolean,
    val isPascha: Boolean,
    val level: FastLevel,
    val fastFree: Boolean,
    val feasts: List<Feast>,
) {
    val fasting: Boolean get() = level.isFast
    val strict: Boolean get() = level.isStrict
    val light: Boolean get() = level.isLight
}

data class MonthGrid(
    val year: Int,
    val month: Int,           // 1..12, unlike the JS which was 0-based
    val name: String,
    val cells: List<DayCell?>,
)

/* Everything the Today screen needs about one day. `saintKey` is the church
   MM-DD; the UI joins the lives to it, so the engine stays free of the data. */
data class DayInfo(
    val date: LocalDate,
    val churchDate: LocalDate,
    val churchKey: String,
    val style: CalStyle,
    val weekLabel: String,
    val paschaOffset: Long,
    val feasts: List<Feast>,
    val fast: FastRule,
) {
    val isPascha: Boolean get() = paschaOffset == 0L
    val hasGreatFeast: Boolean get() = feasts.any { it.great }
}

/* The shape of a year, for the calendar screen's overview. */
data class YearInfo(
    val year: Int,
    val pascha: LocalDate,
    val triodion: LocalDate,
    val meatfare: LocalDate,
    val cheesefare: LocalDate,
    val lentBegins: LocalDate,
    val lazarus: LocalDate,
    val palm: LocalDate,
    val holyThursday: LocalDate,
    val holyFriday: LocalDate,
    val thomas: LocalDate,
    val radonitsa: LocalDate,
    val ascension: LocalDate,
    val pentecost: LocalDate,
    val allSaints: LocalDate,
    val apostlesFast: LocalDate,
    val apostlesEnd: LocalDate,
    val apostlesDays: Long,
    val dormitionFast: LocalDate,
    val dormition: LocalDate,
    val nativityFast: LocalDate,
    val nativity: LocalDate,
    val theophany: LocalDate,
    val meeting: LocalDate,
    val annunciation: LocalDate,
    val transfiguration: LocalDate,
    val nativityTheotokos: LocalDate,
    val elevation: LocalDate,
    val entrance: LocalDate,
    val protection: LocalDate,
    val churchNewYear: LocalDate,
)