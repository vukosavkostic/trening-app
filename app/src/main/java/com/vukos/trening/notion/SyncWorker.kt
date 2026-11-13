package com.vukos.trening.notion

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vukos.trening.TreningApp
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Pozadinsko slanje izmena u Notion (radi samo kad ima interneta). */
class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as TreningApp
        if (!app.settings.notionConfigured) return Result.success()
        return try {
            val r = NotionSync.push(app.settings)
            app.settings.recordSync(null, r.created + r.updated + r.archived)
            Result.success()
        } catch (e: NotionException) {
            app.settings.recordSync(e.message ?: com.vukos.trening.i18n.tr("Greška", "Error"), 0)
            if (e.status == 429 || e.status >= 500) Result.retry() else Result.failure()
        } catch (e: IOException) {
            app.settings.recordSync(com.vukos.trening.i18n.tr("Nema veze sa Notionom", "No connection to Notion"), 0)
            Result.retry()
        }
    }

    companion object {
        private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Zakazuje slanje (sa malim kasnjenjem da se vise izmena posalje odjednom). */
        fun request(context: Context, delaySeconds: Long = 20) {
            val req = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(network)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("notion-sync", ExistingWorkPolicy.APPEND_OR_REPLACE, req)
        }

        fun schedulePeriodic(context: Context) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS).setConstraints(network).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("notion-sync-periodic", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
