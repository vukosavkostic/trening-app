package com.vukos.trening

import com.vukos.trening.data.EquipmentGroup
import com.vukos.trening.data.ExerciseSearch
import com.vukos.trening.data.ExerciseSeed
import com.vukos.trening.data.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExerciseSearchTest {
    private val all = ExerciseSeed.parse(File("src/main/assets/exercises.json").readText())

    @Test fun parsesWholeDatabase() {
        assertEquals(876, all.size)
        assertTrue(all.all { it.name.isNotBlank() && it.primaryMuscle.isNotBlank() })
        assertTrue(all.first { it.id == "3_4_Sit-Up" }.imageUrls.first().endsWith("3_4_Sit-Up/0.jpg"))
    }

    @Test fun wordsInAnyOrder() {
        val r = ExerciseSearch.filter(all, "bench dumbbell").map { it.name }
        assertTrue(r.toString(), "Dumbbell Bench Press" in r)
    }

    @Test fun popularStaplesComeFirst() {
        fun top(q: String) = ExerciseSearch.filter(all, q).first().name
        assertEquals("Barbell Squat", top("squat"))
        assertEquals("Barbell Bench Press - Medium Grip", top("bench press"))
        assertEquals("Barbell Deadlift", top("deadlift"))
        assertEquals("Side Lateral Raise", top("lateral raise"))
        assertEquals("Barbell Curl", top("curl"))
        assertEquals("Wide-Grip Lat Pulldown", top("lat"))
    }

    @Test fun prefixBeatsContainsAmongOthers() {
        val r = ExerciseSearch.filter(all, "squat jerk").map { it.name }
        assertEquals("Squat Jerk", r.first())
        // nepopularne: naziv koji pocinje upitom ide pre onog koji ga samo sadrzi
        val s = ExerciseSearch.filter(all, "jump squat").map { it.name }
        assertTrue(s.toString(), s.indexOf("Freehand Jump Squat") < s.indexOf("Kneeling Jump Squat") || s.first().lowercase().startsWith("jump squat"))
    }

    @Test fun exactPhraseBeforeScatteredWords() {
        val r = ExerciseSearch.filter(all, "shoulder press").map { it.name }
        assertEquals("Dumbbell Shoulder Press", r.first())
    }

    @Test fun noQueryShowsPopularFirst() {
        assertEquals("Barbell Bench Press - Medium Grip", ExerciseSearch.filter(all, "").first().name)
    }

    @Test fun aliasAndCaseInsensitive() {
        val r = ExerciseSearch.filter(all, "DB ROW").map { it.name }
        assertTrue(r.toString(), r.any { it.contains("Dumbbell") && it.contains("Row") })
    }

    @Test fun muscleAndEquipmentFilters() {
        val r = ExerciseSearch.filter(all, "", setOf(MuscleGroup.CHEST), setOf(EquipmentGroup.DUMBBELL))
        assertTrue(r.isNotEmpty())
        assertTrue(r.all { it.primaryMuscle == "chest" && it.equipment == "dumbbell" })
    }

    @Test fun serbianMuscleNameSearch() {
        val r = ExerciseSearch.filter(all, "grudi")
        assertTrue(r.isNotEmpty() && r.all { it.primaryMuscle == "chest" })
    }
}
