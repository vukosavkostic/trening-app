package com.vukos.trening.ui.routines

import com.vukos.trening.i18n.tr
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Repeat

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.data.JournalStats
import com.vukos.trening.data.Rotation
import com.vukos.trening.data.Routine
import com.vukos.trening.data.RoutineExercise
import com.vukos.trening.data.TrainingStats
import com.vukos.trening.ui.body.srRelative
import com.vukos.trening.ui.common.EmptyState
import com.vukos.trening.ui.common.OutlinePill
import com.vukos.trening.ui.common.PrimaryPill
import com.vukos.trening.ui.journal.RoundButton
import com.vukos.trening.ui.theme.Accent
import com.vukos.trening.ui.theme.Ds
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.IsoFields

private val PlanRowH = 76.dp

/**
 * Tab Trening (dizajn v2): "Sledeći" po rotaciji, ova nedelja / niz, slobodan trening,
 * pa "Moji planovi" - drzi i prevuci da promenis redosled (redosled je rotacija).
 */
@Composable
fun RoutinesScreen(
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onNotion: () -> Unit,
    onSettings: () -> Unit,
    onStart: (String?) -> Unit,
    onOpenStats: () -> Unit,
) {
    val db = TreningApp.instance.db
    val scope = rememberCoroutineScope()
    val routines by remember { db.routines().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val links by remember { db.routines().observeAllExercises() }.collectAsStateWithLifecycle(emptyList())
    val exercises by remember { db.exercises().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val workouts by remember { db.workouts().observeFinished() }.collectAsStateWithLifecycle(emptyList())
    val names = remember(exercises) { exercises.associate { it.id to it.name } }
    val byRoutine = remember(links) { links.groupBy { it.routineId } }
    // Kad je koji plan poslednji put odradjen.
    val lastDone = remember(workouts) {
        workouts.filter { it.routineId != null }.groupBy { it.routineId!! }
            .mapValues { (_, ws) -> ws.maxOf { it.startedAt } }
    }
    val rotation = remember(routines, workouts) { Rotation.next(routines, workouts) }

    val goal = remember { TreningApp.instance.settings.weeklyGoal }
    val today = LocalDate.now()
    val week = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    val dates = remember(workouts) { workouts.map { JournalStats.dateOf(it) } }
    val thisWeek = dates.count { TrainingStats.weekStart(it) == TrainingStats.weekStart(today) }
    val streak = remember(dates, goal) { TrainingStats.streak(dates, goal, today) }

    fun saveOrder(ordered: List<Routine>) {
        val now = System.currentTimeMillis()
        scope.launch {
            ordered.forEachIndexed { i, r -> if (r.sortOrder != i) db.routines().upsert(r.copy(sortOrder = i, updatedAt = now, dirty = true)) }
        }
    }

    Scaffold(containerColor = Ds.bg) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Trening", "Train"), fontSize = 34.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.68).sp, color = Ds.ink)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoundButton(Icons.Filled.CloudDownload, tr("Planovi iz Notiona", "Plans from Notion"), Ds.s1, Ds.ink, 40.dp, onNotion)
                        RoundButton(Icons.Filled.Settings, tr("Podešavanja", "Settings"), Ds.s1, Ds.ink, 40.dp, onSettings)
                    }
                }
            }
            rotation.next?.let { next ->
                item {
                    val ex = byRoutine[next.id].orEmpty()
                    val past = workouts.filter { it.routineId == next.id && it.finishedAt != null }
                    val avgMin = past.takeIf { it.isNotEmpty() }?.map { (it.finishedAt!! - it.startedAt) / 60_000 }?.average()?.toInt()
                    UpNextCard(next, rotation.last, ex, names, avgMin, onStart = { if (ex.isEmpty()) onOpen(next.id) else onStart(next.id) })
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(Ds.s1).clickable(onClick = onOpenStats).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(tr("Ova nedelja", "This week"), fontSize = 13.sp, color = Ds.sub)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$thisWeek", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.52).sp, color = Ds.ink)
                            Text("/$goal", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.52).sp, color = Ds.faint)
                        }
                        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(goal.coerceAtLeast(1)) { i ->
                                Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (i < thisWeek) Accent else Ds.s3))
                            }
                        }
                    }
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(Ds.s1).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(tr("Niz", "Streak"), fontSize = 13.sp, color = Ds.sub)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$streak", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.52).sp, color = Ds.ink)
                            Text(
                                tr(" nedelja", " ${if (streak == 1) "week" else "weeks"}"),
                                Modifier.padding(bottom = 3.dp), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ds.sub,
                            )
                        }
                        Text(tr("zaredom sa ispunjenim ciljem", "on goal in a row"), fontSize = 13.sp, color = Ds.sub)
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Ds.s1).clickable { onStart(null) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Slobodan trening", "Free workout"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink)
                        Text(tr("Bez plana, vežbe dodaješ usput", "No plan, add exercises as you go"), fontSize = 13.sp, color = Ds.sub)
                    }
                    Icon(Icons.Filled.ChevronRight, null, Modifier.size(16.dp), tint = Ds.faint)
                }
            }
            item {
                Column {
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                        Text(tr("Moji planovi", "My plans"), Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.44).sp, color = Ds.ink)
                        Text(tr("Novi plan", "New plan"), Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onNew).padding(4.dp),
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText)
                    }
                    if (routines.size > 1) Text(
                        tr("Drži plan i prevuci da promeniš redosled. Redosled je tvoja rotacija.", "Hold a plan and drag to reorder. The order sets your rotation."),
                        Modifier.padding(top = 8.dp), fontSize = 13.sp, color = Ds.sub,
                    )
                }
            }
            if (routines.isEmpty()) item {
                EmptyState(
                    Icons.Filled.FitnessCenter, tr("Još nemaš planova", "No plans yet"),
                    tr("Napravi plan sa vežbama koje radiš, ili ga uvezi iz Notiona. Plan možeš da započneš bilo kog dana.", "Create a plan with the exercises you do, or import one from Notion. You can start a plan on any day."),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryPill(tr("Napravi plan", "Create plan"), onNew, Modifier.fillMaxWidth(), Icons.Filled.Add)
                        OutlinePill(tr("Uvezi iz Notiona", "Import from Notion"), onNotion, Modifier.fillMaxWidth(), Icons.Filled.CloudDownload)
                    }
                }
            } else item {
                PlanList(
                    routines, byRoutine, lastDone,
                    onStart = { r -> if (byRoutine[r.id].isNullOrEmpty()) onOpen(r.id) else onStart(r.id) },
                    onEdit = { onOpen(it.id) }, onReorder = ::saveOrder,
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun UpNextCard(
    next: Routine, last: Routine?, ex: List<RoutineExercise>, names: Map<String, String>, avgMin: Int?, onStart: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Ds.s1).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column {
            com.vukos.trening.ui.common.Kicker(tr("Sledeći", "Up next"), Ds.aText)
            val fs = com.vukos.trening.ui.common.titleSize(next.name, 40)
            Text(next.name, Modifier.padding(top = 10.dp), fontSize = fs, fontWeight = FontWeight.Bold, letterSpacing = (fs.value * -0.03f).sp,
                lineHeight = (fs.value * 1.08f).sp, style = androidx.compose.ui.text.TextStyle(lineBreak = androidx.compose.ui.text.style.LineBreak.Heading),
                color = Ds.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            last?.let {
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Repeat, null, Modifier.padding(top = 2.dp).size(13.dp), tint = Ds.sub)
                    Text(tr("Posle: ${it.name}", "Follows ${it.name}"), fontSize = 13.sp, lineHeight = 18.sp, color = Ds.sub)
                }
            }
            androidx.compose.foundation.layout.FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                com.vukos.trening.ui.common.IconText(Icons.AutoMirrored.Filled.List, tr("${ex.size} ${if (ex.size == 1) "vežba" else "vežbi"}", "${ex.size} ${if (ex.size == 1) "exercise" else "exercises"}"), Ds.sub)
                if (ex.isNotEmpty()) com.vukos.trening.ui.common.IconText(Icons.Filled.Layers, tr("${ex.sumOf { it.targetSets }} setova", "${ex.sumOf { it.targetSets }} sets"), Ds.sub)
                avgMin?.let { com.vukos.trening.ui.common.IconText(Icons.Filled.Schedule, "~$it min", Ds.sub) }
            }
        }
        if (ex.isNotEmpty()) Column {
            HorizontalDivider(thickness = 1.dp, color = Ds.line)
            ex.take(3).forEach { e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 11.dp)) {
                    Text(names[e.exerciseId] ?: "?", Modifier.weight(1f), fontSize = 15.sp, color = Ds.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${e.targetSets} × ${repsText(e)}", Modifier.padding(start = 12.dp), fontSize = 15.sp, color = Ds.sub)
                }
                HorizontalDivider(thickness = 1.dp, color = Ds.line)
            }
            if (ex.size > 3) Text(tr("+ još ${ex.size - 3}", "+ ${ex.size - 3} more"), Modifier.padding(top = 11.dp), fontSize = 15.sp, color = Ds.sub)
        }
        Box(
            Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(Accent).clickable(onClick = onStart),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (ex.isEmpty()) tr("Dodaj vežbe u plan", "Add exercises to the plan") else tr("Započni trening", "Start workout"),
                fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
            )
        }
    }
}

