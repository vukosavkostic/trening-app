package com.vukos.trening.ui.journal

import com.vukos.trening.i18n.tr
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background

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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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

    var showHelp by remember { mutableStateOf(false) }
    val problems = !notifOk || !exactOk || batteryRestricted

    val D = com.vukos.trening.ui.theme.Ds
    val p = adding || editing != null
    if (p) {
        ReminderEditScreen(
            initial = editing,
            onCancel = { adding = false; editing = null },
            onSave = { r ->
                save(list.filter { it.id != r.id } + r); adding = false; editing = null
                if (!notifOk) fixNotifications()
            },
            onDelete = editing?.let { e -> { save(list.filter { it.id != e.id }); editing = null } },
        )
        androidx.activity.compose.BackHandler { adding = false; editing = null }
    } else LazyColumn(
        Modifier.fillMaxSize().background(D.bg).statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth()) {
                CircleBtn(Icons.Filled.ChevronLeft, tr("Nazad", "Back"), onBack)
                Spacer(Modifier.weight(1f))
                Box {
                    CircleBtn(Icons.AutoMirrored.Filled.HelpOutline, tr("Kako rade podsetnici", "How reminders work")) { showHelp = true }
                    if (problems) Box(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp).size(8.dp).clip(CircleShape)
                        .background(com.vukos.trening.ui.theme.Accent))
                }
            }
        }
        item { Text(tr("Podsetnici", "Reminders"), fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.68).sp, color = D.ink) }
        if (list.isEmpty()) item {
            EmptyState(Icons.Filled.NotificationsActive, tr("Bez podsetnika", "No reminders"),
                tr("Dodaj podsetnik i svaki dan u zadato vreme stiže notifikacija.", "Add a reminder and you get a notification every day at that time."))
        } else item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(D.s1)) {
                list.forEachIndexed { i, r ->
                    val taken = r.enabled && ExerciseSearch.normalize(r.name) in takenNames
                    Row(
                        Modifier.fillMaxWidth().alpha(if (r.enabled) 1f else 0.45f).padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).clickable { editing = r }) {
                            Text("%02d:%02d".format(r.hour, r.minute), fontSize = 34.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.68).sp,
                                lineHeight = 38.sp, color = D.ink)
                            Text(listOf(r.name, r.dose.takeIf { it.isNotBlank() }, tr("Svaki dan", "Every day")).filterNotNull().joinToString(" · "),
                                Modifier.padding(top = 2.dp), fontSize = 14.sp, color = D.sub)
                            if (taken) Text(tr("Danas uneto. Sledeći put sutra.", "Logged today. Next reminder tomorrow."),
                                Modifier.padding(top = 2.dp), fontSize = 12.sp, color = D.green)
                        }
                        Box(Modifier.clip(RoundedCornerShape(16.dp)).clickable {
                            save(list.map { if (it.id == r.id) it.copy(enabled = !r.enabled) else it })
                        }) { com.vukos.trening.ui.workout.IosSwitch(r.enabled) }
                    }
                    if (i < list.size - 1) androidx.compose.material3.HorizontalDivider(thickness = 1.dp, color = D.line)
                }
            }
        }
        item {
            Box(
                Modifier.padding(top = 4.dp).fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(com.vukos.trening.ui.theme.Accent)
                    .clickable { adding = true },
                contentAlignment = Alignment.Center,
            ) { Text(tr("Dodaj podsetnik", "Add reminder"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color.White) }
        }
    }

    if (showHelp) androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = { showHelp = false },
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = D.sheet,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 34.dp)) {
            Text(tr("Kako rade podsetnici", "How reminders work"), fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.48).sp, color = D.ink)
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf(
                    tr("Svaki dan **u zadato vreme** stiže notifikacija.", "You get a notification **every day at the set time**."),
                    tr("Dugme **Uzeo sam** u notifikaciji odmah upisuje suplement u Dnevnik. Ako si ga taj dan već uneo, **podsetnik se preskače**.",
                        "The **Taken** button in the notification logs the supplement in your Journal right away. If you already logged it that day, **the reminder is skipped**."),
                    tr("**Prekidačem** privremeno isključuješ podsetnik. **Dodirni podsetnik** da promeniš vreme ili da ga obrišeš.",
                        "Use the **switch** to pause a reminder. **Tap a reminder** to change the time or delete it."),
                ).forEach { Text(boldText(it, D.ink), fontSize = 16.sp, lineHeight = 24.sp, color = D.sub) }
            }
            Text(tr("Notifikacije", "Notifications"), Modifier.padding(start = 4.dp, top = 26.dp, bottom = 8.dp), fontSize = 13.sp, color = D.sub)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(D.s1)) {
                HelpRow(
                    tr("Provera telefona", "Phone check"),
                    if (problems) tr("Telefon blokira ili odlaže notifikacije ove aplikacije.", "Your phone is blocking or delaying this app's notifications.")
                    else tr("Telefon je podešen. Podsetnici stižu na vreme.", "Your phone is set up. Reminders arrive on time."),
                ) { Box(Modifier.size(8.dp).clip(CircleShape).background(if (problems) com.vukos.trening.ui.theme.Accent else D.green)) }
                if (!notifOk) HelpRow(tr("Notifikacije su isključene", "Notifications are off"), tr("Bez njih podsetnici ne stižu", "Reminders cannot arrive without them")) {
                    ActionText(tr("Uključi", "Turn on")) { fixNotifications() }
                }
                if (!exactOk && Build.VERSION.SDK_INT >= 31) HelpRow(tr("Tačni alarmi nisu dozvoljeni", "Exact alarms not allowed"), tr("Podsetnik može da kasni nekoliko minuta", "A reminder can be a few minutes late")) {
                    ActionText(tr("Dozvoli", "Allow")) { ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + ctx.packageName))) }
                }
                if (batteryRestricted) HelpRow(tr("Podsetnici mogu da kasne", "Reminders may arrive late"), tr("Štednja baterije može da ih odloži", "Battery saver can delay them")) {
                    ActionText(tr("Isključi", "Turn off")) {
                        runCatching {
                            ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + ctx.packageName)))
                        }.onFailure { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clickable {
                        val r = list.firstOrNull() ?: SuppReminder(id = "test", name = tr("Probni podsetnik", "Test reminder"), hour = 0, minute = 0)
                        if (!SupplementReminders.notify(ctx, r)) fixNotifications()
                    }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(tr("Pošalji probnu notifikaciju", "Send a test notification"), Modifier.weight(1f), fontSize = 16.sp, color = D.ink)
                    Icon(Icons.Filled.ChevronRight, null, Modifier.size(16.dp), tint = D.faint)
                }
            }
        }
    }
}

