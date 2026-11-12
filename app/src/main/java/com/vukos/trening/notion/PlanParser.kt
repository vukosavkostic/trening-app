package com.vukos.trening.notion

import com.vukos.trening.data.Exercise
import com.vukos.trening.data.ExerciseSearch

/** Jedna vezba iz Notion plana, npr. "Dumbbell Bench Press 3x8-12 20kg". */
data class PlanLine(
    val name: String,
    val sets: Int = 3,
    val repsMin: Int? = 8,
    val repsMax: Int? = 12,
    val kg: Double? = null,
)

object PlanParser {
    // "3x8-12", "3 x 10", "4×6–8", "3*12"
    private val SETS_REPS = Regex("""(\d{1,2})\s*[x×*]\s*(\d{1,3})(?:\s*[-–]\s*(\d{1,3}))?""", RegexOption.IGNORE_CASE)
    private val KG = Regex("""(\d{1,3}(?:[.,]\d{1,2})?)\s*kg\b""", RegexOption.IGNORE_CASE)
    private val BULLET = Regex("""^\s*(?:[-*•]|\d{1,2}[.)])\s+""")

    /** Parsira jednu liniju; null ako u njoj nema naziva vezbe. */
    fun parseLine(raw: String): PlanLine? {
        var s = raw.replace(BULLET, "").trim()
        if (s.isEmpty()) return null
        var sets = 3; var rMin: Int? = 8; var rMax: Int? = 12; var kg: Double? = null
        SETS_REPS.find(s)?.let { m ->
            sets = m.groupValues[1].toInt().coerceIn(1, 20)
            rMin = m.groupValues[2].toInt()
            rMax = m.groupValues[3].takeIf { it.isNotEmpty() }?.toInt() ?: rMin
            s = s.removeRange(m.range)
        }
        KG.find(s)?.let { m ->
            kg = m.groupValues[1].replace(',', '.').toDouble()
            s = s.removeRange(m.range)
        }
        val name = s.replace(Regex("""[\s,;:–-]+$"""), "").replace(Regex("""\s+"""), " ").trim()
        return if (name.length < 2) null else PlanLine(name, sets, rMin, rMax, kg)
    }

    /**
     * Tekst iz polja "Plan" (svaka linija = vezba).
     */
    fun parseText(text: String): List<PlanLine> = text.lines().mapNotNull { parseLine(it) }

    /**
     * Blokovi iz tela stranice: uzimaju se stavke liste (bulleted / numbered / to_do),
     * a obican paragraf samo ako ima oblik "N x M" (da se uputstva i opisi ne racunaju kao vezbe).
     */
    fun parseBlocks(blocks: List<Pair<String, String>>): List<PlanLine> = blocks.mapNotNull { (type, text) ->
        when (type) {
            "bulleted_list_item", "numbered_list_item", "to_do" -> parseLine(text)
            "paragraph" -> if (SETS_REPS.containsMatchIn(text)) parseLine(text) else null
            else -> null
        }
    }
}

object ExerciseMatcher {
    /** Uparuje naziv iz Notiona sa bazom: tacan naziv, pa pretraga (sve reci moraju da se nadju). */
    fun match(name: String, all: List<Exercise>): Exercise? {
        val n = ExerciseSearch.normalize(name)
        all.firstOrNull { ExerciseSearch.normalize(it.name) == n }?.let { return it }
        val results = ExerciseSearch.filter(all, name)
        // Prednost: najkraci naziv medju onima koji pocinju upitom, inace najkraci rezultat.
        return results.filter { ExerciseSearch.normalize(it.name).startsWith(n) }.minByOrNull { it.name.length }
            ?: results.minByOrNull { it.name.length }
    }
}
