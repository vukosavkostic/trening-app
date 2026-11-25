package com.vukos.trening.data

import android.content.Context
import android.util.Base64

/**
 * Podesavanja. Notion token se cuva sifrovan (Keystore), nikad u cistom tekstu,
 * i nije ugradjen u APK - unosi se jednom u aplikaciji.
 * ID-jevi Notion baza se ne upisuju rucno: aplikacija ih sama pronalazi na stranici "Trening".
 */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val crypto = KeystoreCrypto("trening_settings")

    var notionToken: String?
        get() = prefs.getString("notion_token", null)?.let {
            runCatching { String(crypto.decrypt(Base64.decode(it, Base64.NO_WRAP))) }.getOrNull()
        }
        set(value) {
            prefs.edit().apply {
                if (value.isNullOrBlank()) remove("notion_token")
                else putString("notion_token", Base64.encodeToString(crypto.encrypt(value.trim().toByteArray()), Base64.NO_WRAP))
            }.apply()
        }

    /** Naziv baze -> ID (Exercises, Routines, Workouts, Sets, Body, Food). */
    var notionDatabases: Map<String, String>
        get() = prefs.getString("notion_dbs", "")!!.split(';').filter { '=' in it }
            .associate { it.substringBefore('=') to it.substringAfter('=') }
        set(value) { prefs.edit().putString("notion_dbs", value.entries.joinToString(";") { "${it.key}=${it.value}" }).apply() }

    val notionConfigured get() = notionToken != null && notionDatabases.containsKey("Routines")

    /** Poslednja sinhronizacija: vreme, greska (null = uspeh), broj promenjenih stranica. */
    fun recordSync(error: String?, changed: Int) {
        prefs.edit().putLong("sync_at", System.currentTimeMillis()).putString("sync_error", error)
            .putInt("sync_changed", changed).apply()
    }
    val lastSyncAt get() = prefs.getLong("sync_at", 0L)
    val lastSyncError: String? get() = prefs.getString("sync_error", null)
    val lastSyncChanged get() = prefs.getInt("sync_changed", 0)

    /** Dnevni ciljevi ishrane (0 = nije postavljen) i nedeljni cilj treninga. */
    var kcalGoal: Int
        get() = prefs.getInt("kcal_goal", 0)
        set(v) { prefs.edit().putInt("kcal_goal", v).apply() }
    var proteinGoal: Int
        get() = prefs.getInt("protein_goal", 0)
        set(v) { prefs.edit().putInt("protein_goal", v).apply() }
    /** Podrazumevani odmor izmedju setova (sekunde); pokrece se kad se set stiklira. */
    var restDefaultSec: Int
        get() = prefs.getInt("rest_default_sec", 90)
        set(v) { prefs.edit().putInt("rest_default_sec", v).apply() }

    var weeklyGoal: Int
        get() = prefs.getInt("weekly_goal", 3)
        set(v) { prefs.edit().putInt("weekly_goal", v).apply() }

    var lastBackupAt: Long
        get() = prefs.getLong("backup_at", 0L)
        set(v) { prefs.edit().putLong("backup_at", v).apply() }
}
