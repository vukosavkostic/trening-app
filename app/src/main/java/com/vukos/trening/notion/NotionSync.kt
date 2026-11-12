package com.vukos.trening.notion

import androidx.room.withTransaction
import com.vukos.trening.TreningApp
import com.vukos.trening.data.BodyEntry
import com.vukos.trening.data.SetType
import com.vukos.trening.data.Exercise
import com.vukos.trening.data.FoodEntry
import com.vukos.trening.data.Routine
import com.vukos.trening.data.RoutineExercise
import com.vukos.trening.data.Settings
import com.vukos.trening.data.Weekdays
import com.vukos.trening.data.Workout
import com.vukos.trening.data.WorkoutMath
import com.vukos.trening.data.WorkoutSet
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Gradjenje Notion "properties" vrednosti. */
object Props {
    private fun textArr(s: String) = JSONArray().put(JSONObject().put("text", JSONObject().put("content", s.take(1900))))
    fun title(s: String) = JSONObject().put("title", textArr(s.ifBlank { "—" }))
    fun text(s: String) = JSONObject().put("rich_text", if (s.isBlank()) JSONArray() else textArr(s))
    fun select(s: String?) = JSONObject().put("select", if (s.isNullOrBlank()) JSONObject.NULL else JSONObject().put("name", s.replace(",", " ")))
    fun multi(list: List<String>) = JSONObject().put("multi_select", JSONArray(list.filter { it.isNotBlank() }.map { JSONObject().put("name", it.replace(",", " ")) }))
    fun number(n: Number?) = JSONObject().put("number", n ?: JSONObject.NULL)
    fun checkbox(b: Boolean) = JSONObject().put("checkbox", b)
    fun date(iso: String) = JSONObject().put("date", JSONObject().put("start", iso))
    fun relation(pageIds: List<String>) = JSONObject().put("relation", JSONArray(pageIds.distinct().map { JSONObject().put("id", it) }))
    fun externalFile(url: String) = JSONObject().put(
        "files", JSONArray().put(JSONObject().put("name", "slika").put("type", "external").put("external", JSONObject().put("url", url)))
    )

    fun dateTime(epochMs: Long): String =
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

data class SyncReport(val created: Int, val updated: Int, val archived: Int)
data class RestoreReport(val routines: Int, val workouts: Int, val sets: Int, val body: Int, val food: Int, val exercises: Int)

/**
 * Sinhronizacija: aplikacija -> Notion (slanje svih izmena) i Notion -> aplikacija (vracanje podataka
 * kojih nema lokalno, npr. posle reinstalacije). Slike tela se NIKAD ne salju.
 */
object NotionSync {
    private val db get() = TreningApp.instance.db
    private val sync get() = db.sync()

    private class Ctx(val c: NotionClient, val dbs: Map<String, String>) {
        var created = 0; var updated = 0; var archived = 0
        fun dbId(name: String) = dbs[name] ?: throw NotionException(404, "Baza $name nije pronađena na stranici Trening.")
    }

    private fun ctx(settings: Settings): Ctx {
        val token = settings.notionToken ?: throw NotionException(401, "Notion token nije unet.")
        return Ctx(NotionClient(token), settings.notionDatabases)
    }

    /** Kreira ili azurira stranicu; ako je stranica obrisana u Notionu, pravi novu. Vraca ID stranice. */
    private suspend fun upsertPage(x: Ctx, db: String, pageId: String?, props: JSONObject): String {
        if (pageId != null) {
            try {
                x.c.request("PATCH", "pages/$pageId", JSONObject().put("properties", props).put("archived", false))
                x.updated++
                return pageId
            } catch (e: NotionException) {
                if (e.status != 404 && e.status != 400) throw e
            }
        }
        val r = x.c.request(
            "POST", "pages",
            JSONObject().put("parent", JSONObject().put("database_id", x.dbId(db))).put("properties", props),
        )
        x.created++
        return r.getString("id")
    }

