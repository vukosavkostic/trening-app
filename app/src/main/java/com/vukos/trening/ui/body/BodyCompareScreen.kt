package com.vukos.trening.ui.body

import androidx.compose.ui.unit.sp

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.draw.shadow

import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.material.icons.filled.ViewColumn

import androidx.compose.material.icons.filled.UnfoldMore

import androidx.compose.material.icons.filled.SwapHoriz

import androidx.compose.material.icons.filled.Lock

import androidx.compose.material.icons.filled.ChevronLeft

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.clickable

import androidx.compose.foundation.border

import com.vukos.trening.i18n.tr

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.data.BodyPhoto
import com.vukos.trening.data.Pose
import com.vukos.trening.data.SecurePhotoStore
import com.vukos.trening.ui.common.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Uporedi slike (dizajn v2.1): klizac (posle preko pre) ili jedna pored druge, izbor poze,
 * kartice Pre / Posle (aktivna ima okvir) i traka merenja sa slikama; brojke ispod.
 * [afterId]: merenje otvoreno iz lista sa slikama postaje Posle, najranije Pre, a fokus je na Pre.
 */
@Composable
fun BodyCompareScreen(onBack: () -> Unit, afterId: String? = null) {
    SecureWindow()
    val dao = TreningApp.instance.db.body()
    val entries by remember { dao.observeAll() }.collectAsStateWithLifecycle(emptyList())
    val photos by remember { dao.observeAllPhotos() }.collectAsStateWithLifecycle(emptyList())
    val byEntry = remember(photos) { photos.groupBy { it.bodyEntryId } }
    val days = remember(entries, byEntry) { entries.filter { !it.deleted && byEntry.containsKey(it.id) }.sortedBy { it.epochDay } }
    val D = com.vukos.trening.ui.theme.Ds
    val accent = com.vukos.trening.ui.theme.Accent

    var before by rememberSaveable { mutableStateOf<Long?>(null) }
    var after by rememberSaveable { mutableStateOf<Long?>(null) }
    var slotBefore by rememberSaveable { mutableStateOf(false) }
    var pose by rememberSaveable { mutableStateOf(Pose.FRONT) }
    var sideBySide by rememberSaveable { mutableStateOf(false) }
    var split by remember { mutableFloatStateOf(0.5f) }
    LaunchedEffect(days) {
        if (days.size >= 2 && before == null && after == null) {
            val opened = days.firstOrNull { it.id == afterId }
            before = days.first().epochDay
            after = (opened ?: days.last()).epochDay
            if (opened != null) slotBefore = true
            if (before == after) before = days.firstOrNull { it.epochDay != after }?.epochDay
        }
    }
    val eBefore = days.firstOrNull { it.epochDay == before }
    val eAfter = days.firstOrNull { it.epochDay == after }
    val pBefore = eBefore?.let { e -> byEntry[e.id]?.firstOrNull { it.pose == pose.key } }
    val pAfter = eAfter?.let { e -> byEntry[e.id]?.firstOrNull { it.pose == pose.key } }
    val imgBefore = rememberDecrypted(pBefore)
    val imgAfter = rememberDecrypted(pAfter)
    val dB = eBefore?.let { LocalDate.ofEpochDay(it.epochDay).srDate() } ?: "—"
    val dA = eAfter?.let { LocalDate.ofEpochDay(it.epochDay).srDate() } ?: "—"

    Column(
        Modifier.fillMaxSize().background(D.bg).statusBarsPadding().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(D.s1).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ChevronLeft, tr("Nazad", "Back"), Modifier.size(20.dp), tint = D.ink)
            }
            Spacer(Modifier.weight(1f))
            if (days.size >= 2) Row(
                Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(D.s1).clickable { val t = before; before = after; after = t }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.SwapHoriz, null, Modifier.size(15.dp), tint = D.ink)
                Text(tr("Zameni", "Swap"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = D.ink)
            }
        }
        Column {
            Text(tr("Uporedi slike", "Compare photos"), fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.68).sp, color = D.ink)
            if (days.size >= 2) Text(tr("${pose.label}, $dB do $dA", "${pose.label} view, $dB to $dA"), Modifier.padding(top = 2.dp), fontSize = 15.sp, color = D.sub)
        }
        if (days.size < 2) {
            Text(tr("Potrebna su bar dva dana sa slikama.", "You need at least two days with photos."), fontSize = 15.sp, color = D.sub)
            return@Column
        }
        Segments(Pose.entries.map { it.label }, Pose.entries.indexOf(pose)) { pose = Pose.entries[it] }
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(24.dp)).background(D.s2)) {
            if (!sideBySide) {
                Box(
                    Modifier.fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures { split = (it.x / size.width).coerceIn(0f, 1f) } }
                        .pointerInput(Unit) { detectDragGestures { change, _ -> split = (change.position.x / size.width).coerceIn(0f, 1f) } },
                ) {
                    if (imgBefore != null) Image(imgBefore, tr("Pre", "Before"), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    if (imgAfter != null) Image(
                        imgAfter, tr("Posle", "After"),
                        Modifier.fillMaxSize().drawWithContent { clipRect(left = size.width * split) { this@drawWithContent.drawContent() } },
                        contentScale = ContentScale.Crop,
                    )
                    // Granica sa okruglom ruckom.
                    Box(Modifier.fillMaxHeight().fillMaxWidth(split).align(Alignment.CenterStart)) {
                        Box(Modifier.fillMaxHeight().width(2.dp).offset(x = 1.dp).background(Color.White).align(Alignment.CenterEnd))
                        Box(
                            Modifier.align(Alignment.CenterEnd).offset(x = 22.dp).size(44.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.UnfoldMore, null, Modifier.size(20.dp).graphicsLayer { rotationZ = 90f }, tint = Color(0xFF111113)) }
                    }
                    Tag(tr("Pre", "Before"), dB, Modifier.align(Alignment.TopStart).padding(12.dp))
                    Tag(tr("Posle", "After"), dA, Modifier.align(Alignment.TopEnd).padding(12.dp))
                }
            } else Row(Modifier.fillMaxSize().background(D.bg), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight().background(D.s2)) {
                    if (imgBefore != null) Image(imgBefore, tr("Pre", "Before"), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Tag(null, dB, Modifier.align(Alignment.TopStart).padding(10.dp))
                }
                Box(Modifier.weight(1f).fillMaxHeight().background(D.s2)) {
                    if (imgAfter != null) Image(imgAfter, tr("Posle", "After"), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Tag(null, dA, Modifier.align(Alignment.TopEnd).padding(10.dp))
                }
            }
            if (pBefore == null || pAfter == null) Text(
                tr("Nema slike „${pose.label}“ za ", "No “${pose.label}” photo for ") + listOfNotNull(tr("pre", "before").takeIf { pBefore == null }, tr("posle", "after").takeIf { pAfter == null }).joinToString(tr(" i ", " and ")),
                Modifier.align(Alignment.Center).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.6f)).padding(10.dp), color = Color.White, fontSize = 14.sp,
            )
        }
        Segments(listOf(tr("Klizač", "Slider"), tr("Jedna pored druge", "Side by side")), if (sideBySide) 1 else 0, icons = listOf(Icons.Filled.UnfoldMore, Icons.Filled.ViewColumn)) {
            sideBySide = it == 1
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Triple(true, tr("Pre", "Before"), eBefore), Triple(false, tr("Posle", "After"), eAfter)).forEach { (isB, kicker, e) ->
                val active = slotBefore == isB
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(D.s1)
                        .border(1.5.dp, if (active) accent else Color.Transparent, RoundedCornerShape(20.dp)).clickable { slotBefore = isB }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    com.vukos.trening.ui.common.Kicker(kicker, if (active) D.aText else D.sub)
                    Text(e?.let { LocalDate.ofEpochDay(it.epochDay).srDate() } ?: "—", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = D.ink)
                    Text(e?.weightKg?.kg() ?: "—", fontSize = 13.sp, color = D.sub)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (slotBefore) tr("Izaberi merenje Pre", "Pick the Before check-in") else tr("Izaberi merenje Posle", "Pick the After check-in"),
                Modifier.padding(horizontal = 4.dp), fontSize = 13.sp, color = D.sub)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(days, key = { it.id }) { e ->
                    val isB = e.epochDay == before
                    val isA = e.epochDay == after
                    val act = if (slotBefore) isB else isA
                    Column(
                        Modifier.clip(RoundedCornerShape(14.dp)).clickable {
                            if (slotBefore) { before = e.epochDay; slotBefore = false } else after = e.epochDay
                        },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.size(60.dp, 80.dp).clip(RoundedCornerShape(14.dp)).background(D.s2)
                            .border(2.dp, if (act) accent else Color.Transparent, RoundedCornerShape(14.dp))) {
                            PhotoContent(null, byEntry[e.id]?.firstOrNull { it.pose == pose.key }, ContentScale.Crop, Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                            if (isA || isB) Text(
                                if (isB) tr("P", "B") else tr("PO", "A"),
                                Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).clip(RoundedCornerShape(5.dp)).background(if (isA) accent else D.ink).padding(horizontal = 5.dp),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 15.sp, color = if (isA) Color.White else D.bg,
                            )
                        }
                        Text(LocalDate.ofEpochDay(e.epochDay).srShort(), fontSize = 12.sp, fontWeight = if (act) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (act) D.aText else D.sub, maxLines = 1)
                    }
                }
            }
        }
        if (eBefore != null && eAfter != null) {
            val gap = kotlin.math.abs(eAfter.epochDay - eBefore.epochDay)
            val lo = minOf(eBefore.epochDay, eAfter.epochDay)
            val hi = maxOf(eBefore.epochDay, eAfter.epochDay)
            val between = entries.count { !it.deleted && it.epochDay in (lo + 1) until hi }
            val wB = eBefore.weightKg
            val wA = eAfter.weightKg
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(D.s1).padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    Triple(if (wB != null && wA != null) (wA - wB).signed() else "—", tr("promena kilaže", "weight change"), Alignment.Start),
                    Triple(if (gap >= 14) tr("${gap / 7} ned.", "${(gap + 3) / 7} weeks") else tr("$gap dana", "$gap days"), tr("između slika", "between photos"), Alignment.CenterHorizontally),
                    Triple("$between", tr("merenja između", "check-ins between"), Alignment.End),
                ).forEach { (v, l, al) ->
                    Column(Modifier.weight(1f), horizontalAlignment = al) {
                        Text(v, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.44).sp, color = D.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(l, Modifier.padding(top = 2.dp), fontSize = 13.sp, color = D.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun Tag(label: String?, date: String, modifier: Modifier) {
    Row(modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label != null) Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Text(date, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = if (label != null) 0.75f else 1f))
    }
}

@Composable
private fun Segments(options: List<String>, selected: Int, icons: List<androidx.compose.ui.graphics.vector.ImageVector>? = null, onPick: (Int) -> Unit) {
    val D = com.vukos.trening.ui.theme.Ds
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(D.s1).padding(3.dp)) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            Row(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(9.dp)).background(if (on) D.s3 else Color.Transparent).clickable { onPick(i) },
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                icons?.getOrNull(i)?.let { Icon(it, null, Modifier.size(15.dp).graphicsLayer { if (i == 0) rotationZ = 90f }, tint = if (on) D.ink else D.sub) }
                Text(o, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) D.ink else D.sub, maxLines = 1)
            }
        }
    }
}

/** Desifruje sliku iz sigurnog skladista (van glavne niti). */
@Composable
private fun rememberDecrypted(photo: BodyPhoto?): ImageBitmap? {
    val img by produceState<ImageBitmap?>(null, photo?.id) {
        value = photo?.let { p -> withContext(Dispatchers.IO) { SecurePhotoStore.load(p.localPath)?.asImageBitmap() } }
    }
    return img
}
