package com.vukos.trening.data

/** Grupe misica i opreme za filter-cipove (nazivi na srpskom, vrednosti iz free-exercise-db). */
enum class MuscleGroup(val label: String, val muscles: Set<String>) {
    CHEST("Grudi", setOf("chest")),
    BACK("Leđa", setOf("lats", "middle back", "lower back", "traps")),
    SHOULDERS("Ramena", setOf("shoulders", "neck")),
    BICEPS("Biceps", setOf("biceps")),
    TRICEPS("Triceps", setOf("triceps")),
    FOREARMS("Podlaktice", setOf("forearms")),
    LEGS("Noge", setOf("quadriceps", "hamstrings", "calves", "adductors", "abductors")),
    GLUTES("Gluteus", setOf("glutes")),
    ABS("Stomak", setOf("abdominals")),
}

enum class EquipmentGroup(val label: String, val values: Set<String>) {
    BARBELL("Šipka", setOf("barbell", "e-z curl bar")),
    DUMBBELL("Bučice", setOf("dumbbell")),
    CABLE("Sajla", setOf("cable")),
    MACHINE("Mašina", setOf("machine")),
    BODY("Telo", setOf("body only", "")),
    KETTLEBELL("Girja", setOf("kettlebells")),
    OTHER("Ostalo", setOf("bands", "medicine ball", "exercise ball", "foam roll", "other")),
}

/** Srpski nazivi misica za prikaz. */
val muscleLabels = mapOf(
    "chest" to "grudi", "lats" to "latisimus", "middle back" to "srednja leđa",
    "lower back" to "donja leđa", "traps" to "trapez", "shoulders" to "ramena", "neck" to "vrat",
    "biceps" to "biceps", "triceps" to "triceps", "forearms" to "podlaktice",
    "quadriceps" to "kvadriceps", "hamstrings" to "zadnja loža", "calves" to "listovi",
    "adductors" to "aduktori", "abductors" to "abduktori", "glutes" to "gluteus",
    "abdominals" to "stomak",
)

val equipmentLabels = mapOf(
    "barbell" to "šipka", "e-z curl bar" to "EZ šipka", "dumbbell" to "bučice", "cable" to "sajla",
    "machine" to "mašina", "body only" to "sopstvena težina", "kettlebells" to "girja",
    "bands" to "elastične trake", "medicine ball" to "medicinka", "exercise ball" to "pilates lopta",
    "foam roll" to "foam roller", "other" to "ostalo",
)

object ExerciseSearch {
    /**
     * Svaka rec iz upita mora da se nadje u nazivu, misicima ili opremi (redosled nebitan),
     * tako da "row db" nalazi "Dumbbell Row"... a "bench dumbbell" nalazi "Dumbbell Bench Press".
     * Rezultati: prvo oni ciji naziv pocinje upitom, pa ostali po abecedi.
     */
    fun filter(
        all: List<Exercise>,
        query: String,
        muscles: Set<MuscleGroup> = emptySet(),
        equipment: Set<EquipmentGroup> = emptySet(),
    ): List<Exercise> {
        val tokens = normalize(query).split(' ').filter { it.isNotEmpty() }.map { ALIASES[it] ?: it }
        val muscleSet = muscles.flatMap { it.muscles }.toSet()
        val equipSet = equipment.flatMap { it.values }.toSet()
        val q = normalize(query)
        return all.asSequence()
            .filter { muscleSet.isEmpty() || it.primaryMuscle in muscleSet }
            .filter { equipSet.isEmpty() || it.equipment in equipSet }
            .filter { ex ->
                if (tokens.isEmpty()) return@filter true
                val hay = normalize(
                    ex.name + " " + ex.primaryMuscle + " " + ex.equipment + " " +
                        (muscleLabels[ex.primaryMuscle] ?: "")
                )
                tokens.all { it in hay }
            }
            .sortedWith(compareBy({ !normalize(it.name).startsWith(q) }, { it.name.lowercase() }))
            .toList()
    }

    private val ALIASES = mapOf("db" to "dumbbell", "bb" to "barbell", "kb" to "kettlebell")

    fun normalize(s: String): String =
        s.lowercase()
            .replace('š', 's').replace('đ', 'd').replace('č', 'c').replace('ć', 'c').replace('ž', 'z')
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex(" +"), " ")
            .trim()
}