    private suspend fun archive(x: Ctx, pageId: String?) {
        if (pageId == null) return
        try {
            x.c.request("PATCH", "pages/$pageId", JSONObject().put("archived", true)); x.archived++
        } catch (e: NotionException) {
            if (e.status != 404 && e.status != 400) throw e
        }
    }

    /** Stranica vezbe (pravi se po potrebi, za relacije iz planova i setova). */
    private suspend fun exercisePageId(x: Ctx, id: String): String? {
        val ex = db.exercises().get(id) ?: return null
        ex.notionPageId?.let { return it }
        val pageId = upsertPage(x, "Exercises", null, exerciseProps(ex))
        sync.exercisePage(id, pageId)
        return pageId
    }

    private fun exerciseProps(ex: Exercise) = JSONObject()
        .put("Name", Props.title(ex.name))
        .put("Primary muscle", Props.select(ex.primaryMuscle))
        .put("Secondary muscles", Props.multi(ex.secondaryList))
        .put("Equipment", Props.select(ex.equipment))
        .put("Category", Props.select(ex.category))
        .put("Custom", Props.checkbox(ex.isCustom))
        .put("Notes", Props.text(ex.notes))
        .put("AppId", Props.text(ex.id))
        .apply { ex.thumbnailUrl?.takeIf { it.startsWith("https://") }?.let { put("Image", Props.externalFile(it)) } }

