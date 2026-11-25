package com.vukos.trening.ui.body

import androidx.compose.material.icons.outlined.Timer

import androidx.compose.runtime.setValue

import androidx.compose.foundation.layout.Box

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.graphics.Path

import androidx.compose.ui.geometry.Offset

import androidx.compose.foundation.layout.widthIn

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.sp

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

/** Tab Profil (dizajn v2): kilaza sa grafikonom, brojke, sledece merenje, mere, poredjenje slika i istorija. */
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
    val D = com.vukos.trening.ui.theme.Ds

    Scaffold(containerColor = D.bg) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Profil", "Profile"), fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.68).sp, color = D.ink)
                    }
                    com.vukos.trening.ui.journal.RoundButton(Icons.Filled.Settings, tr("Podešavanja", "Settings"), D.s1, D.ink, 40.dp, onSettings)
                }
            }
            if (stats.current == null) item {
                EmptyState(Icons.Filled.MonitorWeight, tr("Prvo merenje", "First check-in"),
                    tr("Unesi kilažu da bi pratio napredak. Merenje je jednom nedeljno (nedeljom), a svake druge nedelje i 3 slike.", "Log your weight to track progress. Check-in is once a week (Sunday), plus 3 photos every other week.")) {
                    PrimaryPill(tr("Unesi merenje", "Log check-in"), onNew, Modifier.fillMaxWidth(), Icons.Filled.Add)
                }
            } else {
                item { CurrentWeightCard(stats, entries, onOpenChart) }
            }
            item { NextCheckInCard(due, onNew) }
            // Mere tela: poslednja vrednost i promena od prve.
            val measured = entries.filter { !Measurements.of(it).isEmpty }.sortedBy { it.epochDay }
            if (measured.isNotEmpty()) item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(D.s1).padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Text(tr("Mere", "Measurements"), Modifier.padding(top = 12.dp, bottom = 6.dp), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = D.ink)
                    Measurements.FIELDS.forEach { (label, unit, get) ->
                        val vals = measured.mapNotNull { get(Measurements.of(it)) }
                        val last = vals.lastOrNull() ?: return@forEach
                        androidx.compose.material3.HorizontalDivider(thickness = 1.dp, color = D.line)
                        Row(Modifier.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(label, Modifier.weight(1f), fontSize = 15.sp, color = D.ink)
                            if (vals.size >= 2) {
                                val d = last - vals.first()
                                Text((if (d > 0.04) "+" else if (d < -0.04) "−" else "") + fmtNum(kotlin.math.abs(d)) + " $unit", fontSize = 13.sp, color = D.sub)
                            }
                            Text("${fmtNum(last)} $unit", Modifier.widthIn(min = 64.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = D.ink,
                                textAlign = TextAlign.End)
                        }
                    }
                }
            }
            if (stats.photoDays >= 2) item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(D.s1).clickable(onClick = onCompare).padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Uporedi slike", "Compare photos"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = D.ink)
                        Text(tr("Pre i posle, jedna preko druge", "Before and after, one over the other"), fontSize = 13.sp, color = D.sub)
                    }
                    Icon(Icons.Filled.ChevronRight, null, Modifier.size(16.dp), tint = D.faint)
                }
            }
        }
    }
}

@Composable
private fun SmallTile(value: String, label: String, modifier: Modifier) {
    val D = com.vukos.trening.ui.theme.Ds
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(D.s1).padding(16.dp)) {
        com.vukos.trening.ui.common.FitText(value, androidx.compose.ui.text.TextStyle(fontFamily = com.vukos.trening.ui.theme.Manrope, fontSize = 20.sp, fontWeight = FontWeight.SemiBold), D.ink)
        Text(label, Modifier.padding(top = 2.dp), fontSize = 13.sp, color = D.sub, maxLines = 2)
    }
}

