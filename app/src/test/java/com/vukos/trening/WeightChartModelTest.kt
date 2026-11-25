package com.vukos.trening

import com.vukos.trening.data.WeightChartModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightChartModelTest {
    @Test fun paddedNiceRange() {
        val m = WeightChartModel(listOf(82.4, 82.9, 83.1, 83.6, 84.2, 84.8, 85.3, 86.0), null)
        assertEquals(2.0, m.step, 1e-9)
        assertEquals(80.0, m.yMin, 1e-9)
        assertEquals(88.0, m.yMax, 1e-9)
        assertEquals("80", m.tickLabel(80.0))
        assertTrue(m.ticks.size <= 7)
        assertTrue(m.yMin < 82.4 && m.yMax > 86.0)
    }

    @Test fun smallSpanUsesHalfKg() {
        val m = WeightChartModel(listOf(86.9, 86.1), null)
        assertEquals(0.5, m.step, 1e-9)
        assertEquals(85.5, m.yMin, 1e-9)
        assertEquals(87.5, m.yMax, 1e-9)
        assertEquals("85.5", m.tickLabel(85.5))
    }

    @Test fun singlePointHasTwoSteps() {
        val m = WeightChartModel(listOf(80.0), null)
        assertTrue(m.yMax - m.yMin >= m.step * 2)
        assertEquals(0, m.sel)
    }

    @Test fun selectionAndLabels() {
        val m = WeightChartModel(List(8) { 80.0 + it }, 3)
        assertEquals(3, m.sel)
        assertEquals(listOf(0, 3, 7), m.xLabels)
        assertEquals(7, WeightChartModel(List(8) { 80.0 + it }, 99).sel)
        assertEquals(listOf(0, 1, 2), WeightChartModel(listOf(1.0, 2.0, 3.0), null).xLabels)
    }
}
