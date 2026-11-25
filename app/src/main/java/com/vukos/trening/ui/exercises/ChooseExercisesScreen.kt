package com.vukos.trening.ui.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.data.EquipmentGroup
import com.vukos.trening.data.Exercise
import com.vukos.trening.data.ExerciseSearch
import com.vukos.trening.data.JournalStats
import com.vukos.trening.data.MuscleGroup
import com.vukos.trening.data.TrackType
import com.vukos.trening.data.TrainingStats
import com.vukos.trening.data.WorkoutMath
import com.vukos.trening.data.equipmentLabels
import com.vukos.trening.data.muscleLabels
import com.vukos.trening.i18n.tr
import com.vukos.trening.ui.body.srRelative
import com.vukos.trening.ui.common.ExerciseThumb
import com.vukos.trening.ui.journal.DsField
import com.vukos.trening.ui.theme.Accent
import com.vukos.trening.ui.theme.Ds
import java.time.LocalDate

/**
 * Izbor vezbi (dizajn v2.1): pretraga, oprema + misici u jednom redu, uvid iz nedeljnog cilja, Skoro, Moje i Sve vezbe.
 * Broj na vezbi je redosled dodira; tim redom vezbe ulaze u plan / trening.
 * [created] je ID vezbe upravo napravljene na ekranu Nova vezba (dodaje se u izbor).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseExercisesScreen(
    created: String?,
    onCreatedHandled: () -> Unit,
    onNew: (name: String, muscle: MuscleGroup?, nextNumber: Int) -> Unit,
    onConfirm: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    val db = TreningApp.instance.db
    val all by remember { db.exercises().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val workouts by remember { db.workouts().observeFinished() }.collectAsStateWithLifecycle(emptyList())
    val doneSets by remember { db.workouts().observeAllDoneSets() }.collectAsStateWithLifecycle(emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf<MuscleGroup?>(null) }
    var equip by rememberSaveable { mutableStateOf<EquipmentGroup?>(null) }
    var equipSheet by remember { mutableStateOf(false) }
    val selected = rememberSaveable(
        saver = androidx.compose.runtime.saveable.listSaver<androidx.compose.runtime.snapshots.SnapshotStateList<String>, String>(
            save = { it.toList() }, restore = { it.toMutableStateList() },
        ),
    ) {
        mutableStateListOf<String>()
    }
    LaunchedEffect(created) {
        if (created != null) { if (created !in selected) selected.add(created); query = ""; onCreatedHandled() }
    }
    val byId = remember(all) { all.associateBy { it.id } }
    val q = query.trim()
    val browsing = q.isEmpty() && muscle == null && equip == null
    val results = remember(all, q, muscle, equip) { ExerciseSearch.filter(all, q, setOfNotNull(muscle), setOfNotNull(equip)) }

    // Poslednji put: najbolji set iz poslednjeg treninga sa tom vezbom.
    val last = remember(workouts, doneSets) {
        val when_ = workouts.associate { it.id to it.startedAt }
        doneSets.filter { WorkoutMath.isWorking(it) && it.workoutId in when_ }.groupBy { it.exerciseId }.mapValues { (_, s) ->
            val latest = s.maxOf { when_[it.workoutId]!! }
            val inLast = s.filter { when_[it.workoutId] == latest }
            inLast.maxWith(compareBy({ it.weightKg ?: 0.0 }, { it.reps ?: 0 })) to latest
        }
    }
    fun lastText(e: Exercise): String? = last[e.id]?.let { (s, at) ->
        val rel = java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault()).toLocalDate().srRelative()
        val perf = when (e.trackType) {
            TrackType.TIME -> "${s.reps ?: 0} s"
            TrackType.REPS -> tr("${s.reps ?: 0} ponav.", "${s.reps ?: 0} reps")
            else -> (WorkoutMath.fmtKg(s.weightKg).takeIf { it.isNotEmpty() }?.let { "$it kg × " } ?: "") + "${s.reps ?: 0}"
        }
        "$perf, $rel"
    }
    val recent = remember(last, byId) { last.entries.sortedByDescending { it.value.second }.take(5).mapNotNull { byId[it.key] } }
    val mine = remember(all) { all.filter { it.isCustom } }
    // Uvid: misic sa najmanje radnih setova ove nedelje, ako je ispod cilja (10).
    val gap = remember(workouts, doneSets, all) {
        if (workouts.isEmpty()) null else {
            val today = LocalDate.now()
            val primary = all.associate { it.id to it.primaryMuscle }
            val counts = TrainingStats.muscleSets(workouts, doneSets, primary, TrainingStats.weekStart(today), today)
            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.LEGS)
                .map { g -> g to counts.filter { it.first in g.muscles }.sumOf { it.second } }
                .minByOrNull { it.second }?.takeIf { it.second < 10 }
        }
    }

    Box(Modifier.fillMaxSize().background(Ds.bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Otkaži", "Cancel"), Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onBack).padding(4.dp), fontSize = 17.sp, color = Ds.sub)
                    Text(tr("Dodaj vežbe", "Add exercises"), Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink, textAlign = TextAlign.Center)
                    Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable { onNew(q, muscle, selected.size + 1) }.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Add, null, Modifier.size(17.dp), tint = Ds.aText)
                        Text(tr("Nova", "New"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText)
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(Ds.s1).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        DsField(query, { query = it }, tr("Pretraži ${all.size} vežbi", "Search ${all.size} exercises"), Icons.Filled.Search)
                    }
                    if (q.isNotEmpty()) Box(Modifier.size(32.dp).clip(CircleShape).background(Ds.s3).clickable { query = "" }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, tr("Obriši pretragu", "Clear search"), Modifier.size(14.dp), tint = Ds.ink)
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        val set = equip != null
                        Row(
                            Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (set) Ds.aSoft else Color.Transparent)
                                .border(1.dp, if (set) Color.Transparent else Ds.s3, RoundedCornerShape(18.dp)).clickable { equipSheet = true }.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            val ink = if (set) Ds.aText else Ds.ink
                            Icon(Icons.Filled.Tune, null, Modifier.size(14.dp), tint = ink)
                            Text(equip?.label ?: tr("Oprema", "Equipment"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ink)
                            Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(14.dp), tint = ink)
                        }
                    }
                    items(listOf<MuscleGroup?>(null) + MuscleGroup.entries) { g ->
                        val on = muscle == g
                        Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (on) Ds.ink else Ds.s1).clickable { muscle = g }.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center) {
                            Text(g?.label ?: tr("Sve", "All"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) Ds.bg else Ds.ink)
                        }
                    }
                }
            }
            if (browsing) gap?.let { (g, n) ->
                item {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Ds.aSoft).padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.TrackChanges, null, Modifier.size(20.dp), tint = Accent)
                        Column(Modifier.weight(1f)) {
                            Text(tr("${g.label} je ispod cilja", "${g.label} is below target"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ds.aOn)
                            Text(tr("${WorkoutMath.fmtKg(n)} od 10 radnih setova ove nedelje", "${WorkoutMath.fmtKg(n)} of 10 working sets this week"),
                                Modifier.padding(top = 1.dp), fontSize = 13.sp, color = Ds.aOn.copy(alpha = 0.8f))
                        }
                        Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(Accent).clickable { muscle = g }.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center) { Text(tr("Prikaži", "Show"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
                    }
                }
            }
            val sections: List<Pair<String, List<Exercise>>> = if (browsing) listOfNotNull(
                (tr("Moje vežbe", "Your exercises") to mine).takeIf { mine.isNotEmpty() },
                (tr("Skoro", "Recent") to recent).takeIf { recent.isNotEmpty() },
                tr("Sve vežbe", "All exercises") to results,
            ) else listOfNotNull((when {
                q.isNotEmpty() -> tr("Rezultati", "Results")
                muscle != null -> muscle!!.label
                else -> equip!!.label
            } to results).takeIf { results.isNotEmpty() })
            sections.forEach { (title, list) ->
                item(key = "h-$title") {
                    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.Bottom) {
                        com.vukos.trening.ui.common.Kicker(title, Ds.sub, Modifier.weight(1f))
                        if (title != tr("Skoro", "Recent")) Text("${list.size}", fontSize = 13.sp, color = Ds.faint)
                    }
                }
                itemsIndexed(list, key = { _, e -> "$title-${e.id}" }) { i, e ->
                    val k = selected.indexOf(e.id)
                    val shape = RoundedCornerShape(
                        topStart = if (i == 0) 20.dp else 0.dp, topEnd = if (i == 0) 20.dp else 0.dp,
                        bottomStart = if (i == list.lastIndex) 20.dp else 0.dp, bottomEnd = if (i == list.lastIndex) 20.dp else 0.dp,
                    )
                    Column(Modifier.fillMaxWidth().padding(top = if (i == 0) 0.dp else 0.dp).clip(shape).background(Ds.s1)) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 72.dp).background(if (k >= 0) Ds.s2 else Color.Transparent)
                                .clickable { if (k >= 0) selected.removeAt(k) else selected.add(e.id) }
                                .padding(start = 12.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ExerciseThumb(e, 48.dp)
                            Column(Modifier.weight(1f)) {
                                Text(e.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, color = Ds.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Row(Modifier.padding(top = 3.dp)) {
                                    MuscleEquip((muscleLabels[e.primaryMuscle] ?: e.primaryMuscle).replaceFirstChar { it.uppercase() },
                                        (equipmentLabels[e.equipment] ?: e.equipment.ifBlank { "—" }).replaceFirstChar { it.uppercase() })
                                }
                                lastText(e)?.let { com.vukos.trening.ui.common.IconText(Icons.Filled.History, it, Ds.sub, Modifier.padding(top = 3.dp), 13.sp, 12.dp) }
                            }
                            Box(
                                Modifier.size(30.dp).clip(CircleShape).background(if (k >= 0) Accent else Color.Transparent)
                                    .then(if (k >= 0) Modifier else Modifier.border(1.5.dp, Ds.faint, CircleShape)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (k >= 0) Text("${k + 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                else Icon(Icons.Filled.Add, null, Modifier.size(16.dp), tint = Ds.sub)
                            }
                        }
                        if (i < list.lastIndex) HorizontalDivider(thickness = 1.dp, color = Ds.line)
                    }
                }
            }
            if (q.isNotEmpty() && all.none { ExerciseSearch.normalize(it.name) == ExerciseSearch.normalize(q) }) item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Ds.s1).clickable { onNew(q, muscle, selected.size + 1) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Ds.aSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp), tint = Ds.aText)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(tr("Napravi „$q“", "Create “$q”"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(tr("Čuva se kao tvoja vežba", "Saved as your own exercise"), Modifier.padding(top = 1.dp), fontSize = 13.sp, color = Ds.sub)
                    }
                }
            }
        }

        // Donja traka: koliko je izabrano i dugme Dodaj.
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Ds.bg)) {
            HorizontalDivider(thickness = 1.dp, color = Ds.line)
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val n = selected.size
                Column(Modifier.weight(1f)) {
                    Text(if (n > 0) tr("Izabrano: $n", "$n selected") else tr("Ništa nije izabrano", "Nothing selected"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink)
                    Text(
                        if (n > 0) selected.mapNotNull { byId[it]?.name }.joinToString(", ") else tr("Dodiruj vežbe redom kojim ih želiš", "Tap exercises in the order you want them"),
                        Modifier.padding(top = 1.dp), fontSize = 13.sp, color = Ds.sub, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    Modifier.height(52.dp).clip(RoundedCornerShape(26.dp)).background(if (n > 0) Accent else Ds.s3)
                        .clickable(enabled = n > 0) { onConfirm(selected.toList()) }.padding(horizontal = 26.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(if (n > 0) tr("Dodaj $n", "Add $n") else tr("Dodaj", "Add"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (n > 0) Color.White else Ds.faint) }
            }
        }
    }

    if (equipSheet) ModalBottomSheet(
        onDismissRequest = { equipSheet = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ds.sheet,
    ) {
        Column(Modifier.padding(bottom = 34.dp)) {
            Text(tr("Oprema", "Equipment"), Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp), fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold, color = Ds.ink, textAlign = TextAlign.Center)
            Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp)).background(Ds.s1)) {
                val opts = listOf<EquipmentGroup?>(null) + EquipmentGroup.entries
                opts.forEachIndexed { i, g ->
                    Row(Modifier.fillMaxWidth().height(52.dp).clickable { equip = g; equipSheet = false }.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(g?.label ?: tr("Bilo koja oprema", "Any equipment"), Modifier.weight(1f), fontSize = 16.sp, color = Ds.ink)
                        if (g == equip) Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = Accent)
                    }
                    if (i < opts.size - 1) HorizontalDivider(thickness = 1.dp, color = Ds.line)
                }
            }
        }
    }
}