/** "**reč**" -> podebljano, u boji [ink]. */
private fun boldText(s: String, ink: androidx.compose.ui.graphics.Color) = buildAnnotatedString {
    s.split("**").forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(color = ink, fontWeight = FontWeight.SemiBold)) { append(part) } else append(part)
    }
}

@Composable
private fun CircleBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    val D = com.vukos.trening.ui.theme.Ds
    Box(Modifier.size(40.dp).clip(CircleShape).background(D.s1).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, Modifier.size(20.dp), tint = D.ink)
    }
}

@Composable
private fun HelpRow(title: String, sub: String, trailing: @Composable () -> Unit) {
    val D = com.vukos.trening.ui.theme.Ds
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = D.ink)
            Text(sub, Modifier.padding(top = 2.dp), fontSize = 13.sp, lineHeight = 18.sp, color = D.sub)
        }
        trailing()
    }
    androidx.compose.material3.HorizontalDivider(thickness = 1.dp, color = D.line)
}

@Composable
private fun ActionText(text: String, onClick: () -> Unit) {
    Text(text, Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(4.dp), fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold, color = com.vukos.trening.ui.theme.Ds.aText)
}

/** Novi / izmena podsetnika preko celog ekrana: vreme strelicama, pretraga suplementa, doza. */
@Composable
private fun ReminderEditScreen(initial: SuppReminder?, onCancel: () -> Unit, onSave: (SuppReminder) -> Unit, onDelete: (() -> Unit)?) {
    val D = com.vukos.trening.ui.theme.Ds
    val history by remember { TreningApp.instance.db.food().observeRecent() }.collectAsStateWithLifecycle(emptyList())
    var picked by remember { mutableStateOf(initial?.let { SupplementDef(it.name, it.dose, "", it.kcal, it.proteinG) }) }
    var query by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf(initial?.dose ?: "") }
    var hour by remember { mutableStateOf(initial?.hour ?: 9) }
    var minute by remember { mutableStateOf(initial?.minute ?: 0) }
    val results = remember(query, history) { Supplements.search(query, history) }
    val shown = remember(results, picked, query) {
        val base = if (query.isBlank()) results.take(5) else results.take(12)
        val p = picked
        if (p != null && base.none { ExerciseSearch.normalize(it.name) == ExerciseSearch.normalize(p.name) }) listOf(p) + base else base
    }
    val canSave = picked != null

    LazyColumn(
        Modifier.fillMaxSize().background(D.bg).statusBarsPadding().imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Otkaži", "Cancel"), Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onCancel).padding(4.dp), fontSize = 17.sp, color = D.sub)
                Text(if (initial == null) tr("Novi podsetnik", "New reminder") else tr("Izmena podsetnika", "Edit reminder"),
                    Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = D.ink, textAlign = TextAlign.Center)
                Text(tr("Sačuvaj", "Save"), Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = canSave) {
                    val d = picked!!
                    onSave((initial ?: SuppReminder(name = d.name, hour = hour, minute = minute)).copy(
                        name = d.name, dose = dose.trim(), hour = hour, minute = minute, enabled = true, kcal = d.kcal, proteinG = d.proteinG,
                    ))
                }.padding(4.dp), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (canSave) D.aText else D.faint)
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(D.s1).padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TimeColumn("%02d".format(hour), { hour = (hour + 1) % 24 }, { hour = (hour + 23) % 24 })
                    Text(":", Modifier.padding(bottom = 8.dp), fontSize = 64.sp, fontWeight = FontWeight.Light, color = D.faint)
                    TimeColumn("%02d".format(minute), { minute = (minute / 5 * 5 + 5) % 60 }, { minute = ((minute + 4) / 5 * 5 + 55) % 60 })
                }
                Text(tr("Svaki dan", "Every day"), Modifier.padding(top = 6.dp), fontSize = 14.sp, color = D.sub)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Suplement", "Supplement"), Modifier.padding(start = 4.dp, top = 6.dp), fontSize = 13.sp, color = D.sub)
                DsField(query, { query = it.take(40) }, tr("Pretraži suplemente", "Search supplements"), Icons.Filled.Search)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(D.s1)) {
                    shown.forEachIndexed { i, d ->
                        val sel = picked?.let { ExerciseSearch.normalize(it.name) == ExerciseSearch.normalize(d.name) } == true
                        OptionRow(d.name,
                            listOfNotNull(if (d.category == "Skoro korišćeno") tr("Skoro korišćeno", "Recent") else null, d.dose.takeIf { it.isNotBlank() }).joinToString(" · "),
                            sel, divider = i < shown.size - 1 || query.isNotBlank()) { picked = d; dose = d.dose; query = "" }
                    }
                    val q = query.trim()
                    if (q.isNotEmpty() && shown.none { ExerciseSearch.normalize(it.name) == ExerciseSearch.normalize(q) }) {
                        OptionRow(tr("Dodaj „$q“", "Add “$q”"), tr("Novi suplement", "New supplement"), false, divider = false) {
                            picked = SupplementDef(q, "", ""); query = ""
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Doza", "Dose"), Modifier.padding(start = 4.dp, top = 6.dp), fontSize = 13.sp, color = D.sub)
                DsField(dose, { dose = it.take(30) }, tr("npr. 5 g", "e.g. 5 g"), null)
            }
        }
        if (onDelete != null) item {
            Text(tr("Obriši podsetnik", "Delete reminder"), Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onDelete).padding(8.dp),
                fontSize = 15.sp, color = D.danger, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TimeColumn(value: String, onUp: () -> Unit, onDown: () -> Unit) {
    val D = com.vukos.trening.ui.theme.Ds
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp, 36.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onUp), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.KeyboardArrowUp, tr("Više", "Up"), Modifier.size(24.dp), tint = D.sub)
        }
        Text(value, fontSize = 72.sp, fontWeight = FontWeight.Light, letterSpacing = (-2.88).sp, lineHeight = 74.sp, color = D.ink)
        Box(Modifier.size(56.dp, 36.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onDown), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.KeyboardArrowDown, tr("Manje", "Down"), Modifier.size(24.dp), tint = D.sub)
        }
    }
}

