package com.vukos.trening.ui.stats

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

private enum class Period(val label: String) { WEEK("Nedelja"), MONTH("Mesec"), YEAR("Godina") }

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
            Triple(it, it.plusDays(6), if (offset == 0) "Ova nedelja" else "${it.srDate()} – ${it.plusDays(6).srDate()}")
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
            title = { Text("Statistika", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Nazad") } },
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
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Period.entries.forEachIndexed { i, p ->
                        SegmentedButton(
                            selected = period == p, onClick = { period = p; offset = 0 },
                            shape = SegmentedButtonDefaults.itemShape(i, Period.entries.size),
                        ) { Text(p.label) }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { offset-- }) { Icon(Icons.Filled.ChevronLeft, "Prethodni") }
                    Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    IconButton(onClick = { offset++ }, enabled = offset < 0) { Icon(Icons.Filled.ChevronRight, "Sledeći") }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("${summary.workouts}", "treninga", Modifier.weight(1f))
                    StatTile(JournalStats.fmtMinutes(summary.minutes), "vreme", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("${WorkoutMath.fmtKg(summary.volumeKg / 1000)} t", "podignuto", Modifier.weight(1f))
                    StatTile("${summary.sets}", "radnih setova", Modifier.weight(1f))
                }
            }
            item { SectionTitle("Setovi po mišićima") }
            item {
                AppCard {
                    if (muscles.isEmpty()) Text("Nema treninga u ovom periodu.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            item { SectionTitle(if (period == Period.YEAR) "Treninzi po mesecima" else "Treninzi po nedeljama") }
            item {
                AppCard {
                    if (period == Period.YEAR) {
                        val data = TrainingStats.monthlyCounts(dates, from.year)
                        Bars(data.map { it.second }, data.map { MONTHS[it.first.monthValue - 1].take(1) }, null)
                    } else {
                        val data = TrainingStats.weeklyCounts(dates, today, 12)
                        Bars(data.map { it.second }, data.map { "${it.first.dayOfMonth}.${it.first.monthValue}" }, goal)
                        Text("Poslednjih 12 nedelja · linija = cilj ($goal)", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
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
                Text("Nedeljni cilj", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$thisWeek / $goal treninga", style = MaterialTheme.typography.headlineSmall)
            }
            RoundBtn(Icons.Filled.Remove, "Manje") { if (goal > 1) onGoal(goal - 1) }
            Spacer(Modifier.width(8.dp))
            RoundBtn(Icons.Filled.Add, "Više") { if (goal < 14) onGoal(goal + 1) }
        }
        Spacer(Modifier.height(12.dp))
        WeekProgress(thisWeek, goal)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = if (streak > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(
                when (streak) { 0 -> "Ispuni cilj ove nedelje i počni niz"; 1 -> "1 nedelja zaredom"; else -> "$streak nedelja zaredom" },
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

/** Stubici (sa opcionom linijom cilja). */
@Composable
private fun Bars(values: List<Int>, labels: List<String>, goal: Int?) {
    val max = maxOf(values.maxOrNull() ?: 0, goal ?: 0, 1)
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(Modifier.fillMaxWidth().height(140.dp)) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            values.forEach { v ->
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (v > 0) Text("$v", style = MaterialTheme.typography.labelSmall)
                    Box(
                        Modifier.fillMaxWidth().fillMaxHeight((v.toFloat() / max * 0.85f).coerceAtLeast(0.03f))
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (goal != null && v >= goal) accent else if (v > 0) accent.copy(alpha = 0.45f) else muted)
                    )
                }
            }
        }
        if (goal != null && goal > 0) {
            val frac = goal.toFloat() / max * 0.85f
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(1f - frac))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.35f)))
                Spacer(Modifier.weight(frac.coerceAtLeast(0.001f)))
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, l ->
            Text(if (labels.size <= 12 && (labels.size <= 7 || i % 2 == 0)) l else "", Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}
