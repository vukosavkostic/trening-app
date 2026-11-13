package com.vukos.trening.reminders

import com.vukos.trening.i18n.tr

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vukos.trening.MainActivity
import com.vukos.trening.R
import com.vukos.trening.TreningApp
import com.vukos.trening.data.ExerciseSearch
import com.vukos.trening.data.FoodEntry
import com.vukos.trening.data.Supplements
import com.vukos.trening.data.newId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Podsetnik za suplement: svaki dan u zadato vreme. */
data class SuppReminder(
    val id: String = newId(),
    val name: String,
    val dose: String = "",
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val kcal: Int? = null,
    val proteinG: Int? = null,
) {
    val time: LocalTime get() = LocalTime.of(hour, minute)
    val label get() = Supplements.entryText(name, dose)

    fun toJson(): JSONObject = JSONObject().put("id", id).put("name", name).put("dose", dose).put("hour", hour)
        .put("minute", minute).put("enabled", enabled).put("kcal", kcal ?: JSONObject.NULL).put("protein", proteinG ?: JSONObject.NULL)

    companion object {
        fun fromJson(o: JSONObject) = SuppReminder(
            id = o.getString("id"), name = o.getString("name"), dose = o.optString("dose"),
            hour = o.getInt("hour"), minute = o.getInt("minute"), enabled = o.optBoolean("enabled", true),
            kcal = if (o.isNull("kcal")) null else o.optInt("kcal"),
            proteinG = if (o.isNull("protein")) null else o.optInt("protein"),
        )
    }
}

/**
 * Podsetnici za suplemente (lokalno, na telefonu). Notifikacija ima dugme "Uzeo sam" koje odmah upisuje
 * suplement u Dnevnik. Ako je suplement tog dana vec unet, podsetnik se preskace.
 */
object SupplementReminders {
    const val CHANNEL = "supplements"
    private const val PREFS = "supp_reminders"
    private const val ACTION_FIRE = "com.vukos.trening.SUPP_FIRE"
    private const val ACTION_TAKEN = "com.vukos.trening.SUPP_TAKEN"
    private const val EXTRA_ID = "id"

    fun load(context: Context): List<SuppReminder> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("list", "[]")!!
        return runCatching { JSONArray(raw).let { a -> (0 until a.length()).map { SuppReminder.fromJson(a.getJSONObject(it)) } } }
            .getOrDefault(emptyList()).sortedBy { it.time }
    }

    fun save(context: Context, list: List<SuppReminder>) {
        val old = load(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("list", JSONArray(list.map { it.toJson() }).toString()).apply()
        old.filter { o -> list.none { it.id == o.id } }.forEach { cancel(context, it.id) }
        scheduleAll(context)
    }

    /** Sledece okidanje (danas ako vreme jos nije proslo, inace sutra). */
    fun nextTrigger(r: SuppReminder, now: LocalDateTime): LocalDateTime {
        val today = now.toLocalDate().atTime(r.time)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    fun scheduleAll(context: Context) {
        load(context).forEach { if (it.enabled) schedule(context, it) else cancel(context, it.id) }
    }

    private fun schedule(context: Context, r: SuppReminder) {
        val am = context.getSystemService(AlarmManager::class.java)
        val at = nextTrigger(r, LocalDateTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Prozor od 5 min: ne trazi dozvolu za tacne alarme, a stize skoro na vreme.
        am.setWindow(AlarmManager.RTC_WAKEUP, at, 5 * 60_000L, firePending(context, r.id))
    }

    private fun cancel(context: Context, id: String) {
        context.getSystemService(AlarmManager::class.java).cancel(firePending(context, id))
    }

    private fun firePending(context: Context, id: String) = PendingIntent.getBroadcast(
        context, id.hashCode(),
        Intent(context, SupplementReminderReceiver::class.java).setAction(ACTION_FIRE).putExtra(EXTRA_ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun createChannel(context: Context) {
        val ch = NotificationChannel(CHANNEL, tr("Suplementi", "Supplements"), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun notifId(id: String) = 1000 + (id.hashCode() and 0xFFFF)

    fun notify(context: Context, r: SuppReminder) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            context, 3, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val taken = PendingIntent.getBroadcast(
            context, r.id.hashCode() + 1,
            Intent(context, SupplementReminderReceiver::class.java).setAction(ACTION_TAKEN).putExtra(EXTRA_ID, r.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(tr("Vreme je za: ${r.name}", "Time for: ${r.name}"))
            .setContentText(if (r.dose.isBlank()) tr("Dodirni „Uzeo sam“ da se upiše u Dnevnik.", "Tap “Taken” to log it in your Journal.") else tr("Doza: ${r.dose}", "Dose: ${r.dose}"))
            .setContentIntent(open)
            .addAction(0, tr("Uzeo sam ✓", "Taken ✓"), taken)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notifId(r.id), n)
    }

    /** Da li je suplement vec unet danas. */
    suspend fun takenToday(r: SuppReminder): Boolean =
        TreningApp.instance.db.food().getDay(LocalDate.now().toEpochDay()).any {
            it.meal == Supplements.MEAL && ExerciseSearch.normalize(Supplements.nameOf(it.text)) == ExerciseSearch.normalize(r.name)
        }

    suspend fun logTaken(context: Context, r: SuppReminder) {
        if (!takenToday(r)) TreningApp.instance.db.food().upsert(
            FoodEntry(epochDay = LocalDate.now().toEpochDay(), meal = Supplements.MEAL, text = r.label, kcal = r.kcal, proteinG = r.proteinG)
        )
        NotificationManagerCompat.from(context).cancel(notifId(r.id))
    }

    internal suspend fun handle(context: Context, action: String?, id: String?) {
        val r = load(context).firstOrNull { it.id == id } ?: return
        when (action) {
            ACTION_FIRE -> {
                if (r.enabled && !takenToday(r)) notify(context, r)
                if (r.enabled) schedule(context, r)
            }
            ACTION_TAKEN -> logTaken(context, r)
        }
    }
}

class SupplementReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { SupplementReminders.handle(context, intent.action, intent.getStringExtra("id")) } finally { pending.finish() }
        }
    }
}
