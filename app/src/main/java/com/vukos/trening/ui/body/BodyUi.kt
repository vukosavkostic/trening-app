package com.vukos.trening.ui.body

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vukos.trening.data.BodyEntry
import com.vukos.trening.data.CheckInSchedule
import com.vukos.trening.ui.theme.Danger
import java.time.LocalDate
import java.util.Locale

private val DAYS = listOf("pon", "uto", "sre", "čet", "pet", "sub", "ned")

fun LocalDate.sr(): String = "${DAYS[dayOfWeek.value - 1]} $dayOfMonth.$monthValue.$year."
fun LocalDate.srShort(): String = "$dayOfMonth.$monthValue."
fun Double.kg(): String = String.format(Locale.US, "%.1f", this).removeSuffix(".0") + " kg"

fun parseKg(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 20.0..400.0 }

fun CheckInSchedule.Due.whatMissing(): String = when {
    weightMissing && photosMissing -> "kilaža + 3 slike"
    photosMissing -> "3 slike (spreda, bok, leđa)"
    else -> "kilaža"
}

/**
 * Traka na vrhu aplikacije. Nedelja (termin): narandzasta "Danas je merenje".
 * Propusteno (od ponedeljka): crvena dok se ne unese.
 */
@Composable
fun CheckInBanner(due: CheckInSchedule.Due?, onClick: () -> Unit) {
    if (due == null || !due.anythingMissing) return
    val bg = if (due.overdue) Danger else MaterialTheme.colorScheme.primaryContainer
    val fg = if (due.overdue) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
    Row(
        Modifier.fillMaxWidth().background(bg).statusBarsPadding().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (due.overdue) Icons.Filled.Warning else Icons.Filled.MonitorWeight, null, tint = fg)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (due.overdue) "Propušteno merenje (${due.checkInDay.sr()})" else "Danas je merenje",
                color = fg, fontWeight = FontWeight.Bold,
            )
            Text("Unesi: ${due.whatMissing()}", color = fg, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = fg)
    }
}

/** Jednostavan linijski grafikon kilaze (najstarije levo). */
@Composable
fun WeightChart(
    entries: List<BodyEntry>,
    modifier: Modifier = Modifier,
    chartHeight: androidx.compose.ui.unit.Dp = 140.dp,
    onClick: (() -> Unit)? = null,
) {
    val pts = entries.filter { it.weightKg != null }.sortedBy { it.epochDay }
    if (pts.size < 2) return
    val min = pts.minOf { it.weightKg!! }
    val max = pts.maxOf { it.weightKg!! }
    val span = (max - min).coerceAtLeast(1.0)
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Kilaža", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            val diff = pts.last().weightKg!! - pts.first().weightKg!!
            Text(
                (if (diff >= 0) "+" else "−") + kotlin.math.abs(diff).kg() + " od ${LocalDate.ofEpochDay(pts.first().epochDay).srShort()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
            )
            if (onClick != null) Icon(Icons.Filled.ChevronRight, "Otvori grafikon", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        Row {
            Column(Modifier.height(chartHeight).width(52.dp)) {
                Text(max.kg(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(min.kg(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Canvas(Modifier.weight(1f).height(chartHeight)) {
                val n = pts.size
                fun p(i: Int): Offset {
                    val x = if (n == 1) size.width / 2 else size.width * i / (n - 1)
                    val y = size.height * (1 - ((pts[i].weightKg!! - min) / span)).toFloat()
                    return Offset(x, y.coerceIn(4f, size.height - 4f))
                }
                drawLine(grid, Offset(0f, 0f), Offset(size.width, 0f))
                drawLine(grid, Offset(0f, size.height), Offset(size.width, size.height))
                val path = Path().apply { moveTo(p(0).x, p(0).y); for (i in 1 until n) lineTo(p(i).x, p(i).y) }
                drawPath(path, line, style = Stroke(width = 3.dp.toPx()))
                for (i in 0 until n) drawCircle(line, 4.dp.toPx(), p(i))
            }
        }
        Row(Modifier.padding(start = 52.dp)) {
            Text(LocalDate.ofEpochDay(pts.first().epochDay).srShort(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(LocalDate.ofEpochDay(pts.last().epochDay).srShort(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun Dot(color: Color) = Spacer(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
