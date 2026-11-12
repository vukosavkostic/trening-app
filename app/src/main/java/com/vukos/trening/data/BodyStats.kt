package com.vukos.trening.data

import java.time.LocalDate

/** Statistika kilaze iz merenja (cista funkcija, testira se bez Androida). */
data class BodyStats(
    val count: Int,
    val current: Double?,
    val currentDay: LocalDate?,
    val first: Double?,
    val firstDay: LocalDate?,
    val min: Double?,
    val max: Double?,
    /** Promena u odnosu na prethodno merenje. */
    val lastChange: Double?,
    /** Promena u poslednjih ~30 dana (poslednje merenje minus merenje pre 30+ dana, ili prvo). */
    val change30: Double?,
    val totalChange: Double?,
    val photoDays: Int,
) {
    companion object {
        fun compute(entries: List<BodyEntry>, photoEntryIds: Set<String>): BodyStats {
            val w = entries.filter { it.weightKg != null && !it.deleted }.sortedBy { it.epochDay }
            val last = w.lastOrNull()
            val first = w.firstOrNull()
            val prev = if (w.size >= 2) w[w.size - 2] else null
            val month = last?.let { l -> w.lastOrNull { it.epochDay <= l.epochDay - 30 } ?: first }
            return BodyStats(
                count = w.size,
                current = last?.weightKg,
                currentDay = last?.let { LocalDate.ofEpochDay(it.epochDay) },
                first = first?.weightKg,
                firstDay = first?.let { LocalDate.ofEpochDay(it.epochDay) },
                min = w.minOfOrNull { it.weightKg!! },
                max = w.maxOfOrNull { it.weightKg!! },
                lastChange = if (last != null && prev != null) last.weightKg!! - prev.weightKg!! else null,
                change30 = if (last != null && month != null && month !== last) last.weightKg!! - month.weightKg!! else null,
                totalChange = if (last != null && first != null && w.size >= 2) last.weightKg!! - first.weightKg!! else null,
                photoDays = entries.count { !it.deleted && it.id in photoEntryIds },
            )
        }
    }
}

/** Opseg za grafikon. */
enum class ChartRange(val label: String, val days: Long?) {
    M1("1M", 31), M3("3M", 92), M6("6M", 183), Y1("1G", 366), ALL("Sve", null);

    fun filter(entries: List<BodyEntry>, today: LocalDate): List<BodyEntry> =
        if (days == null) entries else entries.filter { it.epochDay >= today.toEpochDay() - days }

    fun includes(date: LocalDate, today: LocalDate): Boolean =
        days == null || date.toEpochDay() >= today.toEpochDay() - days
}
