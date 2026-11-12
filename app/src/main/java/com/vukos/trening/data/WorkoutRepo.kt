package com.vukos.trening.data

import androidx.room.withTransaction
import com.vukos.trening.TreningApp
import java.util.Locale

/** Pomocne cisto-Kotlin funkcije za trening (testiraju se bez Androida). */
object WorkoutMath {
    fun volume(sets: List<WorkoutSet>): Double =
        sets.filter { it.done && !it.deleted }.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }

    fun doneCount(sets: List<WorkoutSet>) = sets.count { it.done && !it.deleted }

    /** Predlog za set: prvo isti set iz proslog treninga, pa poslednji prosli set, pa cilj iz plana. */
    fun hint(setIndex: Int, previous: List<WorkoutSet>, target: RoutineExercise?): Pair<Double?, Int?> {
        val prev = previous.firstOrNull { it.setIndex == setIndex } ?: previous.lastOrNull()
        if (prev != null) return prev.weightKg to prev.reps
        return target?.targetKg to (target?.repsMax ?: target?.repsMin)
    }

    fun fmtKg(kg: Double?): String =
        if (kg == null) "" else String.format(Locale.US, "%.2f", kg).trimEnd('0').trimEnd('.')

    fun parseKg(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 && it < 1000 }
    fun parseReps(s: String): Int? = s.trim().toIntOrNull()?.takeIf { it in 0..999 }

    fun duration(startedAt: Long, finishedAt: Long?): String {
        val min = (((finishedAt ?: System.currentTimeMillis()) - startedAt) / 60_000).coerceAtLeast(0)
        return if (min >= 60) "${min / 60}h ${min % 60}min" else "${min}min"
    }
}

object WorkoutRepo {
    private val db get() = TreningApp.instance.db
    private val dao get() = db.workouts()

    /** Ako vec postoji trening u toku, vraca njega (nikad dva istovremeno). */
    suspend fun startFromRoutine(routineId: String): String {
        dao.getActive()?.let { return it.id }
        val routine = db.routines().get(routineId) ?: return startEmpty()
        val items = db.routines().getExercises(routineId)
        val w = Workout(routineId = routine.id, name = routine.name)
        val sets = items.flatMapIndexed { pos, re ->
            (1..re.targetSets.coerceAtLeast(1)).map { i ->
                WorkoutSet(workoutId = w.id, exerciseId = re.exerciseId, exercisePosition = pos, setIndex = i)
            }
        }
        db.withTransaction { dao.upsert(w); dao.upsertSets(sets) }
        return w.id
    }

    suspend fun startEmpty(): String {
        dao.getActive()?.let { return it.id }
        val w = Workout(name = "Trening")
        dao.upsert(w)
        return w.id
    }

    suspend fun addExercises(workoutId: String, exerciseIds: List<String>) {
        val sets = dao.getSets(workoutId)
        var pos = (sets.maxOfOrNull { it.exercisePosition } ?: -1) + 1
        dao.upsertSets(exerciseIds.flatMap { ex ->
            val p = pos++
            (1..3).map { WorkoutSet(workoutId = workoutId, exerciseId = ex, exercisePosition = p, setIndex = it) }
        })
    }

    suspend fun addSet(workoutId: String, exerciseId: String, position: Int) {
        val last = dao.getSets(workoutId).filter { it.exercisePosition == position }.maxOfOrNull { it.setIndex } ?: 0
        dao.upsertSet(WorkoutSet(workoutId = workoutId, exerciseId = exerciseId, exercisePosition = position, setIndex = last + 1))
    }

    suspend fun updateSet(set: WorkoutSet) = dao.upsertSet(set.copy(updatedAt = System.currentTimeMillis(), dirty = true))

    /** Brise set i prenumerise ostale setove te vezbe (1, 2, 3...). */
    suspend fun deleteSet(set: WorkoutSet) {
        val now = System.currentTimeMillis()
        val rest = dao.getSets(set.workoutId)
            .filter { it.exercisePosition == set.exercisePosition && it.id != set.id }
            .sortedBy { it.setIndex }
            .mapIndexed { i, s -> s.copy(setIndex = i + 1, updatedAt = now, dirty = true) }
        db.withTransaction {
            dao.upsertSet(set.copy(deleted = true, dirty = true, updatedAt = now))
            dao.upsertSets(rest)
        }
    }

    suspend fun removeExercise(workoutId: String, position: Int) {
        val now = System.currentTimeMillis()
        dao.upsertSets(dao.getSets(workoutId).filter { it.exercisePosition == position }
            .map { it.copy(deleted = true, dirty = true, updatedAt = now) })
    }

    /** Zavrsava trening: neuradjeni setovi se brisu. Vraca false ako nema nijednog uradjenog seta. */
    suspend fun finish(workoutId: String): Boolean {
        val w = dao.get(workoutId) ?: return false
        val sets = dao.getSets(workoutId)
        if (sets.none { it.done }) return false
        val now = System.currentTimeMillis()
        db.withTransaction {
            dao.upsertSets(sets.filter { !it.done }.map { it.copy(deleted = true, dirty = true, updatedAt = now) })
            dao.upsert(w.copy(finishedAt = now, updatedAt = now, dirty = true))
        }
        return true
    }

    suspend fun discard(workoutId: String) {
        val w = dao.get(workoutId) ?: return
        dao.upsert(w.copy(deleted = true, dirty = true, updatedAt = System.currentTimeMillis()))
    }
}
