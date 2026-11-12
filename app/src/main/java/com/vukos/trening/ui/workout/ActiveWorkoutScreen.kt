package com.vukos.trening.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.data.Exercise
import com.vukos.trening.data.RoutineExercise
import com.vukos.trening.data.WorkoutMath
import com.vukos.trening.data.WorkoutRepo
import com.vukos.trening.data.WorkoutSet
import com.vukos.trening.ui.common.ExerciseThumb
import com.vukos.trening.ui.common.OutlinePill
import com.vukos.trening.ui.common.StatTile
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

private val DoneGreen = Color(0xFF22A06B)

/** Logovanje treninga: za svaku vezbu tabela Set | Prethodno | kg | Ponav. | ✓. Sve se odmah cuva u bazu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    workoutId: String,
    savedState: SavedStateHandle?,
    onPickExercises: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onFinished: (String) -> Unit,
    onClose: () -> Unit,
) {
    val db = TreningApp.instance.db
    val scope = rememberCoroutineScope()
    val workout by remember(workoutId) { db.workouts().observe(workoutId) }.collectAsStateWithLifecycle(null)
    val sets by remember(workoutId) { db.workouts().observeSets(workoutId) }.collectAsStateWithLifecycle(emptyList())
    val exercises by remember { db.exercises().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val byId = remember(exercises) { exercises.associateBy { it.id } }
    val previous = remember { mutableStateMapOf<String, List<WorkoutSet>>() }
    val targets = remember { mutableStateMapOf<String, RoutineExercise>() }
    var menu by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }

    // Ciljevi iz plana (za predloge kad vezba nema istoriju).
    LaunchedEffect(workout?.routineId) {
        workout?.routineId?.let { rid -> db.routines().getExercises(rid).forEach { targets[it.exerciseId] = it } }
    }
    // "Prethodno" za svaku vezbu u treningu.
    val exerciseIds = sets.map { it.exerciseId }.distinct()
    LaunchedEffect(exerciseIds) {
        exerciseIds.filter { it !in previous }.forEach { previous[it] = db.workouts().previousSets(it, workoutId) }
    }
    // Vezbe izabrane u pickeru.
    val picked = savedState?.getStateFlow<ArrayList<String>?>("picked", null)?.collectAsStateWithLifecycle()
    LaunchedEffect(picked?.value) {
        picked?.value?.let { WorkoutRepo.addExercises(workoutId, it); savedState["picked"] = null }
    }
    // Trening je zavrsen ili odbacen negde drugde.
    LaunchedEffect(workout) { if (workout?.deleted == true) onClose() }

    // Ekran treninga se ne napusta dok se trening ne zavrsi ili odbaci.
    androidx.activity.compose.BackHandler { dialog = "leave" }

    val groups = sets.groupBy { it.exercisePosition }.toSortedMap()
    val start = workout?.startedAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Column {
                    Text(workout?.name ?: "Trening", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "Početak ${start?.let { "%02d:%02d".format(it.hour, it.minute) } ?: ""}",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = {
                Button(onClick = { dialog = "finish" }, modifier = Modifier.padding(end = 4.dp), shape = androidx.compose.foundation.shape.CircleShape) {
                    Text("Završi", style = MaterialTheme.typography.labelLarge)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Još") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Odbaci trening") }, onClick = { menu = false; dialog = "discard" })
                    }
                }
            },
        )
    }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("${WorkoutMath.fmtKg(WorkoutMath.volume(sets))} kg", "volumen", Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                    StatTile("${WorkoutMath.doneCount(sets)} / ${sets.size}", "setova", Modifier.weight(1f))
                    StatTile("${groups.size}", "vežbi", Modifier.weight(1f))
                }
            }
            groups.forEach { (pos, groupSets) ->
                val ex = byId[groupSets.first().exerciseId]
                item(key = "ex-$pos") {
                    ExerciseBlock(
                        exercise = ex,
                        sets = groupSets,
                        previous = previous[groupSets.first().exerciseId].orEmpty(),
                        target = targets[groupSets.first().exerciseId],
                        onOpen = { ex?.let { onOpenExercise(it.id) } },
                        onRemove = { scope.launch { WorkoutRepo.removeExercise(workoutId, pos) } },
                        onAddSet = { scope.launch { WorkoutRepo.addSet(workoutId, groupSets.first().exerciseId, pos) } },
                        onUpdate = { s -> scope.launch { WorkoutRepo.updateSet(s) } },
                        onDelete = { s -> scope.launch { WorkoutRepo.deleteSet(s) } },
                    )
                }
            }
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinePill("Dodaj vežbu", onPickExercises, Modifier.fillMaxWidth(), Icons.Filled.Add)
                }
            }
        }
    }

    when (dialog) {
        "finish" -> {
            val done = WorkoutMath.doneCount(sets)
            val notDone = sets.size - done
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(if (done == 0) "Nema unetih setova" else "Završi trening?") },
                text = {
                    Text(
                        if (done == 0) "Upiši kilažu i ponavljanja bar za jedan set, ili odbaci trening."
                        else "Čuva se $done setova." + if (notDone > 0) "\nPrazni setovi ($notDone) se ne čuvaju." else ""
                    )
                },
                confirmButton = {
                    if (done > 0) TextButton(onClick = {
                        dialog = null
                        scope.launch { if (WorkoutRepo.finish(workoutId)) onFinished(workoutId) }
                    }) { Text("Završi") }
                    else TextButton(onClick = { dialog = "discard" }) { Text("Odbaci") }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text("Nazad") } },
            )
        }
        "leave" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Trening je u toku") },
            text = { Text("Sve što si uneo je sačuvano. Završi trening kad budeš gotov, ili ga odbaci.") },
            confirmButton = { TextButton(onClick = { dialog = "finish" }) { Text("Završi") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { dialog = "discard" }) { Text("Odbaci", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { dialog = null }) { Text("Nastavi") }
                }
            },
        )
        "discard" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Odbaci trening?") },
            text = { Text("Svi uneti setovi ovog treninga se brišu.") },
            confirmButton = {
                TextButton(onClick = { dialog = null; scope.launch { WorkoutRepo.discard(workoutId); onClose() } }) { Text("Odbaci") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Nazad") } },
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ExerciseBlock(
    exercise: Exercise?,
    sets: List<WorkoutSet>,
    previous: List<WorkoutSet>,
    target: RoutineExercise?,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onAddSet: () -> Unit,
    onUpdate: (WorkoutSet) -> Unit,
    onDelete: (WorkoutSet) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(vertical = 12.dp)
    ) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            exercise?.let { ExerciseThumb(it, 40.dp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
                Text(exercise?.name ?: "?", style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                target?.let { t ->
                    Text(
                        "Cilj: ${t.targetSets}×${t.repsMin ?: ""}${if (t.repsMax != null && t.repsMax != t.repsMin) "–${t.repsMax}" else ""}" +
                            (t.targetKg?.let { " @ ${WorkoutMath.fmtKg(it)} kg" } ?: ""),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Opcije vežbe") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Ukloni vežbu") }, onClick = { menu = false; onRemove() })
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp)) {
            Header("Set", Modifier.width(36.dp))
            Header("Prethodno", Modifier.weight(1.3f))
            Header("kg", Modifier.weight(1f))
            Header("Ponav.", Modifier.weight(1f))
            Header("✓", Modifier.width(44.dp))
        }
        sets.forEach { s ->
            androidx.compose.runtime.key(s.id) { SetRow(s, previous, target, onUpdate, onDelete) }
        }
        TextButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Icon(Icons.Filled.Add, null); Text("  Dodaj set")
        }
    }
}

@Composable
private fun Header(text: String, modifier: Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center)
}

@Composable
private fun SetRow(
    set: WorkoutSet,
    previous: List<WorkoutSet>,
    target: RoutineExercise?,
    onUpdate: (WorkoutSet) -> Unit,
    onDelete: (WorkoutSet) -> Unit,
) {
    // Tekst polja zivi lokalno (bez skakanja kursora); baza se azurira na svaku promenu.
    var kg by remember(set.id) { mutableStateOf(WorkoutMath.fmtKg(set.weightKg)) }
    var reps by remember(set.id) { mutableStateOf(set.reps?.toString() ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }
    val prev = previous.firstOrNull { it.setIndex == set.setIndex }
    val (hintKg, hintReps) = WorkoutMath.hint(set.setIndex, previous, target)
    val bg = if (set.done) DoneGreen.copy(alpha = 0.22f) else Color.Transparent

    Row(
        Modifier.padding(horizontal = 8.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg).padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${set.setIndex}", Modifier.width(36.dp).clickable { confirmDelete = true },
            textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        Text(
            prev?.let { "${WorkoutMath.fmtKg(it.weightKg)} × ${it.reps ?: "-"}" } ?: "—",
            Modifier.weight(1.3f), textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
        )
        NumField(kg, WorkoutMath.fmtKg(hintKg), true, Modifier.weight(1f)) {
            kg = it; onUpdate(set.copy(weightKg = WorkoutMath.parseKg(it), reps = WorkoutMath.parseReps(reps)))
        }
        NumField(reps, hintReps?.toString() ?: "", false, Modifier.weight(1f)) {
            reps = it; onUpdate(set.copy(weightKg = WorkoutMath.parseKg(kg), reps = WorkoutMath.parseReps(it)))
        }
        Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (set.done) DoneGreen else MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable {
                        if (set.done) onUpdate(set.copy(weightKg = WorkoutMath.parseKg(kg), reps = WorkoutMath.parseReps(reps), done = false))
                        else {
                            // Prazno polje = prihvata se predlog (prethodno / cilj).
                            val k = WorkoutMath.parseKg(kg) ?: hintKg
                            val r = WorkoutMath.parseReps(reps) ?: hintReps
                            if (r != null) {
                                kg = WorkoutMath.fmtKg(k); reps = r.toString()
                                onUpdate(set.copy(weightKg = k, reps = r, done = true))
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Check, "Urađeno", tint = if (set.done) Color.White else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Obrisati set ${set.setIndex}?") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete(set) }) { Text("Obriši") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Otkaži") } },
    )
}

@Composable
private fun NumField(value: String, hint: String, decimal: Boolean, modifier: Modifier, onChange: (String) -> Unit) {
    val style = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center)
    Box(
        modifier.padding(horizontal = 4.dp).height(38.dp).clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = value,
            onValueChange = { v -> onChange(v.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }.take(6)) },
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) Text(hint, style = style.copy(color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Normal))
                    inner()
                }
            },
        )
    }
}
