package com.vukos.trening.ui.exercises

import com.vukos.trening.i18n.tr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.data.EquipmentGroup
import com.vukos.trening.data.Exercise
import com.vukos.trening.data.ExerciseSearch
import com.vukos.trening.data.MuscleGroup
import com.vukos.trening.data.equipmentLabels
import com.vukos.trening.data.muscleLabels
import com.vukos.trening.ui.common.ExerciseThumb

/**
 * Lista vezbi sa pretragom i filterima.
 * selectionMode = true -> picker (vise vezbi odjednom, dugme "Dodaj (N)" vraca izabrane ID-jeve).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    selectionMode: Boolean,
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onConfirm: (List<String>) -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    val all by remember { TreningApp.instance.db.exercises().observeAll() }
        .collectAsStateWithLifecycle(emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    val muscles = remember { mutableStateListOf<MuscleGroup>() }
    val equipment = remember { mutableStateListOf<EquipmentGroup>() }
    val selected = remember { mutableStateListOf<String>() }

    val results = remember(all, query, muscles.toList(), equipment.toList()) {
        ExerciseSearch.filter(all, query, muscles.toSet(), equipment.toSet())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectionMode) tr("Izaberi vežbe", "Choose exercises") else tr("Vežbe", "Exercises")) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Nazad", "Back"))
                    }
                },
                actions = {
                    TextButton(onClick = onNew) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text(tr("Nova vežba", "New exercise"))
                    }
                },
            )
        },
        bottomBar = {
            if (selectionMode) {
                Button(
                    onClick = { onConfirm(selected.toList()) },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                ) { Text(if (selected.isEmpty()) tr("Izaberi vežbe", "Choose exercises") else tr("Dodaj (${selected.size})", "Add (${selected.size})")) }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).consumeWindowInsets(pad).imePadding().fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(tr("Pretraži vežbe (npr. bench, row, squat)", "Search exercises (e.g. bench, row, squat)")) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, tr("Obriši", "Delete")) }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            ChipRow(MuscleGroup.entries, { it.label }, muscles)
            ChipRow(EquipmentGroup.entries, { it.label }, equipment)
            Text(
                tr("${results.size} vežbi", "${results.size} exercises"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(results, key = { it.id }) { ex ->
                    ExerciseRow(
                        ex,
                        selectionMode = selectionMode,
                        selected = ex.id in selected,
                        onClick = {
                            if (selectionMode) {
                                if (ex.id in selected) selected.remove(ex.id) else selected.add(ex.id)
                            } else onOpen(ex.id)
                        },
                    )
                    HorizontalDivider(Modifier.padding(start = 84.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun <T> ChipRow(options: List<T>, label: (T) -> String, chosen: MutableList<T>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { opt ->
            FilterChip(
                selected = opt in chosen,
                onClick = { if (opt in chosen) chosen.remove(opt) else chosen.add(opt) },
                label = { Text(label(opt)) },
            )
        }
    }
}

@Composable
fun ExerciseRow(ex: Exercise, selectionMode: Boolean, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExerciseThumb(ex)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(ex.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    muscleLabels[ex.primaryMuscle] ?: ex.primaryMuscle,
                    equipmentLabels[ex.equipment],
                    if (ex.isCustom) "moja" else null,
                ).joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selectionMode) {
            Icon(
                if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
        }
    }
}
