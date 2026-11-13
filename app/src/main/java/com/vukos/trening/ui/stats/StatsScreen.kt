package com.vukos.trening.ui.stats

import com.vukos.trening.i18n.tr

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
                            Text((muscleLabels[m] ?: m).replaceFirstChar { it.uppercase() }, Modifier.width(110.dp),
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
                if (period == Period.YEAR) {
                    val data = TrainingStats.monthlyCounts(dates, from.year)
                    ActivityChart(
                        title = tr("Treninzi u ${from.year}.", "Workouts in ${from.year}"),
                        subtitle = tr("Broj treninga po mesecima", "Workouts per month"),
                        values = data.map { it.second },
                        labels = data.map { MONTH_SHORT[it.first.monthValue - 1] },
                        highlight = if (from.year == today.year) today.monthValue - 1 else -1,
                        goal = null,
                        unit = tr("mesečno", "per month"),
                    )
                } else {
                    val data = TrainingStats.weeklyCounts(dates, today, 12)
                    // Oznaka meseca samo na prvoj nedelji u mesecu; tekuca nedelja = "ova".
                    val labels = data.mapIndexed { i, (start, _) ->
                        when {
                            i == data.lastIndex -> tr("ova", "now")
                            i == 0 || start.monthValue != data[i - 1].first.monthValue -> MONTH_SHORT[start.monthValue - 1]
                            else -> ""
                        }
                    }
                    ActivityChart(
                        title = tr("Aktivnost", "Activity"),
                        subtitle = tr("Broj treninga po nedelji · poslednjih 12 nedelja", "Workouts per week · last 12 weeks"),
                        values = data.map { it.second },
                        labels = labels,
                        highlight = data.lastIndex,
                        goal = goal,
                        unit = tr("nedeljno", "per week"),
                    )
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

private val MONTH_SHORT: List<String> get() = if (com.vukos.trening.i18n.Lang.en)
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
else listOf("jan", "feb", "mar", "apr", "maj", "jun", "jul", "avg", "sep", "okt", "nov", "dec")

/**
 * Grafikon aktivnosti: naslov i objasnjenje, tri brojke (prosek, najbolje, cilj ispunjen),
 * stubici sa vrednoscu iznad, isprekidana linija cilja sa natpisom i oznake meseci na x osi.
 */
@Composable
private fun ActivityChart(
    title: String, subtitle: String, values: List<Int>, labels: List<String>,
    highlight: Int, goal: Int?, unit: String,
) {
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.surfaceContainerHighest
    val onVar = MaterialTheme.colorScheme.onSurfaceVariant
    val total = values.sum()
    // Prosek bez tekuceg (nezavrsenog) perioda, ako ga ima.
    val closed = if (highlight in values.indices) values.filterIndexed { i, _ -> i != highlight } else values
    val avg = if (closed.isEmpty()) 0.0 else closed.sum().toDouble() / closed.size
    val best = values.maxOrNull() ?: 0
    val met = if (goal != null) closed.count { it >= goal } else 0
    val top = maxOf(best, goal ?: 0, 1)

    AppCard {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = onVar)
        Spacer(Modifier.height(16.dp))
        Row {
            MiniStat(String.format(java.util.Locale.US, "%.1f", avg).replace('.', ','), tr("prosek $unit", "average $unit"), Modifier.weight(1f))
            MiniStat("$best", tr("najviše", "best"), Modifier.weight(1f))
            if (goal != null) MiniStat("$met/${closed.size}", tr("nedelja sa ciljem", "weeks on goal"), Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        if (total == 0) {
            Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center) {
                Text(tr("Još nema treninga u ovom periodu.\nZavrši trening i ovde se pojavljuje stubić.", "No workouts in this period yet.\nFinish a workout and a bar appears here."),
                    style = MaterialTheme.typography.bodySmall, color = onVar, textAlign = TextAlign.Center)
            }
            return@AppCard
        }
        val chartH = 150.dp
        Box(Modifier.fillMaxWidth().height(chartH)) {
            // Isprekidana linija cilja + natpis
            if (goal != null && goal > 0) {
                val lineColor = Color.White.copy(alpha = 0.5f)
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val usable = size.height - 18.dp.toPx()
                    val y = size.height - usable * goal / top
                    drawLine(lineColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                }
            }
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                values.forEachIndexed { i, v ->
                    val hit = goal != null && v >= goal
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (v > 0) "$v" else "", style = MaterialTheme.typography.labelSmall,
                            color = if (i == highlight) accent else onVar)
                        Spacer(Modifier.height(2.dp))
                        val frac = v.toFloat() / top
                        Box(
                            Modifier.fillMaxWidth().fillMaxHeight(if (v == 0) 0.04f else (frac * 0.88f).coerceAtLeast(0.06f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                .background(
                                    when {
                                        v == 0 -> muted
                                        i == highlight -> accent
                                        hit || goal == null -> accent.copy(alpha = 0.75f)
                                        else -> accent.copy(alpha = 0.35f)
                                    }
                                )
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEachIndexed { i, l ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Text(l, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false,
                        color = if (i == highlight) accent else onVar)
                }
            }
        }
        if (goal != null) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(18.dp).height(2.dp).background(Color.White.copy(alpha = 0.5f)))
                Text(tr("  cilj: $goal treninga nedeljno", "  goal: $goal workouts per week"), style = MaterialTheme.typography.labelMedium, color = onVar)
                Spacer(Modifier.width(14.dp))
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(accent))
                Text(tr("  ova nedelja", "  this week"), style = MaterialTheme.typography.labelMedium, color = onVar)
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}
