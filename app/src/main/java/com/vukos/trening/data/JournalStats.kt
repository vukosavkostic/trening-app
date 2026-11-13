package com.vukos.trening.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Uvidi za mesec u Dnevniku. */
data class MonthInsights(val workouts: Int, val minutes: Long, val volumeKg: Double, val trainingDays: Int)

object JournalStats {
    fun dateOf(w: Workout, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(w.startedAt).atZone(zone).toLocalDate()

    fun month(
        workouts: List<Workout>,
        doneSets: List<WorkoutSet>,
        ym: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): MonthInsights {
        val inMonth = workouts.filter { it.finishedAt != null && !it.deleted && YearMonth.from(dateOf(it, zone)) == ym }
        val ids = inMonth.map { it.id }.toSet()
        return MonthInsights(
            workouts = inMonth.size,
            minutes = inMonth.sumOf { ((it.finishedAt!! - it.startedAt) / 60_000).coerceAtLeast(0) },
            volumeKg = WorkoutMath.volume(doneSets.filter { it.workoutId in ids }),
            trainingDays = inMonth.map { dateOf(it, zone) }.distinct().size,
        )
    }

    fun fmtMinutes(min: Long) = "${min / 60}h ${"%02d".format(min % 60)}m"
}

/** Obroci (isti nazivi kao Meal u Notion bazi Food). */
val MEALS = listOf("Doručak", "Ručak", "Večera", "Užina")

/** Naziv obroka za prikaz (u bazi i Notionu ostaje srpski naziv). */
fun mealLabel(meal: String): String = if (!com.vukos.trening.i18n.Lang.en) meal else when (meal) {
    "Doručak" -> "Breakfast"; "Ručak" -> "Lunch"; "Večera" -> "Dinner"; "Užina" -> "Snack"; "Suplementi" -> "Supplements"; else -> meal
}
