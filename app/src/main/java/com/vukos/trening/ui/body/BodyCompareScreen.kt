package com.vukos.trening.ui.body

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

/** Poredjenje slika dva dana: "posle" preko "pre", klizacem se pomera granica. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyCompareScreen(onBack: () -> Unit) {
    SecureWindow()
    val dao = TreningApp.instance.db.body()
    val entries by remember { dao.observeAll() }.collectAsStateWithLifecycle(emptyList())
    val photos by remember { dao.observeAllPhotos() }.collectAsStateWithLifecycle(emptyList())
    val byEntry = remember(photos) { photos.groupBy { it.bodyEntryId } }
    val days = remember(entries, byEntry) { entries.filter { byEntry.containsKey(it.id) }.sortedBy { it.epochDay } }

    var before by remember { mutableStateOf<Long?>(null) }
    var after by remember { mutableStateOf<Long?>(null) }
    var pose by remember { mutableStateOf(Pose.FRONT) }
    var split by remember { mutableFloatStateOf(0.5f) }
    LaunchedEffect(days) {
        if (days.size >= 2) {
            if (before == null) before = days.first().epochDay
            if (after == null) after = days.last().epochDay
        }
    }
    val eBefore = days.firstOrNull { it.epochDay == before }
    val eAfter = days.firstOrNull { it.epochDay == after }
    val pBefore = eBefore?.let { e -> byEntry[e.id]?.firstOrNull { it.pose == pose.key } }
    val pAfter = eAfter?.let { e -> byEntry[e.id]?.firstOrNull { it.pose == pose.key } }
    val imgBefore = rememberDecrypted(pBefore)
    val imgAfter = rememberDecrypted(pAfter)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(tr("Uporedi slike", "Compare photos"), style = MaterialTheme.typography.titleLarge) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Nazad", "Back")) } },
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (days.size < 2) {
                Text(tr("Potrebna su bar dva dana sa slikama.", "You need at least two days with photos."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pose.entries.forEach { p -> FilterChip(selected = pose == p, onClick = { pose = p }, label = { Text(p.label) }) }
            }
            Box(
                Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .pointerInput(Unit) {
                        detectTapGestures { split = (it.x / size.width).coerceIn(0f, 1f) }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ -> split = (change.position.x / size.width).coerceIn(0f, 1f) }
                    },
            ) {
                if (imgAfter != null) Image(imgAfter, tr("Posle", "After"), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                if (imgBefore != null) Image(
                    imgBefore, tr("Pre", "Before"),
                    Modifier.fillMaxSize().drawWithContent { clipRect(right = size.width * split) { this@drawWithContent.drawContent() } },
                    contentScale = ContentScale.Crop,
                )
                // Granica
                Box(Modifier.fillMaxHeight().fillMaxWidth(split).align(Alignment.CenterStart)) {
                    Box(Modifier.fillMaxHeight().width(2.dp).background(Color.White).align(Alignment.CenterEnd))
                }
                Row(Modifier.fillMaxWidth().padding(12.dp)) {
                    Chip(tr("Pre", "Before"), color = Color.White, bg = Color.Black.copy(alpha = 0.5f))
                    Spacer(Modifier.weight(1f))
                    Chip(tr("Posle", "After"), color = Color.White, bg = Color.Black.copy(alpha = 0.5f))
                }
                if (pBefore == null || pAfter == null) Text(
                    tr("Nema slike „${pose.label}“ za ", "No “${pose.label}” photo for ") + listOfNotNull(tr("pre", "before").takeIf { pBefore == null }, tr("posle", "after").takeIf { pAfter == null }).joinToString(" i "),
                    color = Color.White, modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.6f)).padding(10.dp),
                )
            }
            Slider(value = split, onValueChange = { split = it })
            val wB = eBefore?.weightKg
            val wA = eAfter?.weightKg
            if (eBefore != null && eAfter != null) {
                val weeks = (eAfter.epochDay - eBefore.epochDay) / 7
                Text(
                    "${LocalDate.ofEpochDay(eBefore.epochDay).srDate()} → ${LocalDate.ofEpochDay(eAfter.epochDay).srDate()}" +
                        (if (weeks > 0) tr("\nRazmak: $weeks ${if (weeks == 1L) "nedelja" else if (weeks in 2..4) "nedelje" else "nedelja"}", "\nGap: $weeks ${if (weeks == 1L) "week" else "weeks"}") else "") +
                        (if (wB != null && wA != null) tr("\nPromena kilaže: ", "\nWeight change: ") + (wA - wB).signed() else ""),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            DayPicker(tr("Pre", "Before"), days.map { it.epochDay }, before) { before = it }
            DayPicker(tr("Posle", "After"), days.map { it.epochDay }, after) { after = it }
        }
    }
}

@Composable
private fun DayPicker(label: String, days: List<Long>, selected: Long?, onPick: (Long) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(days.reversed()) { d ->
                FilterChip(selected = d == selected, onClick = { onPick(d) }, label = { Text(LocalDate.ofEpochDay(d).srDate()) })
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
