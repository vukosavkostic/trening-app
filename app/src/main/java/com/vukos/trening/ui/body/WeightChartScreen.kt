package com.vukos.trening.ui.body

import com.vukos.trening.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.data.BodyPhoto
import com.vukos.trening.data.ChartRange
import com.vukos.trening.data.Pose
import com.vukos.trening.ui.theme.Ds
import java.time.LocalDate

/** Kilaza (dizajn v2.1): grafikon sa izborom merenja, detalj izabranog, slike tog dana i istorija. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightChartScreen(onBack: () -> Unit, onOpenDay: (LocalDate) -> Unit, onCompare: (String) -> Unit = {}) {
    val dao = TreningApp.instance.db.body()
    val entries by remember { dao.observeAll() }.collectAsStateWithLifecycle(emptyList())
    val photos by remember { dao.observeAllPhotos() }.collectAsStateWithLifecycle(emptyList())
    val byEntry = remember(photos) { photos.groupBy { it.bodyEntryId } }
    var range by rememberSaveable { mutableStateOf(ChartRange.M3) }
    val shown = remember(entries, range) {
        range.filter(entries.filter { it.weightKg != null && !it.deleted }, LocalDate.now()).sortedBy { it.epochDay }
    }
    var sel by rememberSaveable(range) { mutableStateOf<Int?>(null) }
    var photosOpen by remember { mutableStateOf(false) }
    val selIdx = sel?.takeIf { it in shown.indices } ?: shown.lastIndex
    val sgn = { v: Double -> (if (v > 0.04) "+" else if (v < -0.04) "−" else "±") + String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(v)) }
    val fmt = { v: Double -> String.format(java.util.Locale.US, "%.1f", v) }

    LazyColumn(
        Modifier.fillMaxSize().background(Ds.bg).statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Ds.s1).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ChevronLeft, tr("Nazad", "Back"), Modifier.size(20.dp), tint = Ds.ink)
            }
        }
        item {
            Column {
                Text(tr("Kilaža", "Weight"), fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.68).sp, color = Ds.ink)
                if (shown.isNotEmpty()) Text(
                    tr("Merenja: ${shown.size}", "${shown.size} check-in${if (shown.size == 1) "" else "s"}"),
                    Modifier.padding(top = 2.dp), fontSize = 15.sp, color = Ds.sub,
                )
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Ds.s1).padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 18.dp)) {
                if (shown.isEmpty()) Text(
                    tr("U izabranom periodu nema merenja.", "No check-ins in the selected period."), fontSize = 15.sp, color = Ds.sub,
                ) else {
                    val first = shown.first()
                    val last = shown.last()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(fmt(last.weightKg!!), fontSize = 52.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-2.08).sp, lineHeight = 52.sp, color = Ds.ink)
                            Text("kg", Modifier.padding(bottom = 6.dp), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Ds.sub)
                        }
                        Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Ds.aSoft).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                tr("${sgn(last.weightKg!! - first.weightKg!!)} kg od ${LocalDate.ofEpochDay(first.epochDay).srDate()}", "${sgn(last.weightKg!! - first.weightKg!!)} kg since ${LocalDate.ofEpochDay(first.epochDay).srDate()}"),
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText, maxLines = 1,
                            )
                        }
                    }
                    WeightTrendChart(
                        values = shown.map { it.weightKg!! },
                        xLabel = { LocalDate.ofEpochDay(shown[it].epochDay).srDate() },
                        pointLabel = { "${LocalDate.ofEpochDay(shown[it].epochDay).srLong()}, ${shown[it].weightKg!!.kg()}" },
                        selected = selIdx, onSelect = { sel = it }, chartHeight = 210.dp, modifier = Modifier.padding(top = 22.dp),
                    )
                    val e = shown[selIdx]
                    val prev = shown.getOrNull(selIdx - 1)
                    Row(
                        Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Ds.s2)
                            .clickable { onOpenDay(LocalDate.ofEpochDay(e.epochDay)) }.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(LocalDate.ofEpochDay(e.epochDay).srLong().replaceFirstChar { it.uppercase() }, Modifier.weight(1f), fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, color = Ds.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(prev?.let { "${sgn(e.weightKg!! - it.weightKg!!)} kg" } ?: tr("prvo merenje", "first check-in"), fontSize = 14.sp, color = Ds.sub)
                        Icon(Icons.Filled.ChevronRight, tr("Otvori merenje", "Open check-in"), Modifier.size(16.dp), tint = Ds.faint)
                    }
                    val dayPhotos = byEntry[e.id].orEmpty()
                    if (dayPhotos.isNotEmpty()) Row(
                        Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Ds.s2)
                            .clickable { photosOpen = true }.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Filled.PhotoCamera, null, Modifier.size(16.dp), tint = Ds.aText)
                        Text(tr("Slike: ${dayPhotos.size}", "${dayPhotos.size} photos"), Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink)
                        Text(Pose.entries.filter { p -> dayPhotos.any { it.pose == p.key } }.joinToString(", ") { it.label }, fontSize = 14.sp, color = Ds.sub, maxLines = 1)
                        Icon(Icons.Filled.ChevronRight, null, Modifier.size(16.dp), tint = Ds.faint)
                    }
                    if (shown.size < 3) Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Info, null, Modifier.padding(top = 2.dp).size(14.dp), tint = Ds.sub)
                        Text(
                            tr("Merenja do sada: ${shown.size}. Trend postaje jasniji posle nekoliko nedelja.", "${shown.size} check-in${if (shown.size == 1) "" else "s"} so far. The trend gets clearer after a few Sundays."),
                            fontSize = 13.sp, lineHeight = 19.sp, color = Ds.sub,
                        )
                    }
                }
                Row(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Ds.bg).padding(3.dp)) {
                    ChartRange.entries.forEach { r ->
                        val on = r == range
                        Box(Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(9.dp)).background(if (on) Ds.s3 else Color.Transparent).clickable { range = r },
                            contentAlignment = Alignment.Center) {
                            Text(r.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (on) Ds.ink else Ds.sub)
                        }
                    }
                }
            }
        }
        if (shown.isNotEmpty()) {
            item { Text(tr("Istorija", "History"), Modifier.padding(top = 6.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.44).sp, color = Ds.ink) }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Ds.s1)) {
                    shown.indices.reversed().forEach { i ->
                        val e = shown[i]
                        val day = LocalDate.ofEpochDay(e.epochDay)
                        Row(
                            Modifier.fillMaxWidth().clickable { sel = i }.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(day.srLong().replaceFirstChar { it.uppercase() }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink)
                                Text(day.srRelative(), fontSize = 13.sp, color = Ds.sub)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (byEntry[e.id].orEmpty().isNotEmpty()) Icon(Icons.Filled.PhotoCamera, tr("Ima slike", "Has photos"), Modifier.size(15.dp), tint = Ds.sub)
                                    Text("${fmt(e.weightKg!!)} kg", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink)
                                }
                                Text(shown.getOrNull(i - 1)?.let { "${sgn(e.weightKg!! - it.weightKg!!)} kg" } ?: tr("prvo", "first"), fontSize = 12.sp, color = Ds.sub)
                            }
                        }
                        if (i > 0) HorizontalDivider(thickness = 1.dp, color = Ds.line)
                    }
                }
            }
        }
    }

    val e = shown.getOrNull(selIdx)
    if (photosOpen && e != null) ModalBottomSheet(
        onDismissRequest = { photosOpen = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ds.sheet,
    ) {
        SecureWindow()
        PhotosSheet(LocalDate.ofEpochDay(e.epochDay), e.weightKg!!, byEntry[e.id].orEmpty(), onCompare = { photosOpen = false; onCompare(e.id) }) { photosOpen = false }
    }
}

/** Slike izabranog merenja (sifrovane; ekran je zasticen od screenshota dok je list otvoren). */
@Composable
private fun PhotosSheet(day: LocalDate, kg: Double, photos: List<BodyPhoto>, onCompare: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 34.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text(day.srLong().replaceFirstChar { it.uppercase() }, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.44).sp, color = Ds.ink)
            Text(kg.kg(), Modifier.padding(top = 2.dp), fontSize = 14.sp, color = Ds.sub)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pose.entries.forEach { p ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(14.dp)).background(Ds.s2), contentAlignment = Alignment.Center) {
                        PhotoContent(null, photos.firstOrNull { it.pose == p.key }, ContentScale.Crop, Modifier.fillMaxSize())
                    }
                    Text(p.label, Modifier.fillMaxWidth(), fontSize = 13.sp, color = Ds.sub, textAlign = TextAlign.Center)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, null, Modifier.size(12.dp), tint = Ds.sub)
            Text(tr("Slike ostaju šifrovane na ovom telefonu", "Photos stay encrypted on this phone"), fontSize = 12.sp, color = Ds.sub, maxLines = 1)
        }
        Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(Ds.s1).clickable(onClick = onCompare), contentAlignment = Alignment.Center) {
            Text(tr("Uporedi sa drugim merenjem", "Compare with another check-in"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText)
        }
        Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
            Text(tr("Zatvori", "Close"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink)
        }
    }
}
