package com.vukos.trening

import android.app.Application
import com.vukos.trening.data.AppDatabase
import com.vukos.trening.data.ExerciseSeed
import com.vukos.trening.data.Settings
import com.vukos.trening.notion.SyncWorker
import com.vukos.trening.reminders.Reminders
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TreningApp : Application() {
    val db: AppDatabase by lazy { AppDatabase.create(this) }
    val settings: Settings by lazy { Settings(this) }
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        appScope.launch { ExerciseSeed.ensureSeeded(this@TreningApp, db.exercises()) }
        // Sinhronizacija: kad god postoje neposlate izmene (i Notion je povezan), zakazi slanje.
        SyncWorker.schedulePeriodic(this)
        appScope.launch {
            db.sync().observePending().distinctUntilChanged().collect { pending ->
                if (pending > 0 && settings.notionConfigured) SyncWorker.request(this@TreningApp)
            }
        }
        Reminders.createChannel(this)
        Reminders.schedule(this)
        com.vukos.trening.reminders.SupplementReminders.createChannel(this)
        com.vukos.trening.reminders.SupplementReminders.scheduleAll(this)
        // Zaostali privremeni fajlovi kamere (npr. ako je merenje otkazano) se brisu.
        File(cacheDir, "capture").listFiles()?.forEach { it.delete() }
    }

    companion object {
        lateinit var instance: TreningApp
            private set
    }
}
