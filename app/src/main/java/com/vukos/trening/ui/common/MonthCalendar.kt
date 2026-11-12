package com.vukos.trening.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

private val MONTHS = listOf(
    "Januar", "Februar", "Mart", "April", "Maj", "Jun",
    "Jul", "Avgust", "Septembar", "Oktobar", "Novembar", "Decembar",
)
private val WEEKDAYS = listOf("Pon", "Uto", "Sre", "Čet", "Pet", "Sub", "Ned")

/** Oznaka za dan u kalendaru: do 2 tackice u boji (npr. merenje, slike, trening). */
data class DayMarker(val colors: List<Color>)

/**
 * Mesecni kalendar (ponedeljak prvi). Dani sa oznakom su klikabilni.
 * Koristi se na tabu Telo, a kasnije i u Dnevniku za treninge.
 */
@Composable
fun MonthCalendar(
    markers: Map<LocalDate, DayMarker>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    initialMonth: YearMonth = YearMonth.now(),
) {
    var month by remember { mutableStateOf(initialMonth) }
    val today = LocalDate.now()
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${MONTHS[month.monthValue - 1]} ${month.year}",
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Filled.ChevronLeft, "Prethodni mesec") }
            IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Filled.ChevronRight, "Sledeći mesec") }
        }
        Row(Modifier.fillMaxWidth()) {
            WEEKDAYS.forEach {
                Text(
                    it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val first = month.atDay(1)
        val offset = first.dayOfWeek.value - 1
        val cells = offset + month.lengthOfMonth()
        val rows = (cells + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val dayNum = r * 7 + c - offset + 1
                    Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..month.lengthOfMonth()) {
                            val date = month.atDay(dayNum)
                            val marker = markers[date]
                            Column(
                                Modifier.clip(RoundedCornerShape(8.dp))
                                    .then(if (marker != null) Modifier.clickable { onDayClick(date) } else Modifier)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "$dayNum",
                                    fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        date == today -> MaterialTheme.colorScheme.primary
                                        marker != null -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(8.dp)) {
                                    marker?.colors?.take(3)?.forEach { col ->
                                        Box(Modifier.size(6.dp).clip(CircleShape).background(col))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
