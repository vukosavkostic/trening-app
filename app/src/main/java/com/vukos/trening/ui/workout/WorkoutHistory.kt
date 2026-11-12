package com.vukos.trening.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.vukos.trening.ui.body.sr
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
            Text("Trening u toku: ${workout.name}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            Text(
                "${WorkoutMath.doneCount(sets)} setova · ${WorkoutMath.duration(workout.startedAt, null)} · nastavi",
                color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodySmall,
            )
        }
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onPrimary)
    }
}

/** Kartica zavrsenog treninga (lista u Dnevniku). */
@Composable
fun WorkoutCard(w: Workout, sets: List<WorkoutSet>, exerciseNames: Map<String, String>, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick).padding(16.dp)
    ) {
        Row {
            Text(w.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(w.date().sr(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "${WorkoutMath.duration(w.startedAt, w.finishedAt)} · ${WorkoutMath.fmtKg(WorkoutMath.volume(sets))} kg · ${sets.size} setova",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
        )
        sets.groupBy { it.exercisePosition }.toSortedMap().values.forEach { g ->
            Text("${g.size} × ${exerciseNames[g.first().exerciseId] ?: "?"}", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp))
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

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(w?.name ?: "") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Nazad") } },
            actions = { IconButton(onClick = { confirm = true }) { Icon(Icons.Filled.Delete, "Obriši") } },
        )
    }) { pad ->
        val workout = w ?: return@Scaffold
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(workout.date().sr(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.padding(top = 8.dp)) {
                    Info("Trajanje", WorkoutMath.duration(workout.startedAt, workout.finishedAt), Modifier.weight(1f))
                    Info("Volumen", "${WorkoutMath.fmtKg(WorkoutMath.volume(sets))} kg", Modifier.weight(1f))
                    Info("Setovi", "${sets.size}", Modifier.weight(1f))
                }
            }
            items(sets.groupBy { it.exercisePosition }.toSortedMap().values.toList()) { g ->
                val ex = byId[g.first().exerciseId]
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { ex?.let { onOpenExercise(it.id) } }) {
                        ex?.let { ExerciseThumb(it, 40.dp) }
                        Spacer(Modifier.width(12.dp))
                        Text(ex?.name ?: "?", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                    g.forEach { s ->
                        Row(Modifier.padding(top = 6.dp)) {
                            Text("${s.setIndex}", Modifier.width(32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${WorkoutMath.fmtKg(s.weightKg).ifEmpty { "0" }} kg × ${s.reps ?: 0}")
                        }
                    }
                }
            }
        }
    }

    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Obrisati trening?") },
        confirmButton = { TextButton(onClick = { confirm = false; scope.launch { WorkoutRepo.discard(workoutId); onBack() } }) { Text("Obriši") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Otkaži") } },
    )
}

@Composable
private fun Info(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

/** Privremeni Dnevnik (faza 4): lista zavrsenih treninga. Kalendar i ishrana dolaze u fazi 5. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(onOpen: (String) -> Unit) {
    val db = TreningApp.instance.db
    val workouts by remember { db.workouts().observeFinished() }.collectAsStateWithLifecycle(emptyList())
    val sets by remember { db.workouts().observeAllDoneSets() }.collectAsStateWithLifecycle(emptyList())
    val exercises by remember { db.exercises().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val names = remember(exercises) { exercises.associate { it.id to it.name } }
    val byWorkout = remember(sets) { sets.groupBy { it.workoutId } }

    Scaffold(topBar = { TopAppBar(title = { Text("Dnevnik") }) }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (workouts.isEmpty()) item {
                Text("Još nema završenih treninga. Započni trening iz tab-a Planovi.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(workouts, key = { it.id }) { w ->
                WorkoutCard(w, byWorkout[w.id].orEmpty(), names) { onOpen(w.id) }
            }
        }
    }
}
