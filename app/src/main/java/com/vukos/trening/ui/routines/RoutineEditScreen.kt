package com.vukos.trening.ui.routines

import com.vukos.trening.i18n.tr
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState

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

private val PlanStride = 86.dp

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
    val D = com.vukos.trening.ui.theme.Ds
    var open by remember { mutableIntStateOf(-1) }
    var dragIdx by remember { mutableIntStateOf(-1) }
    var dragDy by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val stridePx = with(density) { PlanStride.toPx() }

    // Rezultat pickera (lista ID-jeva) stize preko savedStateHandle.
    val picked = savedState?.getStateFlow<ArrayList<String>?>("picked", null)?.collectAsStateWithLifecycle()
    LaunchedEffect(picked?.value) {
        picked?.value?.let { vm.addExercises(it); savedState["picked"] = null }
    }
    val canSave = vm.loaded && vm.name.isNotBlank()
    val n = vm.items.size
    val target = if (dragIdx >= 0) (dragIdx + Math.round(dragDy / stridePx)).coerceIn(0, n - 1) else -1

    LazyColumn(
        Modifier.fillMaxSize().background(D.bg).statusBarsPadding().imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Otkaži", "Cancel"), Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onDone).padding(4.dp), fontSize = 17.sp, color = D.sub)
                Text(if (routineId == null) tr("Novi plan", "New plan") else tr("Izmena plana", "Edit plan"), Modifier.weight(1f),
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = D.ink, textAlign = TextAlign.Center)
                Text(tr("Sačuvaj", "Save"), Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = canSave) { scope.launch { vm.save(); onDone() } }.padding(4.dp),
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (canSave) D.aText else D.faint)
            }
        }
        item {
            Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                val style = TextStyle(fontFamily = com.vukos.trening.ui.theme.Manrope, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.68).sp, color = D.ink)
                BasicTextField(
                    vm.name, { vm.name = it.take(40) }, Modifier.fillMaxWidth(), singleLine = true, textStyle = style,
                    cursorBrush = SolidColor(com.vukos.trening.ui.theme.Accent),
                    decorationBox = { inner -> Box { if (vm.name.isEmpty()) Text(tr("Naziv plana", "Plan name"), style = style.copy(color = D.faint)); inner() } },
                )
                Text(
                    tr("${vm.items.size} vežbi · ${vm.items.sumOf { it.sets }} setova", "${vm.items.size} exercises · ${vm.items.sumOf { it.sets }} sets") +
                        if (vm.items.isNotEmpty()) tr(" · dodirni za izmenu, drži za pomeranje", " · tap to edit, hold to move") else "",
                    Modifier.padding(top = 2.dp), fontSize = 15.sp, color = D.sub,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                vm.items.forEachIndexed { i, d ->
                    androidx.compose.runtime.key("$i-${d.exerciseId}") {
                        val me = i == dragIdx
                        val shift = when {
                            dragIdx < 0 || me -> 0.dp
                            dragIdx < i && i <= target -> -PlanStride
                            dragIdx > i && i >= target -> PlanStride
                            else -> 0.dp
                        }
                        val animShift by animateDpAsState(shift, tween(220), label = "shift")
                        val ex = byId[d.exerciseId]
                        val isOpen = open == i && dragIdx < 0
                        Column(
                            Modifier.fillMaxWidth().zIndex(if (me) 2f else 1f)
                                .offset(y = if (me) 0.dp else animShift)
                                .graphicsLayer { if (me) { translationY = dragDy; scaleX = 1.03f; scaleY = 1.03f } }
                                .then(if (me) Modifier.shadow(18.dp, RoundedCornerShape(24.dp)) else Modifier)
                                .clip(RoundedCornerShape(24.dp)).background(if (me) D.s2 else D.s1)
                                .pointerInput(i, n) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); open = -1; dragIdx = i; dragDy = 0f },
                                        onDrag = { c, amount -> c.consume(); dragDy += amount.y },
                                        onDragEnd = {
                                            val to = (dragIdx + Math.round(dragDy / stridePx)).coerceIn(0, n - 1)
                                            if (dragIdx >= 0 && to != dragIdx) { val x = vm.items.removeAt(dragIdx); vm.items.add(to, x) }
                                            dragIdx = -1; dragDy = 0f
                                        },
                                        onDragCancel = { dragIdx = -1; dragDy = 0f },
                                    )
                                }
                                .padding(start = 10.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, indication = null) { open = if (open == i) -1 else i },
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(Icons.Filled.DragIndicator, null, Modifier.size(16.dp), tint = D.faint)
                                if (ex != null) ExerciseThumb(ex, 48.dp) else Box(Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)).background(D.s2))
                                Column(Modifier.weight(1f)) {
                                    Text(ex?.name ?: "?", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = D.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(summary(d), fontSize = 14.sp, color = D.sub, maxLines = 1)
                                }
                                Icon(if (isOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, Modifier.size(18.dp), tint = D.faint)
                            }
                            if (isOpen) {
                                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(D.bg).padding(10.dp)) {
                                        Text(tr("Setovi", "Sets"), fontSize = 12.sp, color = D.sub)
                                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Step("−") { if (d.sets > 1) vm.items[i] = d.copy(sets = d.sets - 1) }
                                            Text("${d.sets}", Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = D.ink, textAlign = TextAlign.Center)
                                            Step("+") { if (d.sets < 20) vm.items[i] = d.copy(sets = d.sets + 1) }
                                        }
                                    }
                                    NumTile(tr("Ponav.", "Reps"), d.repsMin, Modifier.weight(1f)) { vm.items[i] = d.copy(repsMin = it) }
                                    NumTile(tr("do", "to"), d.repsMax, Modifier.weight(1f)) { vm.items[i] = d.copy(repsMax = it) }
                                    NumTile("kg", d.kg, Modifier.weight(1f), decimal = true) { vm.items[i] = d.copy(kg = it) }
                                }
                                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(tr("Drži i prevuci za pomeranje", "Hold and drag to move"), Modifier.weight(1f), fontSize = 13.sp, color = D.faint)
                                    Text(tr("Ukloni", "Remove"), Modifier.clip(RoundedCornerShape(8.dp)).clickable { open = -1; vm.items.removeAt(i) }.padding(horizontal = 4.dp, vertical = 6.dp),
                                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = D.danger)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(D.s1).clickable(onClick = onPickExercises), contentAlignment = Alignment.Center) {
                Text(tr("+ Dodaj vežbe", "+ Add exercises"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = D.aText)
            }
        }
        if (routineId != null) item {
            Text(tr("Obriši plan", "Delete plan"), Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(8.dp)).clickable { confirmDelete = true }.padding(8.dp),
                fontSize = 15.sp, color = D.danger, textAlign = TextAlign.Center)
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(tr("Obrisati plan?", "Delete plan?")) },
        text = { Text(tr("Odrađeni treninzi po ovom planu ostaju sačuvani.", "Completed workouts from this plan are kept.")) },
        confirmButton = { TextButton(onClick = { scope.launch { vm.delete(); confirmDelete = false; onDone() } }) { Text(tr("Obriši", "Delete"), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("Otkaži", "Cancel")) } },
    )
}

