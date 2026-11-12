package com.vukos.trening.reminders

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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Podsetnici za merenje:
 *  - nedelja 08:00 i 20:00, ako merenje (kilaza / slike) nije uneto
 *  - ako je nedelja propustena: svako jutro u 08:00 dok se ne unese
 * Alarm je "neprecizan" (moze kasniti par minuta), pa ne trazi posebnu dozvolu za tacne alarme.
 * Prijemnik pri svakom okidanju proverava bazu i zakazuje sledece okidanje.
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

    /** Da li u ovom trenutku treba poslati notifikaciju. */
    fun shouldNotify(due: CheckInSchedule.Due?, now: LocalDateTime): Boolean {
        if (due == null || !due.anythingMissing) return false
        val isCheckInSunday = now.toLocalDate() == due.checkInDay
        return isCheckInSunday || (due.overdue && now.toLocalTime() < LocalTime.NOON)
    }

    fun schedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val at = nextTrigger(LocalDateTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context))
    }

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 1, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun createChannel(context: Context) {
        val ch = NotificationChannel(CHANNEL, "Merenje (kilaža i slike)", NotificationManager.IMPORTANCE_HIGH)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun notify(context: Context, due: CheckInSchedule.Due) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val what = when {
            due.weightMissing && due.photosMissing -> "kilaža + 3 slike"
            due.photosMissing -> "3 slike (spreda, bok, leđa)"
            else -> "kilaža"
        }
        val title = if (due.overdue) "Propušteno merenje" else "Danas je merenje"
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
            .setContentText("Unesi: $what")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(1, n)
    }

    fun cancelNotification(context: Context) = NotificationManagerCompat.from(context).cancel(1)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as TreningApp
                val due = CheckInSchedule.dueFromDb(app.db.body(), LocalDate.now())
                if (Reminders.shouldNotify(due, LocalDateTime.now())) Reminders.notify(context, due!!)
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
    }
}
