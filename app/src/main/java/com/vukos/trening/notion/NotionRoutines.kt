package com.vukos.trening.notion

import com.vukos.trening.i18n.tr

import androidx.room.withTransaction
import com.vukos.trening.TreningApp
import com.vukos.trening.data.Exercise
import com.vukos.trening.data.Routine
import com.vukos.trening.data.RoutineExercise
import com.vukos.trening.data.Settings
import com.vukos.trening.data.Weekdays
import com.vukos.trening.data.newId

/** Plan iz Notion baze Routines, kako se prikazuje na ekranu "Notion treninzi". */
data class NotionRoutine(
    val pageId: String,
    val name: String,
    val days: List<String>,
    val notes: String,
    /** Lokalni ID iz polja AppId (ako je plan vec dodat u aplikaciju). */
    val appId: String?,
    val lines: List<PlanLine>,
)

data class ImportResult(val routineId: String, val matched: Int, val created: List<String>)

object NotionRoutines {
    private val app get() = TreningApp.instance
    private fun client(settings: Settings) =
        NotionClient(settings.notionToken ?: throw NotionException(401, tr("Notion token nije unet (Podešavanja).", "Notion token is not set (Settings).")))

    /** Proverava token i pamti ID-jeve baza. */
    suspend fun connect(settings: Settings, token: String): Map<String, String> {
        val dbs = NotionClient(token.trim()).discoverDatabases()
        settings.notionToken = token
        settings.notionDatabases = dbs
        return dbs
    }

    /** Svi planovi iz Notiona; vezbe iz polja "Plan" ili, ako je prazno, iz tela stranice. */
    suspend fun list(settings: Settings): List<NotionRoutine> {
        val c = client(settings)
        val dbId = settings.notionDatabases["Routines"] ?: throw NotionException(404, tr("Baza Routines nije pronađena.", "Routines database not found."))
        return c.queryDatabase(dbId).filter { !it.optBoolean("archived") }.map { page ->
            val planText = NotionClient.text(page, "Plan")
            val lines = if (planText.isNotBlank()) PlanParser.parseText(planText)
            else PlanParser.parseBlocks(c.children(page.getString("id")).map(NotionClient::blockText))
            NotionRoutine(
                pageId = page.getString("id"),
                name = NotionClient.titleOf(page).ifBlank { "Bez naziva" },
                days = NotionClient.multiSelect(page, "Days"),
                notes = NotionClient.text(page, "Notes"),
                appId = NotionClient.text(page, "AppId").ifBlank { null },
                lines = lines,
            )
        }.sortedBy { it.name.lowercase() }
    }

    /**
     * Dodaje plan u aplikaciju: vezbe se uparuju sa bazom, a one koje ne postoje se prave kao
     * sopstvene vezbe (da se nista ne izgubi). U Notion se upisuje AppId -> plan postaje "Dodato".
     */
    suspend fun import(settings: Settings, nr: NotionRoutine): ImportResult {
        val db = app.db
        val all = db.exercises().getAll()
        val routine = Routine(
            name = nr.name,
            weekdays = Weekdays.fromLabels(nr.days),
            notes = nr.notes,
            notionPageId = nr.pageId,
            dirty = false,
        )
        val created = mutableListOf<String>()
        var matched = 0
        val items = nr.lines.mapIndexed { i, line ->
            val ex = ExerciseMatcher.match(line.name, all)?.also { matched++ }
                ?: Exercise(
                    id = "custom_" + newId(), name = line.name, primaryMuscle = "",
                    isCustom = true, dirty = true,
                ).also { db.exercises().upsert(it); created += line.name }
            RoutineExercise(
                routineId = routine.id, exerciseId = ex.id, position = i,
                targetSets = line.sets, repsMin = line.repsMin, repsMax = line.repsMax, targetKg = line.kg,
            )
        }
        db.withTransaction {
            db.routines().upsert(routine)
            db.routines().upsertExercises(items)
        }
        client(settings).updatePage(nr.pageId, org.json.JSONObject().put("AppId", NotionClient.richText(routine.id)))
        return ImportResult(routine.id, matched, created)
    }
}
