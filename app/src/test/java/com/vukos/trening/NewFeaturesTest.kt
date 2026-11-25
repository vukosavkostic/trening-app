package com.vukos.trening

import com.vukos.trening.data.BackupCrypto
import com.vukos.trening.data.BackupException
import com.vukos.trening.data.ExerciseStats
import com.vukos.trening.data.FoodEntry
import com.vukos.trening.data.FoodQuick
import com.vukos.trening.data.OpenFoodFacts
import com.vukos.trening.data.RoutineExercise
import com.vukos.trening.data.SetType
import com.vukos.trening.data.Supplements
import com.vukos.trening.data.TrainingStats
import com.vukos.trening.data.Workout
import com.vukos.trening.data.WorkoutMath
import com.vukos.trening.data.WorkoutSet
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class NewFeaturesTest {
    private val zone = ZoneId.systemDefault()
    private fun set(i: Int, kg: Double?, reps: Int?, type: String = SetType.NORMAL, ex: String = "e", w: String = "w", pos: Int = 0) =
        WorkoutSet(workoutId = w, exerciseId = ex, exercisePosition = pos, setIndex = i, weightKg = kg, reps = reps, done = true, setType = type)
    private val target = RoutineExercise(routineId = "r", exerciseId = "e", position = 0, targetSets = 3, repsMin = 8, repsMax = 12)

    @Test fun progressionWhenAllWorkingSetsHitTop() {
        val prev = listOf(set(1, 40.0, 10, SetType.WARMUP), set(2, 80.0, 12), set(3, 80.0, 12), set(4, 77.5, 12))
        assertEquals(82.5, WorkoutMath.progression(prev, target)!!, 1e-9)
        assertEquals(82.5 to 8, WorkoutMath.hint(2, prev, target))
        // zagrevanje ne dobija progresiju
        assertEquals(40.0 to 10, WorkoutMath.hint(1, prev, target, SetType.WARMUP))
    }

    @Test fun noProgressionIfAnySetMissedOrNoPlan() {
        assertNull(WorkoutMath.progression(listOf(set(1, 80.0, 12), set(2, 80.0, 10)), target))
        assertNull(WorkoutMath.progression(listOf(set(1, 80.0, 12)), null))
    }

    @Test fun warmupNotInVolume() {
        assertEquals(800.0, WorkoutMath.volume(listOf(set(1, 40.0, 10, SetType.WARMUP), set(2, 80.0, 10))), 1e-9)
    }

    @Test fun planUpdateDetectsChanges() {
        val plan = listOf(target, RoutineExercise(routineId = "r", exerciseId = "x", position = 1, targetSets = 3))
        // isti trening kao plan -> nema izmene
        val same = (1..3).map { set(it, 50.0, 10) } + (1..3).map { set(it, 20.0, 10, ex = "x", pos = 1) }
        assertNull(WorkoutMath.planUpdate("r", plan, same))
        // dodat set, uklonjena vezba x, dodata y
        val changed = (1..4).map { set(it, 50.0, 10) } + listOf(set(1, 10.0, 15, ex = "y", pos = 1))
        val u = WorkoutMath.planUpdate("r", plan, changed)!!
        assertEquals(listOf("y"), u.added)
        assertEquals(listOf("x"), u.removed)
        assertEquals(listOf(Triple("e", 3, 4)), u.setChanges)
        assertEquals(50.0, u.items[0].targetKg!!, 1e-9)
        assertEquals(8, u.items[0].repsMin)   // zadrzava ponavljanja iz plana
    }

    @Test fun newRecordsComparedToEarlierOnly() {
        val day = 86_400_000L
        val w1 = Workout(id = "w1", name = "A", startedAt = 0, finishedAt = 1000)
        val w2 = Workout(id = "w2", name = "A", startedAt = day, finishedAt = day + 1000)
        val sets = listOf(set(1, 80.0, 8, w = "w1"), set(1, 85.0, 5, w = "w2"), set(2, 100.0, 1, SetType.WARMUP, w = "w2"))
        val prs = ExerciseStats.newRecords("w2", listOf(w1, w2), sets)
        assertEquals(listOf("Najveća težina"), prs.map { it.kind })   // zagrevanje 100 kg se ne racuna
        assertTrue(ExerciseStats.newRecords("w1", listOf(w1, w2), sets).isEmpty())   // prvi put = nije rekord
    }

    @Test fun streakAndWeeks() {
        val today = LocalDate.of(2026, 11, 13)   // petak
        val dates = listOf(
            LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12),                    // ova nedelja: 2 (cilj 3, jos traje)
            LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 6),
            LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 28), LocalDate.of(2026, 10, 30),
            LocalDate.of(2026, 10, 20),                                                 // samo 1 -> prekid
        )
        assertEquals(2, TrainingStats.streak(dates, 3, today))
        assertEquals(3, TrainingStats.streak(dates, 2, today))
        val weeks = TrainingStats.weeklyCounts(dates, today, 4)
        assertEquals(listOf(1, 3, 3, 2), weeks.map { it.second })
    }

    @Test fun muscleSetsCountsWorkingSetsOnly() {
        val w = Workout(id = "w", name = "A", startedAt = 0, finishedAt = 1000)
        val sets = listOf(set(1, 40.0, 10, SetType.WARMUP), set(2, 80.0, 8), set(3, 80.0, 8), set(1, 20.0, 10, ex = "x", pos = 1))
        val d = LocalDate.ofInstant(java.time.Instant.EPOCH, zone)
        val r = TrainingStats.muscleSets(listOf(w), sets, mapOf("e" to "chest", "x" to "triceps"), d, d)
        assertEquals(listOf("chest" to 2.0, "triceps" to 1.0), r)
        val r2 = TrainingStats.muscleSets(listOf(w), sets, mapOf("e" to "chest", "x" to "triceps"), d, d, secondaryMuscles = mapOf("e" to listOf("triceps", "shoulders")))
        assertEquals(listOf("chest" to 2.0, "triceps" to 2.0, "shoulders" to 1.0), r2)
    }

    @Test fun backupRoundTripAndWrongPassword() {
        val data = ByteArray(5000) { (it % 251).toByte() }
        val enc = BackupCrypto.encrypt(data, "tajna-lozinka".toCharArray())
        assertArrayEquals(data, BackupCrypto.decrypt(enc, "tajna-lozinka".toCharArray()))
        try { BackupCrypto.decrypt(enc, "pogresna".toCharArray()); fail() } catch (e: BackupException) { }
        try { BackupCrypto.decrypt(byteArrayOf(1, 2, 3), "x".toCharArray()); fail() } catch (e: BackupException) { }
    }

    @Test fun openFoodFactsParse() {
        val p = OpenFoodFacts.parse("""{"status":1,"product":{"product_name":"Skyr","brands":"Arla, X","nutriments":{"energy-kcal_100g":63,"proteins_100g":11}}}""")!!
        assertEquals("Skyr (Arla)", p.name)
        assertEquals(63.0, p.kcal100!!, 1e-9)
        assertEquals(11.0, p.protein100!!, 1e-9)
        assertNull(OpenFoodFacts.parse("""{"status":0}"""))
    }

    @Test fun supplementSearchAndHistory() {
        assertEquals("Kreatin monohidrat", Supplements.search("creatine", emptyList()).first().name)
        assertEquals("Omega-3 (riblje ulje)", Supplements.search("omega", emptyList()).first().name)
        assertTrue(Supplements.search("magnezijum", emptyList()).any { it.name.startsWith("Magnezijum") })
        val hist = listOf(FoodEntry(epochDay = 1, meal = Supplements.MEAL, text = "Moj mix · 2 kapsule"))
        val first = Supplements.search("", hist).first()
        assertEquals("Moj mix", first.name); assertEquals("2 kapsule", first.dose)
        assertEquals("Kreatin · 5 g", Supplements.entryText("Kreatin", "5 g"))
        assertTrue(Supplements.LIST.size >= 40)
    }

    @Test fun foodSuggestionsSkipSupplementsAndPreferMeal() {
        val recent = listOf(
            FoodEntry(epochDay = 1, meal = "Doručak", text = "Ovsene", kcal = 300, createdAt = 3),
            FoodEntry(epochDay = 2, meal = "Doručak", text = "ovsene ", kcal = 320, createdAt = 5),
            FoodEntry(epochDay = 1, meal = "Ručak", text = "Piletina", createdAt = 4),
            FoodEntry(epochDay = 1, meal = Supplements.MEAL, text = "Kreatin · 5 g", createdAt = 6),
        )
        val s = FoodQuick.suggestions(recent, "Ručak")
        assertEquals(listOf("Piletina", "ovsene"), s.map { it.text })
        assertEquals(320, s[1].kcal)
        assertNotNull(s.first())
    }
}
