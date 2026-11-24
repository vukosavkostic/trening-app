package com.vukos.trening.ui.settings

import com.vukos.trening.i18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.vukos.trening.ui.body.srRelative
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
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
fun NotionSettingsScreen(onBack: () -> Unit, onHelp: () -> Unit) {
    val settings = TreningApp.instance.settings
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var configured by remember { mutableStateOf(settings.notionConfigured) }
    var dbs by remember { mutableStateOf(settings.notionDatabases.keys.sorted()) }
    var token by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    SubScreen("Notion", onBack) {
            if (configured) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer).padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = com.vukos.trening.ui.theme.Success)
                        Text(tr("  Povezano sa stranicom Trening", "  Connected to the Trening page"), fontWeight = FontWeight.SemiBold)
                    }
                    Text(tr("Baze: ${dbs.joinToString(", ")}", "Databases: ${dbs.joinToString(", ")}"), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
                SyncSection()
                OutlinedButton(onClick = {
                    settings.notionToken = null
                    settings.notionDatabases = emptyMap()
                    configured = false; dbs = emptyList(); message = tr("Token je uklonjen sa telefona.", "The token was removed from this phone.")
                }) { Text(tr("Ukloni token", "Remove token")) }
            } else {
                Text(
                    tr("Nalepi Internal Integration Secret (ntn_…) integracije \"Trening app\". ", "Paste the Internal Integration Secret (ntn_…) of the \"Trening app\" integration. ") +
                        tr("Stranica Trening mora biti povezana sa integracijom (••• → Connections).", "The Trening page must be connected to the integration (••• → Connections)."),
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
                                .onFailure { message = tr("Neuspešno: ${it.message}", "Failed: ${it.message}") }
                            busy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (busy) tr("Povezujem…", "Connecting…") else tr("Sačuvaj i poveži", "Save & connect")) }
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            com.vukos.trening.ui.common.LockNote(tr("Token se čuva šifrovan (Android Keystore) samo na ovom telefonu i ne nalazi se u APK-u.", "The token is stored encrypted (Android Keystore) on this phone only and is not part of the APK."))
            Spacer(Modifier.height(8.dp))
            com.vukos.trening.ui.common.SettingsGroup {
                com.vukos.trening.ui.common.SettingsRow(tr("Uputstvo korak po korak", "Step-by-step guide"), onClick = onHelp, divider = false)
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(tr("Sinhronizacija", "Sync"), fontWeight = FontWeight.SemiBold)
        Text(
            when {
                lastAt == 0L -> tr("Još nije sinhronizovano.", "Not synced yet.")
                lastError != null -> tr("Poslednji pokušaj: ${fmtTime(lastAt)}. Greška: $lastError", "Last attempt: ${fmtTime(lastAt)}. Error: $lastError")
                else -> tr("Poslednja: ${fmtTime(lastAt)}", "Last: ${fmtTime(lastAt)}")
            },
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (pending == 0) tr("Sve je poslato u Notion.", "Everything is in Notion.") else tr("Čeka slanje: $pending (šalje se automatski kad ima interneta).", "Waiting to send: $pending (sent automatically when online)."),
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
        }, modifier = Modifier.fillMaxWidth()) { Text(if (syncing) tr("Šaljem…", "Sending…") else tr("Sinhronizuj sad", "Sync now")) }
        OutlinedButton(enabled = !restoring, onClick = { confirmRestore = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (restoring) tr("Vraćam…", "Restoring…") else tr("Vrati podatke iz Notiona", "Restore from Notion"))
        }
        restoreMsg?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Text(
            tr("Slike tela se nikad ne šalju u Notion, pa ih vraćanje ne može vratiti.", "Body photos are never sent to Notion, so restoring cannot bring them back."),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (confirmRestore) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirmRestore = false },
        title = { Text(tr("Vrati iz Notiona?", "Restore from Notion?")) },
        text = { Text(tr("Dodaju se planovi, treninzi, merenja i obroci iz Notiona kojih nema na telefonu. Postojeći podaci se ne menjaju.", "Plans, workouts, check-ins and meals from Notion that are missing on this phone are added. Existing data is not changed.")) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                confirmRestore = false; restoring = true; restoreMsg = null
                scope.launch {
                    runCatching { com.vukos.trening.notion.NotionSync.restore(settings) }
                        .onSuccess { r ->
                            restoreMsg = tr("Vraćeno: ${r.routines} planova, ${r.workouts} treninga (${r.sets} setova), ", "Restored: ${r.routines} plans, ${r.workouts} workouts (${r.sets} sets), ") +
                                tr("${r.body} merenja, ${r.food} obroka, ${r.exercises} vežbi.", "${r.body} check-ins, ${r.food} meals, ${r.exercises} exercises.")
                        }
                        .onFailure { restoreMsg = tr("Neuspešno: ${it.message}", "Failed: ${it.message}") }
                    restoring = false
                }
            }) { Text(tr("Vrati", "Restore")) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmRestore = false }) { Text(tr("Otkaži", "Cancel")) } },
    )
}

