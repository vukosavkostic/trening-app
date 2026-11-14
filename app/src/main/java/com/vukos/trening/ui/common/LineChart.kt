package com.vukos.trening.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/**
 * Linijski grafikon po uzoru na Fitpass (datum -> vrednost, najstarije levo, x-osa vremenska):
 *  - tackasta linija sa tackama, tackaste linije mreze na najvecoj i najmanjoj vrednosti, puna osnovna linija
 *  - dodir ili prevlacenje bira tacku: isprekidana vertikala i "pilula" sa vrednoscu
 *  - datumi ispod bez preklapanja, poslednji beli i podebljan
 */
@Composable
fun LineChart(
    points: List<Pair<LocalDate, Double>>,
    format: (Double) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    interactive: Boolean = true,
) {
    val pts = remember(points) { points.sortedBy { it.first } }
    if (pts.isEmpty()) return
    var selected by remember(pts) { mutableIntStateOf(pts.lastIndex) }
    val min = pts.minOf { it.second }
    val max = pts.maxOf { it.second }
    val span = (max - min).takeIf { it > 0 } ?: 1.0
    val d0 = pts.first().first.toEpochDay()
    val dSpan = (pts.last().first.toEpochDay() - d0).coerceAtLeast(1)

    val accent = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outline
    val guide = MaterialTheme.colorScheme.onSurfaceVariant
    val pillBg = MaterialTheme.colorScheme.onSurface
    val measurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val lastStyle = axisStyle.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
    val pillStyle = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.surface)

    Canvas(
        modifier.fillMaxWidth().height(height).then(
            if (!interactive) Modifier else Modifier
                .pointerInput(pts) { detectTapGestures { o -> selected = nearest(o.x, size.width.toFloat(), pts, d0, dSpan) } }
                .pointerInput(pts) { detectHorizontalDragGestures { ch, _ -> selected = nearest(ch.position.x, size.width.toFloat(), pts, d0, dSpan) } }
        )
    ) {
        val side = 10.dp.toPx()                 // da krajnje tacke i pilula ne budu odsecene
        val pillH = 28.dp.toPx()
        val top = pillH + 14.dp.toPx()
        val labelH = 26.dp.toPx()
        val baseY = size.height - labelH
        val plotW = size.width - side * 2
        fun x(i: Int) = if (pts.size == 1) size.width / 2 else side + plotW * (pts[i].first.toEpochDay() - d0) / dSpan
        fun y(v: Double) = if (max == min) (top + baseY) / 2 else (baseY - 8.dp.toPx()) - (baseY - 8.dp.toPx() - top) * ((v - min) / span).toFloat()

        val dotted = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()))
        // Mreza: tackaste linije na najvecoj i najmanjoj vrednosti, puna osnovna linija.
        listOf(max, min).distinct().forEach { v ->
            drawLine(grid, Offset(0f, y(v)), Offset(size.width, y(v)), strokeWidth = 1.dp.toPx(), pathEffect = dotted, cap = StrokeCap.Round)
        }
        drawLine(grid, Offset(0f, baseY), Offset(size.width, baseY), strokeWidth = 1.dp.toPx())

        // Linija vrednosti (tackasta) i tacke.
        if (pts.size > 1) {
            val path = Path().apply { moveTo(x(0), y(pts[0].second)); for (i in 1 until pts.size) lineTo(x(i), y(pts[i].second)) }
            drawPath(path, accent, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 6.dp.toPx()))))
        }
        if (pts.size <= 40) pts.indices.forEach { drawCircle(accent, 3.5.dp.toPx(), Offset(x(it), y(pts[it].second))) }

        // Izabrana tacka: vertikala do ose, veca tacka, pilula sa vrednoscu.
        val s = selected.coerceIn(0, pts.lastIndex)
        val sx = x(s); val sy = y(pts[s].second)
        drawLine(guide, Offset(sx, pillH), Offset(sx, baseY), strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
        drawCircle(pillBg, 6.dp.toPx(), Offset(sx, sy))
        drawCircle(accent, 4.dp.toPx(), Offset(sx, sy))
        val t = measurer.measure(format(pts[s].second), pillStyle)
        val pw = t.size.width + 24.dp.toPx()
        val px = (sx - pw / 2).coerceIn(0f, size.width - pw)
        drawRoundRect(pillBg, Offset(px, 0f), Size(pw, pillH), CornerRadius(pillH / 2))
        drawText(t, topLeft = Offset(px + (pw - t.size.width) / 2, (pillH - t.size.height) / 2))

        // Datumi: do 5 ravnomerno rasporedjenih, bez preklapanja; poslednji (najnoviji) beli.
        val ticks = (0..4).map { d0 + dSpan * it / 4 }.distinct()
        val gap = 8.dp.toPx()
        val taken = mutableListOf<ClosedFloatingPointRange<Float>>()
        (listOf(ticks.last(), ticks.first()) + ticks).distinct().forEach { day ->
            val isLast = day == ticks.last()
            val m = measurer.measure(short(LocalDate.ofEpochDay(day)), if (isLast) lastStyle else axisStyle)
            val cx = if (pts.size == 1) size.width / 2 else side + plotW * (day - d0) / dSpan
            val left = (cx - m.size.width / 2f).coerceIn(0f, size.width - m.size.width)
            val r = (left - gap)..(left + m.size.width + gap)
            if (taken.any { it.start < r.endInclusive && r.start < it.endInclusive }) return@forEach
            taken += r
            drawText(m, topLeft = Offset(left, baseY + 7.dp.toPx()))
        }
    }
}

private fun nearest(px: Float, width: Float, pts: List<Pair<LocalDate, Double>>, d0: Long, dSpan: Long): Int {
    if (pts.size == 1) return 0
    val day = d0 + dSpan * (px / width).coerceIn(0f, 1f)
    return pts.indices.minBy { kotlin.math.abs(pts[it].first.toEpochDay() - day) }
}

private fun short(d: LocalDate) = "${d.dayOfMonth}.${d.monthValue}."
