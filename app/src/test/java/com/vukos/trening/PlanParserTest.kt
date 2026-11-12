package com.vukos.trening

import com.vukos.trening.data.ExerciseSeed
import com.vukos.trening.notion.ExerciseMatcher
import com.vukos.trening.notion.PlanLine
import com.vukos.trening.notion.PlanParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class PlanParserTest {
    private val all = ExerciseSeed.parse(File("src/main/assets/exercises.json").readText())

    @Test fun fullLine() {
        assertEquals(PlanLine("Triceps Pushdown", 3, 10, 12, 25.0), PlanParser.parseLine("- Triceps Pushdown 3x10-12 25kg"))
    }

    @Test fun variants() {
        assertEquals(PlanLine("Incline Dumbbell Press", 3, 10, 10), PlanParser.parseLine("Incline Dumbbell Press 3x10"))
        assertEquals(PlanLine("Squat", 4, 6, 8, 102.5), PlanParser.parseLine("1. Squat 4 × 6–8 102,5 kg"))
        assertEquals(PlanLine("Plank"), PlanParser.parseLine("• Plank"))
        assertNull(PlanParser.parseLine("   "))
        assertNull(PlanParser.parseLine("- 3x10"))
    }

    @Test fun blocksIgnoreDescriptionParagraphs() {
        val blocks = listOf(
            "paragraph" to "Jedna vežba po redu: naziv × setovi × ponavljanja (kilaža opciono).",
            "bulleted_list_item" to "Dumbbell Bench Press 3x8-12",
            "paragraph" to "Side Lateral Raise 3x12-15",
            "heading_2" to "Noge",
            "to_do" to "Calf Raise",
        )
        assertEquals(
            listOf("Dumbbell Bench Press", "Side Lateral Raise", "Calf Raise"),
            PlanParser.parseBlocks(blocks).map { it.name },
        )
    }

    @Test fun exampleRoutineFromNotionMatchesDatabase() {
        val lines = PlanParser.parseText(
            """
            - Dumbbell Bench Press 3x8-12
            - Incline Dumbbell Press 3x10
            - Side Lateral Raise 3x12-15
            - Triceps Pushdown 3x10-12 25kg
            """.trimIndent()
        )
        val matched = lines.map { ExerciseMatcher.match(it.name, all)?.name }
        assertEquals(listOf("Dumbbell Bench Press", "Incline Dumbbell Press", "Side Lateral Raise", "Triceps Pushdown"), matched)
    }

    @Test fun fuzzyAndMissing() {
        assertEquals("Dumbbell Bench Press", ExerciseMatcher.match("bench press db", all)?.name)
        assertEquals("Dumbbell Bench Press", ExerciseMatcher.match("dumbbell bench press", all)?.name)
        assertNull(ExerciseMatcher.match("Čučanj na jednoj nozi sa skokom", all))
    }
}

class WeekdaysTest {
    @org.junit.Test fun labelsRoundTrip() {
        val m = com.vukos.trening.data.Weekdays.fromLabels(listOf("Pon", "Čet"))
        org.junit.Assert.assertEquals(0b0001001, m)
        org.junit.Assert.assertEquals(listOf("Pon", "Čet"), com.vukos.trening.data.Weekdays.toLabels(m))
        org.junit.Assert.assertTrue(com.vukos.trening.data.Weekdays.has(m, java.time.DayOfWeek.THURSDAY))
        org.junit.Assert.assertEquals(0b1000000, com.vukos.trening.data.Weekdays.fromLabels(listOf("Nedelja")))
    }
}
