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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.vukos.trening.data.Workout
import com.vukos.trening.data.WorkoutSet
import com.vukos.trening.ui.common.FitText
import com.vukos.trening.ui.common.MONTHS
import com.vukos.trening.ui.theme.Accent
import com.vukos.trening.ui.theme.Ds
import com.vukos.trening.ui.theme.Manrope
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

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

    if (!(adding || editing != null)) Scaffold(containerColor = Ds.bg) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(LocalDate.now().srLong().replaceFirstChar { it.uppercase() }, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ds.sub)
                        Text(tr("Dnevnik", "Journal"), Modifier.padding(top = 2.dp), fontSize = 34.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.68).sp, color = Ds.ink)
                    }
                    RoundButton(Icons.Filled.BarChart, tr("Statistika", "Statistics"), Ds.s1, Ds.ink, 40.dp, onOpenStats)
                }
            }
            item {
                JournalCalendar(
                    markers = markers, selected = selected, month = ym,
                    onMonthChange = { month = it.toString() },
                    onDayClick = { selectedDay = it.toEpochDay() },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonthTile("${insights.workouts}", tr("treninga", "workouts"), Modifier.weight(1f))
                    MonthTile(JournalStats.fmtMinutes(insights.minutes), tr("u teretani", "in the gym"), Modifier.weight(1f))
                    MonthTile("${WorkoutMath.fmtKg(insights.volumeKg / 1000)} t", tr("podignuto", "lifted"), Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (selected == LocalDate.now()) tr("Danas", "Today") else selected.srLong().replaceFirstChar { it.uppercase() },
                        Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.44).sp, color = Ds.ink,
                    )
                    Text(MONTHS[ym.monthValue - 1], fontSize = 13.sp, color = Ds.sub)
                }
            }
            if (dayWorkouts.isEmpty()) item {
                InfoRow(tr("Trening", "Workout"), if (selected.isAfter(LocalDate.now())) "—" else tr("Bez treninga", "No workout"), null)
            }
            items(dayWorkouts, key = { it.id }) { w ->
                JournalWorkoutCard(w, setsByWorkout[w.id].orEmpty(), names) { onOpenWorkout(w.id) }
            }
            dayWeight?.let { e ->
                item { InfoRow(tr("Kilaža", "Weight"), e.weightKg!!.kg()) { onOpenBodyDay(selected) } }
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

    if (adding || editing != null) {
        androidx.activity.compose.BackHandler { adding = false; editing = null }
        FoodEditScreen(
            initial = editing,
            day = selected,
            recent = recentFood,
            dayKcal = food.mapNotNull { it.kcal }.sum() - (editing?.kcal ?: 0),
            dayProt = food.mapNotNull { it.proteinG }.sum() - (editing?.proteinG ?: 0),
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
}

/** Okruglo dugme sa ikonom (statistika, +). */
@Composable
internal fun RoundButton(icon: ImageVector, desc: String, bg: Color, tint: Color, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(18.dp)) }
}

/** Plocica sa brojem za mesec (treninzi, vreme, podignuto). */
@Composable
private fun MonthTile(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Ds.s1).padding(horizontal = 16.dp, vertical = 14.dp)) {
        FitText(value, TextStyle(fontFamily = Manrope, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.44).sp), Ds.ink)
        Text(label, Modifier.padding(top = 2.dp), fontSize = 13.sp, color = Ds.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun InfoRow(label: String, value: String, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Ds.s1)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, Modifier.weight(1f), fontSize = 16.sp, color = Ds.ink)
        Text(value, fontSize = 16.sp, fontWeight = if (onClick != null) FontWeight.SemiBold else FontWeight.Normal,
            color = if (onClick != null) Ds.ink else Ds.sub)
        if (onClick != null) Icon(Icons.Filled.ChevronRight, null, Modifier.size(16.dp), tint = Ds.faint)
    }
}

