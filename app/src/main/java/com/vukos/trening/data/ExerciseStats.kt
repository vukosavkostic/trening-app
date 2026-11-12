package com.vukos.trening.data

import java.time.LocalDate
import java.time.ZoneId

/** Metrike za grafikon napretka na vezbi (kao u Hevy-ju). */
enum class ExerciseMetric(val label: String, val unit: String) {
    HEAVIEST("Najveća težina", "kg"),
    ONE_RM("Procenjeni 1RM", "kg"),
    BEST_SET_VOLUME("Najbolji set", "kg"),
    SESSION_VOLUME("Volumen treninga", "kg"),
    TOTAL_REPS("Ukupno ponavljanja", ""),
}

/** Jedan trening na kome je radjena vezba. */
data class ExerciseSession(
    val workoutId: String,
    val date: LocalDate,
    val sets: List<WorkoutSet>,
) {
    val heaviest get() = sets.maxOfOrNull { it.weightKg ?: 0.0 } ?: 0.0
    val oneRm get() = sets.maxOfOrNull { ExerciseStats.epley(it.weightKg ?: 0.0, it.reps ?: 0) } ?: 0.0
    val bestSetVolume get() = sets.maxOfOrNull { (it.weightKg ?: 0.0) * (it.reps ?: 0) } ?: 0.0
    val volume get() = sets.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
    val reps get() = sets.sumOf { it.reps ?: 0 }

    fun value(m: ExerciseMetric): Double = when (m) {
        ExerciseMetric.HEAVIEST -> heaviest
        ExerciseMetric.ONE_RM -> oneRm
        ExerciseMetric.BEST_SET_VOLUME -> bestSetVolume
        ExerciseMetric.SESSION_VOLUME -> volume
        ExerciseMetric.TOTAL_REPS -> reps.toDouble()
    }
}

data class ExerciseRecords(
    val heaviest: WorkoutSet?,
    val bestOneRm: Double,
    val bestSet: WorkoutSet?,
    val mostReps: WorkoutSet?,
    val sessions: Int,
)

object ExerciseStats {
    /** Epley: 1RM = kg * (1 + ponavljanja / 30); za 1 ponavljanje = kg. */
    fun epley(kg: Double, reps: Int): Double = when {
        reps <= 0 || kg <= 0 -> 0.0
        reps == 1 -> kg
        else -> kg * (1 + reps / 30.0)
    }

    /** Sesije za vezbu (najstarija prva), samo zavrseni treninzi i uradjeni setovi. */
    fun sessions(
        exerciseId: String,
        workouts: List<Workout>,
        doneSets: List<WorkoutSet>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<ExerciseSession> {
        val byId = workouts.filter { it.finishedAt != null && !it.deleted }.associateBy { it.id }
        return doneSets.filter { it.exerciseId == exerciseId && it.done && !it.deleted && it.workoutId in byId }
            .groupBy { it.workoutId }
            .map { (wid, sets) -> ExerciseSession(wid, JournalStats.dateOf(byId.getValue(wid), zone), sets.sortedBy { it.setIndex }) }
            .sortedBy { byId.getValue(it.workoutId).startedAt }
    }

    fun records(sessions: List<ExerciseSession>): ExerciseRecords {
        val all = sessions.flatMap { it.sets }
        return ExerciseRecords(
            heaviest = all.maxByOrNull { it.weightKg ?: 0.0 },
            bestOneRm = all.maxOfOrNull { epley(it.weightKg ?: 0.0, it.reps ?: 0) } ?: 0.0,
            bestSet = all.maxByOrNull { (it.weightKg ?: 0.0) * (it.reps ?: 0) },
            mostReps = all.maxByOrNull { it.reps ?: 0 },
            sessions = sessions.size,
        )
    }
}
