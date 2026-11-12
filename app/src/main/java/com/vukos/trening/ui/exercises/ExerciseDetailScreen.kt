package com.vukos.trening.ui.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.vukos.trening.TreningApp
import com.vukos.trening.data.equipmentLabels
import com.vukos.trening.data.muscleLabels
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(id: String, onBack: () -> Unit) {
    val dao = TreningApp.instance.db.exercises()
    val ex by remember(id) { dao.observe(id) }.collectAsStateWithLifecycle(null)
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(ex?.name ?: "", maxLines = 1) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Nazad") } },
            actions = {
                if (ex?.isCustom == true) IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, "Obriši")
                }
            },
        )
    }) { pad ->
        val e = ex ?: return@Scaffold
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())) {
            // Staticna slika (pocetni polozaj vezbe).
            val url = e.thumbnailUrl
            Box(
                Modifier.fillMaxWidth().aspectRatio(1.4f).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                if (url == null) {
                    Text("Nema slike", color = Color.Gray)
                } else {
                    AsyncImage(
                        model = url,
                        contentDescription = e.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(e.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Info("Glavni mišić", muscleLabels[e.primaryMuscle] ?: e.primaryMuscle)
                if (e.secondaryList.isNotEmpty()) {
                    Info("Pomoćni", e.secondaryList.joinToString(", ") { muscleLabels[it] ?: it })
                }
                if (e.equipment.isNotBlank()) Info("Oprema", equipmentLabels[e.equipment] ?: e.equipment)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Istorija i grafikon napretka stižu u fazi 5.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (e.instructions.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Uputstvo (EN)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    e.instructions.split('\n').forEachIndexed { i, step ->
                        Row {
                            Text("${i + 1}.", Modifier.width(24.dp), color = MaterialTheme.colorScheme.primary)
                            Text(step, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Obrisati vežbu?") },
        text = { Text("Vežba nestaje iz liste. Stari treninzi sa njom ostaju sačuvani.") },
        confirmButton = {
            TextButton(onClick = {
                confirmDelete = false
                scope.launch { dao.softDeleteCustom(id); onBack() }
            }) { Text("Obriši") }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Otkaži") } },
    )
}

@Composable
private fun Info(label: String, value: String) {
    Row {
        Text("$label: ", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}