@Composable
private fun OptionRow(name: String, sub: String, selected: Boolean, divider: Boolean, onClick: () -> Unit) {
    val D = com.vukos.trening.ui.theme.Ds
    Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 16.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = D.ink)
            if (sub.isNotEmpty()) Text(sub, fontSize = 13.sp, color = D.sub)
        }
        if (selected) Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = com.vukos.trening.ui.theme.Accent)
    }
    if (divider) androidx.compose.material3.HorizontalDivider(thickness = 1.dp, color = D.line)
}

/** Polje za unos u stilu dizajna (siva kartica, bez okvira). */
@Composable
internal fun DsField(value: String, onChange: (String) -> Unit, placeholder: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, height: androidx.compose.ui.unit.Dp = 48.dp, radius: androidx.compose.ui.unit.Dp = 16.dp) {
    val D = com.vukos.trening.ui.theme.Ds
    val style = androidx.compose.ui.text.TextStyle(fontFamily = com.vukos.trening.ui.theme.Manrope, fontSize = 16.sp, color = D.ink)
    Row(
        Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(radius)).background(D.s1).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        icon?.let { Icon(it, null, Modifier.size(16.dp), tint = D.sub) }
        androidx.compose.foundation.text.BasicTextField(
            value, onChange, Modifier.weight(1f), singleLine = true, textStyle = style,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(com.vukos.trening.ui.theme.Accent),
            decorationBox = { inner -> Box { if (value.isEmpty()) Text(placeholder, style = style.copy(color = D.faint)); inner() } },
        )
    }
}
