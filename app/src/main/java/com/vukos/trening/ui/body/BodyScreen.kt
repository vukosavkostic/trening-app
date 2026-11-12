package com.vukos.trening.ui.body

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.data.BodyStats
import com.vukos.trening.data.CheckInSchedule
import com.vukos.trening.ui.common.DayMarker
import com.vukos.trening.ui.common.MonthCalendar
import java.time.LocalDate

private val PhotoBlue = Color(0xFF4F8DF7)

/** Tab Telo: statistika, grafikon (otvara se), kalendar merenja i lista dana. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScreen(onOpenDay: (LocalDate) -> Unit, onNew: () -> Unit, onOpenChart: () -> Unit) {
    val dao = TreningApp.instance.db.body()
    val entries by remember { dao.observeAll() }.collectAsStateWithLifecycle(emptyList())
    val photos by remember { dao.observeAllPhotos() }.collectAsStateWithLifecycle(emptyList())
    val photoCount = remember(photos) { photos.groupingBy { it.bodyEntryId }.eachCount() }
    val stats = remember(entries, photoCount) { BodyStats.compute(entries, photoCount.keys) }
    val accent = MaterialTheme.colorScheme.primary
    val markers = remember(entries, photoCount, accent) {
        entries.associate { e ->
            LocalDate.ofEpochDay(e.epochDay) to DayMarker(
                listOfNotNull(accent.takeIf { e.weightKg != null }, PhotoBlue.takeIf { (photoCount[e.id] ?: 0) > 0 })
            )
        }
    }
    val next = CheckInSchedule.nextCheckInDay(LocalDate.now())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Telo") },
                actions = { IconButton(onClick = onOpenChart) { Icon(Icons.Filled.ShowChart, "Grafikon") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Merenje") },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { StatsCard(stats) }
            item {
                if (stats.count >= 2) WeightChart(entries, Modifier.fillMaxWidth(), onClick = onOpenChart)
                else Card {
                    Text("Grafikon kilaže", fontWeight = FontWeight.SemiBold)
                    Text("Pojaviće se posle drugog merenja.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                MonthCalendar(markers = markers, onDayClick = onOpenDay, modifier = Modifier.fillMaxWidth())
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Legend(accent, "kilaža"); Spacer(Modifier.width(16.dp)); Legend(PhotoBlue, "slike")
                }
            }
            item {
                Card {
                    Text("Sledeće merenje", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium)
                    Text(
                        next.sr() + if (CheckInSchedule.isPhotoDay(next)) " · kilaža + 3 slike" else " · kilaža",
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Icon(Icons.Filled.Lock, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            " Slike su šifrovane i vidi ih samo ova aplikacija.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (entries.isNotEmpty()) item {
                Text("Sva merenja", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            }
            items(entries, key = { it.id }) { e ->
                val day = LocalDate.ofEpochDay(e.epochDay)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable { onOpenDay(day) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(day.sr(), modifier = Modifier.weight(1f))
                    val n = photoCount[e.id] ?: 0
                    if (n > 0) {
                        Icon(Icons.Filled.PhotoCamera, null, tint = PhotoBlue, modifier = Modifier.size(18.dp))
                        Text(" $n", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(16.dp))
                    }
                    Text(e.weightKg?.kg() ?: "—", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatsCard(s: BodyStats) {
    Card {
        Row {
            Column(Modifier.weight(1f)) {
                Text("Trenutno", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Text(s.current?.kg() ?: "—", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                s.currentDay?.let {
                    Text(it.sr(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.size(12.dp))
        Row {
            Stat("Od prošlog", s.lastChange.signed(), Modifier.weight(1f))
            Stat("30 dana", s.change30.signed(), Modifier.weight(1f))
            Stat("Ukupno", s.totalChange.signed(), Modifier.weight(1f))
        }
        Spacer(Modifier.size(12.dp))
        Row {
            Stat("Najmanje", s.min?.kg() ?: "—", Modifier.weight(1f))
            Stat("Najviše", s.max?.kg() ?: "—", Modifier.weight(1f))
            Stat("Merenja / slike", "${s.count} / ${s.photoDays}", Modifier.weight(1f))
        }
    }
}

private fun Double?.signed(): String = when {
    this == null -> "—"
    this > 0.04 -> "+" + kg()
    this < -0.04 -> "−" + kotlin.math.abs(this).kg()
    else -> "0 kg"
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)
    ) { content() }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text("  $label", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
