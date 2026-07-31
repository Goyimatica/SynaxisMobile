package com.goyimatica.synaxismobile.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object Cal {

    val MONTHS = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )

    /* Monday first, matching the grid — and matching java.time's DayOfWeek.value */
    val DOW = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val DOW_SHORT = listOf("M", "T", "W", "T", "F", "S", "S")

    val YEARS: List<Int> = (2025..2040).toList()

    /* ─ names and keys ─ */
    fun dayName(d: LocalDate): String = DOW[d.dayOfWeek.value - 1]
    fun monthName(m: Int): String = MONTHS[m - 1]
    fun key(d: LocalDate): String = mmdd(d)
    fun iso(d: LocalDate): String = d.toString()
    fun fmt(d: LocalDate): String =
        "${dayName(d)}, ${d.dayOfMonth} ${monthName(d.monthValue)} ${d.year}"
    fun fmtShort(d: LocalDate): String =
        "${d.dayOfMonth} ${monthName(d.monthValue).take(3)}"

    /* ─ civil ↔ church ─ */
    fun church(civil: LocalDate, style: CalStyle): LocalDate = civil.minusDays(style.shift)
    fun churchKey(civil: LocalDate, style: CalStyle): String = mmdd(church(civil, style))

    /** month is 1..12. Gives the civil day on which a fixed church date falls. */
    fun civilOf(year: Int, month: Int, day: Int, style: CalStyle): LocalDate =
        LocalDate.of(year, month, day).plusDays(style.shift)

    /* "Friday · 18 July on the church calendar · Week 5 after Pentecost" */
    fun churchLine(civil: LocalDate, style: CalStyle): String {
        val ch = church(civil, style)
        val week = Pascha.weekLabel(Pascha.offset(civil))
        val tail = if (style == CalStyle.JULIAN) " on the church calendar" else " · New Calendar"
        val core = "${ch.dayOfMonth} ${monthName(ch.monthValue)}$tail"
        return dayName(civil) + " · " + core + if (week.isNotEmpty()) " · $week" else ""
    }

    /* ─ what is kept on a day ─ */
    fun feastsOn(civil: LocalDate, style: CalStyle): List<Feast> = buildList {
        MOVABLE[Pascha.offset(civil)]?.let { add(it) }
        FIXED[churchKey(civil, style)]?.let { add(it) }
    }

    /* The lives are joined by churchKey in the UI layer, which is why nothing
       here knows that a saint exists. */
    fun dayInfo(civil: LocalDate, style: CalStyle): DayInfo {
        val po = Pascha.offset(civil)
        return DayInfo(
            date = civil,
            churchDate = church(civil, style),
            churchKey = churchKey(civil, style),
            style = style,
            weekLabel = Pascha.weekLabel(po),
            paschaOffset = po,
            feasts = feastsOn(civil, style),
            fast = fastFor(civil, style),
        )
    }

    /* ─ a month for the grid; month is 1..12, weeks begin on Monday ─ */
    fun month(year: Int, month: Int, style: CalStyle): MonthGrid {
        val first = LocalDate.of(year, month, 1)
        val pad = first.dayOfWeek.value - 1          // Monday = 1 → no padding
        val length = first.lengthOfMonth()

        val cells = ArrayList<DayCell?>(pad + length)
        repeat(pad) { cells.add(null) }

        for (day in 1..length) {
            val d = LocalDate.of(year, month, day)
            val ch = church(d, style)
            val po = Pascha.offset(d)
            val f = fastFor(d, style)
            val feasts = feastsOn(d, style)
            cells.add(
                DayCell(
                    date = d,
                    day = day,
                    churchDay = ch.dayOfMonth,
                    churchKey = mmdd(ch),
                    great = feasts.any { it.great },
                    isPascha = po == 0L,
                    level = f.level,
                    fastFree = f.level == FastLevel.NONE && (
                        po == 0L || po in 1L..6L || po in 50L..55L ||
                            po in -69L..-64L || between(mmdd(ch), "12-25", "01-04")
                        ),
                    feasts = feasts,
                ),
            )
        }
        return MonthGrid(year, month, monthName(month), cells)
    }

    /* ─ the shape of a whole year ─ */
    fun yearInfo(year: Int, style: CalStyle): YearInfo {
        val p = Pascha.of(year)
        val apostlesStart = p.plusDays(57)
        val apostlesEnd = civilOf(year, 6, 28, style)     // church 28 June
        return YearInfo(
            year = year,
            pascha = p,
            triodion = p.minusDays(70),
            meatfare = p.minusDays(56),
            cheesefare = p.minusDays(50),
            lentBegins = p.minusDays(48),
            lazarus = p.minusDays(8),
            palm = p.minusDays(7),
            holyThursday = p.minusDays(3),
            holyFriday = p.minusDays(2),
            thomas = p.plusDays(7),
            radonitsa = p.plusDays(9),
            ascension = p.plusDays(39),
            pentecost = p.plusDays(49),
            allSaints = p.plusDays(56),
            apostlesFast = apostlesStart,
            apostlesEnd = apostlesEnd,
            apostlesDays = maxOf(0L, ChronoUnit.DAYS.between(apostlesStart, apostlesEnd) + 1),
            dormitionFast = civilOf(year, 8, 1, style),
            dormition = civilOf(year, 8, 15, style),
            nativityFast = civilOf(year, 11, 15, style),
            nativity = civilOf(year, 12, 25, style),
            theophany = civilOf(year, 1, 6, style),
            meeting = civilOf(year, 2, 2, style),
            annunciation = civilOf(year, 3, 25, style),
            transfiguration = civilOf(year, 8, 6, style),
            nativityTheotokos = civilOf(year, 9, 8, style),
            elevation = civilOf(year, 9, 14, style),
            entrance = civilOf(year, 11, 21, style),
            protection = civilOf(year, 10, 1, style),
            churchNewYear = civilOf(year, 9, 1, style),
        )
    }

    fun count(): Int = FIXED.size + MOVABLE.size
}