package com.vukos.trening.ui.stats

import com.vukos.trening.i18n.tr
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.drawText
import androidx.compose.ui.input.pointer.pointerInput
import com.vukos.trening.ui.body.srLong

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.data.JournalStats
import com.vukos.trening.data.TrainingStats
import com.vukos.trening.data.WorkoutMath
import com.vukos.trening.data.muscleLabels
import com.vukos.trening.ui.body.srDate
import com.vukos.trening.ui.common.AppCard
import com.vukos.trening.ui.common.MONTHS
import com.vukos.trening.ui.common.SectionTitle
import com.vukos.trening.ui.common.StatTile
import java.time.LocalDate
import java.time.YearMonth

private enum class Period(private val sr: String, private val en: String) {
    WEEK("Nedelja", "Week"), MONTH("Mesec", "Month"), YEAR("Godina", "Year");

    val label: String get() = tr(sr, en)
}

/** Statistika: nedelja / mesec / godina, nedeljni cilj i niz, setovi po misicima, treninzi kroz vreme. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val db = TreningApp.instance.db
    val settings = TreningApp.instance.settings
    val workouts by remember { db.workouts().observeFinished() }.collectAsStateWithLifecycle(emptyList())
    val sets by remember { db.workouts().observeAllDoneSets() }.collectAsStateWithLifecycle(emptyList())
    val exercises by remember { db.exercises().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val primary = remember(exercises) { exercises.associate { it.id to it.primaryMuscle } }
    val today = LocalDate.now()
    val dates = remember(workouts) { workouts.map { JournalStats.dateOf(it) } }

    var period by rememberSaveable { mutableStateOf(Period.WEEK) }
    var offset by rememberSaveable { mutableIntStateOf(0) }   // 0 = tekuci period, -1 = prethodni...
    var goal by remember { mutableIntStateOf(settings.weeklyGoal) }

    val (from, to, title) = when (period) {
        Period.WEEK -> TrainingStats.weekStart(today).plusWeeks(offset.toLong()).let {
            Triple(it, it.plusDays(6), if (offset == 0) tr("Ova nedelja", "This week") else "${it.srDate()} – ${it.plusDays(6).srDate()}")
        }
        Period.MONTH -> YearMonth.from(today).plusMonths(offset.toLong()).let {
            Triple(it.atDay(1), it.atEndOfMonth(), "${MONTHS[it.monthValue - 1]} ${it.year}")
        }
        Period.YEAR -> (today.year + offset).let { Triple(LocalDate.of(it, 1, 1), LocalDate.of(it, 12, 31), "$it") }
    }
    val summary = remember(workouts, sets, from, to) { TrainingStats.summary(workouts, sets, from, to) }
    val muscles = remember(workouts, sets, primary, from, to) { TrainingStats.muscleSets(workouts, sets, primary, from, to) }
    val streak = remember(dates, goal) { TrainingStats.streak(dates, goal, today) }
    val thisWeek = dates.count { TrainingStats.weekStart(it) == TrainingStats.weekStart(today) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(tr("Statistika", "Statistics"), style = MaterialTheme.typography.titleLarge) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Nazad", "Back")) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
    }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { GoalCard(goal, thisWeek, streak) { g -> goal = g; settings.weeklyGoal = g } }
            item {
                com.vukos.trening.ui.common.SegmentedToggle(
                    Period.entries.map { it.label }, period.ordinal, { period = Period.entries[it]; offset = 0 },
                    Modifier.padding(top = 8.dp),
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { offset-- }) { Icon(Icons.Filled.ChevronLeft, tr("Prethodni", "Previous")) }
                    Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    IconButton(onClick = { offset++ }, enabled = offset < 0) { Icon(Icons.Filled.ChevronRight, tr("Sledeći", "Next")) }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("${summary.workouts}", tr("treninga", "workouts"), Modifier.weight(1f))
                    StatTile(JournalStats.fmtMinutes(summary.minutes), tr("vreme", "time"), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("${WorkoutMath.fmtKg(summary.volumeKg / 1000)} t", tr("podignuto", "lifted"), Modifier.weight(1f))
                    StatTile("${summary.sets}", tr("radnih setova", "working sets"), Modifier.weight(1f))
                }
            }
            item { SectionTitle(tr("Setovi po mišićima", "Sets per muscle")) }
            item {
                AppCard {
                    if (muscles.isEmpty()) Text(tr("Nema treninga u ovom periodu.", "No workouts in this period."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val max = muscles.maxOfOrNull { it.second } ?: 1
                    muscles.forEach { (m, n) ->
                        Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text((muscleLabels[m] ?: if (m == "ostalo") tr("ostalo", "other") else m).replaceFirstChar { it.uppercase() }, Modifier.width(110.dp),
                                style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Box(Modifier.weight(1f).height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                                Box(Modifier.fillMaxWidth(n.toFloat() / max).fillMaxHeight().clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            }
                            Text("$n", Modifier.width(36.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
            item {
                val fmt1 = { v: Double -> String.format(java.util.Locale.US, "%.1f", v).let { if (com.vukos.trening.i18n.Lang.en) it else it.replace('.', ',') } }
                when (period) {
                    Period.WEEK -> {
                        // Izabrana nedelja, dan po dan.
                        val days = (0..6).map { from.plusDays(it.toLong()) }
                        val values = days.map { d -> dates.count { it == d } }
                        val total = values.sum()
                        ActivityChart(
                            title = tr("Treninzi po danima", "Workouts by day"),
                            values = values,
                            labels = WEEKDAY_SHORT,
                            captions = days.map { it.srLong() },
                            highlight = days.indexOf(today),
                            goal = null,
                            stats = listOf(
                                "$total" to tr("treninga", "workouts"),
                                "${values.count { it > 0 }}" to tr("dana sa treningom", "active days"),
                                "$total/$goal" to tr("nedeljni cilj", "weekly goal"),
                            ),
                        )
                    }
                    Period.MONTH -> {
                        // Nedelje (pon-ned) koje padaju u izabrani mesec; broji se cela nedelja.
                        val weeks = generateSequence(TrainingStats.weekStart(from)) { it.plusWeeks(1) }.takeWhile { !it.isAfter(to) }.toList()
                        val values = weeks.map { w -> dates.count { !it.isBefore(w) && !it.isAfter(w.plusDays(6)) } }
                        val cur = weeks.indexOf(TrainingStats.weekStart(today))
                        val closed = weeks.indices.filter { weeks[it].plusDays(6).isBefore(today) }.map { values[it] }
                        ActivityChart(
                            title = tr("Treninzi po nedeljama", "Workouts by week"),
                            values = values,
                            labels = weeks.map { "${it.dayOfMonth}–${it.plusDays(6).dayOfMonth}" },
                            captions = weeks.map { tr("Nedelja ", "Week of ") + "${it.srDate()} – ${it.plusDays(6).srDate()}" },
                            highlight = cur,
                            goal = goal,
                            stats = listOf(
                                fmt1(if (closed.isEmpty()) 0.0 else closed.average()) to tr("prosek nedeljno", "weekly average"),
                                "${values.maxOrNull() ?: 0}" to tr("najviše", "best week"),
                                "${closed.count { it >= goal }}/${closed.size}" to tr("nedelja sa ciljem", "weeks on goal"),
                            ),
                        )
                    }
                    Period.YEAR -> {
                        val data = TrainingStats.monthlyCounts(dates, from.year)
                        val values = data.map { it.second }
                        val closed = data.filter { it.first.atEndOfMonth().isBefore(today) }.map { it.second }
                        ActivityChart(
                            title = tr("Treninzi po mesecima", "Workouts by month"),
                            values = values,
                            labels = data.map { MONTH_SHORT[it.first.monthValue - 1] },
                            captions = data.map { "${MONTHS[it.first.monthValue - 1]} ${it.first.year}" },
                            highlight = if (from.year == today.year) today.monthValue - 1 else -1,
                            goal = null,
                            stats = listOf(
                                "${values.sum()}" to tr("ukupno", "total"),
                                fmt1(if (closed.isEmpty()) 0.0 else closed.average()) to tr("prosek mesečno", "monthly average"),
                                "${values.maxOrNull() ?: 0}" to tr("najviše", "best month"),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalCard(goal: Int, thisWeek: Int, streak: Int, onGoal: (Int) -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Nedeljni cilj", "Weekly goal"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(tr("$thisWeek / $goal treninga", "$thisWeek / $goal workouts"), style = MaterialTheme.typography.headlineSmall)
            }
            RoundBtn(Icons.Filled.Remove, tr("Manje", "Fewer")) { if (goal > 1) onGoal(goal - 1) }
            Spacer(Modifier.width(8.dp))
            RoundBtn(Icons.Filled.Add, tr("Više", "More")) { if (goal < 14) onGoal(goal + 1) }
        }
        Spacer(Modifier.height(12.dp))
        WeekProgress(thisWeek, goal)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = if (streak > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(
                when (streak) { 0 -> tr("Ispuni cilj ove nedelje i počni niz", "Hit your goal this week to start a streak"); 1 -> tr("1 nedelja zaredom", "1 week in a row"); else -> tr("$streak nedelja zaredom", "$streak weeks in a row") },
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

/** Segmenti napretka ka nedeljnom cilju. */
@Composable
fun WeekProgress(done: Int, goal: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(goal.coerceAtLeast(1)) { i ->
            Box(
                Modifier.weight(1f).height(8.dp).clip(CircleShape)
                    .background(if (i < done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
            )
        }
    }
}

@Composable
private fun RoundBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(18.dp)) }
}