private fun repsText(e: RoutineExercise): String = when {
    e.repsMin != null && e.repsMax != null && e.repsMin != e.repsMax -> "${e.repsMin}–${e.repsMax}"
    else -> (e.repsMin ?: e.repsMax)?.toString() ?: "—"
}

/** Lista planova u jednoj kartici; dugo drzanje podigne plan, prevlacenje menja redosled. */
@Composable
private fun PlanList(
    routines: List<Routine>,
    byRoutine: Map<String, List<RoutineExercise>>,
    lastDone: Map<String, Long>,
    onStart: (Routine) -> Unit,
    onEdit: (Routine) -> Unit,
    onReorder: (List<Routine>) -> Unit,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val rowPx = with(density) { PlanRowH.toPx() }
    // Novi redosled se prikazuje odmah, dok ga baza ne potvrdi (bez skakanja posle spustanja).
    var pending by remember { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(routines) { if (pending == routines.map { it.id }) pending = null }
    val byId = routines.associateBy { it.id }
    val order = pending?.takeIf { p -> p.size == routines.size && p.all { it in byId } }?.map { byId.getValue(it) } ?: routines
    var dragIdx by remember { mutableIntStateOf(-1) }
    var dragDy by remember { mutableFloatStateOf(0f) }
    val n = order.size
    val target = if (dragIdx >= 0) (dragIdx + Math.round(dragDy / rowPx)).coerceIn(0, n - 1) else -1

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Ds.s1)) {
        order.forEachIndexed { i, r ->
            val me = i == dragIdx
            val shift = when {
                dragIdx < 0 || me -> 0.dp
                dragIdx < i && i <= target -> -PlanRowH
                dragIdx > i && i >= target -> PlanRowH
                else -> 0.dp
            }
            val animShift by animateDpAsState(shift, tween(220), label = "shift")
            val lift by animateFloatAsState(if (me) 1f else 0f, tween(200), label = "lift")
            val ex = byRoutine[r.id].orEmpty()
            val exText = tr("${ex.size} ${if (ex.size == 1) "vežba" else "vežbi"}", "${ex.size} ${if (ex.size == 1) "exercise" else "exercises"}")
            val lastText = lastDone[r.id]?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate().srRelative() }
                ?: tr("još nije rađen", "not done yet")
            Box(
                Modifier.fillMaxWidth().height(PlanRowH).zIndex(if (me) 2f else 1f)
                    .offset(y = if (me) 0.dp else animShift)
                    .graphicsLayer {
                        if (me) translationY = dragDy
                        val s = 1f + 0.03f * lift
                        scaleX = s; scaleY = s
                    }
                    .then(if (me) Modifier.shadow(18.dp, RoundedCornerShape(20.dp)) else Modifier)
                    .clip(if (me) RoundedCornerShape(20.dp) else RoundedCornerShape(0.dp))
                    .background(if (me) Ds.s2 else Ds.s1)
                    .pointerInput(r.id, order) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                dragIdx = order.indexOfFirst { it.id == r.id }; dragDy = 0f
                            },
                            onDrag = { change, amount -> change.consume(); dragDy += amount.y },
                            onDragEnd = {
                                val from = dragIdx
                                val to = (from + Math.round(dragDy / rowPx)).coerceIn(0, n - 1)
                                if (from >= 0 && to != from) {
                                    val a = order.toMutableList(); val m = a.removeAt(from); a.add(to, m)
                                    pending = a.map { it.id }; onReorder(a)
                                }
                                dragIdx = -1; dragDy = 0f
                            },
                            onDragCancel = { dragIdx = -1; dragDy = 0f },
                        )
                    }
                    .clickable { onEdit(r) },
            ) {
                Row(
                    Modifier.fillMaxSize().padding(start = 12.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.DragIndicator, null, Modifier.size(16.dp), tint = Ds.faint)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(r.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (r.notionPageId != null) Text(
                                "Notion", Modifier.clip(RoundedCornerShape(5.dp)).background(Ds.s3).padding(horizontal = 6.dp),
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp, color = Ds.sub,
                            )
                            Text(exText, fontSize = 13.sp, color = Ds.sub, maxLines = 1)
                            com.vukos.trening.ui.common.IconText(Icons.Filled.History, lastText, Ds.sub, Modifier.weight(1f, fill = false), 13.sp, 12.dp)
                        }
                    }
                    Row(
                        Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Ds.aSoft).clickable { onStart(r) }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, Modifier.size(14.dp), tint = Ds.aText)
                        Text(tr("Kreni", "Start"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText)
                    }
                }
                if (i < n - 1 && !me) HorizontalDivider(Modifier.align(Alignment.BottomCenter), thickness = 1.dp, color = Ds.line)
            }
        }
    }
}