/** Trenutna kilaza, promena od proslog merenja, grafikon sa izborom opsega (1M ... Sve). */
@Composable
private fun CurrentWeightCard(s: BodyStats, entries: List<com.vukos.trening.data.BodyEntry>, onOpenChart: () -> Unit) {
    val D = com.vukos.trening.ui.theme.Ds
    val range = com.vukos.trening.data.ChartRange.M3
    val today = LocalDate.now()
    val pts = remember(entries, range) { range.filter(entries, today).filter { it.weightKg != null && !it.deleted }.sortedBy { it.epochDay } }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(D.s1).padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Trenutna kilaža", "Current weight"), Modifier.weight(1f), fontSize = 14.sp, color = D.sub)
            Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenChart).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Sva merenja", "All check-ins"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = D.aText)
                Icon(Icons.Filled.ChevronRight, null, Modifier.size(15.dp), tint = D.aText)
            }
        }
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.current!!.kg().removeSuffix(" kg"), fontSize = 60.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-2.4).sp, lineHeight = 60.sp, color = D.ink)
                Text("kg", Modifier.padding(bottom = 8.dp), fontSize = 20.sp, fontWeight = FontWeight.Medium, color = D.sub)
            }
            Spacer(Modifier.weight(1f))
            s.lastChange?.let {
                Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(D.aSoft).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text(it.signed().removeSuffix(" kg") + tr(" od prošlog", " since last"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = D.aText)
                }
            }
        }
        s.currentDay?.let {
            Text(tr("Izmereno ${it.srRelative()}, ${it.srDate()}", "Measured ${it.srRelative()}, ${it.srDate()}"), Modifier.padding(top = 8.dp), fontSize = 14.sp, color = D.sub)
        }
        if (pts.isNotEmpty()) {
            var sel by androidx.compose.runtime.saveable.rememberSaveable(range) { androidx.compose.runtime.mutableStateOf<Int?>(null) }
            WeightTrendChart(
                values = pts.map { it.weightKg!! },
                xLabel = { LocalDate.ofEpochDay(pts[it].epochDay).srDate() },
                pointLabel = { "${LocalDate.ofEpochDay(pts[it].epochDay).srLong()}, ${pts[it].weightKg!!.kg()}" },
                selected = sel, onSelect = { sel = it }, chartHeight = 150.dp, modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}

/** Kartica sledeceg merenja: datum, sta se meri, dugme i napomena o slikama. */
@Composable
private fun NextCheckInCard(due: CheckInSchedule.Due?, onNew: () -> Unit) {
    val D = com.vukos.trening.ui.theme.Ds
    val today = LocalDate.now()
    val pending = due?.takeIf { it.anythingMissing }
    val day = pending?.checkInDay ?: CheckInSchedule.nextCheckInDay(today)
    val photos = pending?.photosMissing ?: CheckInSchedule.isPhotoDay(day)
    val label = when {
        pending?.overdue == true -> tr("Propušteno merenje", "Missed check-in")
        pending != null -> tr("Danas je merenje", "Check-in today")
        else -> tr("Sledeće merenje", "Next check-in")
    }
    val what = if (pending != null) pending.whatMissing().replaceFirstChar { it.uppercase() }
        else if (photos) tr("Kilaža i 3 slike (spreda, bok, leđa)", "Weight and 3 photos (front, side, back)") else tr("Kilaža", "Weight")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(D.s1).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.vukos.trening.ui.common.Kicker(label, if (pending?.overdue == true) D.danger else D.aText, Modifier.weight(1f))
                if (pending == null) Row(
                    Modifier.height(26.dp).clip(RoundedCornerShape(13.dp)).background(D.aSoft).padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(Icons.Outlined.Timer, null, Modifier.size(13.dp), tint = D.aText)
                    Text(day.srRelative(today), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = D.aText, maxLines = 1)
                }
            }
            Text(day.srLong().replaceFirstChar { it.uppercase() }, Modifier.padding(top = 10.dp), fontSize = 26.sp, fontWeight = FontWeight.Bold,
                letterSpacing = (-0.52).sp, color = D.ink)
            Text(what, Modifier.padding(top = 2.dp), fontSize = 15.sp, color = D.sub)
        }
        val bg = if (pending != null) com.vukos.trening.ui.theme.Accent else D.ink
        val ink = if (pending != null) Color.White else D.bg
        Box(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).background(bg).clickable(onClick = onNew), contentAlignment = Alignment.Center) {
            Text(tr("Unesi merenje", "Log check-in"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = ink)
        }
        Row(Modifier.fillMaxWidth().padding(top = 0.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            Icon(Icons.Filled.Lock, null, Modifier.size(12.dp), tint = D.sub)
            Text(tr("Slike ostaju šifrovane na ovom telefonu", "Photos stay encrypted on this phone"), fontSize = 12.sp, color = D.sub, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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
