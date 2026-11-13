package com.vukos.trening.ui.workout

import com.vukos.trening.i18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.data.Workout
import com.vukos.trening.data.WorkoutMath
import com.vukos.trening.data.WorkoutRepo
import com.vukos.trening.data.WorkoutSet
import com.vukos.trening.data.SetType
import com.vukos.trening.data.ExerciseStats
import androidx.compose.material.icons.filled.EmojiEvents
import com.vukos.trening.ui.body.srLong
import com.vukos.trening.ui.common.AppCard
import com.vukos.trening.ui.common.Chip
import com.vukos.trening.ui.common.IconBadge
import com.vukos.trening.ui.common.StatTile
import androidx.compose.ui.text.style.TextOverflow
import com.vukos.trening.ui.common.ExerciseThumb
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun Workout.date(): LocalDate = Instant.ofEpochMilli(startedAt).atZone(ZoneId.systemDefault()).toLocalDate()

/** Traka na vrhu dok je trening u toku. */
@Composable
fun ActiveWorkoutBanner(workout: Workout?, sets: List<WorkoutSet>, onClick: () -> Unit) {
    if (workout == null) return
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).statusBarsPadding()
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.FitnessCenter, null, tint = MaterialTheme.colorScheme.onPrimary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tr("Trening u toku: ${workout.name}", "Workout in progress: ${workout.name}"), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            Text(
                tr("Setova: ${WorkoutMath.doneCount(sets)}, trajanje ${WorkoutMath.duration(workout.startedAt, null)}. Dodirni za nastavak.", "Sets: ${WorkoutMath.doneCount(sets)}, duration ${WorkoutMath.duration(workout.startedAt, null)}. Tap to continue."),
                color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodySmall,
            )
        }
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onPrimary)
    }
}

/** Kartica zavrsenog treninga: naziv, datum, brojke i najbolji set po vezbi. */
@Composable
fun WorkoutCard(w: Workout, sets: List<WorkoutSet>, exerciseNames: Map<String, String>, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.FitnessCenter)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(w.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(w.date().srLong(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(WorkoutMath.duration(w.startedAt, w.finishedAt))
            Chip("${WorkoutMath.fmtKg(WorkoutMath.volume(sets))} kg")
            Chip(tr("${sets.size} setova", "${sets.size} sets"))
        }
        val groups = sets.groupBy { it.exercisePosition }.toSortedMap().values
        if (groups.isNotEmpty()) Spacer(Modifier.height(12.dp))
        groups.forEach { g ->
            val best = g.maxWithOrNull(compareBy<WorkoutSet>({ it.weightKg ?: 0.0 }, { it.reps ?: 0 }))
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${g.size} × ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(exerciseNames[g.first().exerciseId] ?: "?", style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                best?.let {
                    Text(
                        (WorkoutMath.fmtKg(it.weightKg).takeIf { k -> k.isNotEmpty() }?.let { k -> "$k kg × " } ?: "") + "${it.reps ?: 0}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Pregled zavrsenog treninga: vezbe i setovi (kg × ponavljanja). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(workoutId: String, onBack: () -> Unit, onOpenExercise: (String) -> Unit) {
    val db = TreningApp.instance.db
    val scope = rememberCoroutineScope()
    val w by remember { db.workouts().observe(workoutId) }.collectAsStateWithLifecycle(null)
    val sets by remember { db.workouts().observeSets(workoutId) }.collectAsStateWithLifecycle(emptyList())
    val exercises by remember { db.exercises().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val byId = remember(exercises) { exercises.associateBy { it.id } }
    var confirm by remember { mutableStateOf(false) }
    val allWorkouts by remember { db.workouts().observeFinished() }.collectAsStateWithLifecycle(emptyList())
    val allSets by remember { db.workouts().observeAllDoneSets() }.collectAsStateWithLifecycle(emptyList())
    val records = remember(allWorkouts, allSets) { ExerciseStats.newRecords(workoutId, allWorkouts, allSets) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(w?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Nazad", "Back")) } },
            actions = { IconButton(onClick = { confirm = true }) { Icon(Icons.Filled.Delete, tr("Obriši", "Delete")) } },
        )
    }) { pad ->
        val workout = w ?: return@Scaffold
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(workout.date().srLong().replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(WorkoutMath.duration(workout.startedAt, workout.finishedAt), tr("trajanje", "duration"), Modifier.weight(1f))
                    StatTile("${WorkoutMath.fmtKg(WorkoutMath.volume(sets))} kg", tr("volumen", "volume"), Modifier.weight(1f))
                    StatTile("${sets.size}", tr("setova", "sets"), Modifier.weight(1f))
                }
            }
            if (records.isNotEmpty()) item {
                AppCard(color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(if (records.size == 1) tr("Novi lični rekord!", "New personal record!") else tr("${records.size} nova lična rekorda!", "${records.size} new personal records!"),
                            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    records.forEach { r ->
                        Column(Modifier.padding(top = 10.dp)) {
                            Text(byId[r.exerciseId]?.name ?: "?", style = MaterialTheme.typography.titleSmall)
                            Text(
                                tr("${r.kind}: ${WorkoutMath.fmtKg(r.value)} kg (pre: ${WorkoutMath.fmtKg(r.previous)} kg)", "${r.kind}: ${WorkoutMath.fmtKg(r.value)} kg (was: ${WorkoutMath.fmtKg(r.previous)} kg)"),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
            items(sets.groupBy { it.exercisePosition }.toSortedMap().values.toList()) { g ->
                val ex = byId[g.first().exerciseId]
                AppCard(padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { ex?.let { onOpenExercise(it.id) } }) {
                        ex?.let { ExerciseThumb(it, 44.dp) }
                        Spacer(Modifier.width(12.dp))
                        Text(ex?.name ?: "?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ChevronRight, tr("Statistika vežbe", "Exercise stats"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    g.forEach { s ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(26.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                                Text(SetType.short(s.setType) ?: "${s.setIndex}", style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("${WorkoutMath.fmtKg(s.weightKg).ifEmpty { "0" }} kg", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(tr("${s.reps ?: 0} ponav.", "${s.reps ?: 0} reps"), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(tr("Obrisati trening?", "Delete workout?")) },
        confirmButton = { TextButton(onClick = { confirm = false; scope.launch { WorkoutRepo.discard(workoutId); onBack() } }) { Text(tr("Obriši", "Delete")) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(tr("Otkaži", "Cancel")) } },
    )
}

@Composable
private fun Info(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
