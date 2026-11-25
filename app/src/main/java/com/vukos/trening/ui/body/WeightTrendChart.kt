package com.vukos.trening.ui.body

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vukos.trening.data.WeightChartModel
import com.vukos.trening.ui.theme.Accent
import com.vukos.trening.ui.theme.Ds
import kotlin.math.roundToInt

/**
 * Zajednicki grafikon kilaze (Profil i ekran Kilaza): oznake y desno u koloni od 40dp, oznake x u svom redu,
 * svako merenje je tacka (dodir 32dp), izabrana je popunjena, sa oblacicem iznad i isprekidanom linijom nadole.
 */
@Composable
fun WeightTrendChart(
    values: List<Double>,
    xLabel: (Int) -> String,
    pointLabel: (Int) -> String,
    selected: Int?,
    onSelect: (Int) -> Unit,
    chartHeight: Dp,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty()) return
    val m = WeightChartModel(values, selected)
    val line = Accent
    val grid = Ds.line
    val guide = Ds.faint
    val card = Ds.s1
    val faint = Ds.faint
    val tipBg = Ds.ink
    val tipInk = Ds.bg
    val density = LocalDensity.current
    androidx.compose.foundation.layout.Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            BoxWithConstraints(Modifier.weight(1f).height(chartHeight)) {
                val w = with(density) { maxWidth.toPx() }
                val h = with(density) { chartHeight.toPx() }
                val px = { v: Float -> with(density) { v.dp.toPx() } }
                fun x(i: Int) = if (m.n == 1) w / 2 else px(WeightChartModel.PAD_X) + i.toFloat() / (m.n - 1) * (w - 2 * px(WeightChartModel.PAD_X))
                fun y(v: Double) = (px(WeightChartModel.PAD_T) + (m.yMax - v) / (m.yMax - m.yMin) * (h - px(WeightChartModel.PAD_T) - px(WeightChartModel.PAD_B))).toFloat()
                Canvas(Modifier.fillMaxSize()) {
                    m.ticks.forEach { t -> drawLine(grid, Offset(0f, y(t)), Offset(size.width, y(t)), 1.dp.toPx()) }
                    if (m.n > 1) {
                        val path = Path().apply { moveTo(x(0), y(values[0])); for (i in 1 until m.n) lineTo(x(i), y(values[i])) }
                        val area = Path().apply { addPath(path); lineTo(x(m.n - 1), size.height); lineTo(x(0), size.height); close() }
                        drawPath(area, Brush.verticalGradient(listOf(line.copy(alpha = 0.24f), Color.Transparent)))
                        drawLine(guide, Offset(x(m.sel), y(values[m.sel]) + 8.dp.toPx()), Offset(x(m.sel), size.height), 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())))
                        drawPath(path, line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
                values.indices.forEach { i ->
                    val on = i == m.sel
                    val d = if (on) 14.dp else 10.dp
                    Box(
                        Modifier.offset { androidx.compose.ui.unit.IntOffset((x(i) - px(16f)).roundToInt(), (y(values[i]) - px(16f)).roundToInt()) }
                            .size(32.dp).clip(CircleShape).clickable { onSelect(i) }.semantics { contentDescription = pointLabel(i) },
                        contentAlignment = Alignment.Center,
                    ) { Box(Modifier.size(d).clip(CircleShape).background(if (on) line else card).border(2.5.dp, line, CircleShape)) }
                }
                // Oblacic sa vrednoscu izabrane tacke; uz ivice se pomera da ne izlazi iz grafikona.
                val sx = x(m.sel)
                val sy = y(values[m.sel])
                val f = when { sx / w < 0.15f -> 0.14f; sx / w > 0.85f -> 0.86f; else -> 0.5f }
                Text(
                    "${String.format(java.util.Locale.US, "%.1f", values[m.sel])} kg",
                    Modifier.layout { me, c ->
                        val p = me.measure(c.copy(minWidth = 0, minHeight = 0))
                        layout(0, 0) { p.place((sx - p.width * f).roundToInt(), (sy - px(12f) - p.height).roundToInt()) }
                    }.clip(RoundedCornerShape(10.dp)).background(tipBg).padding(horizontal = 9.dp, vertical = 5.dp),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = tipInk, maxLines = 1,
                )
            }
            Spacer(Modifier.width(6.dp))
            Box(Modifier.width(40.dp).height(chartHeight)) {
                val h = with(density) { chartHeight.toPx() }
                m.ticks.forEach { t ->
                    val ty = (with(density) { WeightChartModel.PAD_T.dp.toPx() } + (m.yMax - t) / (m.yMax - m.yMin) *
                        (h - with(density) { (WeightChartModel.PAD_T + WeightChartModel.PAD_B).dp.toPx() })).toFloat()
                    Text(
                        m.tickLabel(t),
                        Modifier.align(Alignment.TopEnd).layout { me, c ->
                            val p = me.measure(c.copy(minWidth = 0, minHeight = 0))
                            layout(p.width, 0) { p.place(0, (ty - p.height / 2f).roundToInt()) }
                        },
                        fontSize = 11.sp, color = faint, maxLines = 1,
                    )
                }
            }
        }
        // Oznake x: prva uz levu ivicu, poslednja uz desnu, ostale centrirane ispod tacke.
        BoxWithConstraints(Modifier.padding(top = 10.dp, end = 46.dp).fillMaxWidth().height(16.dp)) {
            val w = with(density) { maxWidth.toPx() }
            val padX = with(density) { WeightChartModel.PAD_X.dp.toPx() }
            m.xLabels.forEach { i ->
                val cx = if (m.n == 1) w / 2 else padX + i.toFloat() / (m.n - 1) * (w - 2 * padX)
                Text(
                    xLabel(i),
                    Modifier.layout { me, c ->
                        val p = me.measure(c.copy(minWidth = 0, minHeight = 0))
                        val left = when {
                            m.n == 1 -> cx - p.width / 2f
                            i == 0 -> 0f
                            i == m.n - 1 -> w - p.width
                            else -> cx - p.width / 2f
                        }
                        layout(0, 0) { p.place(left.roundToInt(), 0) }
                    },
                    fontSize = 12.sp, color = faint, maxLines = 1, softWrap = false,
                )
            }
        }
    }
}
