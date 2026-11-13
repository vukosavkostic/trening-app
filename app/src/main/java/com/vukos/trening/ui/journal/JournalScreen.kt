package com.vukos.trening.ui.journal

import com.vukos.trening.i18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.vukos.trening.ui.common.AppCard
import com.vukos.trening.ui.common.IconBadge
import com.vukos.trening.ui.common.SectionTitle
import com.vukos.trening.ui.common.StatTile
import com.vukos.trening.ui.common.TabTopBar
import com.vukos.trening.ui.body.srLong
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.data.FoodEntry
import com.vukos.trening.data.FoodProduct
import com.vukos.trening.data.FoodQuick
import com.vukos.trening.data.Supplements
import com.vukos.trening.data.OpenFoodFacts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import com.vukos.trening.ui.common.OutlinePill
import com.vukos.trening.data.JournalStats
import com.vukos.trening.data.MEALS
import com.vukos.trening.data.WorkoutMath
import com.vukos.trening.ui.body.kg
import com.vukos.trening.ui.body.sr
import com.vukos.trening.ui.common.DayMarker
import com.vukos.trening.ui.common.MonthCalendar
import com.vukos.trening.ui.workout.WorkoutCard
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private val FoodGreen = Color(0xFF3DD68C)
private val WeightBlue = Color(0xFF5B9CFF)

