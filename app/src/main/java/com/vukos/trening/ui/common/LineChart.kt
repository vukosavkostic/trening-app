package com.vukos.trening.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/** Linijski grafikon (datum -> vrednost), najstarije levo; x-osa je vremenska. */
@Composable
fun LineChart(
    points: List<Pair<LocalDate, Double>>,
    format: (Double) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
) {
    val pts = points.sortedBy { it.first }
    if (pts.isEmpty()) return
    val min = pts.minOf { it.second }
    val max = pts.maxOf { it.second }
    val span = (max - min).takeIf { it > 0 } ?: 1.0
    val d0 = pts.first().first.toEpochDay()
    val dSpan = (pts.last().first.toEpochDay() - d0).coerceAtLeast(1)
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier) {
        Row {
            Column(Modifier.height(height).width(56.dp)) {
                Text(format(max), style = MaterialTheme.typography.labelSmall, color = labelColor)
                Spacer(Modifier.weight(1f))
                Text(format(min), style = MaterialTheme.typography.labelSmall, color = labelColor)
            }
            Canvas(Modifier.weight(1f).height(height)) {
                fun p(i: Int): Offset {
                    val x = if (pts.size == 1) size.width / 2 else size.width * (pts[i].first.toEpochDay() - d0) / dSpan
                    val y = if (max == min) size.height / 2 else size.height * (1 - ((pts[i].second - min) / span)).toFloat()
                    return Offset(x.toFloat(), y.coerceIn(4f, size.height - 4f))
                }
                for (k in 0..3) {
                    val y = size.height * k / 3
                    drawLine(grid, Offset(0f, y), Offset(size.width, y))
                }
                if (pts.size > 1) {
                    val path = Path().apply { moveTo(p(0).x, p(0).y); for (i in 1 until pts.size) lineTo(p(i).x, p(i).y) }
                    drawPath(path, line, style = Stroke(width = 2.5.dp.toPx()))
                }
                for (i in pts.indices) drawCircle(line, 3.5.dp.toPx(), p(i))
            }
        }
        Row(Modifier.padding(start = 56.dp, top = 4.dp)) {
            Text(short(pts.first().first), style = MaterialTheme.typography.labelSmall, color = labelColor, modifier = Modifier.weight(1f))
            if (pts.size > 1) Text(short(pts.last().first), style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
    }
}

private fun short(d: LocalDate) = "${d.dayOfMonth}.${d.monthValue}.${d.year % 100}"
