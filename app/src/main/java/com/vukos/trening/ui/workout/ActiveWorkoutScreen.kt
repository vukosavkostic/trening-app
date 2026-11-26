package com.vukos.trening.ui.workout

import com.vukos.trening.i18n.tr
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.automirrored.outlined.Assignment

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.data.Exercise
import com.vukos.trening.data.ExerciseMetric
import com.vukos.trening.data.ExerciseStats
import com.vukos.trening.data.JournalStats
import com.vukos.trening.data.PlanUpdate
import com.vukos.trening.data.RoutineExercise
import com.vukos.trening.data.SetType
import com.vukos.trening.data.TrainingStats
import com.vukos.trening.data.WorkoutMath
import com.vukos.trening.data.WorkoutRepo
import com.vukos.trening.data.WorkoutSet
import com.vukos.trening.data.muscleLabels
import com.vukos.trening.ui.body.srLong
import com.vukos.trening.ui.theme.Accent
import com.vukos.trening.ui.theme.Ds
import com.vukos.trening.ui.theme.Manrope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

private val WarmupInk = Color(0xFFF5B83D)

/**
 * Trening u toku (dizajn v2): jedna vezba u isto vreme, trake na vrhu za skok na vezbu,
 * odmor u donjoj traci (dodir otvara tajmer preko celog ekrana), "Završi" vodi na pregled treninga.
 * Sve se odmah cuva u bazu.
 */
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
    var dialog by remember { mutableStateOf<String?>(null) }
    var summary by rememberSaveable { mutableStateOf(false) }
    var restOpen by rememberSaveable { mutableStateOf(false) }
    var restSheet by remember { mutableStateOf(false) }

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
    LaunchedEffect(workout) { if (workout?.deleted == true) { RestTimer.skip(); onClose() } }

    // Nazad: sa pregleda / tajmera se vraca na logovanje, inace pitanje "Trening je u toku".
    androidx.activity.compose.BackHandler {
        when {
            restOpen -> restOpen = false
            summary -> summary = false
            else -> dialog = "leave"
        }
    }

    // Ekran ostaje upaljen tokom treninga (gasi se samo kad ga korisnik zakljuca).
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Sat za trajanje i odmor.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(1000 - now % 1000) }
    }
    // Kraj odmora: vibracija, pa traka prelazi na "Sledeće".
    LaunchedEffect(RestTimer.endsAt) {
        val end = RestTimer.endsAt
        if (end == 0L) return@LaunchedEffect
        delay((end - System.currentTimeMillis()).coerceAtLeast(0))
        if (RestTimer.endsAt == end) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            RestTimer.skip(); restOpen = false
        }
    }

    // Razlika u odnosu na plan (za "Ažuriraj plan" na pregledu).
    var planItems by remember { mutableStateOf<List<RoutineExercise>>(emptyList()) }
    LaunchedEffect(workout?.routineId, summary) {
        if (summary) workout?.routineId?.let { planItems = db.routines().getExercises(it) }
    }
    val planUpdate = remember(planItems, sets, summary) {
        workout?.routineId?.takeIf { summary }?.let { WorkoutMath.planUpdate(it, planItems, sets) }
    }
    var updatePlan by remember { mutableStateOf(false) }
    var noteFor by remember { mutableStateOf<Exercise?>(null) }

    val groups = sets.groupBy { it.exercisePosition }.toSortedMap()
    val positions = groups.keys.toList()
    var exIdx by rememberSaveable { mutableIntStateOf(0) }
    // Posle dodavanja vezbi prikazi prvu novu.
    var lastCount by rememberSaveable { mutableIntStateOf(-1) }
    LaunchedEffect(positions.size) {
        if (lastCount >= 0 && positions.size > lastCount) exIdx = lastCount
        lastCount = positions.size
    }
    val idx = exIdx.coerceIn(0, (positions.size - 1).coerceAtLeast(0))
    val curPos = positions.getOrNull(idx)
    val curSets = curPos?.let { groups[it] }.orEmpty()
    val curEx = curSets.firstOrNull()?.let { byId[it.exerciseId] }
    val curPrev = curSets.firstOrNull()?.let { previous[it.exerciseId] }.orEmpty()
    val curTarget = curSets.firstOrNull()?.let { targets[it.exerciseId] }
    val listState = rememberLazyListState()
    LaunchedEffect(idx) { listState.scrollToItem(0) }

    val elapsedSec = workout?.let { ((now - it.startedAt) / 1000).coerceAtLeast(0) } ?: 0L
    val planName = workout?.name ?: tr("Trening", "Workout")

    // Sledeci set (za traku kad nema odmora i za tajmer).
    val openSet = curSets.firstOrNull { !it.done }
    val upNext: Pair<String, String> = if (openSet != null) {
        val (hk, hr) = WorkoutMath.hint(openSet.setIndex, curPrev, curTarget, openSet.setType, curSets)
        val k = openSet.weightKg ?: hk
        val r = openSet.reps ?: hr
        tr("Set ${curSets.indexOf(openSet) + 1} od ${curSets.size}", "Set ${curSets.indexOf(openSet) + 1} of ${curSets.size}") to
            listOfNotNull(k?.let { "${WorkoutMath.fmtKg(it)} kg" }, r?.toString()).joinToString(" × ").ifEmpty { "—" }
    } else {
        val nextEx = positions.getOrNull(idx + 1)?.let { p -> groups[p]?.firstOrNull()?.let { byId[it.exerciseId]?.name } }
        tr("sledeća vežba", "next exercise") to (nextEx ?: tr("Završi", "Finish"))
    }

    Box(Modifier.fillMaxSize().background(Ds.bg)) {
        if (summary) {
            SummaryContent(
                name = planName, elapsedSec = elapsedSec, sets = sets, byId = byId, workoutId = workoutId,
                startedAt = workout?.startedAt ?: now, planUpdate = planUpdate, updatePlan = updatePlan,
                onTogglePlan = { updatePlan = !updatePlan }, onClose = { summary = false },
                onSave = {
                    val upd = planUpdate.takeIf { updatePlan }
                    val rid = workout?.routineId
                    scope.launch {
                        if (WorkoutRepo.finish(workoutId)) {
                            if (upd != null && rid != null) WorkoutRepo.applyPlanUpdate(rid, upd)
                            RestTimer.skip()
                            onFinished(workoutId)
                        }
                    }
                },
                onDiscard = { dialog = "discard" },
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize().statusBarsPadding().imePadding(),
                state = listState,
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                        RoundIcon(Icons.Filled.KeyboardArrowDown, tr("Nazad", "Back"), Ds.s1) { dialog = "leave" }
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(Accent))
                            Text(hms(elapsedSec), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink)
                        }
                        Text(tr("Završi", "Finish"), Modifier.clip(RoundedCornerShape(8.dp)).clickable { summary = true }.padding(4.dp),
                            fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText)
                    }
                }
                item {
                    // Jedna traka po vezbi: trenutna narandzasta, zavrsena bleda narandzasta.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (if (positions.isEmpty()) listOf(-1) else positions).forEachIndexed { i, p ->
                            val all = p >= 0 && groups[p].orEmpty().let { g -> g.isNotEmpty() && g.all { it.done } }
                            Box(
                                Modifier.weight(1f).height(14.dp).clickable(enabled = p >= 0) { exIdx = i },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(
                                    when { i == idx && p >= 0 -> Accent; all -> Accent.copy(alpha = 0.45f); else -> Ds.s3 }
                                ))
                            }
                        }
                    }
                }
                if (curPos == null) item {
                    Text(
                        tr("Još nema vežbi.\nDodaj prvu i loguj usput.", "No exercises yet.\nAdd the first one and log as you go."),
                        Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 8.dp), textAlign = TextAlign.Center,
                        fontSize = 15.sp, lineHeight = 22.sp, color = Ds.sub,
                    )
                } else {
                    item(key = "head-$curPos") {
                        ExerciseHeader(
                            curEx, idx, positions.size, planName, curPrev, curTarget,
                            onOpen = { curEx?.let { onOpenExercise(it.id) } },
                            onNote = { noteFor = curEx },
                            onRemove = { scope.launch { WorkoutRepo.removeExercise(workoutId, curPos) } },
                        )
                    }
                    item(key = "sets-$curPos") {
                        SetTable(
                            curSets, curPrev, curTarget, curEx?.trackType ?: com.vukos.trening.data.TrackType.WEIGHT,
                            onUpdate = { s -> scope.launch { WorkoutRepo.updateSet(s) } },
                            onDelete = { s -> scope.launch { WorkoutRepo.deleteSet(s) } },
                            onChecked = { RestTimer.start() },
                            onAddSet = { scope.launch { WorkoutRepo.addSet(workoutId, curSets.first().exerciseId, curPos) } },
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NavButton(tr("Prethodna", "Previous"), true, idx > 0, Modifier.weight(1f)) { exIdx = idx - 1 }
                            NavButton(tr("Sledeća vežba", "Next exercise"), false, idx < positions.size - 1, Modifier.weight(1f)) { exIdx = idx + 1 }
                        }
                    }
                }
                item {
                    Box(
                        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(Ds.s1).clickable(onClick = onPickExercises),
                        contentAlignment = Alignment.Center,
                    ) { Text(tr("+ Dodaj vežbu", "+ Add exercise"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText) }
                }
            }

            RestBar(
                now = now, upNextValue = upNext.second,
                onOpen = { if (RestTimer.active()) restOpen = true },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }

        AnimatedVisibility(
            visible = restOpen && !summary,
            enter = fadeIn(tween(250)) + slideInVertically(tween(350)) { it / 20 },
            exit = fadeOut(tween(250)) + slideOutVertically(tween(350)) { it / 20 },
        ) {
            RestFullScreen(
                now = now, title = curEx?.name ?: planName, elapsedSec = elapsedSec, upNext = upNext,
                onClose = { restOpen = false }, onDefault = { restSheet = true },
            )
        }
    }

    if (restSheet) {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { restSheet = false }, sheetState = state, containerColor = Ds.s2) {
            RestDefaultSheet { RestTimer.setDefault(it); restSheet = false }
        }
    }

    if (dialog == "leave") {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { dialog = null }, sheetState = state, containerColor = Ds.s2) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 34.dp)) {
                Text(tr("Trening je u toku", "Workout in progress"), fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.44).sp, color = Ds.ink)
                Text(
                    tr("Sve što si uneo je sačuvano. Završi trening kad budeš gotov, ili ga odbaci.", "Everything you entered is saved. Finish the workout when you are done, or discard it."),
                    Modifier.padding(top = 8.dp), fontSize = 15.sp, lineHeight = 22.sp, color = Ds.sub,
                )
                Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigButton(tr("Nastavi", "Continue"), Accent, Color.White) { dialog = null }
                    BigButton(tr("Završi trening", "Finish workout"), Ds.s1, Ds.ink) { dialog = null; summary = true }
                    Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).clickable { dialog = "discard" }, contentAlignment = Alignment.Center) {
                        Text(tr("Odbaci trening", "Discard workout"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ds.danger)
                    }
                }
            }
        }
    }
    if (dialog == "discard") AlertDialog(
        onDismissRequest = { dialog = null },
        title = { Text(tr("Odbaci trening?", "Discard workout?")) },
        text = { Text(tr("Svi uneti setovi ovog treninga se brišu.", "All sets of this workout will be deleted.")) },
        confirmButton = {
            TextButton(onClick = { dialog = null; RestTimer.skip(); scope.launch { WorkoutRepo.discard(workoutId); onClose() } }) {
                Text(tr("Odbaci", "Discard"), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { dialog = null }) { Text(tr("Nazad", "Back")) } },
    )
    noteFor?.let { ex ->
        NoteDialog(ex, onDismiss = { noteFor = null }) { note ->
            noteFor = null
            scope.launch { WorkoutRepo.saveExerciseNote(ex.id, note) }
        }
    }
}

