package com.vukos.trening.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.notion.NotionRoutines
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onHelp: () -> Unit = {}) {
    val settings = TreningApp.instance.settings
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var configured by remember { mutableStateOf(settings.notionConfigured) }
    var dbs by remember { mutableStateOf(settings.notionDatabases.keys.sorted()) }
    var token by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Podešavanja") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Nazad") } },
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).consumeWindowInsets(pad).imePadding().fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Notion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = onHelp) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, null, Modifier.padding(end = 6.dp)); Text("Uputstvo")
                }
            }
            if (configured) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF43A047))
                        Text("  Povezano sa stranicom Trening", fontWeight = FontWeight.SemiBold)
                    }
                    Text("Baze: ${dbs.joinToString(", ")}", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
                SyncSection()
                OutlinedButton(onClick = {
                    settings.notionToken = null
                    settings.notionDatabases = emptyMap()
                    configured = false; dbs = emptyList(); message = "Token je uklonjen sa telefona."
                }) { Text("Ukloni token") }
            } else {
                Text(
                    "Nalepi Internal Integration Secret (ntn_…) integracije \"Trening app\". " +
                        "Stranica Trening mora biti povezana sa integracijom (••• → Connections).",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = token, onValueChange = { token = it.trim() },
                    label = { Text("Notion token") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = token.startsWith("ntn_") || token.startsWith("secret_"),
                    onClick = {
                        busy = true; message = null
                        scope.launch {
                            runCatching { NotionRoutines.connect(settings, token) }
                                .onSuccess {
                                    configured = true; dbs = it.keys.sorted(); token = ""; message = null
                                    com.vukos.trening.notion.SyncWorker.request(ctx, 0)   // posalji sve sto je vec uneto
                                }
                                .onFailure { message = "Neuspešno: ${it.message}" }
                            busy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (busy) "Povezujem…" else "Sačuvaj i poveži") }
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            com.vukos.trening.ui.common.LockNote("Token se čuva šifrovan (Android Keystore) samo na ovom telefonu i ne nalazi se u APK-u.")
            Spacer(Modifier.height(12.dp))
            GoalsSection()
            Spacer(Modifier.height(12.dp))
            BackupSection()
        }
    }
}


/** Status sinhronizacije, rucno slanje i vracanje podataka iz Notiona. */
@Composable
private fun SyncSection() {
    val app = TreningApp.instance
    val settings = app.settings
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val pending by remember { app.db.sync().observePending() }.collectAsStateWithLifecycle(0)
    var lastAt by remember { mutableStateOf(settings.lastSyncAt) }
    var lastError by remember { mutableStateOf(settings.lastSyncError) }
    var syncing by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }
    var restoreMsg by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Sinhronizacija", fontWeight = FontWeight.SemiBold)
        Text(
            when {
                lastAt == 0L -> "Još nije sinhronizovano."
                lastError != null -> "Poslednji pokušaj: ${fmtTime(lastAt)} — greška: $lastError"
                else -> "Poslednja: ${fmtTime(lastAt)}"
            },
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (pending == 0) "Sve je poslato u Notion." else "Čeka slanje: $pending (šalje se automatski kad ima interneta).",
            style = MaterialTheme.typography.bodySmall,
        )
        Button(enabled = !syncing, onClick = {
            syncing = true
            val started = System.currentTimeMillis()
            com.vukos.trening.notion.SyncWorker.request(context, 0)
            scope.launch {
                // Ceka da radnik zapise rezultat (najvise 2 min).
                repeat(80) {
                    kotlinx.coroutines.delay(1500)
                    if (settings.lastSyncAt >= started) { lastAt = settings.lastSyncAt; lastError = settings.lastSyncError; syncing = false; return@launch }
                }
                syncing = false
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (syncing) "Šaljem…" else "Sinhronizuj sad") }
        OutlinedButton(enabled = !restoring, onClick = { confirmRestore = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (restoring) "Vraćam…" else "Vrati podatke iz Notiona")
        }
        restoreMsg?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Text(
            "Slike tela se nikad ne šalju u Notion, pa ih vraćanje ne može vratiti.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (confirmRestore) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirmRestore = false },
        title = { Text("Vrati iz Notiona?") },
        text = { Text("Dodaju se planovi, treninzi, merenja i obroci iz Notiona kojih nema na telefonu. Postojeći podaci se ne menjaju.") },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                confirmRestore = false; restoring = true; restoreMsg = null
                scope.launch {
                    runCatching { com.vukos.trening.notion.NotionSync.restore(settings) }
                        .onSuccess { r ->
                            restoreMsg = "Vraćeno: ${r.routines} planova, ${r.workouts} treninga (${r.sets} setova), " +
                                "${r.body} merenja, ${r.food} obroka, ${r.exercises} vežbi."
                        }
                        .onFailure { restoreMsg = "Neuspešno: ${it.message}" }
                    restoring = false
                }
            }) { Text("Vrati") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmRestore = false }) { Text("Otkaži") } },
    )
}

