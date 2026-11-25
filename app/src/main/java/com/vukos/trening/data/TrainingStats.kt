package com.vukos.trening.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Sazetak za period (nedelja / mesec / godina). */
data class PeriodSummary(val workouts: Int, val minutes: Long, val volumeKg: Double, val sets: Int)

/** Statistika treninga: periodi, setovi po misicima, nedeljni cilj i niz nedelja. */
object TrainingStats {
    fun weekStart(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun inRange(w: Workout, from: LocalDate, to: LocalDate, zone: ZoneId): Boolean {
        val d = JournalStats.dateOf(w, zone)
        return !d.isBefore(from) && !d.isAfter(to)
    }

    fun summary(workouts: List<Workout>, sets: List<WorkoutSet>, from: LocalDate, to: LocalDate, zone: ZoneId = ZoneId.systemDefault()): PeriodSummary {
        val ws = workouts.filter { it.finishedAt != null && !it.deleted && inRange(it, from, to, zone) }
        val ids = ws.map { it.id }.toSet()
        val s = sets.filter { it.workoutId in ids }
        return PeriodSummary(
            workouts = ws.size,
            minutes = ws.sumOf { ((it.finishedAt!! - it.startedAt) / 60_000).coerceAtLeast(0) },
            volumeKg = WorkoutMath.volume(s),
            sets = s.count(WorkoutMath::isWorking),
        )
    }

    /** Broj radnih setova po glavnom misicu u periodu, od najveceg. */
    fun muscleSets(
        workouts: List<Workout>, sets: List<WorkoutSet>, primaryMuscle: Map<String, String>,
        from: LocalDate, to: LocalDate, zone: ZoneId = ZoneId.systemDefault(),
        secondaryMuscles: Map<String, List<String>> = emptyMap(),
    ): List<Pair<String, Double>> {
        val ids = workouts.filter { it.finishedAt != null && !it.deleted && inRange(it, from, to, zone) }.map { it.id }.toSet()
        val out = mutableMapOf<String, Double>()
        // Glavni misic dobija ceo set, svaki sporedni pola seta.
        sets.filter { it.workoutId in ids && WorkoutMath.isWorking(it) }.forEach { s ->
            val main = primaryMuscle[s.exerciseId] ?: "ostalo"
            out[main] = (out[main] ?: 0.0) + 1.0
            secondaryMuscles[s.exerciseId].orEmpty().filter { it != main }.forEach { m -> out[m] = (out[m] ?: 0.0) + 0.5 }
        }
        return out.toList().sortedByDescending { it.second }
    }

    /** Broj treninga po nedeljama (poslednjih [weeks] nedelja, najstarija prva). */
    fun weeklyCounts(dates: List<LocalDate>, today: LocalDate, weeks: Int = 12): List<Pair<LocalDate, Int>> {
        val byWeek = dates.groupingBy { weekStart(it) }.eachCount()
        val cur = weekStart(today)
        return (weeks - 1 downTo 0).map { k -> cur.minusWeeks(k.toLong()).let { it to (byWeek[it] ?: 0) } }
    }

    fun monthlyCounts(dates: List<LocalDate>, year: Int): List<Pair<YearMonth, Int>> {
        val byMonth = dates.groupingBy { YearMonth.from(it) }.eachCount()
        return (1..12).map { m -> YearMonth.of(year, m).let { it to (byMonth[it] ?: 0) } }
    }

    /**
     * Niz uzastopnih nedelja u kojima je ispunjen cilj. Tekuca nedelja se racuna ako je cilj vec ispunjen,
     * a ako jos nije - ne prekida niz (nedelja jos traje).
     */
    fun streak(dates: List<LocalDate>, goal: Int, today: LocalDate): Int {
        if (goal <= 0) return 0
        val byWeek = dates.groupingBy { weekStart(it) }.eachCount()
        var week = weekStart(today)
        var n = 0
        if ((byWeek[week] ?: 0) >= goal) n++
        week = week.minusWeeks(1)
        while ((byWeek[week] ?: 0) >= goal) { n++; week = week.minusWeeks(1) }
        return n
    }
}
