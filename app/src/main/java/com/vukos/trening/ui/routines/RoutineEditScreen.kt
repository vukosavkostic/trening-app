package com.vukos.trening.ui.routines

import com.vukos.trening.i18n.tr

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.withTransaction
import com.vukos.trening.TreningApp
import com.vukos.trening.data.Routine
import com.vukos.trening.data.RoutineExercise
import com.vukos.trening.data.Weekdays
import com.vukos.trening.ui.common.ExerciseThumb
import kotlinx.coroutines.launch

/** Vezba u planu dok se plan menja. */
data class DraftItem(
    val exerciseId: String,
    val sets: Int = 3,
    val repsMin: String = "8",
    val repsMax: String = "12",
    val kg: String = "",
)

/** Stanje izmene plana; zivi dok je ekran na steku (prezivi odlazak u picker vezbi). */
class RoutineEditViewModel(private val routineId: String?) : ViewModel() {
    private val db = TreningApp.instance.db
    var loaded by mutableStateOf(routineId == null)
    var name by mutableStateOf("")
    var weekdays by mutableIntStateOf(0)
    var notes by mutableStateOf("")
    val items = mutableStateListOf<DraftItem>()
    private var original: Routine? = null

    init {
        if (routineId != null) viewModelScope.launch {
            original = db.routines().get(routineId)
            original?.let { name = it.name; weekdays = it.weekdays; notes = it.notes }
            items.addAll(db.routines().getExercises(routineId).map {
                DraftItem(it.exerciseId, it.targetSets, it.repsMin?.toString() ?: "", it.repsMax?.toString() ?: "",
                    it.targetKg?.let { kg -> kg.toString().removeSuffix(".0") } ?: "")
            })
            loaded = true
        }
    }

    fun addExercises(ids: List<String>) { ids.forEach { items.add(DraftItem(it)) } }
    fun move(i: Int, delta: Int) {
        val j = i + delta
        if (j in items.indices) { val x = items.removeAt(i); items.add(j, x) }
    }

    suspend fun save(): String {
        val base = original ?: Routine(name = name)
        val routine = base.copy(
            name = name.trim(), weekdays = weekdays, notes = notes,
            updatedAt = System.currentTimeMillis(), dirty = true,
        )
        val rows = items.mapIndexed { i, d ->
            RoutineExercise(
                routineId = routine.id, exerciseId = d.exerciseId, position = i,
                targetSets = d.sets, repsMin = d.repsMin.toIntOrNull(), repsMax = d.repsMax.toIntOrNull(),
                targetKg = d.kg.replace(',', '.').toDoubleOrNull(),
            )
        }
        db.withTransaction {
            db.routines().upsert(routine)
            db.routines().clearExercises(routine.id)
            db.routines().upsertExercises(rows)
        }
        return routine.id
    }

    suspend fun delete() { routineId?.let { db.routines().softDelete(it) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditScreen(
    routineId: String?,
    savedState: SavedStateHandle?,
    onPickExercises: () -> Unit,
    onDone: () -> Unit,
) {
    val vm: RoutineEditViewModel = viewModel(key = "routine-${routineId ?: "new"}") { RoutineEditViewModel(routineId) }
    val exercises by remember { TreningApp.instance.db.exercises().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val byId = remember(exercises) { exercises.associateBy { it.id } }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Rezultat pickera (lista ID-jeva) stize preko savedStateHandle.
    val picked = savedState?.getStateFlow<ArrayList<String>?>("picked", null)?.collectAsStateWithLifecycle()
    LaunchedEffect(picked?.value) {
        picked?.value?.let { vm.addExercises(it); savedState["picked"] = null }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (routineId == null) tr("Novi plan", "New plan") else tr("Izmena plana", "Edit plan")) },
            navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.Filled.Close, tr("Zatvori", "Close")) } },
            actions = {
                if (routineId != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, tr("Obriši", "Delete")) }
                TextButton(enabled = vm.loaded && vm.name.isNotBlank(), onClick = {
                    scope.launch { vm.save(); onDone() }
                }) { Text(tr("Sačuvaj", "Save")) }
            },
        )
    }) { pad ->
        LazyColumn(
            Modifier.padding(pad).consumeWindowInsets(pad).imePadding().fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = vm.name, onValueChange = { vm.name = it }, label = { Text(tr("Naziv plana", "Plan name")) }, placeholder = { Text(tr("npr. Push, Pull, Noge, Full Body", "e.g. Push, Pull, Legs, Full Body")) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.titleLarge, shape = RoundedCornerShape(16.dp),
                )
            }
            item { Text(tr("Vežbe (${vm.items.size})", "Exercises (${vm.items.size})"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }
            itemsIndexed(vm.items, key = { i, d -> "$i-${d.exerciseId}" }) { i, d ->
                val ex = byId[d.exerciseId]
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ex?.let { ExerciseThumb(it, 44.dp) }
                        Spacer(Modifier.width(12.dp))
                        Text(ex?.name ?: "?", style = MaterialTheme.typography.titleMedium, maxLines = 2,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.move(i, -1) }, enabled = i > 0) { Icon(Icons.Filled.KeyboardArrowUp, tr("Gore", "Up")) }
                        IconButton(onClick = { vm.move(i, 1) }, enabled = i < vm.items.lastIndex) { Icon(Icons.Filled.KeyboardArrowDown, tr("Dole", "Down")) }
                        IconButton(onClick = { vm.items.removeAt(i) }) { Icon(Icons.Filled.Close, tr("Ukloni", "Remove")) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Setovi", "Sets"), Modifier.width(56.dp), style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { if (d.sets > 1) vm.items[i] = d.copy(sets = d.sets - 1) }) { Icon(Icons.Filled.Remove, tr("Manje", "Fewer")) }
                        Text("${d.sets}", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { if (d.sets < 20) vm.items[i] = d.copy(sets = d.sets + 1) }) { Icon(Icons.Filled.Add, tr("Više", "More")) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallNum(tr("Ponav. od", "Reps from"), d.repsMin, Modifier.weight(1f)) { vm.items[i] = d.copy(repsMin = it) }
                        SmallNum(tr("do", "to"), d.repsMax, Modifier.weight(1f)) { vm.items[i] = d.copy(repsMax = it) }
                        SmallNum(tr("kg (opc.)", "kg (opt.)"), d.kg, Modifier.weight(1f), decimal = true) { vm.items[i] = d.copy(kg = it) }
                    }
                }
            }
            item {
                com.vukos.trening.ui.common.OutlinePill(tr("Dodaj vežbe", "Add exercises"), onPickExercises, Modifier.fillMaxWidth(), Icons.Filled.Add)
            }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(tr("Obrisati plan?", "Delete plan?")) },
        text = { Text(tr("Odrađeni treninzi po ovom planu ostaju sačuvani.", "Completed workouts from this plan are kept.")) },
        confirmButton = { TextButton(onClick = { scope.launch { vm.delete(); confirmDelete = false; onDone() } }) { Text(tr("Obriši", "Delete")) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("Otkaži", "Cancel")) } },
    )
}

@Composable
private fun SmallNum(label: String, value: String, modifier: Modifier, decimal: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }.take(5)) },
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier,
    )
}