private fun hms(sec: Long): String =
    if (sec >= 3600) "%d:%02d:%02d".format(sec / 3600, sec % 3600 / 60, sec % 60) else "%d:%02d".format(sec / 60, sec % 60)

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, bg: Color, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, Modifier.size(20.dp), tint = Ds.ink)
    }
}

@Composable
private fun BigButton(text: String, bg: Color, ink: Color, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = ink)
    }
}

@Composable
private fun NavButton(text: String, back: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val ink = if (enabled) Ds.ink else Ds.faint
    Row(
        modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).background(Ds.s1).clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
    ) {
        if (back) Icon(Icons.Filled.ChevronLeft, null, Modifier.size(16.dp), tint = ink)
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ink, maxLines = 1)
        if (!back) Icon(Icons.Filled.ChevronRight, null, Modifier.size(16.dp), tint = ink)
    }
}

@Composable
private fun ExerciseHeader(
    exercise: Exercise?, idx: Int, count: Int, planName: String, previous: List<WorkoutSet>, target: RoutineExercise?,
    onOpen: () -> Unit, onNote: () -> Unit, onRemove: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val progression = WorkoutMath.progression(previous, target)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.vukos.trening.ui.common.IconText(Icons.AutoMirrored.Outlined.Assignment, planName, Ds.sub, Modifier.weight(1f), 13.sp, 13.dp)
                Row {
                    Text("${idx + 1}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink)
                    Text(tr(" od $count", " of $count"), fontSize = 13.sp, color = Ds.sub)
                }
            }
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                val name = exercise?.name ?: "?"
                val fs = if (name.length > 22) 23 else 28
                Text(name, Modifier.weight(1f).clickable(onClick = onOpen), fontSize = fs.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = (fs * -0.02f).sp, lineHeight = (fs * 1.12f).sp,
                    style = TextStyle(lineBreak = androidx.compose.ui.text.style.LineBreak.Heading), color = Ds.ink)
                Box {
                    Icon(Icons.Filled.MoreHoriz, tr("Opcije vežbe", "Exercise options"),
                        Modifier.padding(start = 8.dp).size(32.dp).clip(CircleShape).clickable { menu = true }.padding(6.dp), tint = Ds.sub)
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(if (exercise?.notes.isNullOrBlank()) tr("Dodaj belešku", "Add note") else tr("Izmeni belešku", "Edit note")) },
                            onClick = { menu = false; onNote() })
                        DropdownMenuItem(text = { Text(tr("Statistika vežbe", "Exercise stats")) }, onClick = { menu = false; onOpen() })
                        DropdownMenuItem(text = { Text(tr("Ukloni vežbu", "Remove exercise"), color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onRemove() })
                    }
                }
            }
            target?.let { t ->
                val reps = "${t.repsMin ?: ""}${if (t.repsMax != null && t.repsMax != t.repsMin) "–${t.repsMax}" else ""}"
                Text(
                    tr("Cilj ${t.targetSets} × $reps", "Target ${t.targetSets} × $reps") + (t.targetKg?.let { " @ ${WorkoutMath.fmtKg(it)} kg" } ?: ""),
                    fontSize = 14.sp, color = Ds.sub,
                )
            }
        }
        exercise?.notes?.takeIf { it.isNotBlank() }?.let { note ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Ds.s1).clickable(onClick = onNote).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.PushPin, null, Modifier.size(16.dp), tint = Ds.sub)
                Text(note, fontSize = 14.sp, color = Ds.ink)
            }
        }
        progression?.let { kg ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Ds.aSoft).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, Modifier.size(17.dp), tint = Accent)
                Text(
                    tr("Prošli put si ispunio sva ponavljanja: probaj ${WorkoutMath.fmtKg(kg)} kg (+${WorkoutMath.fmtKg(WorkoutMath.PROGRESSION_STEP_KG)})", "You hit all reps last time: try ${WorkoutMath.fmtKg(kg)} kg (+${WorkoutMath.fmtKg(WorkoutMath.PROGRESSION_STEP_KG)})"),
                    fontSize = 14.sp, lineHeight = 19.sp, color = Ds.aOn,
                )
            }
        }
    }
}