/** Dnevnik: kalendar, mesec u brojkama i sve za izabrani dan (treninzi, kilaza, ishrana). */
@Composable
fun JournalScreen(onOpenWorkout: (String) -> Unit, onOpenBodyDay: (LocalDate) -> Unit, onOpenStats: () -> Unit, onOpenSuppReminders: () -> Unit) {
    val db = TreningApp.instance.db
    val scope = rememberCoroutineScope()
    val workouts by remember { db.workouts().observeFinished() }.collectAsStateWithLifecycle(emptyList())
    val doneSets by remember { db.workouts().observeAllDoneSets() }.collectAsStateWithLifecycle(emptyList())
    val foodDays by remember { db.food().observeDays() }.collectAsStateWithLifecycle(emptyList())
    val body by remember { db.body().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val exercises by remember { db.exercises().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val names = remember(exercises) { exercises.associate { it.id to it.name } }
    val setsByWorkout = remember(doneSets) { doneSets.groupBy { it.workoutId } }

    var selectedDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    val selected = LocalDate.ofEpochDay(selectedDay)
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val ym = YearMonth.parse(month)
    val food by remember(selectedDay) { db.food().observeDay(selectedDay) }.collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf<FoodEntry?>(null) }
    var adding by remember { mutableStateOf(false) }
    val settings = TreningApp.instance.settings
    var kcalGoal by remember { mutableIntStateOf(settings.kcalGoal) }
    var proteinGoal by remember { mutableIntStateOf(settings.proteinGoal) }
    var goalsDialog by remember { mutableStateOf(false) }
    val recentFood by remember { db.food().observeRecent() }.collectAsStateWithLifecycle(emptyList())
    // Koliko obroka ima prethodni dan (za "Kopiraj juce").
    val prevDay by produceState(emptyList<FoodEntry>(), selectedDay, food.size) { value = db.food().getDay(selectedDay - 1) }
    val meals = food.filter { it.meal != Supplements.MEAL }
    val supps = food.filter { it.meal == Supplements.MEAL }
    var suppEditing by remember { mutableStateOf<FoodEntry?>(null) }
    var suppAdding by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val reminderCount = remember { com.vukos.trening.reminders.SupplementReminders.load(ctx).count { it.enabled } }

    // Trening = pun krug; obrok ili kilaza = tackica.
    val markers = remember(workouts, foodDays, body) {
        val trained = workouts.map { JournalStats.dateOf(it) }.toSet()
        val other = foodDays.map { LocalDate.ofEpochDay(it) }.toSet() +
            body.filter { it.weightKg != null }.map { LocalDate.ofEpochDay(it.epochDay) }
        (trained + other).associateWith { d -> DayMarker(filled = d in trained, dot = d in other) }
    }
    val insights = remember(workouts, doneSets, ym) { JournalStats.month(workouts, doneSets, ym) }
    val dayWorkouts = workouts.filter { JournalStats.dateOf(it) == selected }
    val dayWeight = body.firstOrNull { it.epochDay == selectedDay && it.weightKg != null }

    Scaffold(topBar = {
        TabTopBar(tr("Dnevnik", "Journal")) {
            androidx.compose.material3.IconButton(onClick = onOpenStats) { Icon(Icons.Filled.BarChart, tr("Statistika", "Statistics")) }
        }
    }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MonthCalendar(
                    markers = markers, onDayClick = { selectedDay = it.toEpochDay() },
                    selected = selected, allClickable = true,
                    month = ym, onMonthChange = { month = it.toString() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("${insights.workouts}", tr("treninga", "workouts"), Modifier.weight(1f))
                    StatTile(JournalStats.fmtMinutes(insights.minutes), tr("u teretani", "in the gym"), Modifier.weight(1f))
                    StatTile("${WorkoutMath.fmtKg(insights.volumeKg / 1000)} t", tr("podignuto", "lifted"), Modifier.weight(1f))
                }
            }
            item {
                SectionTitle(if (selected == LocalDate.now()) tr("Danas", "Today") else selected.srLong().replaceFirstChar { it.uppercase() })
            }
            if (dayWorkouts.isEmpty()) item {
                InfoRow(Icons.Filled.FitnessCenter, MaterialTheme.colorScheme.onSurfaceVariant, tr("Trening", "Workout"),
                    if (selected.isAfter(LocalDate.now())) "—" else tr("Bez treninga", "No workout"), null)
            }
            items(dayWorkouts, key = { it.id }) { w ->
                WorkoutCard(w, setsByWorkout[w.id].orEmpty(), names) { onOpenWorkout(w.id) }
            }
            dayWeight?.let { e ->
                item { InfoRow(Icons.Filled.MonitorWeight, WeightBlue, tr("Kilaža", "Weight"), e.weightKg!!.kg()) { onOpenBodyDay(selected) } }
            }
            item {
                FoodCard(
                    meals, kcalGoal, proteinGoal,
                    copyCount = if (meals.isEmpty()) prevDay.count { it.meal != Supplements.MEAL } else 0,
                    totals = food,
                    onAdd = { adding = true }, onEdit = { editing = it }, onGoals = { goalsDialog = true },
                    onCopy = { scope.launch { FoodQuick.copyDay(selectedDay - 1, selectedDay) } },
                )
            }
            item {
                SupplementsCard(
                    supps, copyCount = if (supps.isEmpty()) prevDay.count { it.meal == Supplements.MEAL } else 0,
                    reminders = reminderCount,
                    onAdd = { suppAdding = true }, onEdit = { suppEditing = it }, onReminders = onOpenSuppReminders,
                    onCopy = { scope.launch { FoodQuick.copyDay(selectedDay - 1, selectedDay, supplements = true) } },
                )
            }
        }
    }

    if (suppAdding || suppEditing != null) SupplementDialog(
        initial = suppEditing, history = recentFood,
        onDismiss = { suppAdding = false; suppEditing = null },
        onSave = { text, k, p ->
            val base = suppEditing ?: FoodEntry(epochDay = selectedDay, meal = Supplements.MEAL, text = text)
            scope.launch {
                db.food().upsert(base.copy(text = text, kcal = k, proteinG = p, updatedAt = System.currentTimeMillis(), dirty = true))
            }
            suppAdding = false; suppEditing = null
        },
        onDelete = suppEditing?.let { e ->
            {
                scope.launch { db.food().upsert(e.copy(deleted = true, dirty = true, updatedAt = System.currentTimeMillis())) }
                suppEditing = null
            }
        },
    )

    if (goalsDialog) GoalsDialog(kcalGoal, proteinGoal, onDismiss = { goalsDialog = false }) { k, p ->
        settings.kcalGoal = k; settings.proteinGoal = p; kcalGoal = k; proteinGoal = p; goalsDialog = false
    }

    if (adding || editing != null) FoodDialog(
        initial = editing,
        recent = recentFood,
        onDismiss = { adding = false; editing = null },
        onSave = { meal, text, kcal, prot ->
            scope.launch {
                val base = editing ?: FoodEntry(epochDay = selectedDay, meal = meal, text = text)
                db.food().upsert(base.copy(meal = meal, text = text, kcal = kcal, proteinG = prot,
                    updatedAt = System.currentTimeMillis(), dirty = true))
            }
            adding = false; editing = null
        },
        onDelete = editing?.let { e ->
            {
                scope.launch { db.food().upsert(e.copy(deleted = true, dirty = true, updatedAt = System.currentTimeMillis())) }
                editing = null
            }
        },
    )
}

