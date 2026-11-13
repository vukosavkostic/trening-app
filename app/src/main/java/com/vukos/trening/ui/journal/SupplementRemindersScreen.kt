package com.vukos.trening.ui.journal

import com.vukos.trening.i18n.tr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.height
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.vukos.trening.data.ExerciseSearch
import com.vukos.trening.data.Supplements
import com.vukos.trening.ui.common.OutlinePill
import com.vukos.trening.ui.theme.Success
import java.time.LocalDate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.data.SupplementDef
import com.vukos.trening.reminders.SuppReminder
import com.vukos.trening.reminders.SupplementReminders
import com.vukos.trening.ui.common.AppCard
import com.vukos.trening.ui.common.EmptyState
import com.vukos.trening.ui.common.PrimaryPill

/** Podsetnici za suplemente: lista (vreme, naziv, ukljuceno) + dodavanje (suplement, doza, vreme). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementRemindersScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var list by remember { mutableStateOf(SupplementReminders.load(ctx)) }
    var editing by remember { mutableStateOf<SuppReminder?>(null) }
    var adding by remember { mutableStateOf(false) }
    fun save(l: List<SuppReminder>) { list = l.sortedBy { it.time }; SupplementReminders.save(ctx, list) }

    // Stanje dozvola se ponovo proverava svaki put kad se korisnik vrati iz podesavanja telefona.
    var notifOk by remember { mutableStateOf(true) }
    var exactOk by remember { mutableStateOf(true) }
    var batteryRestricted by remember { mutableStateOf(false) }
    fun refresh() {
        notifOk = SupplementReminders.notificationsAllowed(ctx)
        exactOk = SupplementReminders.exactAllowed(ctx)
        batteryRestricted = SupplementReminders.batteryRestricted(ctx)
        SupplementReminders.scheduleAll(ctx)
    }
    LifecycleResumeEffect(Unit) { refresh(); onPauseOrDispose { } }
    fun openNotifSettings() = ctx.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
    )
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refresh(); if (!granted) openNotifSettings()
    }
    fun fixNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else openNotifSettings()
    }
    val today = LocalDate.now().toEpochDay()
    val recent by remember { TreningApp.instance.db.food().observeRecent() }.collectAsStateWithLifecycle(emptyList())
    val takenNames = remember(recent) {
        recent.filter { it.epochDay == today && it.meal == Supplements.MEAL }
            .map { ExerciseSearch.normalize(Supplements.nameOf(it.text)) }.toSet()
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(tr("Podsetnici za suplemente", "Supplement reminders"), style = MaterialTheme.typography.titleLarge) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Nazad", "Back")) } },
        )
    }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (list.isNotEmpty() && (!notifOk || !exactOk || batteryRestricted)) item {
                AppCard(padding = PaddingValues(18.dp)) {
                    Text(tr("Podsetnici možda neće stići", "Reminders may not arrive"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(tr("Telefon trenutno blokira ili odlaže notifikacije ove aplikacije.", "Your phone is currently blocking or delaying this app's notifications."),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!notifOk) FixRow(tr("Notifikacije su isključene", "Notifications are off"), tr("Uključi", "Turn on")) { fixNotifications() }
                    if (!exactOk && Build.VERSION.SDK_INT >= 31) FixRow(tr("Tačni alarmi nisu dozvoljeni", "Exact alarms not allowed"), tr("Dozvoli", "Allow")) {
                        ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + ctx.packageName)))
                    }
                    if (batteryRestricted) FixRow(tr("Štednja baterije može da ih odloži", "Battery saver can delay them"), tr("Isključi", "Turn off")) {
                        runCatching {
                            ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + ctx.packageName)))
                        }.onFailure { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                    }
                }
            }
            if (list.isEmpty()) item {
                EmptyState(Icons.Filled.NotificationsActive, tr("Bez podsetnika", "No reminders"),
                    tr("Dodaj podsetnik i svaki dan u zadato vreme stiže notifikacija. Dugme „Uzeo sam“ odmah upisuje suplement u Dnevnik. ", "Add a reminder and you get a notification every day at that time. The “Taken” button logs the supplement in your Journal right away. ") +
                        tr("Ako si ga taj dan već uneo, podsetnik se preskače.", "If you already logged it that day, the reminder is skipped."))
            }
            items(list, key = { it.id }) { r ->
                AppCard(onClick = { editing = r }, padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("%02d:%02d".format(r.hour, r.minute), style = MaterialTheme.typography.headlineMedium,
                            color = if (r.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.name, style = MaterialTheme.typography.titleMedium)
                            if (r.dose.isNotBlank()) Text(r.dose, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val taken = ExerciseSearch.normalize(r.name) in takenNames
                            Text(
                                if (taken) tr("danas uneto ✓ · sledeći put sutra", "logged today ✓ · next time tomorrow") else tr("svaki dan", "every day"),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (taken) Success else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = r.enabled, onCheckedChange = { on -> save(list.map { if (it.id == r.id) it.copy(enabled = on) else it }) })
                    }
                }
            }
            item { PrimaryPill(tr("Dodaj podsetnik", "Add reminder"), { adding = true }, Modifier.fillMaxWidth().padding(top = 8.dp), Icons.Filled.Add) }
            if (list.isNotEmpty()) item {
                OutlinePill(tr("Pošalji probnu notifikaciju", "Send a test notification"), {
                    if (!SupplementReminders.notify(ctx, list.first())) fixNotifications()
                }, Modifier.fillMaxWidth(), Icons.Filled.NotificationsActive)
            }
        }
    }

    if (adding || editing != null) ReminderDialog(
        initial = editing,
        onDismiss = { adding = false; editing = null },
        onSave = { r ->
            save(list.filter { it.id != r.id } + r); adding = false; editing = null
            if (!notifOk) fixNotifications()
        },
        onDelete = editing?.let { e -> { save(list.filter { it.id != e.id }); editing = null } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderDialog(initial: SuppReminder?, onDismiss: () -> Unit, onSave: (SuppReminder) -> Unit, onDelete: (() -> Unit)?) {
    val history by remember { TreningApp.instance.db.food().observeRecent() }.collectAsStateWithLifecycle(emptyList())
    var picked by remember { mutableStateOf(initial?.let { SupplementDef(it.name, it.dose, "", it.kcal, it.proteinG) }) }
    var query by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf(initial?.dose ?: "") }
    val time = rememberTimePickerState(initial?.hour ?: 9, initial?.minute ?: 0, is24Hour = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(picked?.name ?: tr("Koji suplement?", "Which supplement?")) },
        text = {
            val p = picked
            if (p == null) SupplementPicker(query, { query = it }, history) { d -> picked = d; dose = d.dose }
            else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(time)
                OutlinedTextField(dose, { dose = it.take(30) }, label = { Text(tr("Doza", "Dose")) }, singleLine = true,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                if (initial == null) TextButton(onClick = { picked = null }) { Text(tr("← Izaberi drugi suplement", "← Choose another supplement")) }
            }
        },
        confirmButton = {
            if (picked != null) TextButton(onClick = {
                val d = picked!!
                onSave((initial ?: SuppReminder(name = d.name, hour = 9, minute = 0)).copy(
                    name = d.name, dose = dose.trim(), hour = time.hour, minute = time.minute, enabled = true,
                    kcal = d.kcal, proteinG = d.proteinG,
                ))
            }) { Text(tr("Sačuvaj", "Save")) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, null); Text(tr(" Obriši", " Delete"), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text(tr("Otkaži", "Cancel")) }
            }
        },
    )
}

@Composable
private fun FixRow(text: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(action, color = MaterialTheme.colorScheme.primary) }
    }
}
