package com.vukos.trening.ui.journal

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

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Podsetnici za suplemente", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Nazad") } },
        )
    }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (list.isEmpty()) item {
                EmptyState(Icons.Filled.NotificationsActive, "Bez podsetnika",
                    "Dodaj podsetnik i svaki dan u zadato vreme stiže notifikacija. Dugme „Uzeo sam“ odmah upisuje suplement u Dnevnik. " +
                        "Ako si ga taj dan već uneo, podsetnik se preskače.")
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
                            Text("svaki dan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = r.enabled, onCheckedChange = { on -> save(list.map { if (it.id == r.id) it.copy(enabled = on) else it }) })
                    }
                }
            }
            item { PrimaryPill("Dodaj podsetnik", { adding = true }, Modifier.fillMaxWidth().padding(top = 8.dp), Icons.Filled.Add) }
        }
    }

    if (adding || editing != null) ReminderDialog(
        initial = editing,
        onDismiss = { adding = false; editing = null },
        onSave = { r -> save(list.filter { it.id != r.id } + r); adding = false; editing = null },
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
        title = { Text(picked?.name ?: "Koji suplement?") },
        text = {
            val p = picked
            if (p == null) SupplementPicker(query, { query = it }, history) { d -> picked = d; dose = d.dose }
            else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(time)
                OutlinedTextField(dose, { dose = it.take(30) }, label = { Text("Doza") }, singleLine = true,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                if (initial == null) TextButton(onClick = { picked = null }) { Text("← Izaberi drugi suplement") }
            }
        },
        confirmButton = {
            if (picked != null) TextButton(onClick = {
                val d = picked!!
                onSave((initial ?: SuppReminder(name = d.name, hour = 9, minute = 0)).copy(
                    name = d.name, dose = dose.trim(), hour = time.hour, minute = time.minute, enabled = true,
                    kcal = d.kcal, proteinG = d.proteinG,
                ))
            }) { Text("Sačuvaj") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, null); Text(" Obriši", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("Otkaži") }
            }
        },
    )
}
