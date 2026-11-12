package com.vukos.trening.ui.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.vukos.trening.TreningApp
import com.vukos.trening.data.ChartRange
import com.vukos.trening.data.Exercise
import com.vukos.trening.data.ExerciseMetric
import com.vukos.trening.data.ExerciseSession
import com.vukos.trening.data.ExerciseStats
import com.vukos.trening.data.WorkoutMath
import com.vukos.trening.data.equipmentLabels
import com.vukos.trening.data.muscleLabels
import com.vukos.trening.ui.body.sr
import com.vukos.trening.ui.common.LineChart
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Detalji vezbe: Pregled (grafikon napretka + rekordi), Istorija, Uputstvo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(id: String, onBack: () -> Unit, onOpenWorkout: (String) -> Unit = {}) {
    val db = TreningApp.instance.db
    val ex by remember(id) { db.exercises().observe(id) }.collectAsStateWithLifecycle(null)
    val workouts by remember { db.workouts().observeFinished() }.collectAsStateWithLifecycle(emptyList())
    val doneSets by remember { db.workouts().observeAllDoneSets() }.collectAsStateWithLifecycle(emptyList())
    val sessions = remember(id, workouts, doneSets) { ExerciseStats.sessions(id, workouts, doneSets) }
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(ex?.name ?: "", maxLines = 1) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Nazad") } },
            actions = {
                if (ex?.isCustom == true) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Obriši") }
            },
        )
    }) { pad ->
        val e = ex ?: return@Scaffold
        Column(Modifier.padding(pad).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                listOf("Pregled", "Istorija", "Uputstvo").forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
                }
            }
            when (tab) {
                0 -> Overview(e, sessions)
                1 -> History(sessions, onOpenWorkout)
                else -> HowTo(e)
            }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Obrisati vežbu?") },
        text = { Text("Vežba nestaje iz liste. Stari treninzi sa njom ostaju sačuvani.") },
        confirmButton = {
            TextButton(onClick = { confirmDelete = false; scope.launch { db.exercises().softDeleteCustom(id); onBack() } }) { Text("Obriši") }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Otkaži") } },
    )
}

@Composable
private fun Overview(e: Exercise, sessions: List<ExerciseSession>) {
    var metric by rememberSaveable { mutableStateOf(ExerciseMetric.HEAVIEST) }
    var range by rememberSaveable { mutableStateOf(ChartRange.Y1) }
    val today = LocalDate.now()
    val shown = sessions.filter { range.includes(it.date, today) }
    val fmt: (Double) -> String = { v -> WorkoutMath.fmtKg(v) + if (metric.unit.isNotEmpty()) " ${metric.unit}" else "" }

    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Box(Modifier.fillMaxWidth().aspectRatio(2f).background(Color.White), contentAlignment = Alignment.Center) {
                val url = e.thumbnailUrl
                if (url == null) Text("Nema slike", color = Color.Gray)
                else AsyncImage(url, e.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
        }
        item {
            Column(Modifier.padding(16.dp)) {
                Text(e.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    listOfNotNull(
                        "Glavni: ${muscleLabels[e.primaryMuscle] ?: e.primaryMuscle.ifBlank { "—" }}",
                        equipmentLabels[e.equipment],
                    ).joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (sessions.isEmpty()) item {
            Text(
                "Još nema urađenih setova za ovu vežbu. Grafikon i rekordi se pojavljuju posle prvog treninga.",
                Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            item {
                val last = shown.lastOrNull()
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom) {
                    Text(last?.let { fmt(it.value(metric)) } ?: "—", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("  ${last?.date?.sr() ?: ""}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ChartRange.entries) { r -> FilterChip(selected = range == r, onClick = { range = r }, label = { Text(r.label) }) }
                }
            }
            item {
                if (shown.isEmpty()) Text("Nema podataka u izabranom periodu.", Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                else LineChart(shown.map { it.date to it.value(metric) }, fmt, Modifier.padding(horizontal = 16.dp))
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExerciseMetric.entries) { m -> FilterChip(selected = metric == m, onClick = { metric = m }, label = { Text(m.label) }) }
                }
            }
            item { Records(sessions) }
        }
    }
}

@Composable
private fun Records(sessions: List<ExerciseSession>) {
    val r = ExerciseStats.records(sessions)
    Column(
        Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
            Text("  Lični rekordi", fontWeight = FontWeight.SemiBold)
        }
        Rec("Najveća težina", r.heaviest?.let { "${WorkoutMath.fmtKg(it.weightKg)} kg × ${it.reps}" })
        Rec("Procenjeni 1RM", "${WorkoutMath.fmtKg(Math.round(r.bestOneRm * 10) / 10.0)} kg")
        Rec("Najbolji set (kg × ponav.)", r.bestSet?.let { "${WorkoutMath.fmtKg(it.weightKg)} kg × ${it.reps}" })
        Rec("Najviše ponavljanja", r.mostReps?.let { "${it.reps} × ${WorkoutMath.fmtKg(it.weightKg)} kg" })
        Rec("Broj treninga", "${r.sessions}")
    }
}

@Composable
private fun Rec(label: String, value: String?) {
    Row {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value ?: "—", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun History(sessions: List<ExerciseSession>, onOpenWorkout: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (sessions.isEmpty()) item { Text("Nema istorije.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(sessions.reversed(), key = { it.workoutId }) { s ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { onOpenWorkout(s.workoutId) }.padding(16.dp)
            ) {
                Row {
                    Text(s.date.sr(), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("1RM ${WorkoutMath.fmtKg(Math.round(s.oneRm * 10) / 10.0)} kg", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                s.sets.forEach { set ->
                    Row {
                        Text("${set.setIndex}", Modifier.width(28.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${WorkoutMath.fmtKg(set.weightKg).ifEmpty { "0" }} kg × ${set.reps ?: 0}")
                    }
                }
            }
        }
    }
}

@Composable
private fun HowTo(e: Exercise) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (e.instructions.isBlank()) item { Text("Nema uputstva.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else items(e.instructions.split('\n').withIndex().toList()) { (i, step) ->
            Row {
                Text("${i + 1}.", Modifier.width(24.dp), color = MaterialTheme.colorScheme.primary)
                Text(step)
            }
        }
        if (e.secondaryList.isNotEmpty()) item {
            Text("Pomoćni mišići: " + e.secondaryList.joinToString(", ") { muscleLabels[it] ?: it },
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