/** Trening izabranog dana: vreme, naziv, trajanje / setovi / tone i najbolji set po vezbi. */
@Composable
private fun JournalWorkoutCard(w: Workout, sets: List<WorkoutSet>, names: Map<String, String>, onClick: () -> Unit) {
    val time = java.time.Instant.ofEpochMilli(w.startedAt).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    val meta = listOf(
        WorkoutMath.duration(w.startedAt, w.finishedAt),
        tr("${sets.size} setova", "${sets.size} sets"),
        "${WorkoutMath.fmtKg(Math.round(WorkoutMath.volume(sets) / 100.0) / 10.0)} t",
    ).joinToString(" · ")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Ds.s1).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(tr("Trening", "Workout") + " · " + "%02d:%02d".format(time.hour, time.minute), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText)
                Text(w.name, Modifier.padding(top = 4.dp), fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.52).sp,
                    color = Ds.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(meta, Modifier.padding(top = 2.dp), fontSize = 14.sp, color = Ds.sub)
            }
            Icon(Icons.Filled.ChevronRight, null, Modifier.padding(top = 4.dp).size(18.dp), tint = Ds.faint)
        }
        val groups = sets.groupBy { it.exercisePosition }.toSortedMap().values
        if (groups.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(thickness = 1.dp, color = Ds.line)
        }
        groups.forEach { g ->
            val best = g.maxWithOrNull(compareBy<WorkoutSet>({ it.weightKg ?: 0.0 }, { it.reps ?: 0 }))
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(names[g.first().exerciseId] ?: "?", Modifier.weight(1f), fontSize = 15.sp, color = Ds.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                best?.let {
                    Text(
                        (WorkoutMath.fmtKg(it.weightKg).takeIf { k -> k.isNotEmpty() }?.let { k -> "$k kg × " } ?: "") + "${it.reps ?: 0}",
                        Modifier.padding(start = 12.dp), fontSize = 15.sp, color = Ds.sub,
                    )
                }
            }
            HorizontalDivider(thickness = 1.dp, color = Ds.line)
        }
    }
}

/** Naslov kartice (Ishrana, Suplementi) sa brojem unosa i okruglim dugmetom. */
@Composable
internal fun CardHeader(title: String, sub: String, trailing: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink)
            Text(sub, Modifier.padding(top = 1.dp), fontSize = 13.sp, color = Ds.sub)
        }
        trailing()
    }
}

/** Red unosa (obrok, suplement): tekst levo, vrednosti desno, tanka linija ispod. */
@Composable
internal fun EntryRow(text: String, meta: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), fontSize = 15.sp, color = Ds.ink)
            if (meta.isNotEmpty()) Text(meta, Modifier.padding(start = 12.dp), fontSize = 13.sp, color = Ds.sub)
        }
        HorizontalDivider(thickness = 1.dp, color = Ds.line)
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
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Ds.s1).padding(horizontal = 20.dp, vertical = 18.dp)) {
        CardHeader(
            tr("Ishrana", "Food"),
            if (food.isEmpty()) tr("Još ništa nije uneto", "Nothing logged yet") else tr("${food.size} ${if (food.size == 1) "unos" else "unosa"}", "${food.size} ${if (food.size == 1) "entry" else "entries"}"),
        ) { RoundButton(Icons.Filled.Add, tr("Dodaj obrok", "Add meal"), Accent, Color.White, 36.dp, onAdd) }
        if (kcalGoal > 0 || proteinGoal > 0) {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onGoals),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (kcalGoal > 0) GoalBar(kcal, kcalGoal, "kcal", Accent, Modifier.weight(1f))
                if (proteinGoal > 0) GoalBar(prot, proteinGoal, tr("g proteina", "g protein"), Ds.green, Modifier.weight(1f))
            }
        } else {
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (kcal > 0 || prot > 0) Text(
                    listOfNotNull(kcal.takeIf { it > 0 }?.let { "$it kcal" }, prot.takeIf { it > 0 }?.let { tr("$it g proteina", "$it g protein") }).joinToString(" · "),
                    Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink,
                ) else Spacer(Modifier.weight(1f))
                Text(tr("Postavi dnevni cilj", "Set a daily goal"), Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onGoals).padding(vertical = 6.dp, horizontal = 4.dp),
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText)
            }
        }
        if (copyCount > 0) OutlinePill(tr("Kopiraj juče ($copyCount)", "Copy yesterday ($copyCount)"), onCopy, Modifier.fillMaxWidth().padding(top = 12.dp), Icons.Filled.ContentCopy)
        MEALS.forEach { meal ->
            val entries = food.filter { it.meal == meal }
            if (entries.isNotEmpty()) {
                Text(com.vukos.trening.data.mealLabel(meal), Modifier.padding(top = 14.dp, bottom = 2.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Ds.sub)
                entries.forEach { f ->
                    EntryRow(f.text, listOfNotNull(f.kcal?.let { "$it kcal" }, f.proteinG?.let { "$it g" }).joinToString(" · ")) { onEdit(f) }
                }
            }
        }
    }
}