private fun fmtTime(ms: Long): String {
    val t = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault())
    return "%d.%d. %02d:%02d".format(t.dayOfMonth, t.monthValue, t.hour, t.minute)
}

@Composable
fun GoalsScreen(onBack: () -> Unit) = SubScreen(tr("Ciljevi", "Goals"), onBack) {
    Text(tr("Dnevni cilj za ishranu i broj treninga nedeljno. Prazno = ne pratiš.", "Daily food goal and workouts per week. Empty = not tracked."),
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    GoalsSection()
}

@Composable
fun BackupScreen(onBack: () -> Unit) = SubScreen(tr("Rezervna kopija", "Backup"), onBack) { BackupSection() }

/** Ekran podesavanja drugog nivoa: naslov u sredini, strelica nazad, sadrzaj sa skrolom. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreen(title: String, onBack: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Scaffold(topBar = {
        // Naslov levo, kao na ostalim ekranima (Statistika, Grafikon kilaže...)
        androidx.compose.material3.TopAppBar(
            title = { Text(title, style = MaterialTheme.typography.titleLarge) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Nazad", "Back")) } },
            colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).consumeWindowInsets(pad).imePadding().fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Ciljevi: kalorije, protein, treninga nedeljno. */
@Composable
private fun GoalsSection() {
    val settings = TreningApp.instance.settings
    var kcal by remember { mutableStateOf(settings.kcalGoal.takeIf { it > 0 }?.toString() ?: "") }
    var prot by remember { mutableStateOf(settings.proteinGoal.takeIf { it > 0 }?.toString() ?: "") }
    var weekly by remember { mutableStateOf(settings.weeklyGoal.toString()) }
    GoalField(tr("Kalorije dnevno (kcal)", "Calories per day (kcal)"), kcal, Modifier.fillMaxWidth()) { kcal = it; settings.kcalGoal = it.toIntOrNull() ?: 0 }
    GoalField(tr("Protein dnevno (g)", "Protein per day (g)"), prot, Modifier.fillMaxWidth()) { prot = it; settings.proteinGoal = it.toIntOrNull() ?: 0 }
    GoalField(tr("Treninga nedeljno", "Workouts per week"), weekly, Modifier.fillMaxWidth()) { weekly = it; it.toIntOrNull()?.takeIf { n -> n in 1..14 }?.let { n -> settings.weeklyGoal = n } }
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            tr("Svi podaci i slike tela u jednom fajlu, šifrovanom tvojom lozinkom. Sačuvaj ga na Drive ili računar. ", "All your data and body photos in one file, encrypted with your password. Save it to Drive or your computer. ") +
                tr("Bez lozinke se ne može otvoriti, pa je ne zaboravi.", "It cannot be opened without the password, so do not forget it."),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (lastAt == 0L) tr("Još nije napravljena.", "Not created yet.") else tr("Poslednja: ${fmtTime(lastAt)}", "Last: ${fmtTime(lastAt)}"),
            style = MaterialTheme.typography.bodySmall,
        )
        Button(enabled = !busy, onClick = {
            create.launch("Trening-backup-${java.time.LocalDate.now()}.trbk")
        }, modifier = Modifier.fillMaxWidth()) { Text(if (busy && mode == null) tr("Radim…", "Working…") else tr("Napravi kopiju", "Create backup")) }
        OutlinedButton(enabled = !busy, onClick = { open.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Vrati iz kopije", "Restore from backup"))
        }
        msg?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }

    val uri = pendingUri
    if (mode != null && uri != null) PasswordDialog(
        export = mode == "export",
        onDismiss = { mode = null; pendingUri = null },
    ) { password ->
        val m = mode
        mode = null; pendingUri = null; busy = true; msg = if (m == "export") tr("Pravim kopiju…", "Creating backup…") else tr("Vraćam podatke…", "Restoring data…")
        scope.launch {
            runCatching {
                if (m == "export") com.vukos.trening.data.Backup.export(context, uri, password)
                else com.vukos.trening.data.Backup.import(context, uri, password)
            }.onSuccess {
                if (m == "export") { lastAt = settings.lastBackupAt; msg = tr("Kopija je sačuvana.", "Backup saved.") }
                else { msg = tr("Podaci su vraćeni. Aplikacija se ponovo pokreće…", "Data restored. Restarting the app…"); com.vukos.trening.data.Backup.restartApp(context) }
            }.onFailure { msg = tr("Neuspešno: ${it.message}", "Failed: ${it.message}") }
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
        title = { Text(if (export) tr("Lozinka za kopiju", "Backup password") else tr("Vrati iz kopije?", "Restore from backup?")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (export) tr("Najmanje 8 znakova. Bez nje se kopija ne može otvoriti.", "At least 8 characters. The backup cannot be opened without it.")
                    else tr("SVI podaci na ovom telefonu biće zamenjeni sadržajem kopije. Notion token ćeš morati ponovo da uneseš.", "ALL data on this phone will be replaced by the backup. You will need to enter the Notion token again."),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(p1, { p1 = it }, label = { Text(tr("Lozinka", "Password")) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                if (export) OutlinedTextField(p2, { p2 = it }, label = { Text(tr("Ponovi lozinku", "Repeat password")) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(enabled = ok, onClick = { onConfirm(p1.toCharArray()) }) {
                Text(if (export) tr("Šifruj i sačuvaj", "Encrypt & save") else tr("Zameni podatke", "Replace data"))
            }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(tr("Otkaži", "Cancel")) } },
    )
}


/** Glavna podesavanja: grupe redova kao u modernim aplikacijama; detalji su na podekranima. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNotion: () -> Unit,
    onBackup: () -> Unit,
    onGoals: () -> Unit,
    onReminders: () -> Unit,
    onHelp: () -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val settings = TreningApp.instance.settings
    var sheet by remember { mutableStateOf<String?>(null) }
    var restDefault by remember { mutableStateOf(settings.restDefaultSec) }
    val goals = listOfNotNull(
        settings.kcalGoal.takeIf { it > 0 }?.let { "$it kcal" },
        settings.proteinGoal.takeIf { it > 0 }?.let { "$it g" },
        tr("${settings.weeklyGoal}× ned.", "${settings.weeklyGoal}× week"),
    ).joinToString(" · ")
    val reminders = remember { com.vukos.trening.reminders.SupplementReminders.load(ctx).count { it.enabled } }
    val backupAt = settings.lastBackupAt
    val D = com.vukos.trening.ui.theme.Ds

    Column(
        Modifier.fillMaxSize().background(D.bg).statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(D.s1).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.ChevronLeft, tr("Nazad", "Back"), Modifier.size(20.dp), tint = D.ink)
        }
        Text(tr("Podešavanja", "Settings"), Modifier.padding(vertical = 8.dp), fontSize = 34.sp, fontWeight = FontWeight.Bold,
            letterSpacing = (-0.68).sp, color = D.ink)
        DsGroup(tr("Opšte", "General")) {
            DsRow(tr("Jezik", "Language"), com.vukos.trening.i18n.Lang.label) { sheet = "lang" }
            DsRow(tr("Izgled", "Appearance"), com.vukos.trening.ui.theme.Appearance.label()) { sheet = "theme" }
            DsRow(tr("Ciljevi", "Goals"), goals, onClick = onGoals)
            DsRow(tr("Podrazumevani odmor", "Default rest"), com.vukos.trening.ui.workout.RestTimer.mmss(restDefault)) { sheet = "rest" }
            DsRow(tr("Podsetnici", "Reminders"), if (reminders == 0) tr("Isključeno", "Off") else tr("aktivno: $reminders", "$reminders active"),
                divider = false, onClick = onReminders)
        }
        DsGroup(tr("Podaci", "Data")) {
            DsRow("Notion", if (settings.notionConfigured) tr("Povezano", "Connected") else tr("Nije povezano", "Not connected"), onClick = onNotion)
            DsRow(
                tr("Rezervna kopija", "Backup"),
                if (backupAt == 0L) tr("Nikad", "Never") else java.time.Instant.ofEpochMilli(backupAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate().srRelative(),
                divider = false, onClick = onBackup,
            )
        }
        DsGroup(tr("Pomoć", "Help")) {
            DsRow(tr("Uputstvo za Notion", "Notion guide"), "", divider = false, onClick = onHelp)
        }
        // Verzija aplikacije na dnu.
        Text(
            "Trening ${tr("verzija", "version")} ${com.vukos.trening.BuildConfig.VERSION_NAME}",
            Modifier.fillMaxWidth().padding(top = 24.dp), fontSize = 13.sp, color = D.faint,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }

    sheet?.let { which ->
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = D.sheet,
        ) {
            when (which) {
                "rest" -> com.vukos.trening.ui.workout.RestDefaultSheet { com.vukos.trening.ui.workout.RestTimer.setDefault(it); restDefault = it; sheet = null }
                "lang" -> PickSheet(tr("Jezik", "Language"), listOf("sr" to "Srpski", "en" to "English"), com.vukos.trening.i18n.Lang.code) {
                    com.vukos.trening.i18n.Lang.set(ctx, it); sheet = null
                }
                else -> {
                    val A = com.vukos.trening.ui.theme.Appearance
                    PickSheet(tr("Izgled", "Appearance"), listOf(A.SYSTEM, A.DARK, A.LIGHT).map { it to A.label(it) }, A.mode) {
                        A.set(ctx, it); sheet = null
                    }
                }
            }
        }
    }
}

/** Grupa redova sa malim naslovom iznad (dizajn v2). */
@Composable
private fun DsGroup(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val D = com.vukos.trening.ui.theme.Ds
    Text(title, Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp), fontSize = 13.sp, color = D.sub)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(D.s1), content = content)
}

@Composable
private fun DsRow(label: String, value: String, divider: Boolean = true, onClick: () -> Unit) {
    val D = com.vukos.trening.ui.theme.Ds
    Row(
        Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, Modifier.weight(1f), fontSize = 16.sp, color = D.ink, maxLines = 1)
        if (value.isNotEmpty()) Text(value, fontSize = 15.sp, color = D.sub, maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp))
        Icon(Icons.Filled.ChevronRight, null, Modifier.size(16.dp), tint = D.faint)
    }
    if (divider) androidx.compose.material3.HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 1.dp, color = D.line)
}

/** List sa izborom jedne vrednosti (jezik, izgled). */
@Composable
private fun PickSheet(title: String, options: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit) {
    val D = com.vukos.trening.ui.theme.Ds
    Column(Modifier.padding(bottom = 34.dp)) {
        Text(title, Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp), fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold, color = D.ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp)).background(D.s1)) {
            options.forEachIndexed { i, (key, name) ->
                Row(Modifier.fillMaxWidth().height(52.dp).clickable { onPick(key) }.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, Modifier.weight(1f), fontSize = 16.sp, color = D.ink)
                    if (key == selected) Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = com.vukos.trening.ui.theme.Accent)
                }
                if (i < options.size - 1) androidx.compose.material3.HorizontalDivider(thickness = 1.dp, color = D.line)
            }
        }
    }
}