/** "3 × 8–12 · 80 kg" */
private fun summary(d: DraftItem): String {
    val reps = when {
        d.repsMin.isNotBlank() && d.repsMax.isNotBlank() && d.repsMin != d.repsMax -> "${d.repsMin}–${d.repsMax}"
        else -> d.repsMin.ifBlank { d.repsMax }.ifBlank { "—" }
    }
    return "${d.sets} × $reps" + (d.kg.takeIf { it.isNotBlank() }?.let { " · $it kg" } ?: "")
}

@Composable
private fun Step(text: String, onClick: () -> Unit) {
    val D = com.vukos.trening.ui.theme.Ds
    Box(Modifier.size(22.dp).clip(CircleShape).background(D.s3).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 14.sp, color = D.ink)
    }
}

@Composable
private fun NumTile(label: String, value: String, modifier: Modifier, decimal: Boolean = false, onChange: (String) -> Unit) {
    val D = com.vukos.trening.ui.theme.Ds
    val style = TextStyle(fontFamily = com.vukos.trening.ui.theme.Manrope, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, color = D.ink)
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(D.bg).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(label, fontSize = 12.sp, color = D.sub, maxLines = 1)
        BasicTextField(
            value, { v -> onChange(v.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }.take(5)) },
            Modifier.fillMaxWidth().padding(top = 4.dp), singleLine = true, textStyle = style,
            keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
            cursorBrush = SolidColor(com.vukos.trening.ui.theme.Accent),
            decorationBox = { inner -> Box { if (value.isEmpty()) Text("—", style = style.copy(color = D.faint)); inner() } },
        )
    }
}
