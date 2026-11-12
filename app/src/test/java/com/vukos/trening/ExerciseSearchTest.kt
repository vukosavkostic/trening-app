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

    @Test fun prefixMatchesComeFirst() {
        val r = ExerciseSearch.filter(all, "squat")
        assertTrue(r.first().name.lowercase().startsWith("squat"))
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
