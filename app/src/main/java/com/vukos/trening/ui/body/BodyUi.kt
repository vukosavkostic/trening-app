package com.vukos.trening.ui.body

import com.vukos.trening.i18n.tr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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

private val en get() = com.vukos.trening.i18n.Lang.en
private val DAYS get() = if (en) listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") else listOf("pon", "uto", "sre", "čet", "pet", "sub", "ned")
private val DAYS_FULL get() = if (en) listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    else listOf("ponedeljak", "utorak", "sreda", "četvrtak", "petak", "subota", "nedelja")
private val MONTHS_GEN get() = if (en) listOf("January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December")
    else listOf("januara", "februara", "marta", "aprila", "maja", "juna", "jula", "avgusta", "septembra", "oktobra", "novembra", "decembra")

fun LocalDate.sr(): String = if (en) "${DAYS[dayOfWeek.value - 1]} $dayOfMonth/$monthValue/$year" else "${DAYS[dayOfWeek.value - 1]} $dayOfMonth.$monthValue.$year."
fun LocalDate.srShort(): String = if (en) "$dayOfMonth/$monthValue" else "$dayOfMonth.$monthValue."

/** "12. novembra" / "November 12" (godina samo ako nije tekuca). */
fun LocalDate.srDate(): String = if (en)
    "${MONTHS_GEN[monthValue - 1]} $dayOfMonth" + if (year != LocalDate.now().year) ", $year" else ""
else "$dayOfMonth. ${MONTHS_GEN[monthValue - 1]}" + if (year != LocalDate.now().year) " $year." else ""

/** "četvrtak, 12. novembra" / "Thursday, November 12" */
fun LocalDate.srLong(): String = "${DAYS_FULL[dayOfWeek.value - 1]}, ${srDate()}"

/** "danas", "sutra", "za 3 dana", "pre 2 dana"... */
fun LocalDate.srRelative(today: LocalDate = LocalDate.now()): String {
    val d = toEpochDay() - today.toEpochDay()
    return if (en) when {
        d == 0L -> "today"
        d == 1L -> "tomorrow"
        d == -1L -> "yesterday"
        d > 1 -> "in $d days"
        d > -7 -> "${-d} days ago"
        d > -14 -> "a week ago"
        d > -60 -> "${-d / 7} weeks ago"
        else -> "${-d / 30} months ago"
    } else when {
        d == 0L -> "danas"
        d == 1L -> "sutra"
        d == -1L -> "juče"
        d > 1 -> "za $d dana"
        d > -7 -> "pre ${-d} dana"
        d > -14 -> "pre nedelju dana"
        d > -60 -> "pre ${-d / 7} nedelje"
        else -> "pre ${-d / 30} meseci"
    }
}
fun Double.kg(): String = String.format(Locale.US, "%.1f", this).removeSuffix(".0") + " kg"

fun parseKg(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 20.0..400.0 }

fun CheckInSchedule.Due.whatMissing(): String = when {
    weightMissing && photosMissing -> tr("kilaža + 3 slike", "weight + 3 photos")
    photosMissing -> tr("3 slike (spreda, bok, leđa)", "3 photos (front, side, back)")
    else -> tr("kilaža", "weight")
}

/**
 * Traka na vrhu aplikacije. Nedelja (termin): narandzasta "Danas je merenje".
 * Propusteno (od ponedeljka): crvena dok se ne unese.
 */
@Composable
fun CheckInBanner(due: CheckInSchedule.Due?, topInset: Boolean = true, onClick: () -> Unit) {
    if (due == null || !due.anythingMissing) return
    val bg = if (due.overdue) Danger else MaterialTheme.colorScheme.primary
    Box(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
            .then(if (topInset) Modifier.statusBarsPadding() else Modifier)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bg).clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center) {
                Icon(if (due.overdue) Icons.Filled.Warning else Icons.Filled.MonitorWeight, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (due.overdue) tr("Propušteno merenje", "Missed check-in") else tr("Danas je merenje", "Check-in today"),
                    color = Color.White, style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    (if (due.overdue) tr("Zakazano za ${due.checkInDay.srDate()}. ", "Due ${due.checkInDay.srDate()}. ") else "") + tr("Unesi ${due.whatMissing()}.", "Log ${due.whatMissing()}."),
                    color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = Color.White)
        }
    }
}

/** Kartica kilaze po uzoru na Fitpass: ikona i naslov, pocetna i trenutna kilaza, promena, pa grafikon. */
@Composable
fun WeightChart(
    entries: List<BodyEntry>,
    modifier: Modifier = Modifier,
    chartHeight: androidx.compose.ui.unit.Dp = 150.dp,
    onClick: (() -> Unit)? = null,
) {
    val pts = entries.filter { it.weightKg != null }.sortedBy { it.epochDay }
    if (pts.size < 2) return
    val first = pts.first(); val last = pts.last()
    val diff = last.weightKg!! - first.weightKg!!
    val onVar = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier.clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.MonitorWeight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(tr("Kilaža", "Weight"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (onClick != null) Icon(Icons.Filled.ChevronRight, tr("Otvori grafikon", "Open chart"), tint = onVar)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(first.weightKg!!.kg(), style = MaterialTheme.typography.headlineSmall)
                Text(LocalDate.ofEpochDay(first.epochDay).srShort(), style = MaterialTheme.typography.labelMedium, color = onVar)
            }
            Column(Modifier.weight(1f)) {
                Text(last.weightKg!!.kg(), style = MaterialTheme.typography.headlineSmall)
                Text(tr("sada", "now"), style = MaterialTheme.typography.labelMedium, color = onVar)
            }
            Text(
                (if (diff >= 0) "+" else "−") + kotlin.math.abs(diff).kg(),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        com.vukos.trening.ui.common.LineChart(
            pts.map { LocalDate.ofEpochDay(it.epochDay) to it.weightKg!! }, { it.kg() }, height = chartHeight + 60.dp, interactive = onClick == null,
        )
    }
}

@Composable
fun Dot(color: Color) = Spacer(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
