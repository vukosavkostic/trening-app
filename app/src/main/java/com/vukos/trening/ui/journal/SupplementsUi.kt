package com.vukos.trening.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vukos.trening.data.FoodEntry
import com.vukos.trening.data.SupplementDef
import com.vukos.trening.data.Supplements
import com.vukos.trening.ui.common.AppCard
import com.vukos.trening.ui.common.IconBadge
import com.vukos.trening.ui.common.OutlinePill

val SupplementPurple = Color(0xFFB18CFF)

/** Kartica suplemenata za izabrani dan: cipovi (klik = izmena), + dodaj, kopiraj juce. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupplementsCard(entries: List<FoodEntry>, copyCount: Int, onAdd: () -> Unit, onEdit: (FoodEntry) -> Unit, onCopy: () -> Unit) {
    AppCard(padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Medication, SupplementPurple, 40.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Suplementi", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (entries.isEmpty()) "Ništa nije uneto" else "${entries.size} uneto",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(SupplementPurple).clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Add, "Dodaj suplement", tint = Color.Black) }
        }
        if (entries.isNotEmpty()) {
            Spacer(Modifier.size(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.forEach { e ->
                    Column(
                        Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onEdit(e) }.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(Supplements.nameOf(e.text), style = MaterialTheme.typography.titleSmall)
                        Supplements.doseOf(e.text).takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        if (copyCount > 0) OutlinePill("Isto kao juče ($copyCount)", onCopy, Modifier.fillMaxWidth().padding(top = 10.dp), Icons.Filled.ContentCopy)
    }
}

/**
 * Dodavanje / izmena suplementa: pretraga kroz spisak (prvo skoro korisceni), izbor doze,
 * ili dodavanje novog suplementa koji nije na spisku.
 */
@Composable
fun SupplementDialog(
    initial: FoodEntry?,
    history: List<FoodEntry>,
    onDismiss: () -> Unit,
    onSave: (text: String, kcal: Int?, protein: Int?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var picked by remember {
        mutableStateOf(initial?.let { SupplementDef(Supplements.nameOf(it.text), Supplements.doseOf(it.text), "", it.kcal, it.proteinG) })
    }
    var query by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf(picked?.dose ?: "") }
    var kcal by remember { mutableStateOf(picked?.kcal?.toString() ?: "") }
    var prot by remember { mutableStateOf(picked?.proteinG?.toString() ?: "") }
    fun pick(d: SupplementDef) { picked = d; dose = d.dose; kcal = d.kcal?.toString() ?: ""; prot = d.proteinG?.toString() ?: "" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) "Izmena suplementa" else if (picked == null) "Dodaj suplement" else picked!!.name) },
        text = {
            val p = picked
            if (p == null) {
                val results = remember(query, history) { Supplements.search(query, history) }
                Column {
                    OutlinedTextField(
                        query, { query = it }, singleLine = true, placeholder = { Text("Traži: kreatin, omega, protein…") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) }, shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.size(8.dp))
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        if (query.isNotBlank() && results.none { it.name.equals(query.trim(), ignoreCase = true) }) item {
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .clickable { pick(SupplementDef(query.trim().replaceFirstChar { it.uppercase() }, "", "")) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text("Dodaj novi: „${query.trim()}“", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                        itemsIndexed(results, key = { _, it -> "${it.category}-${it.name}" }) { i, d ->
                            Column {
                                if (query.isBlank() && (i == 0 || results[i - 1].category != d.category)) {
                                    Text(d.category.uppercase(), style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp, start = 8.dp))
                                }
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { pick(d) }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (d.category == "Skoro korišćeno") {
                                        Icon(Icons.Filled.History, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(d.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                    Text(d.dose, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (initial == null) TextButton(onClick = { picked = null }) { Text("← Izaberi drugi") }
                    OutlinedTextField(dose, { dose = it.take(30) }, label = { Text("Doza") }, placeholder = { Text("npr. 5 g, 2 kapsule") },
                        singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(kcal, { kcal = it.filter(Char::isDigit).take(4) }, label = { Text("kcal (opc.)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f))
                        OutlinedTextField(prot, { prot = it.filter(Char::isDigit).take(3) }, label = { Text("protein g") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f))
                    }
                    Text("Kalorije i protein se računaju u dnevni cilj.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            if (picked != null) TextButton(onClick = {
                onSave(Supplements.entryText(picked!!.name, dose), kcal.toIntOrNull(), prot.toIntOrNull())
            }) { Text("Sačuvaj") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Obriši", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Otkaži") }
            }
        },
    )
}
