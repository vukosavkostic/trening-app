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

private val FoodGreen = Color(0xFF43A047)
private val WeightBlue = Color(0xFF4F8DF7)

/** Dnevnik: kalendar (treninzi / ishrana / kilaza), uvidi za mesec i izabrani dan. */
@OptIn(ExperimentalMaterial3Api::class)
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

    val accent = MaterialTheme.colorScheme.primary
    val markers = remember(workouts, foodDays, body, accent) {
        val trained = workouts.map { JournalStats.dateOf(it) }.toSet()
        val ate = foodDays.map { LocalDate.ofEpochDay(it) }.toSet()
        val weighed = body.filter { it.weightKg != null }.map { LocalDate.ofEpochDay(it.epochDay) }.toSet()
        (trained + ate + weighed).associateWith { d ->
            DayMarker(listOfNotNull(accent.takeIf { d in trained }, FoodGreen.takeIf { d in ate }, WeightBlue.takeIf { d in weighed }))
        }
    }
    val insights = remember(workouts, doneSets, ym) { JournalStats.month(workouts, doneSets, ym) }
    val dayWorkouts = workouts.filter { JournalStats.dateOf(it) == selected }
    val dayWeight = body.firstOrNull { it.epochDay == selectedDay && it.weightKg != null }

    Scaffold(topBar = { TopAppBar(title = { Text("Dnevnik") }) }) { pad ->
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
                Row(Modifier.padding(horizontal = 4.dp)) {
                    Legend(accent, "trening"); Spacer(Modifier.width(14.dp))
                    Legend(FoodGreen, "ishrana"); Spacer(Modifier.width(14.dp))
                    Legend(WeightBlue, "kilaža")
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)
                ) {
                    Text("Uvidi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.height(48.dp)) {
                        Insight("${insights.workouts}", "treninga", Modifier.weight(1f))
                        Divider()
                        Insight(JournalStats.fmtMinutes(insights.minutes), "ukupno", Modifier.weight(1f))
                        Divider()
                        Insight("${WorkoutMath.fmtKg(insights.volumeKg / 1000)} t", "volumen", Modifier.weight(1f))
                    }
                }
            }
            item {
                Text(
                    if (selected == LocalDate.now()) "Danas" else selected.sr(),
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (dayWorkouts.isEmpty()) item {
                Text("Nema treninga ovog dana.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(dayWorkouts, key = { it.id }) { w ->
                WorkoutCard(w, setsByWorkout[w.id].orEmpty(), names) { onOpenWorkout(w.id) }
            }
            dayWeight?.let { e ->
                item {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable { onOpenBodyDay(selected) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.MonitorWeight, null, tint = WeightBlue)
                        Text("  Kilaža", Modifier.weight(1f))
                        Text(e.weightKg!!.kg(), fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Filled.Restaurant, null, tint = FoodGreen)
                    Text("  Ishrana", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    val kcal = food.mapNotNull { it.kcal }.sum()
                    val prot = food.mapNotNull { it.proteinG }.sum()
                    if (kcal > 0 || prot > 0) Text(
                        listOfNotNull(kcal.takeIf { it > 0 }?.let { "$it kcal" }, prot.takeIf { it > 0 }?.let { "$it g proteina" }).joinToString(" · "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            MEALS.forEach { meal ->
                val entries = food.filter { it.meal == meal }
                if (entries.isNotEmpty()) item(key = "meal-$meal") {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer).padding(vertical = 8.dp)
                    ) {
                        Text(meal, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                        entries.forEach { f ->
                            Row(
                                Modifier.fillMaxWidth().clickable { editing = f }.padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(f.text, Modifier.weight(1f))
                                Text(
                                    listOfNotNull(f.kcal?.let { "$it kcal" }, f.proteinG?.let { "${it}g P" }).joinToString(" · "),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
            item {
                TextButton(onClick = { adding = true }) { Icon(Icons.Filled.Add, null); Text("  Dodaj obrok") }
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

@Composable
private fun Insight(value: String, label: String, modifier: Modifier) {
    Column(modifier.padding(horizontal = 8.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Divider() {
    Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text("  $label", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
