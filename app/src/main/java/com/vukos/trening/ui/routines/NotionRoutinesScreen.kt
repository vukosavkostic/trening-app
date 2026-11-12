package com.vukos.trening.ui.routines

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
fun NotionRoutinesScreen(onBack: () -> Unit, onSettings: () -> Unit, onOpenRoutine: (String) -> Unit) {
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
            .onFailure { error = it.message ?: "Greška pri učitavanju." }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Notion treninzi") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Nazad") } },
            actions = { IconButton(onClick = { reload++ }) { Icon(Icons.Filled.Refresh, "Osveži") } },
        )
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                error == "notoken" -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Notion nije povezan.", fontWeight = FontWeight.SemiBold)
                    Text("Unesi Notion token u podešavanjima.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onSettings) { Text("Podešavanja") }
                }
                error != null -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ne mogu da učitam planove iz Notiona.", fontWeight = FontWeight.SemiBold)
                    Text(error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { reload++ }) { Text("Pokušaj ponovo") }
                }
                items == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                items!!.isEmpty() -> Text(
                    "U Notion bazi Routines još nema planova.\nDodaj stranicu sa vežbama, jedna po redu, npr.\n\"Dumbbell Bench Press 3x8-12\".",
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
                                        "${nr.lines.size} vežbi",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                when {
                                    added -> TextButton(onClick = { onOpenRoutine(nr.appId!!) }) {
                                        Icon(Icons.Filled.CheckCircle, null, tint = com.vukos.trening.ui.theme.Success)
                                        Text("  Dodato")
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
                                                    .onFailure { error = "Dodavanje nije uspelo: ${it.message}" }
                                                importing = null
                                            }
                                        },
                                    ) { Text("Dodaj") }
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
                            if (nr.lines.size > 6) Text("… još ${nr.lines.size - 6}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (nr.lines.isEmpty()) Text("Nema vežbi (lista na stranici ili polje Plan).",
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
            title = { Text("Dodato: $name") },
            text = {
                Text(
                    "Uparenih vežbi: ${r.matched}." +
                        if (r.created.isEmpty()) "" else
                            "\n\nNisu pronađene u bazi, pa su dodate kao tvoje vežbe:\n" + r.created.joinToString("\n") { "• $it" }
                )
            },
            confirmButton = { TextButton(onClick = { result = null; onOpenRoutine(r.routineId) }) { Text("Otvori plan") } },
            dismissButton = { TextButton(onClick = { result = null }) { Text("OK") } },
        )
    }
}