private fun fmtTime(ms: Long): String {
    val t = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault())
    return "%d.%d. %02d:%02d".format(t.dayOfMonth, t.monthValue, t.hour, t.minute)
}

/** Ciljevi: kalorije, protein, treninga nedeljno. */
@Composable
private fun GoalsSection() {
    val settings = TreningApp.instance.settings
    var kcal by remember { mutableStateOf(settings.kcalGoal.takeIf { it > 0 }?.toString() ?: "") }
    var prot by remember { mutableStateOf(settings.proteinGoal.takeIf { it > 0 }?.toString() ?: "") }
    var weekly by remember { mutableStateOf(settings.weeklyGoal.toString()) }
    Text("Ciljevi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoalField("kcal", kcal, Modifier.weight(1f)) { kcal = it; settings.kcalGoal = it.toIntOrNull() ?: 0 }
        GoalField("protein g", prot, Modifier.weight(1f)) { prot = it; settings.proteinGoal = it.toIntOrNull() ?: 0 }
        GoalField("tren./ned.", weekly, Modifier.weight(1f)) { weekly = it; it.toIntOrNull()?.takeIf { n -> n in 1..14 }?.let { n -> settings.weeklyGoal = n } }
    }
}

@Composable
private fun GoalField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, { onChange(it.filter(Char::isDigit).take(5)) }, label = { Text(label, maxLines = 1) }, singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
        shape = RoundedCornerShape(14.dp), modifier = modifier,
    )
}

/** Sifrovana rezervna kopija svih podataka (ukljucujuci slike) u fajl po izboru. */
@Composable
private fun BackupSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings = TreningApp.instance.settings
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf<String?>(null) }        // "export" / "import"
    var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var lastAt by remember { mutableStateOf(settings.lastBackupAt) }

    val create = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> if (uri != null) { pendingUri = uri; mode = "export" } }
    val open = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) { pendingUri = uri; mode = "import" } }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Rezervna kopija", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        Text(
            "Svi podaci i slike tela u jednom fajlu, šifrovanom tvojom lozinkom. Sačuvaj ga na Drive ili računar. " +
                "Bez lozinke se ne može otvoriti, pa je ne zaboravi.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (lastAt == 0L) "Još nije napravljena." else "Poslednja: ${fmtTime(lastAt)}",
            style = MaterialTheme.typography.bodySmall,
        )
        Button(enabled = !busy, onClick = {
            create.launch("Trening-backup-${java.time.LocalDate.now()}.trbk")
        }, modifier = Modifier.fillMaxWidth()) { Text(if (busy && mode == null) "Radim…" else "Napravi kopiju") }
        OutlinedButton(enabled = !busy, onClick = { open.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text("Vrati iz kopije")
        }
        msg?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }

    val uri = pendingUri
    if (mode != null && uri != null) PasswordDialog(
        export = mode == "export",
        onDismiss = { mode = null; pendingUri = null },
    ) { password ->
        val m = mode
        mode = null; pendingUri = null; busy = true; msg = if (m == "export") "Pravim kopiju…" else "Vraćam podatke…"
        scope.launch {
            runCatching {
                if (m == "export") com.vukos.trening.data.Backup.export(context, uri, password)
                else com.vukos.trening.data.Backup.import(context, uri, password)
            }.onSuccess {
                if (m == "export") { lastAt = settings.lastBackupAt; msg = "Kopija je sačuvana." }
                else { msg = "Podaci su vraćeni. Aplikacija se ponovo pokreće…"; com.vukos.trening.data.Backup.restartApp(context) }
            }.onFailure { msg = "Neuspešno: ${it.message}" }
            java.util.Arrays.fill(password, ' ')
            busy = false
        }
    }
}

@Composable
private fun PasswordDialog(export: Boolean, onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit) {
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    val ok = if (export) p1.length >= 8 && p1 == p2 else p1.isNotEmpty()
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (export) "Lozinka za kopiju" else "Vrati iz kopije?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (export) "Najmanje 8 znakova. Bez nje se kopija ne može otvoriti."
                    else "SVI podaci na ovom telefonu biće zamenjeni sadržajem kopije. Notion token ćeš morati ponovo da uneseš.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(p1, { p1 = it }, label = { Text("Lozinka") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                if (export) OutlinedTextField(p2, { p2 = it }, label = { Text("Ponovi lozinku") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(enabled = ok, onClick = { onConfirm(p1.toCharArray()) }) {
                Text(if (export) "Šifruj i sačuvaj" else "Zameni podatke")
            }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Otkaži") } },
    )
}
