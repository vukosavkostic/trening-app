package com.vukos.trening

import com.vukos.trening.data.Rotation
import com.vukos.trening.data.Routine
import com.vukos.trening.data.Workout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RotationTest {
    private val push = Routine(id = "push", name = "Push", sortOrder = 0)
    private val pull = Routine(id = "pull", name = "Pull", sortOrder = 1)
    private val legs = Routine(id = "legs", name = "Legs", sortOrder = 2)
    private val plans = listOf(push, pull, legs)
    private fun w(routine: String?, at: Long) = Workout(routineId = routine, name = "x", startedAt = at, finishedAt = at + 1)

    @Test fun firstPlanWhenNothingDone() {
        val r = Rotation.next(plans, emptyList())
        assertEquals("push", r.next?.id); assertNull(r.last)
    }

    @Test fun afterLastDone() {
        assertEquals("legs", Rotation.next(plans, listOf(w("push", 1), w("pull", 2))).next?.id)
    }

    @Test fun wrapsAround() {
        assertEquals("push", Rotation.next(plans, listOf(w("legs", 5))).next?.id)
    }

    @Test fun freeWorkoutDoesNotMoveRotation() {
        val r = Rotation.next(plans, listOf(w("push", 1), w(null, 9)))
        assertEquals("pull", r.next?.id); assertEquals("push", r.last?.id)
    }

    @Test fun skippedAheadContinuesFromActual() {
        assertEquals("push", Rotation.next(plans, listOf(w("push", 1), w("legs", 2))).next?.id)
    }

    @Test fun unfinishedAndDeletedIgnored() {
        val active = Workout(routineId = "legs", name = "x", startedAt = 9)
        val deleted = w("pull", 8).copy(deleted = true)
        assertEquals("pull", Rotation.next(plans, listOf(w("push", 1), active, deleted)).next?.id)
    }
}