private val ColSet = 36.dp
private val ColKg = 70.dp
private val ColReps = 56.dp
private val ColCheck = 44.dp

@Composable
private fun SetTable(
    sets: List<WorkoutSet>, previous: List<WorkoutSet>, target: RoutineExercise?, track: String,
    onUpdate: (WorkoutSet) -> Unit, onDelete: (WorkoutSet) -> Unit, onChecked: () -> Unit, onAddSet: () -> Unit,
) {
    val withKg = track == com.vukos.trening.data.TrackType.WEIGHT
    // Oznaka seta: W za zagrevanje, inace redni broj medju radnim setovima.
    val labels = remember(sets) {
        var n = 0
        sets.associate { s -> s.id to (SetType.short(s.setType)?.takeIf { s.setType == SetType.WARMUP } ?: "${++n}") }
    }
    val firstOpen = sets.firstOrNull { !it.done }?.id
    Column {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 6.dp)) {
            listOfNotNull(
                tr("SET", "SET") to Modifier.width(ColSet), tr("PRETHODNO", "PREVIOUS") to Modifier.weight(1f),
                ("KG" to Modifier.width(ColKg)).takeIf { withKg },
                (if (track == com.vukos.trening.data.TrackType.TIME) tr("SEK.", "SEC") else tr("PONAV.", "REPS")) to Modifier.width(ColReps),
                "" to Modifier.width(ColCheck),
            ).forEach { (t, m) -> Text(t, m, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.24.sp, color = Ds.faint) }
        }
        sets.forEach { s ->
            androidx.compose.runtime.key(s.id) {
                SetRow(s, labels[s.id] ?: "${s.setIndex}", s.id == firstOpen, previous, target, sets, track, onUpdate, onDelete, onChecked)
            }
        }
        Text(tr("+ Dodaj set", "+ Add set"), Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onAddSet).padding(12.dp),
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText)
    }
}

