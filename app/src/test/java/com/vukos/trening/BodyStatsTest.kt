package com.vukos.trening

import com.vukos.trening.data.BodyEntry
import com.vukos.trening.data.BodyStats
import com.vukos.trening.data.ChartRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BodyStatsTest {
    private fun e(id: String, m: Int, d: Int, kg: Double?) =
        BodyEntry(id = id, epochDay = LocalDate.of(2026, m, d).toEpochDay(), weightKg = kg)

    @Test fun empty() {
        val s = BodyStats.compute(emptyList(), emptySet())
        assertEquals(0, s.count); assertNull(s.current); assertNull(s.lastChange)
    }

    @Test fun singleEntryHasNoChange() {
        val s = BodyStats.compute(listOf(e("a", 11, 15, 82.0)), emptySet())
        assertEquals(82.0, s.current!!, 0.0); assertNull(s.lastChange); assertNull(s.totalChange)
    }

    @Test fun changesAndExtremes() {
        val list = listOf(
            e("a", 11, 15, 84.0), e("b", 11, 22, 83.0), e("c", 11, 29, 83.5),
            e("d", 12, 6, null), e("e", 12, 20, 81.0),
        )
        val s = BodyStats.compute(list.shuffled(), setOf("a", "c", "d"))
        assertEquals(4, s.count)
        assertEquals(81.0, s.current!!, 1e-9)
        assertEquals(-2.5, s.lastChange!!, 1e-9)       // 81 - 83.5
        assertEquals(-3.0, s.totalChange!!, 1e-9)      // 81 - 84
        assertEquals(-3.0, s.change30!!, 1e-9)         // poslednje pre 30+ dana = 15.11 (84)
        assertEquals(81.0, s.min!!, 1e-9); assertEquals(84.0, s.max!!, 1e-9)
        assertEquals(3, s.photoDays)
    }

    @Test fun rangeFilter() {
        val today = LocalDate.of(2026, 12, 20)
        val list = listOf(e("a", 6, 1, 90.0), e("b", 11, 22, 83.0), e("c", 12, 20, 81.0))
        assertEquals(2, ChartRange.M1.filter(list, today).size)
        assertEquals(3, ChartRange.ALL.filter(list, today).size)
        assertEquals(2, ChartRange.M3.filter(list, today).size)
    }
}
