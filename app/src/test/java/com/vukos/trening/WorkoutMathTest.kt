package com.vukos.trening

import com.vukos.trening.data.RoutineExercise
import com.vukos.trening.data.WorkoutMath
import com.vukos.trening.data.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutMathTest {
    private fun s(i: Int, kg: Double?, reps: Int?, done: Boolean = true) =
        WorkoutSet(workoutId = "w", exerciseId = "e", exercisePosition = 0, setIndex = i, weightKg = kg, reps = reps, done = done)

    @Test fun volumeCountsEnteredSetsWithOrWithoutCheck() {
        val sets = listOf(
            s(1, 60.0, 10), s(2, 60.0, 8),
            s(3, 70.0, 5, done = false),        // upisano bez kvacice -> racuna se
            s(4, null, 12),                     // bez kilaze (npr. zgibovi)
            s(5, 80.0, null, done = false),     // samo kilaza, bez ponavljanja -> ne racuna se
        )
        assertEquals(1430.0, WorkoutMath.volume(sets), 1e-9)
        assertEquals(4, WorkoutMath.doneCount(sets))
    }

    @Test fun hintPrefersSameSetFromLastTime() {
        val prev = listOf(s(1, 60.0, 10), s(2, 62.5, 8))
        assertEquals(62.5 to 8, WorkoutMath.hint(2, prev, null))
        assertEquals(62.5 to 8, WorkoutMath.hint(4, prev, null))   // vise setova nego proslog puta
    }

    @Test fun hintFallsBackToRoutineTarget() {
        val t = RoutineExercise(routineId = "r", exerciseId = "e", position = 0, targetSets = 3, repsMin = 8, repsMax = 12, targetKg = 20.0)
        assertEquals(20.0 to 12, WorkoutMath.hint(1, emptyList(), t))
        assertEquals(null to null, WorkoutMath.hint(1, emptyList(), null))
    }

    @Test fun formatting() {
        assertEquals("62.5", WorkoutMath.fmtKg(62.5))
        assertEquals("60", WorkoutMath.fmtKg(60.0))
        assertEquals("", WorkoutMath.fmtKg(null))
        assertEquals(22.5, WorkoutMath.parseKg("22,5")!!, 1e-9)
        assertNull(WorkoutMath.parseKg("abc"))
        assertEquals("1h 5min", WorkoutMath.duration(0, 65 * 60_000))
        assertEquals("45min", WorkoutMath.duration(0, 45 * 60_000))
    }
}
