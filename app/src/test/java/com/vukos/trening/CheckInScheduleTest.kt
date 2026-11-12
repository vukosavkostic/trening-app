package com.vukos.trening

import com.vukos.trening.data.CheckInSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CheckInScheduleTest {
    private fun d(m: Int, day: Int, y: Int = 2026) = LocalDate.of(y, m, day)

    @Test fun nothingDueBeforeFirstSunday() {
        assertNull(CheckInSchedule.due(d(11, 12), hasWeight = false, photoCount = 0))
        assertNull(CheckInSchedule.due(d(11, 14), hasWeight = false, photoCount = 0))
    }

    @Test fun firstSundayIsPhotoDay() {
        val due = CheckInSchedule.due(d(11, 15), hasWeight = false, photoCount = 0)!!
        assertEquals(d(11, 15), due.checkInDay)
        assertTrue(due.photoDay)
        assertTrue(due.weightMissing && due.photosMissing)
        assertFalse("na sam dan nije jos crveno", due.overdue)
    }

    @Test fun redFromMondayIfSundayMissed() {
        val due = CheckInSchedule.due(d(11, 16), hasWeight = false, photoCount = 0)!!
        assertTrue(due.overdue)
        assertFalse(CheckInSchedule.due(d(11, 16), hasWeight = true, photoCount = 2)!!.overdue)
        // samo slike fale -> i dalje crveno
        assertTrue(CheckInSchedule.due(d(11, 16), hasWeight = true, photoCount = 0)!!.overdue)
    }

    @Test fun staysRedDuringWeekUntilEntered() {
        val due = CheckInSchedule.due(d(11, 18), hasWeight = false, photoCount = 0)!!
        assertEquals(d(11, 15), due.checkInDay)
        assertTrue(due.anythingMissing)
        assertFalse(CheckInSchedule.due(d(11, 18), hasWeight = true, photoCount = 3)!!.anythingMissing)
    }

    @Test fun alternatingPhotoWeeks() {
        assertFalse(CheckInSchedule.isPhotoDay(d(11, 22)))
        assertTrue(CheckInSchedule.isPhotoDay(d(11, 29)))
        assertFalse(CheckInSchedule.isPhotoDay(d(12, 6)))
        assertTrue(CheckInSchedule.isPhotoDay(d(12, 13)))
        assertTrue(CheckInSchedule.isPhotoDay(d(1, 10, 2027)))
    }

    @Test fun weightOnlyWeekIgnoresPhotos() {
        val due = CheckInSchedule.due(d(11, 22), hasWeight = true, photoCount = 0)!!
        assertFalse(due.photoDay)
        assertFalse(due.anythingMissing)
    }

    @Test fun nextCheckInDay() {
        assertEquals(d(11, 15), CheckInSchedule.nextCheckInDay(d(11, 12)))
        assertEquals(d(11, 15), CheckInSchedule.nextCheckInDay(d(11, 15)))
        assertEquals(d(11, 22), CheckInSchedule.nextCheckInDay(d(11, 16)))
    }
}
