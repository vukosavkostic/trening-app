package com.vukos.trening

import com.vukos.trening.data.JournalStats
import com.vukos.trening.data.Workout
import com.vukos.trening.data.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset

class JournalStatsTest {
    private val z = ZoneOffset.UTC
    private fun at(m: Int, d: Int, h: Int) = LocalDateTime.of(2026, m, d, h, 0).toInstant(z).toEpochMilli()

    @Test fun monthInsights() {
        val w1 = Workout(id = "1", name = "Push", startedAt = at(11, 16, 18), finishedAt = at(11, 16, 19))
        val w2 = Workout(id = "2", name = "Pull", startedAt = at(11, 16, 20), finishedAt = at(11, 16, 20) + 30 * 60_000)
        val w3 = Workout(id = "3", name = "Old", startedAt = at(10, 30, 18), finishedAt = at(10, 30, 19))
        val active = Workout(id = "4", name = "Now", startedAt = at(11, 20, 18))
        val sets = listOf(
            WorkoutSet(workoutId = "1", exerciseId = "e", exercisePosition = 0, setIndex = 1, weightKg = 50.0, reps = 10, done = true),
            WorkoutSet(workoutId = "3", exerciseId = "e", exercisePosition = 0, setIndex = 1, weightKg = 99.0, reps = 1, done = true),
        )
        val m = JournalStats.month(listOf(w1, w2, w3, active), sets, YearMonth.of(2026, 11), z)
        assertEquals(2, m.workouts)
        assertEquals(90, m.minutes)
        assertEquals(500.0, m.volumeKg, 1e-9)
        assertEquals(1, m.trainingDays)
        assertEquals("1h 30m", JournalStats.fmtMinutes(90))
    }
}
