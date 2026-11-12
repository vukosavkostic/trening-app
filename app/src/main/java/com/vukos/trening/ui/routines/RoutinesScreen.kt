package com.vukos.trening.ui.routines

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.data.Routine
import com.vukos.trening.ui.body.srRelative
import com.vukos.trening.ui.common.AppCard
import com.vukos.trening.ui.common.Chip
import com.vukos.trening.ui.common.EmptyState
import com.vukos.trening.ui.common.IconBadge
import com.vukos.trening.ui.common.OutlinePill
import com.vukos.trening.ui.common.PrimaryPill
import com.vukos.trening.ui.common.SectionTitle
import com.vukos.trening.ui.common.TabTopBar

/** Tab Trening: brzi start, pa lista planova (nisu vezani za dan - bilo koji se radi bilo kad). */
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

    Scaffold(
        topBar = {
            TabTopBar("Trening") {
                IconButton(onClick = onNotion) { Icon(Icons.Filled.CloudDownload, "Planovi iz Notiona") }
                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Podešavanja") }
            }
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                // Nedeljni cilj i niz nedelja.
                val goal = remember { TreningApp.instance.settings.weeklyGoal }
                val dates = workouts.map { com.vukos.trening.data.JournalStats.dateOf(it) }
                val today = java.time.LocalDate.now()
                val thisWeek = dates.count { com.vukos.trening.data.TrainingStats.weekStart(it) == com.vukos.trening.data.TrainingStats.weekStart(today) }
                val streak = com.vukos.trening.data.TrainingStats.streak(dates, goal, today)
                AppCard(onClick = onOpenStats, padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Ova nedelja", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$thisWeek / $goal treninga", style = MaterialTheme.typography.titleLarge)
                        }
                        if (streak > 0) Chip("🔥 $streak ${if (streak == 1) "nedelja" else "nedelja"} zaredom",
                            color = MaterialTheme.colorScheme.onPrimaryContainer, bg = MaterialTheme.colorScheme.primaryContainer)
                        Icon(Icons.Filled.ChevronRight, "Statistika", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                    com.vukos.trening.ui.stats.WeekProgress(thisWeek, goal)
                }
            }
            item {
                AppCard(onClick = { onStart(null) }, padding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Bolt)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Slobodan trening", style = MaterialTheme.typography.titleMedium)
                            Text("Bez plana, vežbe dodaješ usput", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                SectionTitle("Moji planovi") {
                    TextButton(onClick = onNew) { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Text(" Novi plan") }
                }
            }
            if (routines.isEmpty()) item {
                EmptyState(
                    Icons.Filled.FitnessCenter, "Još nemaš planova",
                    "Napravi plan sa vežbama koje radiš, ili ga uvezi iz Notiona. Plan možeš da započneš bilo kog dana.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryPill("Napravi plan", onNew, Modifier.fillMaxWidth(), Icons.Filled.Add)
                        OutlinePill("Uvezi iz Notiona", onNotion, Modifier.fillMaxWidth(), Icons.Filled.CloudDownload)
                    }
                }
            }
            items(routines, key = { it.id }) { r ->
                RoutineCard(
                    r, byRoutine[r.id].orEmpty().map { names[it.exerciseId] ?: "?" }, lastDone[r.id],
                    onStart = { onStart(r.id) }, onEdit = { onOpen(r.id) },
                )
            }
        }
    }
}

@Composable
private fun RoutineCard(r: Routine, exerciseNames: List<String>, lastDone: Long?, onStart: () -> Unit, onEdit: () -> Unit) {
    AppCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(r.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    listOfNotNull(
                        "${exerciseNames.size} ${if (exerciseNames.size == 1) "vežba" else "vežbi"}",
                        lastDone?.let { "poslednji put " + java.time.Instant.ofEpochMilli(it)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate().srRelative() } ?: "još nije rađen",
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (r.notionPageId != null) Chip("Notion")
            Icon(Icons.Filled.Edit, "Izmeni plan", tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp).size(20.dp))
        }
        if (exerciseNames.isNotEmpty()) Text(
            exerciseNames.joinToString(" · "), maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(Modifier.height(16.dp))
        if (exerciseNames.isEmpty()) OutlinePill("Dodaj vežbe u plan", onEdit, Modifier.fillMaxWidth(), Icons.Filled.Add)
        else PrimaryPill("Započni trening", onStart, Modifier.fillMaxWidth(), Icons.Filled.PlayArrow)
    }
}
