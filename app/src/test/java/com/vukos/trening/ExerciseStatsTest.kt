package com.vukos.trening

import com.vukos.trening.data.ExerciseMetric
import com.vukos.trening.data.ExerciseStats
import com.vukos.trening.data.Workout
import com.vukos.trening.data.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class ExerciseStatsTest {
    private val z = ZoneOffset.UTC
    private fun t(d: Int) = LocalDateTime.of(2026, 11, d, 18, 0).toInstant(z).toEpochMilli()
    private fun s(w: String, ex: String, i: Int, kg: Double, reps: Int, done: Boolean = true) =
        WorkoutSet(workoutId = w, exerciseId = ex, exercisePosition = 0, setIndex = i, weightKg = kg, reps = reps, done = done)

    @Test fun epley() {
        assertEquals(100.0, ExerciseStats.epley(100.0, 1), 1e-9)
        assertEquals(80.0 * (1 + 10 / 30.0), ExerciseStats.epley(80.0, 10), 1e-9)
        assertEquals(0.0, ExerciseStats.epley(0.0, 10), 1e-9)
    }

    @Test fun sessionsMetricsAndRecords() {
        val workouts = listOf(
            Workout(id = "b", name = "B", startedAt = t(20), finishedAt = t(20) + 1),
            Workout(id = "a", name = "A", startedAt = t(16), finishedAt = t(16) + 1),
            Workout(id = "x", name = "u toku", startedAt = t(21)),
        )
        val sets = listOf(
            s("a", "bench", 1, 60.0, 10), s("a", "bench", 2, 65.0, 8), s("a", "row", 1, 50.0, 10),
            s("b", "bench", 1, 70.0, 5), s("b", "bench", 2, 50.0, 15),
            s("x", "bench", 1, 200.0, 1),          // trening u toku se ne racuna
        )
        val ss = ExerciseStats.sessions("bench", workouts, sets, z)
        assertEquals(listOf(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 20)), ss.map { it.date })
        assertEquals(65.0, ss[0].value(ExerciseMetric.HEAVIEST), 1e-9)
        assertEquals(1120.0, ss[0].value(ExerciseMetric.SESSION_VOLUME), 1e-9)
        assertEquals(750.0, ss[1].value(ExerciseMetric.BEST_SET_VOLUME), 1e-9)
        assertEquals(20.0, ss[1].value(ExerciseMetric.TOTAL_REPS), 1e-9)
        val r = ExerciseStats.records(ss)
        assertEquals(70.0, r.heaviest!!.weightKg!!, 1e-9)
        assertEquals(15, r.mostReps!!.reps)
        assertEquals(2, r.sessions)
        assertEquals(65.0 * (1 + 8 / 30.0), r.bestOneRm, 1e-9)   // 65x8 je najbolji procenjeni 1RM
    }
}
