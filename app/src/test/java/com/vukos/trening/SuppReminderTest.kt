package com.vukos.trening

import com.vukos.trening.reminders.SuppReminder
import com.vukos.trening.reminders.SupplementReminders
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class SuppReminderTest {
    @Test fun nextTriggerTodayOrTomorrow() {
        val r = SuppReminder(name = "Kreatin", dose = "5 g", hour = 9, minute = 30)
        assertEquals(LocalDateTime.of(2026, 11, 13, 9, 30), SupplementReminders.nextTrigger(r, LocalDateTime.of(2026, 11, 13, 8, 0)))
        assertEquals(LocalDateTime.of(2026, 11, 14, 9, 30), SupplementReminders.nextTrigger(r, LocalDateTime.of(2026, 11, 13, 9, 30)))
    }

    @Test fun jsonRoundTrip() {
        val r = SuppReminder(name = "Whey protein", dose = "30 g", hour = 21, minute = 5, enabled = false, kcal = 120, proteinG = 24)
        assertEquals(r, SuppReminder.fromJson(r.toJson()))
        val n = SuppReminder(name = "Omega-3", hour = 8, minute = 0)
        assertEquals(n, SuppReminder.fromJson(n.toJson()))
        assertEquals("Whey protein · 30 g", r.label)
    }
}
