package com.vukos.trening.ui.workout

import com.vukos.trening.i18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
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
import com.vukos.trening.data.SetType
import com.vukos.trening.data.PlanUpdate
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.TrendingUp
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

    // Ekran ostaje upaljen tokom treninga (gasi se samo kad ga korisnik zakljuca).
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Razlika u odnosu na plan (za "Azuriraj plan" pri zavrsetku).
    var planItems by remember { mutableStateOf<List<RoutineExercise>>(emptyList()) }
    LaunchedEffect(workout?.routineId, dialog) {
        if (dialog == "finish") workout?.routineId?.let { planItems = db.routines().getExercises(it) }
    }
    val planUpdate = remember(planItems, sets, dialog) {
        workout?.routineId?.takeIf { dialog == "finish" }?.let { WorkoutMath.planUpdate(it, planItems, sets) }
    }
    var updatePlan by remember { mutableStateOf(false) }
    var noteFor by remember { mutableStateOf<Exercise?>(null) }

    val groups = sets.groupBy { it.exercisePosition }.toSortedMap()
    val start = workout?.startedAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Column(Modifier.padding(end = 12.dp)) {
                    Text(workout?.name ?: tr("Trening", "Workout"), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    Text(
                        tr("Početak", "Started") + " ${start?.let { "%02d:%02d".format(it.hour, it.minute) } ?: ""}",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = {
                Button(
                    onClick = { dialog = "finish" }, modifier = Modifier.padding(end = 2.dp).height(40.dp),
                    shape = androidx.compose.foundation.shape.CircleShape, contentPadding = PaddingValues(horizontal = 18.dp),
                ) {
                    Text(tr("Završi", "Finish"), style = MaterialTheme.typography.labelLarge)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, tr("Još", "More")) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(tr("Odbaci trening", "Discard workout")) }, onClick = { menu = false; dialog = "discard" })
                    }
                }
            },
        )
    }) { pad ->
        LazyColumn(
            Modifier.padding(pad).consumeWindowInsets(pad).imePadding().fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("${WorkoutMath.fmtKg(WorkoutMath.volume(sets))} kg", tr("volumen", "volume"), Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                    StatTile("${WorkoutMath.doneCount(sets)} / ${sets.size}", tr("setova", "sets"), Modifier.weight(1f))
                    StatTile("${groups.size}", tr("vežbi", "exercises"), Modifier.weight(1f))
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
                        onNote = { noteFor = ex },
                    )
                }
            }
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinePill(tr("Dodaj vežbu", "Add exercise"), onPickExercises, Modifier.fillMaxWidth(), Icons.Filled.Add)
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
                title = { Text(if (done == 0) tr("Nema unetih setova", "No sets logged") else tr("Završi trening?", "Finish workout?")) },
                text = {
                    Column {
                        Text(
                            if (done == 0) tr("Upiši kilažu i ponavljanja bar za jedan set, ili odbaci trening.", "Enter weight and reps for at least one set, or discard the workout.")
                            else tr("Čuva se $done setova.", "$done sets will be saved.") + if (notDone > 0) tr("\nPrazni setovi ($notDone) se ne čuvaju.", "\nEmpty sets ($notDone) are not saved.") else ""
                        )
                        if (done > 0 && planUpdate != null) {
                            Spacer(Modifier.height(16.dp))
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { updatePlan = !updatePlan }.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                androidx.compose.material3.Checkbox(checked = updatePlan, onCheckedChange = { updatePlan = it })
                                Column(Modifier.padding(top = 10.dp)) {
                                    Text(tr("Ažuriraj plan „${workout?.name}“", "Update plan “${workout?.name}”"), style = MaterialTheme.typography.titleSmall)
                                    planChanges(planUpdate, byId).forEach {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (done > 0) TextButton(onClick = {
                        dialog = null
                        val upd = planUpdate.takeIf { updatePlan }
                        val rid = workout?.routineId
                        scope.launch {
                            if (WorkoutRepo.finish(workoutId)) {
                                if (upd != null && rid != null) WorkoutRepo.applyPlanUpdate(rid, upd)
                                onFinished(workoutId)
                            }
                        }
                    }) { Text(tr("Završi", "Finish")) }
                    else TextButton(onClick = { dialog = "discard" }) { Text(tr("Odbaci", "Discard")) }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text(tr("Nazad", "Back")) } },
            )
        }
        "leave" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(tr("Trening je u toku", "Workout in progress")) },
            text = { Text(tr("Sve što si uneo je sačuvano. Završi trening kad budeš gotov, ili ga odbaci.", "Everything you entered is saved. Finish the workout when you are done, or discard it.")) },
            confirmButton = { TextButton(onClick = { dialog = "finish" }) { Text(tr("Završi", "Finish")) } },
            dismissButton = {
                Row {
                    TextButton(onClick = { dialog = "discard" }) { Text(tr("Odbaci", "Discard"), color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { dialog = null }) { Text(tr("Nastavi", "Continue")) }
                }
            },
        )
        else -> Unit
    }
    noteFor?.let { ex ->
        NoteDialog(ex, onDismiss = { noteFor = null }) { note ->
            noteFor = null
            scope.launch { WorkoutRepo.saveExerciseNote(ex.id, note) }
        }
    }
    when (dialog) {
        "discard" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(tr("Odbaci trening?", "Discard workout?")) },
            text = { Text(tr("Svi uneti setovi ovog treninga se brišu.", "All sets of this workout will be deleted.")) },
            confirmButton = {
                TextButton(onClick = { dialog = null; scope.launch { WorkoutRepo.discard(workoutId); onClose() } }) { Text(tr("Odbaci", "Discard")) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(tr("Nazad", "Back")) } },
        )
    }
}

