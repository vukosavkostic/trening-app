package com.vukos.trening.ui.journal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vukos.trening.i18n.Lang
import com.vukos.trening.i18n.tr
import com.vukos.trening.ui.common.DayMarker
import com.vukos.trening.ui.common.MONTHS
import com.vukos.trening.ui.theme.Accent
import com.vukos.trening.ui.theme.Ds
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private val RowH = 46.dp
private val HeadH = 34.dp
private val Ease = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * Kalendar Dnevnika (dizajn v2): podrazumevano jedna sedmica (red sa izabranim danom),
 * a povlacenjem nadole ili dodirom na rucku se otvara ceo mesec. Povlacenje nagore ga zatvara.
 * Pun krug = trening, tackica = obrok ili kilaza.
 */
@Composable
fun JournalCalendar(
    markers: Map<LocalDate, DayMarker>,
    selected: LocalDate,
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var open by rememberSaveable { mutableStateOf(false) }
    // 0 = jedna sedmica, 1 = ceo mesec
    val prog = remember { Animatable(if (open) 1f else 0f) }
    var dragTotal by remember { mutableFloatStateOf(0f) }

    // Zatvoren kalendar uvek prikazuje mesec izabranog dana.
    LaunchedEffect(open) { if (!open && YearMonth.from(selected) != month) onMonthChange(YearMonth.from(selected)) }

    val offset = month.atDay(1).dayOfWeek.value - 1
    val rows = (offset + month.lengthOfMonth() + 6) / 7
    val focus = if (YearMonth.from(selected) == month) selected.dayOfMonth else if (YearMonth.from(today) == month) today.dayOfMonth else 1
    val row = (offset + focus - 1) / 7
    val rowPx = with(density) { RowH.toPx() }
    val rangePx = rowPx * (rows - 1)

    fun settle(target: Boolean) {
        open = target
        scope.launch { prog.animateTo(if (target) 1f else 0f, tween(350, easing = Ease)) }
    }
    val dragState = rememberDraggableState { dy ->
        dragTotal += dy
        scope.launch { prog.snapTo((prog.value + dy / rangePx).coerceIn(0f, 1f)) }
    }

    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Ds.s1)
            .draggable(
                dragState, Orientation.Vertical,
                onDragStarted = { dragTotal = 0f },
                onDragStopped = {
                    val d = with(density) { dragTotal.toDp() }
                    settle(if (open) d > (-50).dp else d > 50.dp)
                },
            )
            .padding(start = 10.dp, end = 10.dp, top = 14.dp),
    ) {
        val p = prog.value
        // Naslov meseca sa strelicama: vidi se samo kad je kalendar otvoren.
        Row(
            Modifier.fillMaxWidth().height(HeadH * p).alpha(p).clipToBounds().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                "${MONTHS[month.monthValue - 1]} ${month.year}", Modifier.weight(1f),
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink,
            )
            Icon(
                Icons.Filled.ChevronLeft, tr("Prethodni mesec", "Previous month"),
                Modifier.size(26.dp).clip(CircleShape).clickable(enabled = p > 0.5f) { onMonthChange(month.minusMonths(1)) }.padding(4.dp),
                tint = Ds.sub,
            )
            Icon(
                Icons.Filled.ChevronRight, tr("Sledeći mesec", "Next month"),
                Modifier.size(26.dp).clip(CircleShape).clickable(enabled = p > 0.5f) { onMonthChange(month.plusMonths(1)) }.padding(4.dp),
                tint = if (month < YearMonth.from(today)) Ds.sub else Ds.faint,
            )
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            weekdays().forEach {
                Text(it, Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Ds.sub, textAlign = TextAlign.Center)
            }
        }
        Box(Modifier.fillMaxWidth().height(RowH + RowH * (rows - 1) * p).clipToBounds()) {
            Column(Modifier.graphicsLayer { translationY = -row * rowPx * (1f - p) }) {
                for (r in 0 until rows) Row(Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val dayNum = r * 7 + c - offset + 1
                        Box(Modifier.weight(1f).height(RowH), contentAlignment = Alignment.Center) {
                            if (dayNum in 1..month.lengthOfMonth()) {
                                val date = month.atDay(dayNum)
                                DayCell(dayNum, markers[date], date == selected, date == today, date.isAfter(today)) { onDayClick(date) }
                            }
                        }
                    }
                }
            }
        }
        // Rucka: dodir otvara/zatvara, povlacenje radi na celoj kartici.
        Row(
            Modifier.fillMaxWidth().height(30.dp)
                .clickable(remember { MutableInteractionSource() }, indication = null) { settle(!open) },
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(32.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Ds.s3))
            Icon(
                if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                if (open) tr("Zatvori mesec", "Collapse month") else tr("Otvori mesec", "Expand month"),
                Modifier.size(16.dp), tint = Ds.sub,
            )
        }
    }
}

@Composable
private fun DayCell(n: Int, m: DayMarker?, selected: Boolean, today: Boolean, future: Boolean, onClick: () -> Unit) {
    val trained = m?.filled == true
    val bg = when {
        trained -> Ds.aSoft
        future -> Color.Transparent
        else -> Ds.s2
    }
    Box(
        Modifier.size(36.dp).alpha(if (future && !trained) 0.4f else 1f).clip(CircleShape).background(bg)
            .then(if (selected) Modifier.border(1.5.dp, Ds.ink, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$n", fontSize = 14.sp, fontWeight = if (today || selected) FontWeight.Bold else FontWeight.Medium,
            color = if (trained) Ds.aText else Ds.ink,
        )
        val dot = when {
            trained -> Accent
            m?.dot == true -> Ds.faint
            else -> null
        }
        if (dot != null) Box(Modifier.align(Alignment.BottomCenter).offset(y = (-3).dp).size(4.dp).clip(CircleShape).background(dot))
    }
}

private fun weekdays() = if (Lang.en) listOf("M", "T", "W", "T", "F", "S", "S") else listOf("P", "U", "S", "Č", "P", "S", "N")
