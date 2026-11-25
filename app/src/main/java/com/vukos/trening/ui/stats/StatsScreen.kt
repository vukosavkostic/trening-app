package com.vukos.trening.ui.stats

import androidx.compose.ui.text.style.TextOverflow

import com.vukos.trening.ui.body.srShort

import androidx.compose.ui.text.font.FontWeight

import kotlin.math.roundToInt

import com.vukos.trening.ui.theme.Manrope

import androidx.compose.ui.text.TextStyle

import androidx.compose.ui.unit.sp

import androidx.compose.foundation.layout.offset

import androidx.compose.foundation.layout.BoxWithConstraints

import androidx.compose.foundation.layout.statusBarsPadding

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

/** Statistika (dizajn v2): nedelja / mesec / godina, brojke, setovi po misicima sa ciljnim opsegom, treninzi kroz vreme, aktivnost. */
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
    val D = com.vukos.trening.ui.theme.Ds
    val goal = remember { settings.weeklyGoal }

    var period by rememberSaveable { mutableStateOf(Period.WEEK) }
    var offset by rememberSaveable { mutableIntStateOf(0) }   // 0 = tekuci period, -1 = prethodni...

    val (from, to, title) = when (period) {
        Period.WEEK -> TrainingStats.weekStart(today).plusWeeks(offset.toLong()).let {
            Triple(it, it.plusDays(6), (if (offset == 0) tr("Ova nedelja, ", "This week, ") else "") + shortRange(it, it.plusDays(6)))
        }
        Period.MONTH -> YearMonth.from(today).plusMonths(offset.toLong()).let {
            Triple(it.atDay(1), it.atEndOfMonth(), "${MONTHS[it.monthValue - 1]} ${it.year}")
        }
        Period.YEAR -> (today.year + offset).let { Triple(LocalDate.of(it, 1, 1), LocalDate.of(it, 12, 31), "$it") }
    }
    val summary = remember(workouts, sets, from, to) { TrainingStats.summary(workouts, sets, from, to) }
    val secondary = remember(exercises) {
        exercises.filter { it.secondaryMuscles.isNotBlank() }.associate { e -> e.id to e.secondaryMuscles.split(',').map { it.trim() }.filter { it.isNotEmpty() } }
    }
    val muscles = remember(workouts, sets, primary, secondary, from, to) { TrainingStats.muscleSets(workouts, sets, primary, from, to, secondaryMuscles = secondary) }
    val fmt1 = { v: Double -> String.format(java.util.Locale.US, "%.1f", v).let { if (com.vukos.trening.i18n.Lang.en) it else it.replace('.', ',') } }

    LazyColumn(
        Modifier.fillMaxSize().background(D.bg).statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Box(Modifier.size(40.dp).clip(CircleShape).background(D.s1).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ChevronLeft, tr("Nazad", "Back"), Modifier.size(20.dp), tint = D.ink)
            }
        }
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Statistika", "Statistics"), fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.68).sp, color = D.ink)
                    Text(title, Modifier.padding(top = 2.dp), fontSize = 15.sp, color = D.sub, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Row(Modifier.padding(bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallRound(Icons.Filled.ChevronLeft, tr("Prethodni", "Previous"), true) { offset-- }
                    SmallRound(Icons.Filled.ChevronRight, tr("Sledeći", "Next"), offset < 0) { offset++ }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(D.s1).padding(3.dp)) {
                Period.entries.forEach { p ->
                    val sel = p == period
                    Box(Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(9.dp)).background(if (sel) D.s3 else Color.Transparent)
                        .clickable { period = p; offset = 0 }, contentAlignment = Alignment.Center) {
                        Text(p.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (sel) D.ink else D.sub)
                    }
                }
            }
        }
        item {
            val tiles = listOf(
                "${summary.workouts}" to tr("treninga", "workouts"),
                JournalStats.fmtMinutes(summary.minutes) to tr("vreme", "time"),
                "${WorkoutMath.fmtKg(Math.round(summary.volumeKg / 100.0) / 10.0)} t" to tr("podignuto", "lifted"),
                "${summary.sets}" to tr("radnih setova", "working sets"),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tiles.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (v, l) ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(D.s1).padding(horizontal = 22.dp, vertical = 16.dp)) {
                                com.vukos.trening.ui.common.FitText(v, TextStyle(fontFamily = Manrope, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.52).sp), D.ink)
                                Text(l, fontSize = 14.sp, color = D.sub)
                            }
                        }
                    }
                }
            }
        }
        item {
            // Ciljni opseg: 10-20 radnih setova nedeljno po misicu (za mesec puta broj nedelja; za godinu bez opsega).
            val band = when (period) {
                Period.WEEK -> 1.0
                Period.MONTH -> (java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1) / 7.0
                Period.YEAR -> 0.0
            }
            MusclesCard(muscles, summary.sets, band, period == Period.YEAR)
        }
        item {
            when (period) {
                Period.WEEK -> {
                    val days = (0..6).map { from.plusDays(it.toLong()) }
                    val values = days.map { d -> dates.count { it == d } }
                    val total = values.sum()
                    WorkoutsCard(
                        title = tr("Treninzi po danima", "Workouts by day"),
                        badge = if (total >= goal) tr("Cilj ispunjen $total/$goal", "Goal met $total/$goal") else tr("Cilj $total/$goal", "Goal $total/$goal"),
                        hero = "$total" to tr("treninga", "workouts"),
                        values = values, labels = WEEKDAY_SHORT, captions = days.map { it.srLong().replaceFirstChar { c -> c.uppercase() } },
                        highlight = days.indexOf(today),
                        pair = ("${values.count { it > 0 }}" to tr("aktivnih dana", "active days")) to
                            ((if (summary.workouts > 0) (summary.minutes / summary.workouts).let { if (it < 60) "$it min" else JournalStats.fmtMinutes(it) } else "—") to tr("prosečan trening", "avg session")),
                    )
                }
                Period.MONTH -> {
                    // Nedelje (pon-ned) koje pocinju u izabranom mesecu ili ga preklapaju.
                    val weeks = generateSequence(TrainingStats.weekStart(from)) { it.plusWeeks(1) }.takeWhile { !it.isAfter(to) }.toList()
                    val values = weeks.map { w -> dates.count { !it.isBefore(w) && !it.isAfter(w.plusDays(6)) } }
                    val elapsed = weeks.indices.filter { !weeks[it].isAfter(today) }.map { values[it] }
                    val onGoal = weeks.indices.count { values[it] >= goal }
                    val total = dates.count { !it.isBefore(from) && !it.isAfter(to) }
                    WorkoutsCard(
                        title = tr("Treninzi po nedeljama", "Workouts by week"),
                        badge = tr("Nedelja sa ciljem: $onGoal", "$onGoal ${if (onGoal == 1) "week" else "weeks"} on goal"),
                        hero = "$total" to tr("treninga", "workouts"),
                        values = values, labels = weeks.map { it.srShort() },
                        captions = weeks.map { tr("Nedelja od ", "Week of ") + it.srDate() },
                        highlight = weeks.indexOf(TrainingStats.weekStart(today)),
                        pair = (fmt1(if (elapsed.isEmpty()) 0.0 else elapsed.average()) to tr("nedeljni prosek", "weekly avg")) to
                            ("${values.maxOrNull() ?: 0}" to tr("najbolja nedelja", "best week")),
                    )
                }
                Period.YEAR -> {
                    val data = TrainingStats.monthlyCounts(dates, from.year)
                    val values = data.map { it.second }
                    val firstActive = values.indexOfFirst { it > 0 }
                    val lastIdx = if (from.year == today.year) today.monthValue - 1 else 11
                    val elapsed = if (firstActive < 0 || firstActive > lastIdx) emptyList() else values.subList(firstActive, lastIdx + 1)
                    WorkoutsCard(
                        title = tr("Treninzi po mesecima", "Workouts by month"),
                        badge = if (firstActive >= 0) tr("Od: ${MONTHS[firstActive]}", "Since ${MONTHS[firstActive]}") else "",
                        hero = "${values.sum()}" to tr("treninga", "workouts"),
                        values = values, labels = MONTH_INITIAL, captions = data.map { "${MONTHS[it.first.monthValue - 1]} ${it.first.year}" },
                        highlight = if (from.year == today.year) today.monthValue - 1 else -1,
                        pair = (fmt1(if (elapsed.isEmpty()) 0.0 else elapsed.average()) to tr("mesečni prosek", "monthly avg")) to
                            ("${values.maxOrNull() ?: 0}" to tr("najbolji mesec", "best month")),
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallRound(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    val D = com.vukos.trening.ui.theme.Ds
    Box(Modifier.size(34.dp).clip(CircleShape).background(D.s1).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, Modifier.size(16.dp), tint = if (enabled) D.ink else D.faint)
    }
}

/** Mali naslov kartice velikim slovima, sa oznakom desno. */
@Composable
private fun CardLabel(left: String, right: String, rightInk: Color) {
    val D = com.vukos.trening.ui.theme.Ds
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(left.uppercase(), Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp, color = D.sub, maxLines = 1)
        Text(right.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp, color = rightInk, maxLines = 1)
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier, align: Alignment.Horizontal = Alignment.Start) {
    val D = com.vukos.trening.ui.theme.Ds
    Column(modifier, horizontalAlignment = align) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.44).sp, color = D.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, Modifier.padding(top = 2.dp), fontSize = 13.sp, color = D.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Setovi po misicima: traka + ciljni opseg (10-20 nedeljno), ispod opsega sivo. */
@Composable
private fun MusclesCard(muscles: List<Pair<String, Double>>, total: Int, bandWeeks: Double, year: Boolean) {
    val D = com.vukos.trening.ui.theme.Ds
    val muted = if (com.vukos.trening.ui.theme.LocalDs.current.dark) Color(0xFF4A4A4F) else Color(0xFFC9C9CF)
    val lo = (10 * bandWeeks).roundToInt()
    val hi = (20 * bandWeeks).roundToInt()
    val hasBand = !year && bandWeeks > 0
    val maxV = muscles.maxOfOrNull { it.second } ?: 0.0
    val scale = (if (hasBand) maxOf(hi * 1.2, maxV * 1.05) else maxV * 1.1).coerceAtLeast(1.0)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(D.s1).padding(22.dp)) {
        CardLabel(tr("Setovi po mišićima", "Sets per muscle"), if (hasBand) tr("Cilj $lo–$hi", "Target $lo–$hi") else tr("Ova godina", "This year"), D.sub)
        if (muscles.isEmpty()) Text(tr("Nema treninga u ovom periodu.", "No workouts in this period."), Modifier.padding(top = 18.dp), fontSize = 14.sp, color = D.sub)
        Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            muscles.forEach { (m, n) ->
                val below = hasBand && n < lo
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row {
                        Text((muscleLabels[m] ?: if (m == "ostalo") tr("ostalo", "other") else m).replaceFirstChar { it.uppercase() }, Modifier.weight(1f),
                            fontSize = 15.sp, color = D.ink, maxLines = 1)
                        Text(WorkoutMath.fmtKg(n), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (below) D.sub else D.ink)
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(D.s2)) {
                        if (hasBand) Box(Modifier.offset(x = maxWidth * (lo / scale).toFloat()).width(maxWidth * ((hi - lo) / scale).toFloat()).fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp)).background(D.s3))
                        val w = if (n == 0.0) 0f else (n / scale).toFloat().coerceIn(0.02f, 1f)
                        if (w > 0f) Box(Modifier.fillMaxWidth(w).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(if (below) muted else com.vukos.trening.ui.theme.Accent))
                    }
                }
            }
        }
        if (hasBand && muscles.isNotEmpty()) Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend(D.s3, tr("Ciljni opseg", "Target range"))
            Legend(muted, tr("Ispod cilja", "Below target"))
        }
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(14.dp, 8.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(text, fontSize = 12.sp, color = com.vukos.trening.ui.theme.Ds.sub)
    }
}

