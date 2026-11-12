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

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(day.sr()) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Nazad") } },
            actions = { IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Izmeni") } },
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)
            ) {
                Text("Kilaža", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Text(entry?.weightKg?.kg() ?: "nije uneta", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                val w = entry?.weightKg
                if (w != null && previous != null) {
                    val d = w - previous.weightKg!!
                    Text(
                        (if (d >= 0) "+" else "−") + kotlin.math.abs(d).kg() +
                            " u odnosu na ${LocalDate.ofEpochDay(previous.epochDay).sr()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text("Slike", fontWeight = FontWeight.SemiBold)
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
                                Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(12.dp))
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