    /** Salje sve izmene u Notion. */
    suspend fun push(settings: Settings): SyncReport {
        val x = ctx(settings)

        for (ex in sync.dirtyExercises()) {
            if (ex.deleted) { archive(x, ex.notionPageId); sync.exerciseSynced(ex.id, ex.notionPageId, ex.updatedAt); continue }
            val pid = upsertPage(x, "Exercises", ex.notionPageId, exerciseProps(ex))
            sync.exerciseSynced(ex.id, pid, ex.updatedAt)
        }

        for (r in sync.dirtyRoutines()) {
            if (r.deleted) { archive(x, r.notionPageId); sync.routineSynced(r.id, r.notionPageId, r.updatedAt); continue }
            val items = db.routines().getExercises(r.id)
            val names = items.map { db.exercises().get(it.exerciseId)?.name ?: "?" }
            val plan = items.zip(names).joinToString("\n") { (it, n) -> planLine(n, it) }
            val props = JSONObject()
                .put("Name", Props.title(r.name))
                .put("Days", Props.multi(Weekdays.toLabels(r.weekdays)))
                .put("Exercises", Props.relation(items.mapNotNull { exercisePageId(x, it.exerciseId) }))
                .put("Plan", Props.text(plan))
                .put("Notes", Props.text(r.notes))
                .put("AppId", Props.text(r.id))
            sync.routineSynced(r.id, upsertPage(x, "Routines", r.notionPageId, props), r.updatedAt)
        }

        val workoutPages = mutableMapOf<String, String>()
        for (w in sync.dirtyWorkouts()) {
            if (w.deleted) {
                archive(x, w.notionPageId)
                sync.syncedSetsOf(w.id).forEach { archive(x, it.notionPageId); sync.setSynced(it.id, null, it.updatedAt) }
                sync.workoutSynced(w.id, w.notionPageId, w.updatedAt); continue
            }
            val sets = db.workouts().getSets(w.id)
            val routinePage = w.routineId?.let { db.routines().get(it)?.notionPageId }
            val props = JSONObject()
                .put("Name", Props.title(w.name))
                .put("Date", Props.date(Props.dateTime(w.startedAt)))
                .put("Volume kg", Props.number(WorkoutMath.volume(sets)))
                .put("Sets count", Props.number(WorkoutMath.doneCount(sets)))
                .put("Duration min", Props.number(((w.finishedAt!! - w.startedAt) / 60_000).coerceAtLeast(0)))
                .put("Notes", Props.text(w.notes))
                .put("AppId", Props.text(w.id))
                .apply { if (routinePage != null) put("Routine", Props.relation(listOf(routinePage))) }
            val pid = upsertPage(x, "Workouts", w.notionPageId, props)
            workoutPages[w.id] = pid
            sync.workoutSynced(w.id, pid, w.updatedAt)
        }

        for (s in sync.dirtySets()) {
            if (s.deleted) { archive(x, s.notionPageId); sync.setSynced(s.id, s.notionPageId, s.updatedAt); continue }
            val w = db.workouts().get(s.workoutId) ?: continue
            if (w.deleted) { archive(x, s.notionPageId); sync.setSynced(s.id, null, s.updatedAt); continue }
            val wPage = workoutPages[w.id] ?: w.notionPageId ?: continue   // trening jos nije poslat
            val exName = db.exercises().get(s.exerciseId)?.name ?: "?"
            val props = JSONObject()
                .put("Name", Props.title("$exName #${s.setIndex}"))
                .put("Workout", Props.relation(listOf(wPage)))
                .put("Date", Props.date(Props.dateTime(w.startedAt)))
                .put("Set #", Props.number(s.setIndex))
                .put("Kg", Props.number(s.weightKg))
                .put("Reps", Props.number(s.reps))
                .put("Order", Props.number(s.exercisePosition))
                .put("Type", Props.select(s.setType))
                .put("AppId", Props.text(s.id))
                .apply { exercisePageId(x, s.exerciseId)?.let { put("Exercise", Props.relation(listOf(it))) } }
            sync.setSynced(s.id, upsertPage(x, "Sets", s.notionPageId, props), s.updatedAt)
        }

        for (b in sync.dirtyBody()) {
            if (b.deleted) { archive(x, b.notionPageId); sync.bodySynced(b.id, b.notionPageId, b.updatedAt); continue }
            val day = LocalDate.ofEpochDay(b.epochDay)
            val props = JSONObject()
                .put("Name", Props.title("Merenje ${day.dayOfMonth}.${day.monthValue}.${day.year}."))
                .put("Date", Props.date(day.toString()))
                .put("Weight kg", Props.number(b.weightKg))
                .put("Photos taken", Props.checkbox(sync.photoCount(b.id) > 0))   // samo oznaka, slike ostaju na telefonu
                .put("Waist cm", Props.number(b.waistCm))
                .put("Chest cm", Props.number(b.chestCm))
                .put("Arm cm", Props.number(b.armCm))
                .put("Thigh cm", Props.number(b.thighCm))
                .put("Body fat %", Props.number(b.bodyFatPct))
                .put("Notes", Props.text(b.notes))
                .put("AppId", Props.text(b.id))
            sync.bodySynced(b.id, upsertPage(x, "Body", b.notionPageId, props), b.updatedAt)
        }

        for (f in sync.dirtyFood()) {
            if (f.deleted) { archive(x, f.notionPageId); sync.foodSynced(f.id, f.notionPageId, f.updatedAt); continue }
            val props = JSONObject()
                .put("Name", Props.title(f.text))
                .put("Date", Props.date(LocalDate.ofEpochDay(f.epochDay).toString()))
                .put("Meal", Props.select(f.meal))
                .put("Kcal", Props.number(f.kcal))
                .put("Protein g", Props.number(f.proteinG))
                .put("AppId", Props.text(f.id))
            sync.foodSynced(f.id, upsertPage(x, "Food", f.notionPageId, props), f.updatedAt)
        }
        return SyncReport(x.created, x.updated, x.archived)
    }