@Composable
private fun SetRow(
    set: WorkoutSet,
    label: String,
    current: Boolean,
    previous: List<WorkoutSet>,
    target: RoutineExercise?,
    siblings: List<WorkoutSet>,
    track: String,
    onUpdate: (WorkoutSet) -> Unit,
    onDelete: (WorkoutSet) -> Unit,
    onChecked: () -> Unit,
) {
    val withKg = track == com.vukos.trening.data.TrackType.WEIGHT
    val unit = if (track == com.vukos.trening.data.TrackType.TIME) " s" else ""
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    // Tekst polja zivi lokalno (bez skakanja kursora); baza se azurira na svaku promenu.
    var kg by remember(set.id) { mutableStateOf(WorkoutMath.fmtKg(set.weightKg)) }
    var reps by remember(set.id) { mutableStateOf(set.reps?.toString() ?: "") }
    var typeMenu by remember { mutableStateOf(false) }
    val prev = previous.firstOrNull { it.setIndex == set.setIndex }
    val (hintKg, hintReps) = WorkoutMath.hint(set.setIndex, previous, target, set.setType, siblings)
    val ink = if (set.done) Ds.sub else Ds.ink

    Row(
        Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(16.dp)).background(if (current) Ds.s1 else Color.Transparent)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(ColSet)) {
            val typeColor = when (set.setType) {
                SetType.WARMUP -> WarmupInk; SetType.DROP -> Color(0xFF5B9CFF); SetType.FAILURE -> Ds.danger
                else -> Ds.sub
            }
            Text(
                if (set.setType == SetType.DROP || set.setType == SetType.FAILURE) SetType.short(set.setType)!! else label,
                Modifier.clip(RoundedCornerShape(8.dp)).clickable { typeMenu = true }.padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
                fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = typeColor,
            )
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
                HorizontalDivider()
                DropdownMenuItem(text = { Text(tr("Obriši set", "Delete set"), color = MaterialTheme.colorScheme.error) },
                    onClick = { typeMenu = false; onDelete(set) })
            }
        }
        Text(
            prev?.let { if (withKg) "${WorkoutMath.fmtKg(it.weightKg)} × ${it.reps ?: "-"}" else "${it.reps ?: "-"}$unit" } ?: "—",
            Modifier.weight(1f), fontSize = 15.sp, color = Ds.faint, maxLines = 1,
        )
        if (withKg) NumField(kg, WorkoutMath.fmtKg(hintKg), true, ink, Modifier.width(ColKg), next = true) {
            kg = it; onUpdate(set.copy(weightKg = WorkoutMath.parseKg(it), reps = WorkoutMath.parseReps(reps)))
        }
        NumField(reps, hintReps?.toString() ?: "", false, ink, Modifier.width(ColReps)) {
            reps = it; onUpdate(set.copy(weightKg = WorkoutMath.parseKg(kg), reps = WorkoutMath.parseReps(it)))
        }
        Box(Modifier.width(ColCheck), contentAlignment = Alignment.CenterEnd) {
            Box(
                Modifier.size(30.dp).clip(CircleShape)
                    .background(if (set.done) Accent else Color.Transparent)
                    .then(if (set.done) Modifier else Modifier.border(1.5.dp, Ds.faint, CircleShape))
                    .clickable {
                        focus.clearFocus()   // tastatura se sklanja kad se set stiklira
                        if (set.done) onUpdate(set.copy(weightKg = WorkoutMath.parseKg(kg), reps = WorkoutMath.parseReps(reps), done = false))
                        else {
                            // Prazno polje = prihvata se predlog (prethodno / cilj).
                            val k = if (withKg) WorkoutMath.parseKg(kg) ?: hintKg else null
                            val r = WorkoutMath.parseReps(reps) ?: hintReps
                            if (r != null) {
                                kg = WorkoutMath.fmtKg(k); reps = r.toString()
                                onUpdate(set.copy(weightKg = k, reps = r, done = true))
                                onChecked()
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) { if (set.done) Icon(Icons.Filled.Check, tr("Urađeno", "Done"), Modifier.size(16.dp), tint = Color.White) }
        }
    }
}

@Composable
private fun NumField(value: String, hint: String, decimal: Boolean, ink: Color, modifier: Modifier, next: Boolean = false, onChange: (String) -> Unit) {
    val style = TextStyle(fontFamily = Manrope, color = ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
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
        cursorBrush = SolidColor(Accent),
        // kg -> "Dalje" prelazi na ponavljanja, ponavljanja -> "Gotovo" sklanja tastaturu
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = if (next) ImeAction.Next else ImeAction.Done),
        modifier = modifier.padding(end = 8.dp),
        decorationBox = { inner ->
            Box(Modifier.height(40.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(hint.ifEmpty { "—" }, style = style.copy(color = Ds.faint))
                inner()
            }
        },
    )
}

/** Donja traka: odmor koji tece (dodir otvara tajmer), ili sledeci set. */
@Composable
private fun RestBar(now: Long, upNextValue: String, onOpen: () -> Unit, modifier: Modifier) {
    val resting = RestTimer.active(now)
    val rem = RestTimer.remainingSec(now)
    val frac = if (resting && RestTimer.totalSec > 0) rem.toFloat() / RestTimer.totalSec else 0f
    val animFrac by animateFloatAsState(frac, tween(1000), label = "rest")
    Box(
        modifier.fillMaxWidth().height(64.dp).shadow(16.dp, RoundedCornerShape(32.dp)).clip(RoundedCornerShape(32.dp)).background(Ds.s2),
    ) {
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(animFrac).height(3.dp).background(Accent))
        Row(Modifier.fillMaxSize().padding(start = 24.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).fillMaxHeight().clickable(remember { MutableInteractionSource() }, indication = null, onClick = onOpen),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(tr("Odmor", "Rest"), fontSize = 13.sp, color = Ds.sub)
                Text(RestTimer.mmss(if (resting) rem else RestTimer.defaultSec), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("+15s", Ds.s3, Ds.ink) { RestTimer.add(15) }
                Pill(if (resting) tr("Preskoči", "Skip") else tr("Kreni", "Start"), Ds.ink, Ds.bg) {
                    if (resting) RestTimer.skip() else RestTimer.start()
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String, bg: Color, ink: Color, onClick: () -> Unit) {
    Box(Modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ink)
    }
}

/** Tajmer odmora preko celog ekrana. */
@Composable
private fun RestFullScreen(now: Long, title: String, elapsedSec: Long, upNext: Pair<String, String>, onClose: () -> Unit, onDefault: () -> Unit) {
    val rem = RestTimer.remainingSec(now)
    val total = RestTimer.totalSec.coerceAtLeast(1)
    val frac by animateFloatAsState(rem.toFloat() / total, tween(1000), label = "ring")
    val track = Ds.s2
    Box(
        Modifier.fillMaxSize().background(Ds.bg).clickable(remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp).height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(Icons.Filled.KeyboardArrowDown, tr("Zatvori", "Close"), Ds.s1, onClose)
                Text(title, Modifier.weight(1f).padding(horizontal = 8.dp), fontSize = 15.sp, color = Ds.sub, textAlign = TextAlign.Center,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(hms(elapsedSec), Modifier.width(52.dp), fontSize = 15.sp, color = Ds.sub, textAlign = TextAlign.End)
            }
            Box(Modifier.padding(top = 64.dp).size(300.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = 6.dp.toPx()
                    val inset = w / 2 + 4.dp.toPx()
                    val sz = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
                    val tl = Offset(inset, inset)
                    drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(w))
                    if (frac > 0f) drawArc(Accent, -90f, 360f * frac, false, tl, sz, style = Stroke(w, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tr("ODMOR", "REST"), fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.78.sp, color = Ds.sub)
                    Text(RestTimer.mmss(rem), fontSize = 84.sp, fontWeight = FontWeight.Light, letterSpacing = (-3.36).sp, lineHeight = 88.sp, color = Ds.ink)
                    Text(tr("od ${RestTimer.mmss(total)}", "of ${RestTimer.mmss(total)}"), fontSize = 15.sp, color = Ds.faint)
                    Row(
                        Modifier.padding(top = 10.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onDefault).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(tr("Podrazumevano ${RestTimer.mmss(RestTimer.defaultSec)}", "Default ${RestTimer.mmss(RestTimer.defaultSec)}"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText)
                        Icon(Icons.Outlined.Edit, tr("Promeni", "Change"), Modifier.size(13.dp), tint = Ds.aText)
                    }
                }
            }
            Row(Modifier.padding(top = 44.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleBtn("−15") { RestTimer.subtract(15) }
                Box(
                    Modifier.height(68.dp).clip(RoundedCornerShape(34.dp)).background(Accent).clickable { RestTimer.skip(); onClose() }.padding(horizontal = 40.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(tr("Preskoči odmor", "Skip rest"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
                CircleBtn("+15") { RestTimer.add(15) }
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Ds.s1)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Sledeće", "Up next"), fontSize = 13.sp, color = Ds.sub, maxLines = 1)
                    Text(upNext.first, fontSize = 13.sp, color = Ds.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(upNext.second, Modifier.padding(top = 4.dp), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(tr("Izmeni", "Edit"), Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClose).padding(4.dp),
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText)
        }
    }
}

@Composable
private fun CircleBtn(text: String, onClick: () -> Unit) {
    Box(Modifier.size(68.dp).clip(CircleShape).background(Ds.s1).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink)
    }
}

/** Izbor podrazumevanog odmora (isti list kao u Podešavanjima). */
@Composable
fun RestDefaultSheet(onPick: (Int) -> Unit) {
    val cur = RestTimer.defaultSec
    Column(Modifier.padding(bottom = 34.dp)) {
        Text(tr("Podrazumevani odmor", "Default rest"), Modifier.fillMaxWidth().padding(top = 8.dp, start = 24.dp, end = 24.dp, bottom = 4.dp),
            fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink, textAlign = TextAlign.Center)
        Text(tr("Pokreće se sam kad stikliraš set.", "Starts automatically when you check off a set."),
            Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, bottom = 16.dp), fontSize = 14.sp, lineHeight = 20.sp, color = Ds.sub, textAlign = TextAlign.Center)
        Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp)).background(Ds.s1)) {
            RestTimer.OPTIONS.forEachIndexed { i, v ->
                Row(Modifier.fillMaxWidth().height(52.dp).clickable { onPick(v) }.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(RestTimer.mmss(v), Modifier.weight(1f), fontSize = 16.sp, color = Ds.ink)
                    if (v == cur) Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = Accent)
                }
                if (i < RestTimer.OPTIONS.size - 1) HorizontalDivider(thickness = 1.dp, color = Ds.line)
            }
        }
    }
}

/** Pregled posle "Završi": rekord, brojke, misici, azuriranje plana, pa cuvanje u Dnevnik. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryContent(
    name: String, elapsedSec: Long, sets: List<WorkoutSet>, byId: Map<String, Exercise>, workoutId: String, startedAt: Long,
    planUpdate: PlanUpdate?, updatePlan: Boolean, onTogglePlan: () -> Unit, onClose: () -> Unit, onSave: () -> Unit, onDiscard: () -> Unit,
) {
    val db = TreningApp.instance.db
    val allWorkouts by remember { db.workouts().observeFinished() }.collectAsStateWithLifecycle(emptyList())
    val allSets by remember { db.workouts().observeAllDoneSets() }.collectAsStateWithLifecycle(emptyList())
    val logged = sets.filter(WorkoutMath::isLogged)
    val working = logged.filter(WorkoutMath::isWorking)
    val done = logged.size
    val empty = sets.size - done
    // Rekord: trenutni trening se racuna kao da je zavrsen.
    val pr = remember(allWorkouts, allSets, sets) {
        val me = com.vukos.trening.data.Workout(id = workoutId, name = name, startedAt = startedAt, finishedAt = System.currentTimeMillis())
        ExerciseStats.newRecords(workoutId, allWorkouts.filter { it.id != workoutId } + me, allSets.filter { it.workoutId != workoutId } + logged.map { it.copy(done = true) })
            .firstOrNull { it.kind == ExerciseMetric.HEAVIEST.label }
    }
    val goal = remember { TreningApp.instance.settings.weeklyGoal }
    val today = LocalDate.now()
    val weekDone = allWorkouts.count { TrainingStats.weekStart(JournalStats.dateOf(it)) == TrainingStats.weekStart(today) && it.id != workoutId } + 1
    val vol = WorkoutMath.volume(logged)
    val muscles = logged.mapNotNull { byId[it.exerciseId]?.primaryMuscle?.takeIf { m -> m.isNotBlank() } }.distinct()
        .map { m -> (muscleLabels[m] ?: m).replaceFirstChar { it.uppercase() } }

    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier.fillMaxSize().drawBehind {
            // Narandzasti sjaj iza naslova (samo se crta, ne zauzima mesto).
            drawCircle(Brush.radialGradient(listOf(Accent.copy(alpha = 0.22f), Color.Transparent), center = Offset(size.width / 2, 30.dp.toPx()), radius = 260.dp.toPx()),
                radius = 260.dp.toPx(), center = Offset(size.width / 2, 30.dp.toPx()))
        }.statusBarsPadding().navigationBarsPadding(),
    ) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.height(40.dp).fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Box(
                    Modifier.offset(x = 8.dp).size(40.dp).clip(CircleShape).background(Color(0x29808080)).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Close, tr("Nazad na trening", "Back to workout"), Modifier.size(18.dp), tint = Ds.ink) }
            }
            com.vukos.trening.ui.common.Kicker(tr("Trening završen", "Session complete"), Ds.aText, Modifier.padding(top = 12.dp))
            val fs = com.vukos.trening.ui.common.titleSize(name, 40)
            Text(name, Modifier.padding(top = 8.dp), fontSize = fs, fontWeight = FontWeight.Bold, letterSpacing = (fs.value * -0.03f).sp,
                lineHeight = (fs.value * 1.08f).sp, style = TextStyle(lineBreak = androidx.compose.ui.text.style.LineBreak.Heading), color = Ds.ink)
            FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                com.vukos.trening.ui.common.IconText(Icons.Outlined.CalendarToday, today.srLong().replaceFirstChar { it.uppercase() }, Ds.sub, fontSize = 15.sp)
                com.vukos.trening.ui.common.IconText(Icons.Filled.Schedule, "${(elapsedSec / 60).coerceAtLeast(1)} min", Ds.sub, fontSize = 15.sp)
            }
        }
        pr?.let { hit ->
            run {
                val best = logged.filter { it.exerciseId == hit.exerciseId && it.setType != SetType.WARMUP }.maxByOrNull { it.weightKg ?: 0.0 }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Ds.s1)
                        .border(1.dp, Accent.copy(alpha = 0.3f), RoundedCornerShape(28.dp)).padding(22.dp),
                ) {
                    Text(tr("Novi lični rekord", "New personal record"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText)
                    Text(byId[hit.exerciseId]?.name ?: "?", Modifier.padding(top = 10.dp), fontSize = 17.sp, color = Ds.ink)
                    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(WorkoutMath.fmtKg(hit.value), fontSize = 44.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.32).sp, color = Ds.ink)
                        Text("kg × ${best?.reps ?: "-"}", Modifier.padding(bottom = 8.dp), fontSize = 17.sp, color = Ds.sub)
                        Spacer(Modifier.weight(1f))
                        Text("+${WorkoutMath.fmtKg(hit.value - hit.previous)} kg", Modifier.padding(bottom = 8.dp), fontSize = 14.sp, color = Ds.sub)
                    }
                }
            }
        }
        run {
            val tiles = listOf(
                (if (vol >= 1000) "${WorkoutMath.fmtKg(Math.round(vol / 100.0) / 10.0)} t" else "${WorkoutMath.fmtKg(vol)} kg") to tr("volumen", "volume"),
                "${working.size}" to tr("radnih setova", "working sets"),
                "${working.sumOf { it.reps ?: 0 }}" to tr("ponavljanja", "reps"),
                "${weekDone.coerceAtMost(goal.coerceAtLeast(1))}/$goal" to tr("nedeljni cilj", "weekly goal"),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                tiles.chunked(2).forEach { rowTiles ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowTiles.forEach { (v, l) ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(Ds.s1).padding(horizontal = 18.dp, vertical = 14.dp)) {
                                Text(v, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.52).sp, color = Ds.ink, maxLines = 1)
                                Text(l, Modifier.padding(top = 2.dp), fontSize = 14.sp, color = Ds.sub)
                            }
                        }
                    }
                }
            }
        }
        if (muscles.isNotEmpty()) run {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                muscles.forEach { m ->
                    Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(Ds.s1).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                        Text(m, fontSize = 14.sp, color = Ds.sub)
                    }
                }
            }
        }
        if (done > 0 && planUpdate != null) run {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Ds.s1).clickable(onClick = onTogglePlan).padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Ažuriraj plan „$name“", "Update plan “$name”"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    planChanges(planUpdate, byId).forEach {
                        Text(it, Modifier.padding(top = 2.dp), fontSize = 13.sp, lineHeight = 18.sp, color = Ds.sub)
                    }
                }
                IosSwitch(updatePlan)
            }
        }
        val note = when {
            done == 0 -> tr("Nema unetih setova. Upiši kilažu i ponavljanja bar za jedan set, ili odbaci trening.", "No sets logged. Enter weight and reps for at least one set, or discard the workout.")
            empty > 0 -> tr("Prazni setovi ($empty) se ne čuvaju.", "Empty sets ($empty) are not saved.")
            else -> null
        }
        note?.let { Text(it, Modifier.padding(horizontal = 4.dp), fontSize = 13.sp, lineHeight = 18.sp, color = Ds.sub) }
        // Dugme je uvek na dnu ekrana; skrol samo ako sadrzaj ne stane.
        Spacer(Modifier.weight(1f))
        run {
            if (done == 0) BigButton(tr("Odbaci trening", "Discard workout"), Ds.danger, Color.White, onDiscard)
            else BigButton(tr("Sačuvaj u dnevnik", "Save to journal"), Accent, Color.White, onSave)
        }
    }
}
}

/** Prekidac u stilu dizajna (51×31). */
@Composable
fun IosSwitch(on: Boolean) {
    val x by androidx.compose.animation.core.animateDpAsState(if (on) 20.dp else 0.dp, tween(200), label = "sw")
    Box(Modifier.size(51.dp, 31.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Accent else Ds.s3).padding(2.dp)) {
        Box(Modifier.offset(x = x).size(27.dp).shadow(2.dp, CircleShape).clip(CircleShape).background(Color.White))
    }
}

/** Opis izmena plana za pregled treninga. */
private fun planChanges(u: PlanUpdate, byId: Map<String, Exercise>): List<String> = buildList {
    fun n(id: String) = byId[id]?.name ?: "?"
    u.added.forEach { add("+ ${n(it)}") }
    u.removed.forEach { add("− ${n(it)}") }
    u.setChanges.forEach { (id, a, b) -> add(tr("${n(id)}: $a → $b setova", "${n(id)}: $a → $b sets")) }
    if (u.reordered) add(tr("novi redosled vežbi", "new exercise order"))
    add(tr("Kilaže iz ovog treninga postaju cilj", "Weights from this workout become the target"))
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
