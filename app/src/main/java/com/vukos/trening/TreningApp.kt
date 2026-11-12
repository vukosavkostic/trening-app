package com.vukos.trening

import android.app.Application
import com.vukos.trening.data.AppDatabase
import com.vukos.trening.data.ExerciseSeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TreningApp : Application() {
    val db: AppDatabase by lazy { AppDatabase.create(this) }
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        appScope.launch { ExerciseSeed.ensureSeeded(this@TreningApp, db.exercises()) }
    }

    companion object {
        lateinit var instance: TreningApp
            private set
    }
}
