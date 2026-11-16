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
import com.vukos.trening.data.CheckInSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Podsetnici za merenje:
 *  - nedelja 08:00 i 20:00, ako merenje (kilaza / slike) nije uneto
 *  - ako je nedelja propustena: svako jutro u 08:00 dok se ne unese
 * Alarm je tacan (kao kod suplemenata). Neprecizan alarm je Android u mirovanju odlagao satima,
 * pa notifikacija nije stizala. Prijemnik pri svakom okidanju proverava bazu i zakazuje sledece.
 * Svaki termin ("slot") se javlja najvise jednom. Ako alarm zakasni (telefon ugasen, Doze),
 * notifikacija se ipak posalje cim se alarm okine.
 */
object Reminders {
    const val CHANNEL = "checkin"
    const val EXTRA_OPEN = "open"
    const val OPEN_CHECKIN = "checkin"
    private val MORNING: LocalTime = LocalTime.of(8, 0)
    private val EVENING: LocalTime = LocalTime.of(20, 0)

    /** Sledece vreme provere: svaki dan 08:00 i nedeljom jos 20:00. */
    fun nextTrigger(now: LocalDateTime): LocalDateTime {
        val today = now.toLocalDate()
        val candidates = buildList {
            add(today.atTime(MORNING))
            if (today.dayOfWeek == DayOfWeek.SUNDAY) add(today.atTime(EVENING))
            add(today.plusDays(1).atTime(MORNING))
        }
        return candidates.first { it.isAfter(now) }
    }

    /**
     * Termin za koji je do sada trebalo poslati notifikaciju (npr. "2026-11-15-E"), ili null.
     *  - dan merenja: jutro od 08:00, vece od 20:00
     *  - propusteno merenje: jedno jutro dnevno, od 08:00 (i kasnije, ako alarm zakasni)
     */
    fun slotFor(due: CheckInSchedule.Due?, now: LocalDateTime): String? {
        if (due == null || !due.anythingMissing) return null
        val today = now.toLocalDate()
        val t = now.toLocalTime()
        if (t < MORNING) return null
        return if (today == due.checkInDay && t >= EVENING) "$today-E" else "$today-M"
    }

    /** Da li u ovom trenutku treba poslati notifikaciju (slot jos nije javljen). */
    fun shouldNotify(due: CheckInSchedule.Due?, now: LocalDateTime, lastSlot: String? = null): Boolean {
        val slot = slotFor(due, now) ?: return false
        return slot != lastSlot
    }

    private const val PREFS = "checkin_reminders"
    private const val KEY_LAST = "last_slot"

    /** Proverava bazu i salje notifikaciju ako je termin dosao, a jos nije javljen. */
    suspend fun checkAndNotify(context: Context) {
        val app = context.applicationContext as TreningApp
        val now = LocalDateTime.now()
        val due = CheckInSchedule.dueFromDb(app.db.body(), now.toLocalDate())
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val slot = slotFor(due, now) ?: return
        if (slot == prefs.getString(KEY_LAST, null)) return
        if (notify(context, due!!)) prefs.edit().putString(KEY_LAST, slot).apply()
    }

    fun schedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val at = nextTrigger(LocalDateTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (SupplementReminders.exactAllowed(context)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context))
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context))
    }

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 1, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun createChannel(context: Context) {
        val ch = NotificationChannel(CHANNEL, tr("Merenje (kilaža i slike)", "Check-in (weight and photos)"), NotificationManager.IMPORTANCE_HIGH)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    /** Vraca false ako notifikacije nisu dozvoljene (termin se onda ne obelezava kao javljen). */
    fun notify(context: Context, due: CheckInSchedule.Due): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        val what = when {
            due.weightMissing && due.photosMissing -> tr("kilaža + 3 slike", "weight + 3 photos")
            due.photosMissing -> tr("3 slike (spreda, bok, leđa)", "3 photos (front, side, back)")
            else -> tr("kilaža", "weight")
        }
        val title = if (due.overdue) tr("Propušteno merenje", "Missed check-in") else tr("Danas je merenje", "Check-in today")
        val open = PendingIntent.getActivity(
            context, 2,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN, OPEN_CHECKIN)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(tr("Unesi: $what", "Log: $what"))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(1, n)
        return true
    }

    fun cancelNotification(context: Context) = NotificationManagerCompat.from(context).cancel(1)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Reminders.checkAndNotify(context)
                Reminders.schedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Posle restarta telefona ili update-a aplikacije alarm mora ponovo da se zakaze. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.schedule(context)
        SupplementReminders.scheduleAll(context)
        // Ako je jutarnji termin prosao dok je telefon bio ugasen, javi odmah.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { Reminders.checkAndNotify(context) } finally { pending.finish() }
        }
    }
}
