package com.vukos.trening.data

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Grafikon kilaze (dizajn v2.1, chartModel): opseg y prosiren za 25% raspona sa obe strane i poravnat
 * na "lepe" podele (0.5 / 1 / 2 / 5 kg, najvise 5 podela), tacke nikad ne dodiruju osu ni oznake.
 */
class WeightChartModel(val values: List<Double>, selected: Int?) {
    val n = values.size
    val step: Double
    val yMin: Double
    val yMax: Double

    init {
        require(n > 0)
        val lo = values.min()
        val hi = values.max()
        val span = max(hi - lo, 0.4)
        var st = 0.5; var a = 0.0; var b = 1.0
        for (s in listOf(0.5, 1.0, 2.0, 5.0)) {
            st = s
            a = floor((lo - span * 0.25) / s) * s
            b = ceil((hi + span * 0.25) / s) * s
            if (b - a < s * 2) b = a + s * 2
            if ((b - a) / s <= 4 + 1e-9) break
        }
        step = st; yMin = a; yMax = b
    }

    /** Izabrana tacka (podrazumevano poslednja). */
    val sel: Int = if (selected == null || selected !in values.indices) n - 1 else selected

    val ticks: List<Double> = generateSequence(yMin) { it + step }.takeWhile { it <= yMax + 1e-9 }.toList()

    /** Oznake x ose: sve do 5 tacaka, inace prva, srednja i poslednja. */
    val xLabels: List<Int> = if (n <= 5) values.indices.toList() else listOf(0, (n - 1) / 2, n - 1)

    fun tickLabel(v: Double): String = if (step < 1) String.format(java.util.Locale.US, "%.1f", v) else "${v.toLong()}"

    fun x(i: Int, width: Float): Float = if (n == 1) width / 2 else PAD_X + i.toFloat() / (n - 1) * (width - 2 * PAD_X)

    fun y(v: Double, height: Float): Float = (PAD_T + (yMax - v) / (yMax - yMin) * (height - PAD_T - PAD_B)).toFloat()

    companion object {
        /** U dp: levo/desno, iznad (mesto za oblacic), ispod. */
        const val PAD_X = 12f
        const val PAD_T = 34f
        const val PAD_B = 10f
    }
}
