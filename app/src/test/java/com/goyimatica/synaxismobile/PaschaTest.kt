package com.goyimatica.synaxismobile

import com.goyimatica.synaxismobile.core.CalStyle
import com.goyimatica.synaxismobile.core.Cal
import com.goyimatica.synaxismobile.core.FastLevel
import com.goyimatica.synaxismobile.core.Pascha
import com.goyimatica.synaxismobile.core.fastFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PaschaTest {

    @Test
    fun `pascha falls on the known dates`() {
        assertEquals(LocalDate.of(2026, 4, 12), Pascha.of(2026))
        assertEquals(LocalDate.of(2027, 5, 2), Pascha.of(2027))
        assertEquals(LocalDate.of(2028, 4, 16), Pascha.of(2028))
        assertEquals(LocalDate.of(2029, 4, 8), Pascha.of(2029))
        assertEquals(LocalDate.of(2030, 4, 28), Pascha.of(2030))
    }

    @Test
    fun `pascha is always a sunday`() {
        for (y in 2025..2040) {
            assertEquals("Pascha $y", java.time.DayOfWeek.SUNDAY, Pascha.of(y).dayOfWeek)
        }
    }

    @Test
    fun `the first sunday of great lent 2026 is the first of march`() {
        assertEquals(LocalDate.of(2026, 3, 1), Pascha.of(2026).minusDays(42))
    }

    @Test
    fun `the nativity falls on the seventh of january on the old calendar`() {
        assertEquals(LocalDate.of(2026, 1, 7), Cal.civilOf(2025, 12, 25, CalStyle.JULIAN).plusYears(0))
        assertEquals(LocalDate.of(2026, 12, 25), Cal.civilOf(2026, 12, 25, CalStyle.REVISED))
    }

    @Test
    fun `bright week is fast free even on the friday`() {
        val brightFriday = Pascha.of(2026).plusDays(5)
        assertEquals(java.time.DayOfWeek.FRIDAY, brightFriday.dayOfWeek)
        assertEquals(FastLevel.NONE, fastFor(brightFriday, CalStyle.JULIAN).level)
    }

    @Test
    fun `great and holy friday is the strictest day of the year`() {
        assertEquals(FastLevel.STRICT, fastFor(Pascha.of(2026).minusDays(2), CalStyle.JULIAN).level)
    }

    @Test
    fun `an ordinary wednesday is a fast day`() {
        // 15 July 2026: a Wednesday, outside every fast
        val d = LocalDate.of(2026, 7, 15)
        assertEquals(java.time.DayOfWeek.WEDNESDAY, d.dayOfWeek)
        assertTrue(fastFor(d, CalStyle.JULIAN).level.isFast)
    }

    @Test
    fun `every day of four years has a rule and a plate`() {
        var d = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2030, 1, 1)
        while (d.isBefore(end)) {
            val f = fastFor(d, CalStyle.JULIAN)
            if (f.level != FastLevel.NONE) {
                assertTrue("no label on $d", f.label.isNotEmpty())
                assertTrue("nothing to eat on $d", f.eat.isNotEmpty())
            }
            d = d.plusDays(1)
        }
    }
}