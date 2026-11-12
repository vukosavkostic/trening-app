package com.vukos.trening.ui.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.data.Routine
import com.vukos.trening.data.Weekdays
import java.time.LocalDate

/** Tab Planovi: danas, nedeljni raspored, svi planovi. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onNotion: () -> Unit,
    onSettings: () -> Unit,
) {
    val db = TreningApp.instance.db
    val routines by remember { db.routines().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val links by remember { db.routines().observeAllExercises() }.collectAsStateWithLifecycle(emptyList())
    val exercises by remember { db.exercises().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val names = remember(exercises) { exercises.associate { it.id to it.name } }
    val byRoutine = remember(links) { links.groupBy { it.routineId } }
    val today = LocalDate.now().dayOfWeek
    val todays = routines.filter { Weekdays.has(it.weekdays, today) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planovi") },
                actions = {
                    IconButton(onClick = onNotion) { Icon(Icons.Filled.CloudDownload, "Notion treninzi") }
                    IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Podešavanja") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNew, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Novi plan") })
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card {
                    Text("Danas · ${Weekdays.FULL[today.value - 1]}", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium)
                    if (todays.isEmpty()) {
                        Text("Dan odmora (nema plana za danas)", fontWeight = FontWeight.SemiBold)
                    } else todays.forEach { r ->
                        Text(r.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp))
                        Text(
                            byRoutine[r.id].orEmpty().joinToString(", ") { names[it.exerciseId] ?: "?" },
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                            Text("Započni trening (faza 4)")
                        }
                    }
                }
            }
            item {
                Card {
                    Text("Nedelja", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Weekdays.LABELS.forEachIndexed { i, label ->
                        val plans = routines.filter { it.weekdays and (1 shl i) != 0 }
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text(label, Modifier.width(44.dp),
                                fontWeight = if (i == today.value - 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (i == today.value - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (plans.isEmpty()) "odmor" else plans.joinToString(" + ") { it.name },
                                color = if (plans.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
            if (routines.isEmpty()) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Text("Još nema planova.", fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = onNotion) {
                        Icon(Icons.Filled.CloudDownload, null); Text("  Uvezi iz Notiona")
                    }
                }
            } else item {
                Text("Svi planovi", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            }
            items(routines, key = { it.id }) { r -> RoutineCard(r, byRoutine[r.id].orEmpty().map { names[it.exerciseId] ?: "?" }) { onOpen(r.id) } }
        }
    }
}

@Composable
private fun RoutineCard(r: Routine, exerciseNames: List<String>, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(r.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (r.notionPageId != null) Text("Notion", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "${Weekdays.describe(r.weekdays)} · ${exerciseNames.size} vežbi",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
        )
        if (exerciseNames.isNotEmpty()) Text(
            exerciseNames.joinToString(", "), maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)
    ) { content() }
}