private val WEEKDAY_SHORT: List<String> get() = if (com.vukos.trening.i18n.Lang.en)
    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
else listOf("pon", "uto", "sre", "čet", "pet", "sub", "ned")

private val MONTH_SHORT: List<String> get() = if (com.vukos.trening.i18n.Lang.en)
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
else listOf("jan", "feb", "mar", "apr", "maj", "jun", "jul", "avg", "sep", "okt", "nov", "dec")

/** "1 trening" / "3 treninga" (ili engleski). */
private fun workoutsText(n: Int) = if (n == 1) tr("1 trening", "1 workout") else tr("$n treninga", "$n workouts")

/**
 * Grafikon aktivnosti po uzoru na Apple Health / Hevy:
 *  - tri brojke iznad, pa "oblacic" sa vrednoscu izabranog stubica (dodir ili prevlacenje bira stubic)
 *  - tanke horizontalne linije mreze sa brojevima desno, isprekidana linija cilja
 *  - nula = nema stubica (bez sivih "pilula"), oznake na x osi se nikad ne seku - preskacu se ako nema mesta
 */
@Composable
private fun ActivityChart(
    title: String, values: List<Int>, labels: List<String>, captions: List<String>,
    highlight: Int, goal: Int?, stats: List<Pair<String, String>>,
) {
    val accent = MaterialTheme.colorScheme.primary
    val onVar = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val total = values.sum()
    val best = values.maxOrNull() ?: 0
    var selected by remember(values, labels) { mutableIntStateOf(if (highlight in values.indices) highlight else values.indexOfLast { it > 0 }) }

    AppCard {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Row { stats.forEach { (v, l) -> MiniStat(v, l, Modifier.weight(1f)) } }
        Spacer(Modifier.height(20.dp))
        if (total == 0) {
            Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center) {
                Text(tr("Još nema treninga u ovom periodu.\nZavrši trening i ovde se pojavljuje stubić.", "No workouts in this period yet.\nFinish a workout and a bar appears here."),
                    style = MaterialTheme.typography.bodySmall, color = onVar, textAlign = TextAlign.Center)
            }
            return@AppCard
        }

        // Oblacic: vrednost izabranog stubica
        val sel = selected.takeIf { it in values.indices }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 10.dp)) {
            if (sel != null) {
                Text(workoutsText(values[sel]), style = MaterialTheme.typography.titleMedium)
                Text(captions[sel], style = MaterialTheme.typography.bodySmall, color = onVar)
            } else Text(tr("Dodirni stubić za detalje", "Tap a bar for details"), style = MaterialTheme.typography.bodySmall, color = onVar)
        }
        Spacer(Modifier.height(14.dp))

        val measurer = androidx.compose.ui.text.rememberTextMeasurer()
        val axisStyle = MaterialTheme.typography.labelSmall.copy(color = onVar)
        val selStyle = MaterialTheme.typography.labelSmall.copy(color = accent, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        // Bez y-ose: broj stoji iznad svakog stubica, pa je grafikon simetrican i citljiv.
        // Najmanja skala 2, da jedan trening ne bude stubic preko cele visine.
        val top = maxOf(best, goal ?: 0, 2)
        val n = values.size

        androidx.compose.foundation.Canvas(
            Modifier.fillMaxWidth().height(170.dp)
                .pointerInput(n) {
                    detectTapGestures { o -> selected = barAt(o.x, size.width.toFloat(), 0f, n) }
                }
                .pointerInput(n) {
                    detectHorizontalDragGestures { ch, _ -> selected = barAt(ch.position.x, size.width.toFloat(), 0f, n) }
                }
        ) {
            val labelH = 24.dp.toPx()           // oznake ispod
            val headroom = 20.dp.toPx()         // mesto za broj iznad najviseg stubica
            val plotW = size.width
            val plotH = size.height - labelH
            val slot = plotW / n
            val barW = (slot * 0.56f).coerceAtMost(26.dp.toPx())
            fun y(v: Float) = plotH - (plotH - headroom) * v / top

            // Osnovna linija
            drawLine(grid, Offset(0f, plotH), Offset(plotW, plotH), strokeWidth = 1.dp.toPx())
            // Isprekidana linija cilja
            if (goal != null && goal > 0) {
                val yy = y(goal.toFloat())
                drawLine(accent.copy(alpha = 0.6f), Offset(0f, yy), Offset(plotW, yy), strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())))
            }
            // Stubici
            values.forEachIndexed { i, v ->
                if (v <= 0) return@forEachIndexed
                val cx = slot * i + slot / 2
                val h = plotH - y(v.toFloat())
                val color = when {
                    i == sel -> accent
                    goal != null && v >= goal -> accent.copy(alpha = 0.7f)
                    else -> accent.copy(alpha = 0.45f)
                }
                val r = minOf(barW / 2, 6.dp.toPx())
                drawPath(Path().apply {
                    addRoundRect(RoundRect(Rect(cx - barW / 2, plotH - h, cx + barW / 2, plotH),
                        topLeft = CornerRadius(r), topRight = CornerRadius(r)))
                }, color)
                val t = measurer.measure("$v", if (i == sel) selStyle else axisStyle)
                drawText(t, topLeft = Offset(cx - t.size.width / 2f, plotH - h - t.size.height - 3.dp.toPx()))
            }
            // Oznake ispod: samo kada ima mesta (izabrani i tekuci uvek), bez preklapanja
            val measured = labels.map { measurer.measure(it, axisStyle) }
            val gap = 6.dp.toPx()
            val drawn = mutableListOf<ClosedFloatingPointRange<Float>>()
            val order = (listOfNotNull(sel, highlight.takeIf { it in labels.indices }) + labels.indices).distinct()
            order.forEach { i ->
                if (labels[i].isEmpty()) return@forEach
                val w = measured[i].size.width.toFloat()
                val left = (slot * i + slot / 2 - w / 2).coerceIn(0f, plotW - w)
                val range = (left - gap)..(left + w + gap)
                if (drawn.any { it.start < range.endInclusive && range.start < it.endInclusive }) return@forEach
                drawn += range
                val t = if (i == sel) measurer.measure(labels[i], selStyle) else measured[i]
                drawText(t, topLeft = Offset(left, plotH + 7.dp.toPx()))
            }
        }
        if (goal != null) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Canvas(Modifier.width(18.dp).height(2.dp)) {
                    drawLine(accent, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = size.height,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx())))
                }
                Spacer(Modifier.width(8.dp))
                Text(tr("Cilj: ${workoutsText(goal)} nedeljno", "Goal: ${workoutsText(goal)} per week"), style = MaterialTheme.typography.labelMedium, color = onVar)
            }
        }
    }
}

private fun barAt(x: Float, width: Float, axisW: Float, n: Int): Int =
    ((x / ((width - axisW) / n)).toInt()).coerceIn(0, n - 1)

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}
