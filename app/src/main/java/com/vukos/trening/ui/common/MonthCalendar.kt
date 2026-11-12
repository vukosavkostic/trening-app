package com.vukos.trening.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

val MONTHS = listOf(
    "Januar", "Februar", "Mart", "April", "Maj", "Jun",
    "Jul", "Avgust", "Septembar", "Oktobar", "Novembar", "Decembar",
)
private val WEEKDAYS = listOf("P", "U", "S", "Č", "P", "S", "N")

/**
 * Oznaka dana: [filled] = pun narandzasti krug (glavna aktivnost, npr. trening),
 * [dot] = diskretna tackica ispod broja (ostalo, npr. obrok ili kilaza).
 */
data class DayMarker(val filled: Boolean = false, val dot: Boolean = false)

/** Mesecni kalendar (ponedeljak prvi), bez legende - znacenje se vidi u detaljima izabranog dana. */
@Composable
fun MonthCalendar(
    markers: Map<LocalDate, DayMarker>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    selected: LocalDate? = null,
    allClickable: Boolean = false,
    month: YearMonth? = null,
    onMonthChange: ((YearMonth) -> Unit)? = null,
) {
    var localMonth by remember { mutableStateOf(month ?: YearMonth.now()) }
    val shown = month ?: localMonth
    fun go(m: YearMonth) { localMonth = m; onMonthChange?.invoke(m) }
    val today = LocalDate.now()
    val accent = MaterialTheme.colorScheme.primary

    AppCard(modifier, padding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${MONTHS[shown.monthValue - 1]} ${shown.year}",
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            RoundIcon(Icons.Filled.ChevronLeft, "Prethodni mesec") { go(shown.minusMonths(1)) }
            Spacer(Modifier.width(8.dp))
            RoundIcon(Icons.Filled.ChevronRight, "Sledeći mesec") { go(shown.plusMonths(1)) }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            WEEKDAYS.forEach {
                Text(
                    it, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val offset = shown.atDay(1).dayOfWeek.value - 1
        val rows = (offset + shown.lengthOfMonth() + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val dayNum = r * 7 + c - offset + 1
                    Box(Modifier.weight(1f).height(46.dp), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..shown.lengthOfMonth()) {
                            val date = shown.atDay(dayNum)
                            val m = markers[date]
                            val filled = m?.filled == true
                            Box(
                                Modifier.size(38.dp).clip(CircleShape)
                                    .background(if (filled) accent else Color.Transparent)
                                    .then(if (date == selected) Modifier.border(2.dp, if (filled) Color.White else accent, CircleShape) else Modifier)
                                    .then(if (m != null || allClickable) Modifier.clickable { onDayClick(date) } else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$dayNum",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (filled || date == today) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = when {
                                        filled -> Color.White
                                        date == today -> accent
                                        date.isAfter(today) -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                )
                                if (m?.dot == true) Box(
                                    Modifier.align(Alignment.BottomCenter).offset(y = (-4).dp).size(4.dp).clip(CircleShape)
                                        .background(if (filled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(20.dp)) }
}