    fun planLine(name: String, re: RoutineExercise): String {
        val reps = when {
            re.repsMin == null && re.repsMax == null -> ""
            re.repsMax == null || re.repsMax == re.repsMin -> "${re.repsMin ?: re.repsMax}"
            re.repsMin == null -> "${re.repsMax}"
            else -> "${re.repsMin}-${re.repsMax}"
        }
        return buildString {
            append(name)
            if (reps.isNotEmpty()) append(" ${re.targetSets}x$reps")
            re.targetKg?.let { append(" ${WorkoutMath.fmtKg(it)}kg") }
        }
    }

    /**
     * Vraca iz Notiona sve sto nedostaje lokalno (po AppId). Postojeci lokalni podaci se ne menjaju.
     */
    suspend fun restore(settings: Settings): RestoreReport {
        val x = ctx(settings)
        val exPages = x.c.queryDatabase(x.dbId("Exercises"))
        val rPages = x.c.queryDatabase(x.dbId("Routines"))
        val wPages = x.c.queryDatabase(x.dbId("Workouts"))
        val sPages = x.c.queryDatabase(x.dbId("Sets"))
        val bPages = x.c.queryDatabase(x.dbId("Body"))
        val fPages = x.c.queryDatabase(x.dbId("Food"))

        fun appId(p: JSONObject) = NotionClient.text(p, "AppId").ifBlank { null }
        fun num(p: JSONObject, prop: String): Double? =
            p.optJSONObject("properties")?.optJSONObject(prop)?.let { if (it.isNull("number")) null else it.optDouble("number") }
        fun dateStart(p: JSONObject, prop: String): String? =
            p.optJSONObject("properties")?.optJSONObject(prop)?.optJSONObject("date")?.optString("start")?.ifBlank { null }
        fun relIds(p: JSONObject, prop: String): List<String> {
            val arr = p.optJSONObject("properties")?.optJSONObject(prop)?.optJSONArray("relation") ?: return emptyList()
            return List(arr.length()) { arr.getJSONObject(it).getString("id") }
        }
        fun select(p: JSONObject, prop: String): String? =
            p.optJSONObject("properties")?.optJSONObject(prop)?.optJSONObject("select")?.optString("name")
        fun checkbox(p: JSONObject, prop: String) = p.optJSONObject("properties")?.optJSONObject(prop)?.optBoolean("checkbox") ?: false
        fun epochMs(iso: String): Long = runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }
            .getOrElse { LocalDate.parse(iso.take(10)).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
        fun epochDay(iso: String) = LocalDate.parse(iso.take(10)).toEpochDay()

        // stranica -> AppId, za razresavanje relacija
        val exByPage = exPages.mapNotNull { p -> appId(p)?.let { p.getString("id") to it } }.toMap()
        val routineByPage = rPages.mapNotNull { p -> appId(p)?.let { p.getString("id") to it } }.toMap()
        val workoutByPage = wPages.mapNotNull { p -> appId(p)?.let { p.getString("id") to it } }.toMap()

        val newExercises = exPages.filter { checkbox(it, "Custom") }.mapNotNull { p ->
            val id = appId(p) ?: return@mapNotNull null
            Exercise(
                id = id, name = NotionClient.titleOf(p), primaryMuscle = select(p, "Primary muscle") ?: "",
                equipment = select(p, "Equipment") ?: "", category = select(p, "Category") ?: "strength",
                isCustom = true, notionPageId = p.getString("id"), dirty = false,
            )
        }
        db.sync().insertExercises(newExercises)
        val allExercises = db.exercises().getAll()

        val localRoutines = sync.routineIds().toSet()
        val newRoutines = mutableListOf<Routine>()
        val newRoutineItems = mutableListOf<RoutineExercise>()
        for (p in rPages) {
            val id = appId(p) ?: continue
            if (id in localRoutines) continue
            val r = Routine(
                id = id, name = NotionClient.titleOf(p), weekdays = Weekdays.fromLabels(NotionClient.multiSelect(p, "Days")),
                notes = NotionClient.text(p, "Notes"), notionPageId = p.getString("id"), dirty = false,
            )
            newRoutines += r
            PlanParser.parseText(NotionClient.text(p, "Plan")).forEachIndexed { i, line ->
                val ex = ExerciseMatcher.match(line.name, allExercises) ?: return@forEachIndexed
                newRoutineItems += RoutineExercise(
                    routineId = id, exerciseId = ex.id, position = i, targetSets = line.sets,
                    repsMin = line.repsMin, repsMax = line.repsMax, targetKg = line.kg,
                )
            }
        }

        val localWorkouts = sync.workoutIds().toSet()
        val newWorkouts = wPages.mapNotNull { p ->
            val id = appId(p) ?: return@mapNotNull null
            if (id in localWorkouts) return@mapNotNull null
            val start = dateStart(p, "Date")?.let(::epochMs) ?: return@mapNotNull null
            val minutes = num(p, "Duration min") ?: 0.0
            Workout(
                id = id, name = NotionClient.titleOf(p), startedAt = start, finishedAt = start + (minutes * 60_000).toLong(),
                routineId = relIds(p, "Routine").firstOrNull()?.let { routineByPage[it] },
                notes = NotionClient.text(p, "Notes"), notionPageId = p.getString("id"), dirty = false,
            )
        }
        val restoredWorkoutIds = newWorkouts.map { it.id }.toSet()
        val newSets = sPages.mapNotNull { p ->
            val id = appId(p) ?: return@mapNotNull null
            val wid = relIds(p, "Workout").firstOrNull()?.let { workoutByPage[it] } ?: return@mapNotNull null
            if (wid !in restoredWorkoutIds) return@mapNotNull null
            val exId = relIds(p, "Exercise").firstOrNull()?.let { exByPage[it] } ?: return@mapNotNull null
            WorkoutSet(
                id = id, workoutId = wid, exerciseId = exId,
                exercisePosition = num(p, "Order")?.toInt() ?: 0, setIndex = num(p, "Set #")?.toInt() ?: 1,
                weightKg = num(p, "Kg"), reps = num(p, "Reps")?.toInt(), done = true,
                setType = select(p, "Type")?.takeIf { it in SetType.ALL } ?: SetType.NORMAL,
                notionPageId = p.getString("id"), dirty = false,
            )
        }
        val newBody = bPages.mapNotNull { p ->
            val id = appId(p) ?: return@mapNotNull null
            val d = dateStart(p, "Date") ?: return@mapNotNull null
            if (db.body().getByDay(epochDay(d)) != null) return@mapNotNull null   // vec postoji merenje za taj dan
            BodyEntry(id = id, epochDay = epochDay(d), weightKg = num(p, "Weight kg"), notes = NotionClient.text(p, "Notes"),
                waistCm = num(p, "Waist cm"), chestCm = num(p, "Chest cm"), armCm = num(p, "Arm cm"),
                thighCm = num(p, "Thigh cm"), bodyFatPct = num(p, "Body fat %"),
                notionPageId = p.getString("id"), dirty = false)
        }
        val newFood = fPages.mapNotNull { p ->
            val id = appId(p) ?: return@mapNotNull null
            val d = dateStart(p, "Date") ?: return@mapNotNull null
            FoodEntry(id = id, epochDay = epochDay(d), meal = select(p, "Meal") ?: "Užina", text = NotionClient.titleOf(p),
                kcal = num(p, "Kcal")?.toInt(), proteinG = num(p, "Protein g")?.toInt(),
                notionPageId = p.getString("id"), dirty = false)
        }

        db.withTransaction {
            sync.insertRoutines(newRoutines)
            sync.insertRoutineExercises(newRoutineItems)
            sync.insertWorkouts(newWorkouts)
            sync.insertSets(newSets)
            sync.insertBody(newBody)
            sync.insertFood(newFood)
        }
        return RestoreReport(newRoutines.size, newWorkouts.size, newSets.size, newBody.size, newFood.size, newExercises.size)
    }
}