/** Opis izmena plana za dijalog zavrsetka. */
private fun planChanges(u: PlanUpdate, byId: Map<String, Exercise>): List<String> = buildList {
    fun n(id: String) = byId[id]?.name ?: "?"
    u.added.forEach { add("+ ${n(it)}") }
    u.removed.forEach { add("− ${n(it)}") }
    u.setChanges.forEach { (id, a, b) -> add(tr("${n(id)}: $a → $b setova", "${n(id)}: $a → $b sets")) }
    if (u.reordered) add(tr("novi redosled vežbi", "new exercise order"))
    add(tr("kilaže iz ovog treninga postaju cilj", "weights from this workout become the target"))
}

@Composable
private fun NoteDialog(exercise: Exercise, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(exercise.notes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Beleška: ${exercise.name}", "Note: ${exercise.name}"), maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                text, { text = it.take(300) }, placeholder = { Text(tr("npr. sedište na 4, uži hvat", "e.g. seat at 4, narrow grip")) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(tr("Sačuvaj", "Save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Otkaži", "Cancel")) } },
    )
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
    onNote: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val progression = WorkoutMath.progression(previous, target)
    // Oznaka seta: W/D/F za posebne setove, inace redni broj medju radnim setovima.
    val labels = remember(sets) {
        var n = 0
        sets.associate { s -> s.id to (SetType.short(s.setType)?.takeIf { s.setType == SetType.WARMUP } ?: "${++n}") }
    }
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
                        tr("Cilj: ${t.targetSets}×${t.repsMin ?: ""}${if (t.repsMax != null && t.repsMax != t.repsMin) "–${t.repsMax}" else ""}", "Target: ${t.targetSets}×${t.repsMin ?: ""}${if (t.repsMax != null && t.repsMax != t.repsMin) "–${t.repsMax}" else ""}") +
                            (t.targetKg?.let { " @ ${WorkoutMath.fmtKg(it)} kg" } ?: ""),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, tr("Opcije vežbe", "Exercise options")) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(if (exercise?.notes.isNullOrBlank()) tr("Dodaj belešku", "Add note") else tr("Izmeni belešku", "Edit note")) },
                        onClick = { menu = false; onNote() })
                    DropdownMenuItem(text = { Text(tr("Statistika vežbe", "Exercise stats")) }, onClick = { menu = false; onOpen() })
                    DropdownMenuItem(text = { Text(tr("Ukloni vežbu", "Remove exercise"), color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onRemove() })
                }
            }
        }
        exercise?.notes?.takeIf { it.isNotBlank() }?.let { note ->
            Row(
                Modifier.padding(horizontal = 16.dp).padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable(onClick = onNote)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.PushPin, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(note, style = MaterialTheme.typography.bodySmall)
            }
        }
        progression?.let { kg ->
            Row(
                Modifier.padding(horizontal = 16.dp).padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.TrendingUp, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    tr("Prošli put si ispunio sva ponavljanja: probaj ${WorkoutMath.fmtKg(kg)} kg (+${WorkoutMath.fmtKg(WorkoutMath.PROGRESSION_STEP_KG)})", "You hit all reps last time: try ${WorkoutMath.fmtKg(kg)} kg (+${WorkoutMath.fmtKg(WorkoutMath.PROGRESSION_STEP_KG)})"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp)) {
            Header("Set", Modifier.width(36.dp))
            Header(tr("Prethodno", "Previous"), Modifier.weight(1.3f))
            Header("kg", Modifier.weight(1f))
            Header(tr("Ponav.", "Reps"), Modifier.weight(1f))
            Header("✓", Modifier.width(44.dp))
        }
        sets.forEach { s ->
            androidx.compose.runtime.key(s.id) { SetRow(s, labels[s.id] ?: "${s.setIndex}", previous, target, sets, onUpdate, onDelete) }
        }
        TextButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Icon(Icons.Filled.Add, null); Text(tr("  Dodaj set", "  Add set"))
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
    label: String,
    previous: List<WorkoutSet>,
    target: RoutineExercise?,
    siblings: List<WorkoutSet>,
    onUpdate: (WorkoutSet) -> Unit,
    onDelete: (WorkoutSet) -> Unit,
) {
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    // Tekst polja zivi lokalno (bez skakanja kursora); baza se azurira na svaku promenu.
    var kg by remember(set.id) { mutableStateOf(WorkoutMath.fmtKg(set.weightKg)) }
    var reps by remember(set.id) { mutableStateOf(set.reps?.toString() ?: "") }
    var typeMenu by remember { mutableStateOf(false) }
    val prev = previous.firstOrNull { it.setIndex == set.setIndex }
    val (hintKg, hintReps) = WorkoutMath.hint(set.setIndex, previous, target, set.setType, siblings)
    val bg = if (set.done) DoneGreen.copy(alpha = 0.22f) else Color.Transparent

    Row(
        Modifier.padding(horizontal = 8.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg).padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
            val typeColor = when (set.setType) {
                SetType.WARMUP -> Color(0xFFF5B83D); SetType.DROP -> Color(0xFF5B9CFF); SetType.FAILURE -> Color(0xFFE5484D)
                else -> MaterialTheme.colorScheme.onSurface
            }
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).clickable { typeMenu = true },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (set.setType == SetType.DROP || set.setType == SetType.FAILURE) SetType.short(set.setType)!! else label,
                    textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold, color = typeColor,
                )
            }
            DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                SetType.ALL.forEach { t ->
                    DropdownMenuItem(
                        text = { Text((SetType.short(t)?.let { "$it  " } ?: "") + SetType.label(t)) },
                        trailingIcon = { if (t == set.setType) Icon(Icons.Filled.Check, null) },
                        onClick = {
                            typeMenu = false
                            onUpdate(set.copy(setType = t, weightKg = WorkoutMath.parseKg(kg), reps = WorkoutMath.parseReps(reps)))
                        },
                    )
                }
                androidx.compose.material3.HorizontalDivider()
                DropdownMenuItem(text = { Text(tr("Obriši set", "Delete set"), color = MaterialTheme.colorScheme.error) },
                    onClick = { typeMenu = false; onDelete(set) })
            }
        }
        Text(
            prev?.let { "${WorkoutMath.fmtKg(it.weightKg)} × ${it.reps ?: "-"}" } ?: "—",
            Modifier.weight(1.3f), textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
        )
        NumField(kg, WorkoutMath.fmtKg(hintKg), true, Modifier.weight(1f), next = true) {
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
                        focus.clearFocus()   // tastatura se sklanja kad se set stiklira
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
            ) { Icon(Icons.Filled.Check, tr("Urađeno", "Done"), tint = if (set.done) Color.White else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }

}


@Composable
private fun NumField(value: String, hint: String, decimal: Boolean, modifier: Modifier, next: Boolean = false, onChange: (String) -> Unit) {
    val style = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center)
    Box(
        modifier.padding(horizontal = 4.dp).height(38.dp).clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = value,
            onValueChange = { v ->
                val clean = v.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }.take(6)
                // kg do 999.x, ponavljanja do 999 (inace bi se set sacuvao bez vrednosti)
                val n = clean.replace(',', '.').toDoubleOrNull()
                if (n == null || n < 1000) onChange(clean)
            },
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            // kg -> "Dalje" prelazi na ponavljanja, ponavljanja -> "Gotovo" sklanja tastaturu
            keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
                imeAction = if (next) androidx.compose.ui.text.input.ImeAction.Next else androidx.compose.ui.text.input.ImeAction.Done),
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