@Composable
private fun InfoRow(icon: ImageVector, tint: Color, label: String, value: String, onClick: (() -> Unit)?) {
    AppCard(onClick = onClick, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint, 40.dp)
            Spacer(Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleMedium,
                color = if (onClick != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            if (onClick != null) Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FoodCard(
    food: List<FoodEntry>, kcalGoal: Int, proteinGoal: Int, copyCount: Int, totals: List<FoodEntry>,
    onAdd: () -> Unit, onEdit: (FoodEntry) -> Unit, onGoals: () -> Unit, onCopy: () -> Unit,
) {
    // Zbir ukljucuje i suplemente (npr. protein u prahu).
    val kcal = totals.mapNotNull { it.kcal }.sum()
    val prot = totals.mapNotNull { it.proteinG }.sum()
    AppCard(padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Restaurant, FoodGreen, 40.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Ishrana", "Food"), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (food.isEmpty()) tr("Još ništa nije uneto", "Nothing logged yet") else tr("${food.size} ${if (food.size == 1) "unos" else "unosa"}", "${food.size} ${if (food.size == 1) "entry" else "entries"}"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Add, tr("Dodaj obrok", "Add meal"), tint = Color.White) }
        }
        Spacer(Modifier.height(14.dp))
        if (kcalGoal > 0 || proteinGoal > 0) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onGoals).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                if (kcalGoal > 0) GoalRing(kcal, kcalGoal, "kcal", MaterialTheme.colorScheme.primary)
                if (proteinGoal > 0) GoalRing(prot, proteinGoal, tr("g proteina", "g protein"), FoodGreen)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (kcal > 0 || prot > 0) Text(
                    listOfNotNull(kcal.takeIf { it > 0 }?.let { "$it kcal" }, prot.takeIf { it > 0 }?.let { tr("$it g proteina", "$it g protein") }).joinToString(", "),
                    style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f),
                ) else Spacer(Modifier.weight(1f))
                TextButton(onClick = onGoals) { Text(tr("Postavi dnevni cilj", "Set a daily goal")) }
            }
        }
        if (copyCount > 0) OutlinePill(tr("Kopiraj juče ($copyCount)", "Copy yesterday ($copyCount)"), onCopy, Modifier.fillMaxWidth().padding(top = 8.dp), Icons.Filled.ContentCopy)
        MEALS.forEach { meal ->
            val entries = food.filter { it.meal == meal }
            if (entries.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(com.vukos.trening.data.mealLabel(meal).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                entries.forEach { f ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onEdit(f) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(f.text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            listOfNotNull(f.kcal?.let { "$it kcal" }, f.proteinG?.let { "${it}g P" }).joinToString(", "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

/** Prsten napretka ka dnevnom cilju. */
@Composable
private fun GoalRing(value: Int, goal: Int, unit: String, color: Color) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val frac = (value.toFloat() / goal).coerceIn(0f, 1f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val stroke = androidx.compose.ui.graphics.drawscope.Stroke(10.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                val inset = 5.dp.toPx()
                val sz = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
                val tl = androidx.compose.ui.geometry.Offset(inset, inset)
                drawArc(track, 0f, 360f, false, tl, sz, style = stroke)
                if (frac > 0f) drawArc(color, -90f, 360f * frac, false, tl, sz, style = stroke)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$value", style = MaterialTheme.typography.titleLarge)
                Text("/ $goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (value >= goal) tr("Cilj ispunjen", "Goal reached") else tr("još ${goal - value} $unit", "${goal - value} $unit to go"),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GoalsDialog(kcal: Int, protein: Int, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var k by remember { mutableStateOf(kcal.takeIf { it > 0 }?.toString() ?: "") }
    var p by remember { mutableStateOf(protein.takeIf { it > 0 }?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Dnevni cilj", "Daily goal")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Ostavi prazno ako ne pratiš.", "Leave empty if you do not track it."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(k, { k = it.filter(Char::isDigit).take(5) }, label = { Text(tr("Kalorije (kcal)", "Calories (kcal)")) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(p, { p = it.filter(Char::isDigit).take(4) }, label = { Text(tr("Protein (g)", "Protein (g)")) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onSave(k.toIntOrNull() ?: 0, p.toIntOrNull() ?: 0) }) { Text(tr("Sačuvaj", "Save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Otkaži", "Cancel")) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoodDialog(
    initial: FoodEntry?,
    recent: List<FoodEntry>,
    onDismiss: () -> Unit,
    onSave: (String, String, Int?, Int?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val defaultMeal = remember {
        val h = java.time.LocalTime.now().hour
        when { h < 11 -> "Doručak"; h < 16 -> "Ručak"; h < 21 -> "Večera"; else -> "Užina" }
    }
    var meal by remember { mutableStateOf(initial?.meal ?: defaultMeal) }
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var kcal by remember { mutableStateOf(initial?.kcal?.toString() ?: "") }
    var prot by remember { mutableStateOf(initial?.proteinG?.toString() ?: "") }
    val suggestions = remember(recent, meal) { if (initial == null) FoodQuick.suggestions(recent, meal) else emptyList() }
    // Barkod: proizvod sa vrednostima na 100 g i kolicina u gramima.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var product by remember { mutableStateOf<FoodProduct?>(null) }
    var grams by remember { mutableStateOf("100") }
    var scanMsg by remember { mutableStateOf<String?>(null) }
    fun applyGrams(g: String) {
        grams = g
        val pr = product ?: return
        val f = (g.toDoubleOrNull() ?: 0.0) / 100.0
        kcal = pr.kcal100?.let { Math.round(it * f).toString() } ?: ""
        prot = pr.protein100?.let { Math.round(it * f).toString() } ?: ""
    }
    fun scan() {
        scanMsg = null
        val opts = com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder().setBarcodeFormats(
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13, com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_8,
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A, com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_E,
        ).build()
        com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(ctx, opts).startScan()
            .addOnSuccessListener { b ->
                val code = b.rawValue ?: return@addOnSuccessListener
                scanMsg = tr("Tražim proizvod…", "Looking up product…")
                scope.launch {
                    val pr = runCatching { OpenFoodFacts.lookup(code) }.getOrNull()
                    if (pr == null) scanMsg = tr("Proizvod $code nije pronađen u bazi Open Food Facts. Upiši ručno.", "Product $code was not found in Open Food Facts. Enter it manually.")
                    else { product = pr; text = pr.name; scanMsg = null; applyGrams(grams) }
                }
            }
            .addOnFailureListener { scanMsg = tr("Skener nije dostupan: ${it.message}", "Scanner unavailable: ${it.message}") }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) tr("Novi obrok", "New meal") else tr("Izmena obroka", "Edit meal")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MEALS.forEach { m -> FilterChip(selected = meal == m, onClick = { meal = m }, label = { Text(com.vukos.trening.data.mealLabel(m)) }) }
                }
                if (suggestions.isNotEmpty()) {
                    Text(tr("Često", "Frequent"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        suggestions.forEach { sg ->
                            androidx.compose.material3.SuggestionChip(
                                onClick = { text = sg.text; kcal = sg.kcal?.toString() ?: ""; prot = sg.proteinG?.toString() ?: ""; product = null },
                                label = { Text(sg.text, maxLines = 1) },
                            )
                        }
                    }
                }
                OutlinedTextField(text, { text = it }, label = { Text(tr("Šta si jeo/la", "What did you eat")) }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = ::scan) { Icon(Icons.Filled.QrCodeScanner, null); Text(tr("  Skeniraj barkod", "  Scan barcode")) }
                scanMsg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                product?.let { pr ->
                    Text(
                        tr("Na 100 g: ", "Per 100 g: ") + listOfNotNull(pr.kcal100?.let { "${Math.round(it)} kcal" }, pr.protein100?.let { tr("${WorkoutMath.fmtKg(it)} g proteina", "${WorkoutMath.fmtKg(it)} g protein") })
                            .joinToString(", ").ifEmpty { tr("nema podataka o vrednostima", "no nutrition data") },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(grams, { applyGrams(it.filter(Char::isDigit).take(4)) }, label = { Text(tr("Količina (g)", "Amount (g)")) },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(kcal, { kcal = it.filter(Char::isDigit).take(5) }, label = { Text(tr("kcal (opc.)", "kcal (opt.)")) },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(prot, { prot = it.filter(Char::isDigit).take(4) }, label = { Text("protein g") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onSave(meal, text.trim(), kcal.toIntOrNull(), prot.toIntOrNull()) }) { Text(tr("Sačuvaj", "Save")) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text(tr("Obriši", "Delete"), color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text(tr("Otkaži", "Cancel")) }
            }
        },
    )
}

