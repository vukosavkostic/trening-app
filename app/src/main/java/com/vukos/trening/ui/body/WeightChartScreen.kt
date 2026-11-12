package com.vukos.trening.ui.body

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.data.ChartRange
import java.time.LocalDate

/** Grafikon kilaze preko celog ekrana, sa izborom perioda i listom vrednosti. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightChartScreen(onBack: () -> Unit, onOpenDay: (LocalDate) -> Unit) {
    val entries by remember { TreningApp.instance.db.body().observeAll() }.collectAsStateWithLifecycle(emptyList())
    var range by rememberSaveable { mutableStateOf(ChartRange.M3) }
    val shown = remember(entries, range) {
        range.filter(entries.filter { it.weightKg != null }, LocalDate.now())
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Grafikon kilaže") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Nazad") } },
        )
    }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChartRange.entries.forEach {
                        FilterChip(selected = range == it, onClick = { range = it }, label = { Text(it.label) })
                    }
                }
            }
            item {
                if (shown.size >= 2) WeightChart(shown, Modifier.fillMaxWidth(), chartHeight = 260.dp)
                else Text(
                    "Za grafikon trebaju bar 2 merenja u izabranom periodu.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
            items(shown.sortedByDescending { it.epochDay }, key = { it.id }) { e ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable { onOpenDay(LocalDate.ofEpochDay(e.epochDay)) }
                        .padding(14.dp)
                ) {
                    Text(LocalDate.ofEpochDay(e.epochDay).sr(), Modifier.weight(1f))
                    Text(e.weightKg!!.kg(), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
