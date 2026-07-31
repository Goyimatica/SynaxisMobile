package com.goyimatica.synaxismobile.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/*
 * The Paschalion. Meeus's Julian computus, which every Orthodox Church keeps
 * — Old Calendar and New alike — which is why Pascha falls on the same civil
 * day for all of them while Christmas does not.
 *
 * The algorithm yields a JULIAN date. Adding thirteen days puts it in the
 * civil Gregorian calendar, valid 1900–2099.
 *
 * Verified: 2026-04-12, 2027-05-02, 2028-04-16, 2029-04-08, 2030-04-28.
 * PaschaTest.kt asserts all five. Do not "tidy" this function.
 */
object Pascha {

    const val JULIAN_SHIFT = 13L

    private val cache = HashMap<Int, LocalDate>(32)

    fun of(year: Int): LocalDate = cache.getOrPut(year) {
        val a = year % 4
        val b = year % 7
        val c = year % 19
        val d = (19 * c + 15) % 30
        val e = (2 * a + 4 * b - d + 34) % 7
        val t = d + e + 114
        val month = t / 31          // 3 = March, 4 = April
        val day = (t % 31) + 1
        LocalDate.of(year, month, day).plusDays(JULIAN_SHIFT)
    }

    /* Days from Pascha, measured against whichever Pascha is nearest — so a day
       in February 2027 is counted back from Pascha 2027, not forward from 2026. */
    fun offset(date: LocalDate): Long {
        val y = date.year
        var best = Long.MAX_VALUE
        for (yy in (y - 1)..(y + 1)) {
            val o = ChronoUnit.DAYS.between(of(yy), date)
            if (kotlin.math.abs(o) < kotlin.math.abs(best)) best = o
        }
        return best
    }

    /* "Great Lent, week 3", "Bright Week", "Week 8 after Pentecost" — the line
       under the date on the Today screen. */
    fun weekLabel(po: Long): String = when {
        po == 0L -> "Pascha"
        po in 1L..6L -> "Bright Week"
        po in -6L..-1L -> "Holy Week"
        po in -48L..-7L -> "Great Lent, week " + (((po + 48) / 7) + 1)
        po in -70L..-49L -> "The Triodion"
        po in 7L..48L -> "Pascha, week " + ((po / 7) + 1)
        po == 49L -> "Pentecost"
        po > 49 -> "Week " + ((po - 49 + 6) / 7) + " after Pentecost"
        else -> ""
    }
}