package com.vukos.trening

import com.vukos.trening.data.RoutineExercise
import com.vukos.trening.notion.NotionClient
import com.vukos.trening.notion.NotionSync
import com.vukos.trening.notion.Props
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class NotionPayloadTest {
    @Test fun planLineRoundTripsThroughParser() {
        val re = RoutineExercise(routineId = "r", exerciseId = "e", position = 0, targetSets = 4, repsMin = 6, repsMax = 8, targetKg = 102.5)
        val line = NotionSync.planLine("Barbell Squat", re)
        assertEquals("Barbell Squat 4x6-8 102.5kg", line)
        val parsed = com.vukos.trening.notion.PlanParser.parseLine(line)!!
        assertEquals(listOf("Barbell Squat", 4, 6, 8, 102.5), listOf(parsed.name, parsed.sets, parsed.repsMin, parsed.repsMax, parsed.kg))
        assertEquals("Plank 3x30", NotionSync.planLine("Plank", re.copy(targetSets = 3, repsMin = 30, repsMax = 30, targetKg = null)))
    }

    /**
     * Pravi Notion: pravi po jednu probnu stranicu u svakoj bazi istim formatom kao sinhronizacija,
     * pa je odmah arhivira (ide u Notion Trash). Samo lokalno, uz NOTION_TOKEN_FILE.
     */
    @Test fun liveCreateAndArchiveEveryPayloadShape() = runBlocking {
        val f = System.getenv("NOTION_TOKEN_FILE")?.let(::File)
        assumeTrue(f?.exists() == true)
        val c = NotionClient(f!!.readText().trim())
        val dbs = c.discoverDatabases()
        val now = System.currentTimeMillis()
        val payloads = mapOf(
            "Exercises" to JSONObject().put("Name", Props.title("TEST sync")).put("Primary muscle", Props.select("chest"))
                .put("Secondary muscles", Props.multi(listOf("triceps"))).put("Equipment", Props.select("dumbbell"))
                .put("Category", Props.select("strength")).put("Custom", Props.checkbox(true)).put("AppId", Props.text("test"))
                .put("Image", Props.externalFile("https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/Dumbbell_Bench_Press/0.jpg")),
            "Workouts" to JSONObject().put("Name", Props.title("TEST sync")).put("Date", Props.date(Props.dateTime(now)))
                .put("Volume kg", Props.number(1234.5)).put("Sets count", Props.number(12)).put("Duration min", Props.number(55))
                .put("Notes", Props.text("")).put("AppId", Props.text("test")),
            "Body" to JSONObject().put("Name", Props.title("TEST sync")).put("Date", Props.date("2026-11-15"))
                .put("Weight kg", Props.number(82.4)).put("Photos taken", Props.checkbox(true)).put("AppId", Props.text("test")),
            "Food" to JSONObject().put("Name", Props.title("TEST sync")).put("Date", Props.date("2026-11-15"))
                .put("Meal", Props.select("Ručak")).put("Kcal", Props.number(null)).put("Protein g", Props.number(40)).put("AppId", Props.text("test")),
            "Routines" to JSONObject().put("Name", Props.title("TEST sync")).put("Days", Props.multi(listOf("Pon", "Čet")))
                .put("Plan", Props.text("Dumbbell Bench Press 3x8-12")).put("AppId", Props.text("test")),
        )
        for ((db, props) in payloads) {
            val page = c.request("POST", "pages", JSONObject().put("parent", JSONObject().put("database_id", dbs.getValue(db))).put("properties", props))
            val id = page.getString("id")
            c.request("PATCH", "pages/$id", JSONObject().put("properties", JSONObject().put("Name", Props.title("TEST sync 2"))))
            c.request("PATCH", "pages/$id", JSONObject().put("archived", true))
        }
    }
}
