package com.vukos.trening.ui.body

import com.vukos.trening.i18n.tr

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.data.BodyRepo
import com.vukos.trening.data.BodyStats
import com.vukos.trening.data.CheckInSchedule
import com.vukos.trening.data.Measurements
import com.vukos.trening.ui.common.LockNote
import androidx.compose.material.icons.filled.Compare
import com.vukos.trening.ui.common.AppCard
import com.vukos.trening.ui.common.Chip
import com.vukos.trening.ui.common.EmptyState
import com.vukos.trening.ui.common.IconBadge
import com.vukos.trening.ui.common.OutlinePill
import com.vukos.trening.ui.common.PrimaryPill
import com.vukos.trening.ui.common.SectionTitle
import com.vukos.trening.ui.common.StatTile
import com.vukos.trening.ui.common.TabTopBar
import com.vukos.trening.ui.theme.Danger
import java.time.LocalDate

/** Tab Profil: trenutna kilaza, sledece merenje, grafikon i istorija merenja. */
@Composable
fun BodyScreen(onOpenDay: (LocalDate) -> Unit, onNew: () -> Unit, onOpenChart: () -> Unit, onSettings: () -> Unit, onCompare: () -> Unit) {
    val dao = TreningApp.instance.db.body()
    val entries by remember { dao.observeAll() }.collectAsStateWithLifecycle(emptyList())
    val photos by remember { dao.observeAllPhotos() }.collectAsStateWithLifecycle(emptyList())
    val due by remember { BodyRepo.observeDue() }.collectAsStateWithLifecycle(null)
    val photoCount = remember(photos) { photos.groupingBy { it.bodyEntryId }.eachCount() }
    val stats = remember(entries, photoCount) { BodyStats.compute(entries, photoCount.keys) }
    // Za svako merenje: promena u odnosu na prethodno merenje sa kilazom.
    val deltas = remember(entries) {
        val withKg = entries.filter { it.weightKg != null }.sortedBy { it.epochDay }
        withKg.zipWithNext { a, b -> b.id to (b.weightKg!! - a.weightKg!!) }.toMap()
    }

    Scaffold(topBar = {
        TabTopBar(tr("Profil", "Profile")) {
            IconButton(onClick = onOpenChart) { Icon(Icons.Filled.ShowChart, tr("Grafikon kilaže", "Weight chart")) }
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, tr("Podešavanja", "Settings")) }
        }
    }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (stats.current == null) item {
                EmptyState(Icons.Filled.MonitorWeight, tr("Prvo merenje", "First check-in"),
                    tr("Unesi kilažu da bi pratio napredak. Merenje je jednom nedeljno (nedeljom), a svake druge nedelje i 3 slike.", "Log your weight to track progress. Check-in is once a week (Sunday), plus 3 photos every other week.")) {
                    PrimaryPill(tr("Unesi merenje", "Log check-in"), onNew, Modifier.fillMaxWidth(), Icons.Filled.Add)
                }
            } else {
                item { CurrentWeightCard(stats, onOpenChart) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(stats.change30.signed(), tr("30 dana", "30 days"), Modifier.weight(1f))
                        StatTile(stats.totalChange.signed(), tr("ukupno", "total"), Modifier.weight(1f))
                        StatTile("${stats.count}", if (stats.photoDays > 0) tr("merenja, ${stats.photoDays} sa slikama", "check-ins, ${stats.photoDays} with photos") else tr("merenja", "check-ins"), Modifier.weight(1f))
                    }
                }
                if (stats.count >= 2) item { WeightChart(entries, Modifier.fillMaxWidth(), onClick = onOpenChart) }
            }
            item { NextCheckInCard(due, onNew) }
            // Mere tela: poslednja vrednost i promena od prve.
            val measured = entries.filter { !Measurements.of(it).isEmpty }.sortedBy { it.epochDay }
            if (measured.isNotEmpty()) item {
                AppCard {
                    Text(tr("Mere", "Measurements"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Measurements.FIELDS.forEach { (label, unit, get) ->
                        val vals = measured.mapNotNull { get(Measurements.of(it)) }
                        val last = vals.lastOrNull() ?: return@forEach
                        Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            if (vals.size >= 2) {
                                val d = last - vals.first()
                                Text(
                                    (if (d > 0.04) "+" else if (d < -0.04) "−" else "") + fmtNum(kotlin.math.abs(d)) + " $unit",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(16.dp))
                            }
                            Text("${fmtNum(last)} $unit", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
            if (stats.photoDays >= 2) item {
                AppCard(onClick = onCompare, padding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Compare)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr("Uporedi slike", "Compare photos"), style = MaterialTheme.typography.titleMedium)
                            Text(tr("Pre i posle, jedna preko druge", "Before and after, one over the other"), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (entries.isNotEmpty()) {
                item { SectionTitle(tr("Istorija merenja", "Check-in history")) }
                itemsIndexed(entries, key = { _, e -> e.id }) { i, e ->
                    val day = LocalDate.ofEpochDay(e.epochDay)
                    val first = i == 0
                    val last = i == entries.lastIndex
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(
                                topStart = if (first) 24.dp else 6.dp, topEnd = if (first) 24.dp else 6.dp,
                                bottomStart = if (last) 24.dp else 6.dp, bottomEnd = if (last) 24.dp else 6.dp,
                            ))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable { onOpenDay(day) }.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(day.srLong().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall)
                            Text(day.srRelative(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if ((photoCount[e.id] ?: 0) > 0) {
                            Icon(Icons.Filled.PhotoCamera, tr("Ima slike", "Has photos"), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(e.weightKg?.kg() ?: "—", style = MaterialTheme.typography.titleMedium)
                            deltas[e.id]?.let { Text(it.signed(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentWeightCard(s: BodyStats, onOpenChart: () -> Unit) {
    AppCard(onClick = onOpenChart, padding = PaddingValues(24.dp)) {
        Text(tr("Trenutna kilaža", "Current weight"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        // Broj i "kg" stoje na istoj osnovnoj liniji, a cip je vertikalno centriran u odnosu na red.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(s.current!!.kg().removeSuffix(" kg"), style = MaterialTheme.typography.displaySmall)
                Text(" kg", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.weight(1f))
            s.lastChange?.let { Chip(it.signed() + tr(" od prošlog", " since last")) }
        }
        s.currentDay?.let {
            Text(tr("izmereno ${it.srRelative()} · ${it.srDate()}", "measured ${it.srRelative()} · ${it.srDate()}"), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        Row(Modifier.padding(top = 16.dp)) {
            Text(tr("Najniža ", "Lowest "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(s.min?.kg() ?: "—", style = MaterialTheme.typography.bodySmall)
            Text(tr("   ·   Najviša ", "   ·   Highest "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(s.max?.kg() ?: "—", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Kartica sledeceg merenja (centrirano: ikona, naslov, opis, dugme). */
@Composable
private fun NextCheckInCard(due: CheckInSchedule.Due?, onNew: () -> Unit) {
    val today = LocalDate.now()
    val pending = due?.takeIf { it.anythingMissing }
    val day = pending?.checkInDay ?: CheckInSchedule.nextCheckInDay(today)
    val photos = pending?.photosMissing ?: CheckInSchedule.isPhotoDay(day)
    val title = when {
        pending?.overdue == true -> tr("Propušteno merenje", "Missed check-in")
        pending != null -> tr("Danas je merenje", "Check-in today")
        else -> tr("Sledeće merenje", "Next check-in")
    }
    val what = if (pending != null) pending.whatMissing().replaceFirstChar { it.uppercase() }
        else if (photos) tr("Kilaža i 3 slike (spreda, bok, leđa)", "Weight and 3 photos (front, side, back)") else tr("Kilaža", "Weight")
    AppCard(padding = PaddingValues(24.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(if (pending?.overdue == true) Icons.Filled.Warning else Icons.Filled.MonitorWeight,
                if (pending?.overdue == true) Danger else MaterialTheme.colorScheme.primary, 56.dp)
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                "${day.srLong().replaceFirstChar { it.uppercase() }} · ${day.srRelative(today)}\n$what",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            if (pending != null) PrimaryPill(tr("Unesi merenje", "Log check-in"), onNew, Modifier.fillMaxWidth())
            else OutlinePill(tr("Unesi merenje", "Log check-in"), onNew, Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            LockNote(tr("Slike su šifrovane i vidi ih samo ova aplikacija", "Photos are encrypted and only this app can see them"), center = true)
        }
    }
}

internal fun Double?.signed(): String = when {
    this == null -> "—"
    this > 0.04 -> "+" + kg()
    this < -0.04 -> "−" + kotlin.math.abs(this).kg()
    else -> "0 kg"
}

/** 82.0 -> "82", 82.45 -> "82.5" */
internal fun fmtNum(v: Double): String = String.format(java.util.Locale.US, "%.1f", v).removeSuffix(".0")
