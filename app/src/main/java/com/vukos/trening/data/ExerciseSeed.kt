package com.vukos.trening.data

import android.content.Context
import org.json.JSONArray

/**
 * Uvozi free-exercise-db (assets/exercises.json, Unlicense) u Room pri prvom pokretanju.
 * Kad se JSON azurira, poveca se SEED_VERSION i nove vezbe se dodaju (postojece ostaju).
 */
object ExerciseSeed {
    private const val SEED_VERSION = 1
    private const val PREFS = "seed"

    suspend fun ensureSeeded(context: Context, dao: ExerciseDao) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt("version", 0) >= SEED_VERSION && dao.builtInCount() > 0) return
        val json = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
        dao.insertIgnore(parse(json))
        prefs.edit().putInt("version", SEED_VERSION).apply()
    }

    fun parse(json: String): List<Exercise> {
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            fun list(key: String): List<String> =
                o.optJSONArray(key)?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList()
            Exercise(
                id = o.getString("id"),
                name = o.getString("name"),
                primaryMuscle = list("primaryMuscles").firstOrNull() ?: "",
                secondaryMuscles = (list("primaryMuscles").drop(1) + list("secondaryMuscles")).joinToString(","),
                equipment = o.optString("equipment").takeUnless { it == "null" } ?: "",
                category = o.optString("category", "strength"),
                level = o.optString("level"),
                instructions = list("instructions").joinToString("\n"),
                imagePaths = list("images").joinToString(","),
                dirty = false,
            )
        }
    }
}
