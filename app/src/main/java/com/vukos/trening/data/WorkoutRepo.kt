package com.vukos.trening.data

import androidx.room.withTransaction
import com.vukos.trening.TreningApp
import java.util.Locale

/** Pomocne cisto-Kotlin funkcije za trening (testiraju se bez Androida). */
object WorkoutMath {
    /** Set se racuna kao unet ako je oznacen kvacicom ili su upisana ponavljanja. */
    fun isLogged(s: WorkoutSet) = !s.deleted && (s.done || s.reps != null)

    /** Radni set = unet i nije zagrevanje (racuna se u volumen, rekorde i progresiju). */
    fun isWorking(s: WorkoutSet) = isLogged(s) && s.setType != SetType.WARMUP

    fun volume(sets: List<WorkoutSet>): Double =
        sets.filter(::isWorking).sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }

    fun doneCount(sets: List<WorkoutSet>) = sets.count(::isLogged)

    /** Korak povecanja tezine kod automatske progresije. */
    const val PROGRESSION_STEP_KG = 2.5

    /**
     * Dvostruka progresija: ako su proslog puta SVI radni setovi dostigli gornju granicu ponavljanja iz plana,
     * predlaze se veca tezina (+2,5 kg). Vraca predlozenu tezinu ili null.
     */
    fun progression(previous: List<WorkoutSet>, target: RoutineExercise?): Double? {
        val top = target?.repsMax ?: target?.repsMin ?: return null
        val working = previous.filter { it.setType != SetType.WARMUP && !it.deleted }
        if (working.isEmpty()) return null
        if (working.any { (it.reps ?: 0) < top || (it.weightKg ?: 0.0) <= 0.0 }) return null
        return working.maxOf { it.weightKg!! } + PROGRESSION_STEP_KG
    }

    /**
     * Predlog za set: kod progresije nova tezina i donja granica ponavljanja; inace isti set iz proslog
     * treninga, pa poslednji prosli set, pa cilj iz plana.
     */
    fun hint(
        setIndex: Int,
        previous: List<WorkoutSet>,
        target: RoutineExercise?,
        setType: String = SetType.NORMAL,
    ): Pair<Double?, Int?> {
        val prev = previous.firstOrNull { it.setIndex == setIndex }
            ?: previous.lastOrNull { (it.setType == SetType.WARMUP) == (setType == SetType.WARMUP) }
            ?: previous.lastOrNull()
        if (setType != SetType.WARMUP) progression(previous, target)?.let { kg ->
            return kg to (target?.repsMin ?: target?.repsMax)
        }
        if (prev != null) return prev.weightKg to prev.reps
        return target?.targetKg to (target?.repsMax ?: target?.repsMin)
    }

    /**
     * Da li se odradjeni trening razlikuje od plana (vezbe, redosled, broj radnih setova)?
     * Vraca novu listu stavki plana (sa kilazama iz treninga) ili null ako nema razlike.
     */
    fun planUpdate(routineId: String, current: List<RoutineExercise>, sets: List<WorkoutSet>): PlanUpdate? {
        val groups = sets.filter(::isLogged).groupBy { it.exercisePosition }.toSortedMap().values.toList()
        if (groups.isEmpty()) return null
        val pool = current.sortedBy { it.position }.toMutableList()
        val items = groups.mapIndexed { i, g ->
            val exId = g.first().exerciseId
            val old = pool.firstOrNull { it.exerciseId == exId }?.also { pool.remove(it) }
            val working = g.filter { it.setType != SetType.WARMUP }
            val kg = working.mapNotNull { it.weightKg }.filter { it > 0 }.maxOrNull()
            (old ?: RoutineExercise(routineId = routineId, exerciseId = exId, position = i)).copy(
                id = old?.id ?: newId(), position = i,
                targetSets = working.size.coerceAtLeast(1),
                targetKg = kg ?: old?.targetKg,
            )
        }
        val oldOrder = current.sortedBy { it.position }
        val added = items.filter { n -> oldOrder.none { it.exerciseId == n.exerciseId } }.map { it.exerciseId }
        val removed = oldOrder.filter { o -> items.none { it.exerciseId == o.exerciseId } }.map { it.exerciseId }
        val setChanges = items.mapNotNull { n ->
            oldOrder.firstOrNull { it.exerciseId == n.exerciseId }?.takeIf { it.targetSets != n.targetSets }
                ?.let { Triple(n.exerciseId, it.targetSets, n.targetSets) }
        }
        val reordered = added.isEmpty() && removed.isEmpty() &&
            items.map { it.exerciseId } != oldOrder.map { it.exerciseId }
        if (added.isEmpty() && removed.isEmpty() && setChanges.isEmpty() && !reordered) return null
        return PlanUpdate(items, added, removed, setChanges, reordered)
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

/** Razlika izmedju treninga i plana. setChanges: (vezba, staro, novo). */
data class PlanUpdate(
    val items: List<RoutineExercise>,
    val added: List<String>,
    val removed: List<String>,
    val setChanges: List<Triple<String, Int, Int>>,
    val reordered: Boolean,
)

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
        val w = Workout(name = com.vukos.trening.i18n.tr("Trening", "Workout"))
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

    /**
     * Zavrsava trening: cuva se svaki set sa upisanim ponavljanjima (sa ili bez kvacice),
     * a potpuno prazni setovi se brisu. Vraca false ako nije unet nijedan set.
     */
    suspend fun finish(workoutId: String): Boolean {
        val w = dao.get(workoutId) ?: return false
        val sets = dao.getSets(workoutId)
        if (sets.none(WorkoutMath::isLogged)) return false
        val now = System.currentTimeMillis()
        db.withTransaction {
            dao.upsertSets(sets.map {
                if (WorkoutMath.isLogged(it)) it.copy(done = true, updatedAt = now, dirty = true)
                else it.copy(deleted = true, dirty = true, updatedAt = now)
            })
            dao.upsert(w.copy(finishedAt = now, updatedAt = now, dirty = true))
        }
        return true
    }

    /** Upisuje u plan vezbe, broj setova i kilaze iz odradjenog treninga. */
    suspend fun applyPlanUpdate(routineId: String, update: PlanUpdate) {
        val r = db.routines().get(routineId) ?: return
        db.withTransaction {
            db.routines().clearExercises(routineId)
            db.routines().upsertExercises(update.items)
            db.routines().upsert(r.copy(updatedAt = System.currentTimeMillis(), dirty = true))
        }
    }

    suspend fun setType(set: WorkoutSet, type: String) = updateSet(set.copy(setType = type))

    suspend fun saveExerciseNote(exerciseId: String, note: String) {
        val ex = db.exercises().get(exerciseId) ?: return
        db.exercises().upsert(ex.copy(notes = note.trim(), updatedAt = System.currentTimeMillis(), dirty = true))
    }

    suspend fun discard(workoutId: String) {
        val w = dao.get(workoutId) ?: return
        dao.upsert(w.copy(deleted = true, dirty = true, updatedAt = System.currentTimeMillis()))
    }
}