/** Treninzi kroz vreme: veliki zbir, jednake kolone, izabrani stubic sa detaljem, dve brojke ispod linije. */
@Composable
private fun WorkoutsCard(
    title: String, badge: String, hero: Pair<String, String>, values: List<Int>, labels: List<String>, captions: List<String>,
    highlight: Int, pair: Pair<Pair<String, String>, Pair<String, String>>,
) {
    val D = com.vukos.trening.ui.theme.Ds
    val accent = com.vukos.trening.ui.theme.Accent
    val maxV = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    var selected by remember(values, labels) {
        mutableIntStateOf(if (highlight in values.indices && values[highlight] > 0) highlight else values.indexOfLast { it > 0 }.takeIf { it >= 0 } ?: 0)
    }
    val gap = if (values.size > 7) 6.dp else 10.dp
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(D.s1).padding(22.dp)) {
        CardLabel(title, badge, D.aText)
        Row(Modifier.padding(top = 22.dp).fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Bottom) {
            values.forEachIndexed { i, v ->
                val sel = i == selected
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { selected = i },
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
                ) {
                    Text(if (sel && v > 0) "$v" else "", Modifier.fillMaxWidth(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = D.aText, textAlign = TextAlign.Center)
                    Box(
                        Modifier.fillMaxWidth().height(if (v > 0) (130f * v / maxV).dp else 4.dp).clip(RoundedCornerShape(if (v > 0) 8.dp else 2.dp))
                            .background(if (v == 0) D.s3 else if (sel) accent else accent.copy(alpha = 0.42f))
                    )
                }
            }
        }
        Row(Modifier.padding(top = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            labels.forEachIndexed { i, l ->
                Text(l, Modifier.weight(1f), fontSize = 12.sp, fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (i == selected) D.aText else D.sub, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
            }
        }
        captions.getOrNull(selected)?.let { cap ->
            Row(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(D.s2).padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(workoutsText(values[selected]), Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = D.ink)
                Text(cap, fontSize = 14.sp, color = D.sub)
            }
        }
        androidx.compose.material3.HorizontalDivider(Modifier.padding(top = 16.dp), thickness = 1.dp, color = D.line)
        Row(Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat(pair.first.first, pair.first.second, Modifier.weight(1f))
            MiniStat(pair.second.first, pair.second.second, Modifier.weight(1f), Alignment.End)
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

private val WEEKDAY_SHORT: List<String> get() = if (com.vukos.trening.i18n.Lang.en)
    listOf("M", "T", "W", "T", "F", "S", "S") else listOf("P", "U", "S", "Č", "P", "S", "N")

private val MONTH_INITIAL: List<String> get() = if (com.vukos.trening.i18n.Lang.en)
    listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D") else listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")

/** Kratak opseg nedelje: "23–29. nov" / "23–29 Nov", a preko meseca "30. nov – 6. dec" / "30 Nov – 6 Dec". */
private fun shortRange(a: LocalDate, b: LocalDate): String {
    val m = { d: LocalDate -> MONTH_SHORT[d.monthValue - 1] }
    val en = com.vukos.trening.i18n.Lang.en
    return when {
        a.month == b.month && en -> "${a.dayOfMonth}–${b.dayOfMonth} ${m(b)}"
        a.month == b.month -> "${a.dayOfMonth}–${b.dayOfMonth}. ${m(b)}"
        en -> "${a.dayOfMonth} ${m(a)} – ${b.dayOfMonth} ${m(b)}"
        else -> "${a.dayOfMonth}. ${m(a)} – ${b.dayOfMonth}. ${m(b)}"
    }
}

private val MONTH_SHORT: List<String> get() = if (com.vukos.trening.i18n.Lang.en)
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
else listOf("jan", "feb", "mar", "apr", "maj", "jun", "jul", "avg", "sep", "okt", "nov", "dec")

/** "1 trening" / "3 treninga" (ili engleski). */
private fun workoutsText(n: Int) = if (n == 1) tr("1 trening", "1 workout") else tr("$n treninga", "$n workouts")
