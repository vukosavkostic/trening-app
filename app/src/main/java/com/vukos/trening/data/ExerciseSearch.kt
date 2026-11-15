package com.vukos.trening.data

import com.vukos.trening.i18n.tr

/** Grupe misica i opreme za filter-cipove (nazivi na srpskom, vrednosti iz free-exercise-db). */
enum class MuscleGroup(private val sr: String, private val en: String, val muscles: Set<String>) {
    CHEST("Grudi", "Chest", setOf("chest")),
    BACK("Leđa", "Back", setOf("lats", "middle back", "lower back", "traps")),
    SHOULDERS("Ramena", "Shoulders", setOf("shoulders", "neck")),
    BICEPS("Biceps", "Biceps", setOf("biceps")),
    TRICEPS("Triceps", "Triceps", setOf("triceps")),
    FOREARMS("Podlaktice", "Forearms", setOf("forearms")),
    LEGS("Noge", "Legs", setOf("quadriceps", "hamstrings", "calves", "adductors", "abductors")),
    GLUTES("Gluteus", "Glutes", setOf("glutes")),
    ABS("Stomak", "Abs", setOf("abdominals"));

    val label: String get() = tr(sr, en)
}

enum class EquipmentGroup(private val sr: String, private val en: String, val values: Set<String>) {
    BARBELL("Šipka", "Barbell", setOf("barbell", "e-z curl bar")),
    DUMBBELL("Bučice", "Dumbbell", setOf("dumbbell")),
    CABLE("Sajla", "Cable", setOf("cable")),
    MACHINE("Mašina", "Machine", setOf("machine")),
    BODY("Telo", "Bodyweight", setOf("body only", "")),
    KETTLEBELL("Girja", "Kettlebell", setOf("kettlebells")),
    OTHER("Ostalo", "Other", setOf("bands", "medicine ball", "exercise ball", "foam roll", "other"));

    val label: String get() = tr(sr, en)
}

/** Srpski nazivi misica (pretraga radi na oba jezika). */
val muscleLabelsSr = mapOf(
    "chest" to "grudi", "lats" to "latisimus", "middle back" to "srednja leđa",
    "lower back" to "donja leđa", "traps" to "trapez", "shoulders" to "ramena", "neck" to "vrat",
    "biceps" to "biceps", "triceps" to "triceps", "forearms" to "podlaktice",
    "quadriceps" to "kvadriceps", "hamstrings" to "zadnja loža", "calves" to "listovi",
    "adductors" to "aduktori", "abductors" to "abduktori", "glutes" to "gluteus",
    "abdominals" to "stomak",
)

val equipmentLabelsSr = mapOf(
    "barbell" to "šipka", "e-z curl bar" to "EZ šipka", "dumbbell" to "bučice", "cable" to "sajla",
    "machine" to "mašina", "body only" to "sopstvena težina", "kettlebells" to "girja",
    "bands" to "elastične trake", "medicine ball" to "medicinka", "exercise ball" to "pilates lopta",
    "foam roll" to "foam roller", "other" to "ostalo",
)

/** Nazivi za prikaz na trenutnom jeziku (engleski = originalni naziv iz baze). */
val muscleLabels: Map<String, String> get() = if (com.vukos.trening.i18n.Lang.en) muscleLabelsSr.mapValues { it.key } else muscleLabelsSr
val equipmentLabels: Map<String, String> get() = if (com.vukos.trening.i18n.Lang.en) equipmentLabelsSr.mapValues { it.key } else equipmentLabelsSr

object ExerciseSearch {
    /**
     * Svaka rec iz upita mora da se nadje u nazivu, misicima ili opremi (redosled nebitan),
     * tako da "row db" nalazi "Dumbbell Row"... a "bench dumbbell" nalazi "Dumbbell Bench Press".
     * Rezultati po relevantnosti (kao Hevy/Strong): prvo osnovne, najcesce vezbe (POPULAR),
     * pa naziv koji pocinje upitom, pa naziv koji sadrzi ceo upit kao frazu, pa kraci nazivi.
     * Bez upita: popularne prvo, ostale po abecedi.
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
                        (muscleLabelsSr[ex.primaryMuscle] ?: "")
                )
                tokens.all { it in hay }
            }
            .sortedWith(compareBy<Exercise>(
                { if (q.isEmpty()) 0 else group(rank(normalize(it.name), q, tokens)) },
                { POPULAR_RANK[it.name] ?: Int.MAX_VALUE },
                { if (q.isEmpty()) 0 else rank(normalize(it.name), q, tokens) },
                { if (q.isEmpty()) 0 else it.name.length },
                { it.name.lowercase() },
            ))
            .toList()
    }

    /**
     * 0 = pocinje upitom kao celim recima, 1 = sadrzi upit kao cele reci,
     * 2 = fraza na pocetku reci ("lat" u "lateral"), 3 = sve reci postoje kao pocetak reci, 4 = ostalo.
     */
    private fun rank(name: String, q: String, tokens: List<String>): Int {
        val n = " $name "
        val words = name.split(' ')
        return when {
            n.startsWith(" $q ") -> 0
            n.contains(" $q ") -> 1
            n.contains(" $q") -> 2
            tokens.all { t -> words.any { it.startsWith(t) } } -> 3
            else -> 4
        }
    }

    /** Grupa: cele reci (0) pa pocetak reci (1) pa ostalo (2); unutar grupe popularne vezbe prve. */
    private fun group(r: Int) = when { r <= 1 -> 0; r <= 3 -> 1; else -> 2 }

    /** Osnovne vezbe koje se najcesce rade u teretani (redosled = prioritet medju njima). */
    val POPULAR = listOf(
        "Barbell Bench Press - Medium Grip", "Barbell Squat", "Barbell Deadlift", "Dumbbell Bench Press",
        "Incline Dumbbell Press", "Barbell Incline Bench Press - Medium Grip", "Standing Military Press",
        "Dumbbell Shoulder Press", "Side Lateral Raise", "Wide-Grip Lat Pulldown", "Pullups", "Chin-Up",
        "Bent Over Barbell Row", "One-Arm Dumbbell Row", "Seated Cable Rows", "Romanian Deadlift",
        "Leg Press", "Leg Extensions", "Barbell Hip Thrust",
        "Barbell Lunge", "Dumbbell Lunges", "Goblet Squat", "Front Barbell Squat", "Hack Squat",
        "Standing Calf Raises", "Seated Calf Raise", "Barbell Curl", "Dumbbell Bicep Curl", "Hammer Curls",
        "Preacher Curl", "Lying Leg Curls", "Seated Leg Curl", "Triceps Pushdown", "Dips - Triceps Version", "Dips - Chest Version",
        "Lying Dumbbell Tricep Extension", "Dumbbell Flyes", "Face Pull", "Barbell Shrug", "Dumbbell Shrug",
        "Plank", "Crunches", "Hanging Leg Raise", "Cable Crunch", "Pushups",
    )
    private val POPULAR_RANK = POPULAR.withIndex().associate { it.value to it.index }

    private val ALIASES = mapOf("db" to "dumbbell", "bb" to "barbell", "kb" to "kettlebell")

    fun normalize(s: String): String =
        s.lowercase()
            .replace('š', 's').replace('đ', 'd').replace('č', 'c').replace('ć', 'c').replace('ž', 'z')
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex(" +"), " ")
            .trim()
}