/** Napredak ka dnevnom cilju: broj / cilj, tanka traka i koliko je ostalo. */
@Composable
private fun GoalBar(value: Int, goal: Int, unit: String, color: Color, modifier: Modifier) {
    val frac = (value.toFloat() / goal).coerceIn(0f, 1f)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$value", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.52).sp, color = Ds.ink)
            Text("/ $goal $unit", Modifier.padding(bottom = 4.dp), fontSize = 13.sp, color = Ds.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Ds.s3)) {
            if (frac > 0f) Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(color))
        }
        Text(
            if (value >= goal) tr("Cilj ispunjen", "Goal reached") else tr("još ${goal - value} $unit", "${goal - value} $unit to go"),
            fontSize = 12.sp, color = Ds.sub,
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

/** Novi obrok / izmena obroka preko celog ekrana (dizajn v2). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoodEditScreen(
    initial: FoodEntry?,
    day: LocalDate,
    recent: List<FoodEntry>,
    dayKcal: Int,
    dayProt: Int,
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
    var scanning by remember { mutableStateOf(false) }
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
                scanning = true
                scope.launch {
                    val pr = runCatching { OpenFoodFacts.lookup(code) }.getOrNull()
                    scanning = false
                    if (pr == null) scanMsg = tr("Proizvod $code nije pronađen u bazi Open Food Facts. Upiši ručno.", "Product $code was not found in Open Food Facts. Enter it manually.")
                    else { product = pr; text = pr.name; scanMsg = null; applyGrams(grams) }
                }
            }
            .addOnFailureListener { scanMsg = tr("Skener nije dostupan: ${it.message}", "Scanner unavailable: ${it.message}") }
    }
    val canSave = text.isNotBlank()
    val fk = kcal.toIntOrNull() ?: 0
    val fp = prot.toIntOrNull() ?: 0
    val numStyle = TextStyle(fontFamily = Manrope, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink)

    LazyColumn(
        Modifier.fillMaxSize().background(Ds.bg).statusBarsPadding().imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Otkaži", "Cancel"), Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onDismiss).padding(4.dp), fontSize = 17.sp, color = Ds.sub)
                    Text(if (initial == null) tr("Novi obrok", "New meal") else tr("Izmena obroka", "Edit meal"), Modifier.weight(1f),
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text(tr("Sačuvaj", "Save"), Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = canSave) {
                        onSave(meal, text.trim(), kcal.toIntOrNull(), prot.toIntOrNull())
                    }.padding(4.dp), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (canSave) Ds.aText else Ds.faint)
                }
                Text(
                    (if (day == LocalDate.now()) tr("Danas · ", "Today · ") else "") + day.srLong().replaceFirstChar { it.uppercase() },
                    Modifier.fillMaxWidth(), fontSize = 14.sp, color = Ds.sub, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Ds.s1).padding(3.dp)) {
                MEALS.forEach { m ->
                    val sel = meal == m
                    Box(
                        Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(9.dp)).background(if (sel) Ds.s3 else Color.Transparent).clickable { meal = m },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(com.vukos.trening.data.mealLabel(m), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Ds.ink else Ds.sub, maxLines = 1)
                    }
                }
            }
        }
        item {
            DsField(text, { text = it }, tr("Šta si jeo/la", "What did you eat"), null, height = 56.dp, radius = 18.dp)
        }
        if (product == null && suggestions.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Često", "Frequent"), Modifier.padding(horizontal = 4.dp), fontSize = 13.sp, color = Ds.sub)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { sg ->
                        val sel = text == sg.text
                        Row(
                            Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (sel) Ds.aSoft else Ds.s1)
                                .clickable { text = sg.text; kcal = sg.kcal?.toString() ?: ""; prot = sg.proteinG?.toString() ?: ""; product = null }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(sg.text, fontSize = 14.sp, color = if (sel) Ds.aText else Ds.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 220.dp))
                            sg.kcal?.let { Text("$it kcal", fontSize = 12.sp, color = Ds.sub) }
                        }
                    }
                }
            }
        }
        val pr = product
        if (pr == null) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(Ds.s1).clickable(enabled = !scanning, onClick = ::scan),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.QrCodeScanner, null, Modifier.size(18.dp), tint = Ds.aText)
                    Text(if (scanning) tr("Tražim proizvod…", "Looking up product…") else tr("Skeniraj barkod", "Scan barcode"),
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText)
                }
                scanMsg?.let { Text(it, Modifier.padding(horizontal = 4.dp), fontSize = 13.sp, color = Ds.sub) }
            }
        } else item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Ds.s1).padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Pronađeno u Open Food Facts", "Found in Open Food Facts"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ds.green)
                        Text(pr.name, Modifier.padding(top = 4.dp), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink)
                        Text(
                            tr("Na 100 g: ", "Per 100 g: ") + listOfNotNull(pr.kcal100?.let { "${Math.round(it)} kcal" }, pr.protein100?.let { tr("${WorkoutMath.fmtKg(it)} g proteina", "${WorkoutMath.fmtKg(it)} g protein") })
                                .joinToString(", ").ifEmpty { tr("nema podataka o vrednostima", "no nutrition data") },
                            Modifier.padding(top = 2.dp), fontSize = 14.sp, color = Ds.sub,
                        )
                    }
                    RoundButton(Icons.Filled.Close, tr("Ukloni proizvod", "Remove product"), Ds.s3, Ds.ink, 32.dp) {
                        product = null; text = ""; kcal = ""; prot = ""
                    }
                }
                HorizontalDivider(Modifier.padding(top = 16.dp), thickness = 1.dp, color = Ds.line)
                Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Količina", "Amount"), Modifier.weight(1f), fontSize = 15.sp, color = Ds.ink)
                    Row(Modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).background(Ds.bg).padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        androidx.compose.foundation.text.BasicTextField(
                            grams, { applyGrams(it.filter(Char::isDigit).take(4)) }, Modifier.width(48.dp), singleLine = true,
                            textStyle = TextStyle(fontFamily = Manrope, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent),
                        )
                        Text("g", fontSize = 15.sp, color = Ds.sub)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple(tr("kcal · opciono", "kcal · optional"), kcal) { v: String -> kcal = v.filter(Char::isDigit).take(5) },
                    Triple(tr("protein g · opciono", "protein g · optional"), prot) { v: String -> prot = v.filter(Char::isDigit).take(4) },
                ).forEach { (label, value, set) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Ds.s1).padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(label, fontSize = 12.sp, color = Ds.sub)
                        androidx.compose.foundation.text.BasicTextField(
                            value, set, Modifier.fillMaxWidth().padding(top = 2.dp), singleLine = true, textStyle = numStyle,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent),
                            decorationBox = { inner -> Box { if (value.isEmpty()) Text("—", style = numStyle.copy(color = Ds.faint)); inner() } },
                        )
                    }
                }
            }
        }
        item {
            Text(
                if (fk > 0 || fp > 0) tr("Dan posle čuvanja: ${dayKcal + fk} kcal, ${dayProt + fp} g proteina", "Day total after saving: ${dayKcal + fk} kcal, ${dayProt + fp} g protein")
                else tr("Ostavi kcal i protein prazno ako ih ne pratiš.", "Leave kcal and protein empty if you do not track them."),
                Modifier.padding(horizontal = 4.dp), fontSize = 13.sp, lineHeight = 20.sp, color = Ds.sub,
            )
        }
        if (onDelete != null) item {
            Text(tr("Obriši obrok", "Delete meal"), Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onDelete).padding(8.dp),
                fontSize = 15.sp, color = Ds.danger, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
