package com.vukos.trening.ui.body

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.data.Pose
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf

/** Pregled jednog dana merenja: kilaza, promena i tri slike (klik = preko celog ekrana). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyDayScreen(day: LocalDate, onBack: () -> Unit, onEdit: () -> Unit) {
    SecureWindow()
    val dao = TreningApp.instance.db.body()
    val entries by remember { dao.observeAll() }.collectAsStateWithLifecycle(emptyList())
    val entry = entries.firstOrNull { it.epochDay == day.toEpochDay() }
    val previous = entries.filter { it.epochDay < day.toEpochDay() && it.weightKg != null }.maxByOrNull { it.epochDay }
    val photos by remember(entry?.id) { entry?.let { dao.observePhotos(it.id) } ?: flowOf(emptyList()) }
        .collectAsStateWithLifecycle(emptyList())
    val byPose = photos.associateBy { it.pose }
    var fullscreen by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(day.srLong().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleLarge) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Nazad") } },
            actions = {
                if (entry != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Obriši merenje") }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Izmeni") }
            },
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            com.vukos.trening.ui.common.AppCard(padding = androidx.compose.foundation.layout.PaddingValues(24.dp)) {
                Text("Kilaža", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Text(entry?.weightKg?.kg() ?: "nije uneta", style = MaterialTheme.typography.displaySmall)
                val w = entry?.weightKg
                if (w != null && previous != null) {
                    Spacer(Modifier.height(8.dp))
                    com.vukos.trening.ui.common.Chip(
                        (w - previous.weightKg!!).signed() + " od " + LocalDate.ofEpochDay(previous.epochDay).srDate()
                    )
                }
            }

            Text("Slike", style = MaterialTheme.typography.titleLarge)
            if (photos.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NoPhotography, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("  Ovog dana nema slika.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pose.entries.forEachIndexed { i, pose ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable(enabled = byPose[pose.key] != null) { fullscreen = i },
                                contentAlignment = Alignment.Center,
                            ) {
                                val p = byPose[pose.key]
                                if (p == null) Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                else PhotoContent(null, p, ContentScale.Crop, Modifier.fillMaxSize())
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(pose.label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Obrisati merenje?") },
        text = { Text("Kilaža i slike za ${day.srDate()} se trajno brišu sa telefona.") },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                confirmDelete = false
                scope.launch { com.vukos.trening.data.BodyRepo.delete(day); onBack() }
            }) { Text("Obriši", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) { Text("Otkaži") } },
    )

    // Preko celog ekrana, prevlacenje levo/desno izmedju poza.
    fullscreen?.let { start ->
        val available = Pose.entries.filter { byPose[it.key] != null }
        val startIdx = available.indexOf(Pose.entries[start]).coerceAtLeast(0)
        Dialog(onDismissRequest = { fullscreen = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            SecureWindow()
            val pager = rememberPagerState(startIdx) { available.size }
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                    Box(Modifier.fillMaxSize().clickable { fullscreen = null }) {
                        PhotoContent(null, byPose[available[page].key], ContentScale.Fit, Modifier.fillMaxSize())
                    }
                }
                Text(
                    available.getOrNull(pager.currentPage)?.label ?: "",
                    color = Color.White, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
                )
            }
        }
    }
}
