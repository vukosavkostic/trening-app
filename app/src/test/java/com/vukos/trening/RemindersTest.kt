package com.vukos.trening

import com.vukos.trening.data.CheckInSchedule
import com.vukos.trening.reminders.Reminders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class RemindersTest {
    private fun at(m: Int, d: Int, h: Int, min: Int = 0) = LocalDateTime.of(2026, m, d, h, min)
    private fun due(now: LocalDateTime, weight: Boolean, photos: Int) =
        CheckInSchedule.due(now.toLocalDate(), weight, photos)

    @Test fun triggerTimes() {
        assertEquals(at(11, 13, 8), Reminders.nextTrigger(at(11, 12, 18)))   // cet -> pet 08:00
        assertEquals(at(11, 15, 8), Reminders.nextTrigger(at(11, 15, 7)))    // ned pre 8
        assertEquals(at(11, 15, 20), Reminders.nextTrigger(at(11, 15, 8)))   // ned 8 -> ned 20
        assertEquals(at(11, 16, 8), Reminders.nextTrigger(at(11, 15, 20)))   // ned 20 -> pon 8
        assertEquals(at(11, 17, 8), Reminders.nextTrigger(at(11, 16, 8)))
    }

    @Test fun noNotificationBeforeFirstSunday() {
        assertFalse(Reminders.shouldNotify(due(at(11, 14, 8), false, 0), at(11, 14, 8)))
    }

    @Test fun sundayMorningAndEveningIfMissing() {
        assertTrue(Reminders.shouldNotify(due(at(11, 15, 8), false, 0), at(11, 15, 8)))
        assertTrue(Reminders.shouldNotify(due(at(11, 15, 20), true, 0), at(11, 15, 20)))   // slike fale
        assertFalse(Reminders.shouldNotify(due(at(11, 15, 20), true, 3), at(11, 15, 20)))
    }

    @Test fun dailyMorningWhileOverdue() {
        assertTrue(Reminders.shouldNotify(due(at(11, 17, 8), false, 0), at(11, 17, 8)))
        assertFalse(Reminders.shouldNotify(due(at(11, 17, 8), true, 3), at(11, 17, 8)))
    }

    @Test fun weightOnlyWeek() {
        assertTrue(Reminders.shouldNotify(due(at(11, 22, 8), false, 0), at(11, 22, 8)))
        assertFalse(Reminders.shouldNotify(due(at(11, 22, 8), true, 0), at(11, 22, 8)))
        assertEquals(LocalDate.of(2026, 11, 22), due(at(11, 24, 8), false, 0)!!.checkInDay)
    }

    @Test fun lateAlarmStillNotifiesOnOverdueDay() {
        // alarm odlozen (Doze) do 15:30 u ponedeljak, merenje i dalje fali
        assertTrue(Reminders.shouldNotify(due(at(11, 16, 15, 30), false, 0), at(11, 16, 15, 30)))
        assertFalse(Reminders.shouldNotify(due(at(11, 16, 7), false, 0), at(11, 16, 7)))   // pre 08:00
    }

    @Test fun eachSlotOnlyOnce() {
        val now = at(11, 16, 8)
        val slot = Reminders.slotFor(due(now, false, 0), now)
        assertEquals("2026-11-16-M", slot)
        assertFalse(Reminders.shouldNotify(due(now, false, 0), at(11, 16, 14), slot))       // isti dan, vec javljeno
        assertTrue(Reminders.shouldNotify(due(at(11, 17, 8), false, 0), at(11, 17, 8), slot)) // sledece jutro opet
    }

    @Test fun sundayEveningIsSeparateSlot() {
        val morning = Reminders.slotFor(due(at(11, 15, 8), false, 0), at(11, 15, 8))
        assertEquals("2026-11-15-M", morning)
        assertTrue(Reminders.shouldNotify(due(at(11, 15, 20), false, 0), at(11, 15, 20), morning))
        assertEquals("2026-11-15-E", Reminders.slotFor(due(at(11, 15, 21), false, 0), at(11, 15, 21)))
    }
}
