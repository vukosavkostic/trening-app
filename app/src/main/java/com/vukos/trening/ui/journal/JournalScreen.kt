package com.vukos.trening.ui.journal

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
fun JournalScreen(onOpenWorkout: (String) -> Unit, onOpenBodyDay: (LocalDate) -> Unit) {
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

    Scaffold(topBar = { TabTopBar("Dnevnik") }) { pad ->
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
                    StatTile("${insights.workouts}", "treninga", Modifier.weight(1f))
                    StatTile(JournalStats.fmtMinutes(insights.minutes), "u teretani", Modifier.weight(1f))
                    StatTile("${WorkoutMath.fmtKg(insights.volumeKg / 1000)} t", "podignuto", Modifier.weight(1f))
                }
            }
            item {
                SectionTitle(if (selected == LocalDate.now()) "Danas" else selected.srLong().replaceFirstChar { it.uppercase() })
            }
            if (dayWorkouts.isEmpty()) item {
                InfoRow(Icons.Filled.FitnessCenter, MaterialTheme.colorScheme.onSurfaceVariant, "Trening",
                    if (selected.isAfter(LocalDate.now())) "—" else "Bez treninga", null)
            }
            items(dayWorkouts, key = { it.id }) { w ->
                WorkoutCard(w, setsByWorkout[w.id].orEmpty(), names) { onOpenWorkout(w.id) }
            }
            dayWeight?.let { e ->
                item { InfoRow(Icons.Filled.MonitorWeight, WeightBlue, "Kilaža", e.weightKg!!.kg()) { onOpenBodyDay(selected) } }
            }
            item {
                FoodCard(food, onAdd = { adding = true }, onEdit = { editing = it })
            }
        }
    }

    if (adding || editing != null) FoodDialog(
        initial = editing,
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
private fun FoodCard(food: List<FoodEntry>, onAdd: () -> Unit, onEdit: (FoodEntry) -> Unit) {
    val kcal = food.mapNotNull { it.kcal }.sum()
    val prot = food.mapNotNull { it.proteinG }.sum()
    AppCard(padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Restaurant, FoodGreen, 40.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Ishrana", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        food.isEmpty() -> "Još ništa nije uneto"
                        kcal > 0 || prot > 0 -> listOfNotNull(kcal.takeIf { it > 0 }?.let { "$it kcal" },
                            prot.takeIf { it > 0 }?.let { "$it g proteina" }).joinToString(" · ")
                        else -> "${food.size} ${if (food.size == 1) "unos" else "unosa"}"
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Add, "Dodaj obrok", tint = Color.White) }
        }
        MEALS.forEach { meal ->
            val entries = food.filter { it.meal == meal }
            if (entries.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(meal.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                entries.forEach { f ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onEdit(f) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(f.text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            listOfNotNull(f.kcal?.let { "$it kcal" }, f.proteinG?.let { "${it}g P" }).joinToString(" · "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoodDialog(
    initial: FoodEntry?,
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Novi obrok" else "Izmena obroka") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MEALS.forEach { m -> FilterChip(selected = meal == m, onClick = { meal = m }, label = { Text(m) }) }
                }
                OutlinedTextField(text, { text = it }, label = { Text("Šta si jeo/la") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(kcal, { kcal = it.filter(Char::isDigit).take(5) }, label = { Text("kcal (opc.)") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(prot, { prot = it.filter(Char::isDigit).take(4) }, label = { Text("protein g") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onSave(meal, text.trim(), kcal.toIntOrNull(), prot.toIntOrNull()) }) { Text("Sačuvaj") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Obriši", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Otkaži") }
            }
        },
    )
}

