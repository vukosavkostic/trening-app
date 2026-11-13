package com.vukos.trening.ui.routines

import com.vukos.trening.i18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.notion.ImportResult
import com.vukos.trening.notion.NotionRoutine
import com.vukos.trening.notion.NotionRoutines
import kotlinx.coroutines.launch

/**
 * "Notion treninzi": planovi iz Notion baze Routines.
 * Plan koji nije u aplikaciji ima dugme Dodaj; dodat plan (AppId postoji lokalno) ima oznaku Dodato.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotionRoutinesScreen(onHelp: () -> Unit, onBack: () -> Unit, onSettings: () -> Unit, onOpenRoutine: (String) -> Unit) {
    val app = TreningApp.instance
    val scope = rememberCoroutineScope()
    val local by remember { app.db.routines().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val localIds = local.map { it.id }.toSet()
    var items by remember { mutableStateOf<List<NotionRoutine>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var importing by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Pair<String, ImportResult>?>(null) }

    LaunchedEffect(reload) {
        if (!app.settings.notionConfigured) { error = "notoken"; return@LaunchedEffect }
        error = null; items = null
        runCatching { NotionRoutines.list(app.settings) }
            .onSuccess { items = it }
            .onFailure { error = it.message ?: tr("Greška pri učitavanju.", "Loading failed.") }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(tr("Notion treninzi", "Notion workouts")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Nazad", "Back")) } },
            actions = {
                IconButton(onClick = onHelp) { Icon(Icons.AutoMirrored.Filled.HelpOutline, tr("Uputstvo", "Guide")) }
                IconButton(onClick = { reload++ }) { Icon(Icons.Filled.Refresh, tr("Osveži", "Refresh")) }
            },
        )
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                error == "notoken" -> Column(Modifier.padding(16.dp)) {
                    com.vukos.trening.ui.common.EmptyState(
                        Icons.AutoMirrored.Filled.HelpOutline, tr("Notion nije povezan", "Notion is not connected"),
                        tr("Uz Notion imaš rezervnu kopiju u oblaku, pregled na računaru, a AI može da ti napravi plan treninga koji ovde uvučeš jednim klikom.", "With Notion you get a cloud copy, a view on your computer, and AI can build a workout plan you pull in here with one tap."),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            com.vukos.trening.ui.common.PrimaryPill(tr("Uputstvo korak po korak", "Step-by-step guide"), onHelp, Modifier.fillMaxWidth())
                            com.vukos.trening.ui.common.OutlinePill(tr("Već imam token", "I already have a token"), onSettings, Modifier.fillMaxWidth())
                        }
                    }
                }
                error?.contains("Trening") == true || error?.contains("401") == true -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(tr("Ne mogu da učitam planove iz Notiona.", "Could not load plans from Notion."), fontWeight = FontWeight.SemiBold)
                    Text(error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onHelp) { Text(tr("Pogledaj uputstvo", "See the guide")) }
                }
                error != null -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(tr("Ne mogu da učitam planove iz Notiona.", "Could not load plans from Notion."), fontWeight = FontWeight.SemiBold)
                    Text(error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { reload++ }) { Text(tr("Pokušaj ponovo", "Try again")) }
                }
                items == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                items!!.isEmpty() -> Text(
                    tr("U Notion bazi Routines još nema planova.\nDodaj stranicu sa vežbama, jedna po redu, npr.\n\"Dumbbell Bench Press 3x8-12\".", "There are no plans in the Notion Routines database yet.\nAdd a page with exercises, one per line, e.g.\n\"Dumbbell Bench Press 3x8-12\"."),
                    Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items!!, key = { it.pageId }) { nr ->
                        val added = nr.appId != null && nr.appId in localIds
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer).padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(nr.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        tr("${nr.lines.size} vežbi", "${nr.lines.size} exercises"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                when {
                                    added -> TextButton(onClick = { onOpenRoutine(nr.appId!!) }) {
                                        Icon(Icons.Filled.CheckCircle, null, tint = com.vukos.trening.ui.theme.Success)
                                        Text(tr("  Dodato", "  Added"))
                                    }
                                    importing == nr.pageId -> CircularProgressIndicator(Modifier.padding(8.dp))
                                    else -> Button(
                                        enabled = importing == null && nr.lines.isNotEmpty(),
                                        onClick = {
                                            importing = nr.pageId
                                            scope.launch {
                                                runCatching { NotionRoutines.import(app.settings, nr) }
                                                    .onSuccess { r ->
                                                        result = nr.name to r
                                                        items = items?.map { if (it.pageId == nr.pageId) it.copy(appId = r.routineId) else it }
                                                    }
                                                    .onFailure { error = tr("Dodavanje nije uspelo: ${it.message}", "Adding failed: ${it.message}") }
                                                importing = null
                                            }
                                        },
                                    ) { Text(tr("Dodaj", "Add")) }
                                }
                            }
                            nr.lines.take(6).forEach { l ->
                                Text(
                                    "• ${l.name}  ${l.sets}×${l.repsMin ?: ""}${if (l.repsMax != null && l.repsMax != l.repsMin) "–${l.repsMax}" else ""}" +
                                        (l.kg?.let { "  $it kg" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            if (nr.lines.size > 6) Text(tr("… još ${nr.lines.size - 6}", "… ${nr.lines.size - 6} more"), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (nr.lines.isEmpty()) Text(tr("Nema vežbi (lista na stranici ili polje Plan).", "No exercises (list on the page or the Plan field)."),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    result?.let { (name, r) ->
        AlertDialog(
            onDismissRequest = { result = null },
            title = { Text(tr("Dodato: $name", "Added: $name")) },
            text = {
                Text(
                    tr("Uparenih vežbi: ${r.matched}.", "Matched exercises: ${r.matched}.") +
                        if (r.created.isEmpty()) "" else
                            tr("\n\nNisu pronađene u bazi, pa su dodate kao tvoje vežbe:\n", "\n\nNot found in the database, added as your own exercises:\n") + r.created.joinToString("\n") { "• $it" }
                )
            },
            confirmButton = { TextButton(onClick = { result = null; onOpenRoutine(r.routineId) }) { Text(tr("Otvori plan", "Open plan")) } },
            dismissButton = { TextButton(onClick = { result = null }) { Text("OK") } },
        )
    }
}
