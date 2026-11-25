package com.vukos.trening.ui.journal

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import com.vukos.trening.ui.body.srDate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vukos.trening.data.ExerciseSearch
import com.vukos.trening.data.FoodEntry
import com.vukos.trening.data.SupplementDef
import com.vukos.trening.data.Supplements
import com.vukos.trening.i18n.tr
import com.vukos.trening.reminders.SuppReminder
import com.vukos.trening.reminders.SupplementReminders
import com.vukos.trening.ui.theme.Accent
import com.vukos.trening.ui.theme.Ds
import com.vukos.trening.ui.theme.Manrope
import com.vukos.trening.ui.workout.IosSwitch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt

/** Doza razlozena na broj i jedinicu ("3,2 g" -> 3.2, "g"). */
private data class Dose(val value: Double, val unit: String)

private fun parseDose(s: String): Dose {
    val m = Regex("^\\s*([0-9]+(?:[.,][0-9]+)?)\\s*(.*)$").find(s) ?: return Dose(1.0, s.trim())
    return Dose(m.groupValues[1].replace(',', '.').toDouble(), m.groupValues[2].trim())
}

private fun fmtNum(v: Double): String {
    val t = String.format(java.util.Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')
    return if (com.vukos.trening.i18n.Lang.en) t else t.replace('.', ',')
}

private fun doseText(v: Double, unit: String) = if (unit.isBlank()) fmtNum(v) else "${fmtNum(v)} $unit"

/** Korak za -/+ i predlozi doze (vrednosti iz dizajna v2.1; za ostale po jedinici). */
private val STEPS = listOf(
    "kreatin" to (1.0 to listOf(3.0, 5.0, 10.0)), "kofein" to (50.0 to listOf(100.0, 200.0, 300.0)),
    "pre-workout" to (0.5 to listOf(0.5, 1.0, 1.5)), "beta-alanin" to (0.8 to listOf(1.6, 3.2, 6.4)),
    "citrulin" to (1.0 to listOf(4.0, 6.0, 8.0)), "arginin" to (1.0 to listOf(2.0, 3.0, 5.0)),
    "betain" to (1.25 to listOf(1.25, 2.5, 5.0)), "taurin" to (1.0 to listOf(1.0, 2.0, 3.0)),
    "whey" to (5.0 to listOf(20.0, 30.0, 40.0)), "kazein" to (5.0 to listOf(20.0, 30.0, 40.0)),
    "eaa" to (5.0 to listOf(5.0, 10.0, 15.0)), "omega" to (1.0 to listOf(1.0, 2.0, 3.0)),
    "magnezijum" to (50.0 to listOf(200.0, 300.0, 400.0)), "cink" to (5.0 to listOf(15.0, 25.0, 50.0)),
    "asvaganda" to (100.0 to listOf(300.0, 600.0)), "vitamin d3" to (1000.0 to listOf(1000.0, 2000.0, 4000.0)),
    "vitamin c" to (250.0 to listOf(250.0, 500.0, 1000.0)), "multivitamin" to (1.0 to listOf(1.0, 2.0)),
)

private fun stepsFor(srName: String, d: Dose): Pair<Double, List<Double>> {
    val n = ExerciseSearch.normalize(srName)
    STEPS.firstOrNull { n.contains(ExerciseSearch.normalize(it.first)) }?.let { return it.second }
    val step = when (d.unit.lowercase()) {
        "g" -> if (d.value >= 20) 5.0 else 1.0
        "mg" -> if (d.value >= 1000) 250.0 else 50.0
        "iu" -> 1000.0
        "µg" -> if (d.value >= 500) 250.0 else 25.0
        "ml" -> 10.0
        "merica", "scoop" -> 0.5
        else -> 1.0
    }
    return step to listOf(d.value - step, d.value, d.value + step).filter { it > 0 }
}

/** "Moji svakodnevni" se prave iz ranijih unosa; uklonjen ostaje skriven dok se ponovo ne unese. */
private object Stack {
    private fun prefs(c: Context) = c.getSharedPreferences("supp_stack", Context.MODE_PRIVATE)
    fun hiddenAt(c: Context, name: String): Long = prefs(c).getLong(ExerciseSearch.normalize(name), 0L)
    fun hide(c: Context, name: String) = prefs(c).edit().putLong(ExerciseSearch.normalize(name), System.currentTimeMillis()).apply()
}

/** Dodavanje suplementa (dizajn v2.1): korak 1 lista, korak 2 doza. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSupplementSheet(
    day: LocalDate,
    dayEntries: List<FoodEntry>,
    history: List<FoodEntry>,
    reminders: Int,
    onReminders: () -> Unit,
    onLog: (text: String, kcal: Int?, protein: Int?, at: LocalTime?) -> Unit,
) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<String?>(null) }
    var picked by remember { mutableStateOf<SupplementDef?>(null) }
    var hiddenTick by remember { mutableStateOf(0) }
    val p = picked
    if (p != null) {
        DoseStep(p, day, onBack = { picked = null }) { text, k, pr, remind, time, at ->
            if (remind) {
                val d = parseDose(Supplements.doseOf(text))
                SupplementReminders.save(ctx, SupplementReminders.load(ctx) + SuppReminder(
                    name = Supplements.nameOf(text), dose = doseText(d.value, d.unit), hour = time.hour, minute = time.minute, kcal = k, proteinG = pr,
                ))
            }
            onLog(text, k, pr, at)
        }
        return
    }
    val zone = ZoneId.systemDefault()
    val q = query.trim()
    // Svakodnevni: svi raniji suplementi (najnoviji prvi) sa poslednjom dozom, osim skrivenih.
    val stack = remember(history, hiddenTick) {
        history.filter { it.meal == Supplements.MEAL }.sortedByDescending { it.createdAt }
            .distinctBy { ExerciseSearch.normalize(Supplements.nameOf(it.text)) }
            .filter { it.createdAt > Stack.hiddenAt(ctx, Supplements.nameOf(it.text)) }
    }
    val takenToday = dayEntries.filter { it.meal == Supplements.MEAL }.associateBy { ExerciseSearch.normalize(Supplements.nameOf(it.text)) }
    val list = remember(q, cat, history) {
        Supplements.search(q, emptyList()).filter { q.isNotEmpty() || cat == null || it.category == cat }
    }
    LazyColumn(
        Modifier.fillMaxWidth().imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(tr("Dodaj suplement", "Add supplement"), Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.44).sp, color = Ds.ink)
                Text(if (day == LocalDate.now()) tr("Danas", "Today") else day.srDate(), fontSize = 14.sp, color = Ds.sub)
            }
        }
        item { DsField(query, { query = it.take(40) }, tr("Pretraži kreatin, omega, protein", "Search creatine, omega, protein"), Icons.Filled.Search) }
        if (q.isEmpty() && stack.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.Bottom) {
                    com.vukos.trening.ui.common.Kicker(tr("Moji svakodnevni", "Your daily stack"), Ds.sub, Modifier.weight(1f))
                    Text(tr("${stack.count { ExerciseSearch.normalize(Supplements.nameOf(it.text)) in takenToday }} od ${stack.size} danas",
                        "${stack.count { ExerciseSearch.normalize(Supplements.nameOf(it.text)) in takenToday }} of ${stack.size} today"), fontSize = 13.sp, color = Ds.faint)
                }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Ds.s1)) {
                    stack.forEachIndexed { i, e ->
                        val name = Supplements.nameOf(e.text)
                        val dose = Supplements.doseOf(e.text)
                        val t = takenToday[ExerciseSearch.normalize(name)]
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(name, fontSize = 16.sp, color = Ds.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (t != null) tr("${Supplements.doseOf(t.text)} uneto", "${Supplements.doseOf(t.text)} taken").trim()
                                    else tr("Obično ${dose.ifBlank { "—" }}", "Usually ${dose.ifBlank { "—" }}"),
                                    fontSize = 13.sp, color = Ds.sub, maxLines = 1,
                                )
                            }
                            if (t != null) Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp), tint = Ds.green)
                                val lt = Instant.ofEpochMilli(t.createdAt).atZone(zone).toLocalTime()
                                Text("%02d:%02d".format(lt.hour, lt.minute), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ds.green)
                            } else Box(
                                Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Ds.aSoft).clickable { onLog(e.text, e.kcal, e.proteinG, null) }.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(if (dose.isBlank()) tr("Unesi", "Log") else tr("Unesi $dose", "Log $dose"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText, maxLines = 1)
                            }
                            Box(
                                Modifier.size(32.dp).clip(CircleShape).clickable { Stack.hide(ctx, name); hiddenTick++ },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Filled.Close, tr("Ukloni iz svakodnevnih", "Remove from daily stack"), Modifier.size(16.dp), tint = Ds.faint) }
                        }
                        if (i < stack.size - 1) HorizontalDivider(thickness = 1.dp, color = Ds.line)
                    }
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf<String?>(null) + Supplements.CATEGORIES) { c ->
                    val on = cat == c && q.isEmpty()
                    Box(
                        Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (on) Ds.ink else Ds.s1).clickable { cat = c; query = "" }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(c?.let { Supplements.categoryLabel(it) } ?: tr("Sve", "All"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) Ds.bg else Ds.ink)
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Ds.s1)) {
                list.forEachIndexed { i, d ->
                    CatalogRow(d.name, Supplements.categoryLabel(d.category), d.dose, Ds.ink, FontWeight.Normal) { picked = d }
                    if (i < list.size - 1 || (q.isNotEmpty() && list.none { ExerciseSearch.normalize(it.name) == ExerciseSearch.normalize(q) }))
                        HorizontalDivider(thickness = 1.dp, color = Ds.line)
                }
                if (q.isNotEmpty() && list.none { ExerciseSearch.normalize(it.name) == ExerciseSearch.normalize(q) }) {
                    CatalogRow(tr("Dodaj „$q“", "Add “$q”"), tr("Novi suplement", "New supplement"), "", Ds.aText, FontWeight.SemiBold) {
                        picked = SupplementDef(q, "1 g", "")
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Ds.s1).clickable(onClick = onReminders).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Filled.Alarm, null, Modifier.size(18.dp), tint = Ds.aText)
                Text(tr("Podsetnici", "Reminders"), Modifier.weight(1f), fontSize = 16.sp, color = Ds.ink)
                if (reminders > 0) Text(tr("aktivno: $reminders", "$reminders active"), fontSize = 14.sp, color = Ds.sub)
                Icon(Icons.Filled.ChevronRight, null, Modifier.size(16.dp), tint = Ds.faint)
            }
        }
    }
}

@Composable
private fun CatalogRow(name: String, sub: String, dose: String, ink: Color, weight: FontWeight, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(start = 16.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 16.sp, fontWeight = weight, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub.isNotEmpty()) Text(sub, fontSize = 13.sp, color = Ds.sub, maxLines = 1)
        }
        if (dose.isNotEmpty()) Text(dose, fontSize = 14.sp, color = Ds.sub, maxLines = 1)
        Icon(Icons.Filled.ChevronRight, null, Modifier.size(16.dp), tint = Ds.faint)
    }
}

/** Korak 2: doza (-/+, upis broja, predlozi), vreme, svakodnevni podsetnik i unos. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DoseStep(
    def: SupplementDef,
    day: LocalDate,
    onBack: () -> Unit,
    onLog: (text: String, kcal: Int?, protein: Int?, remind: Boolean, time: LocalTime, at: LocalTime) -> Unit,
) {
    val base = remember(def) { parseDose(def.dose) }
    val srName = remember(def) { def.aliases.split(' ').plus(def.name).joinToString(" ") }
    val (step, presets) = remember(def) { stepsFor(srName, base) }
    var dose by remember(def) { mutableStateOf(base.value) }
    var raw by remember(def) { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var remind by remember { mutableStateOf(false) }
    val now = remember { LocalTime.now() }
    // Vreme unosa u minutima dana; danas ne moze kasnije od sada.
    val nowMin = if (day == LocalDate.now()) now.hour * 60 + now.minute else 23 * 60 + 59
    var tm by remember { mutableStateOf(nowMin) }
    var timeOpen by remember { mutableStateOf(false) }
    fun setTm(t: Int) { tm = t.coerceIn(0, nowMin) }
    val at = LocalTime.of(tm / 60, tm % 60)
    val remindAt = run { val m = minOf(23 * 60 + 55, (tm + 2) / 5 * 5); LocalTime.of(m / 60, m % 60) }
    val value = raw?.let { it.replace(',', '.').toDoubleOrNull() ?: 0.0 } ?: dose
    val ok = value > 0
    val text = Supplements.entryText(def.name, doseText(value, base.unit))
    val scale = if (base.value > 0) value / base.value else 1.0
    val kcal = def.kcal?.let { (it * scale).roundToInt() }
    val prot = def.proteinG?.let { (it * scale).roundToInt() }
    val shown = raw ?: fmtNum(dose)
    val numStyle = TextStyle(fontFamily = Manrope, fontSize = 56.sp, fontWeight = FontWeight.Light, letterSpacing = (-2.24).sp, color = Ds.ink, textAlign = TextAlign.Center)

    Column(Modifier.fillMaxWidth().imePadding().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 34.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(Ds.s1).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ChevronLeft, tr("Nazad na listu", "Back to list"), Modifier.size(18.dp), tint = Ds.ink)
            }
            Column(Modifier.weight(1f)) {
                Text(def.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.44).sp, color = Ds.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (def.category.isBlank()) tr("Tvoj suplement", "Your own supplement") else Supplements.categoryLabel(def.category), fontSize = 13.sp, color = Ds.sub)
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Ds.s1).padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StepButton(Icons.Filled.Remove, tr("Manje", "Less")) { raw = null; dose = maxOf(step, ((value - step) * 100).roundToInt() / 100.0) }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.Bottom) {
                    BasicTextField(
                        shown,
                        { v ->
                            var c = v.replace(',', '.').filter { it.isDigit() || it == '.' }
                            val i = c.indexOf('.')
                            if (i >= 0) c = c.substring(0, i + 1) + c.substring(i + 1).replace(".", "")
                            raw = c.take(6).let { if (com.vukos.trening.i18n.Lang.en) it else it.replace('.', ',') }
                        },
                        Modifier.widthIn(min = 40.dp, max = 180.dp)
                            .onFocusChanged { f ->
                                if (f.isFocused) { editing = true; raw = fmtNum(dose) }
                                else if (editing) { editing = false; if (value > 0) dose = value; raw = null }
                            }
                            .dashedUnderline(if (editing) Accent else Ds.s3),
                        singleLine = true, textStyle = numStyle,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        cursorBrush = SolidColor(Accent),
                    )
                    if (base.unit.isNotBlank()) Text(base.unit, Modifier.padding(bottom = 8.dp), fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Ds.sub)
                }
                StepButton(Icons.Filled.Add, tr("Više", "More")) { raw = null; dose = ((value + step) * 100).roundToInt() / 100.0 }
            }
            Text(
                if (editing) tr("Upiši količinu u ${base.unit.ifBlank { "komadima" }}", "Type the amount in ${base.unit.ifBlank { "units" }}")
                else tr("Dodirni broj da upišeš svoju dozu", "Tap the number to type your own dose"),
                Modifier.padding(top = 0.dp), fontSize = 13.sp, color = Ds.sub,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { v ->
                    val on = kotlin.math.abs(v - value) < 1e-9
                    Box(
                        Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (on) Ds.aSoft else Ds.s3).clickable { raw = null; dose = v }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(doseText(v, base.unit), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) Ds.aText else Ds.ink) }
                }
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Ds.s1)) {
            Row(
                Modifier.fillMaxWidth().height(52.dp).clickable { timeOpen = !timeOpen }.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(tr("Vreme", "Time"), Modifier.weight(1f), fontSize = 16.sp, color = Ds.ink)
                val isNow = tm == nowMin && day == LocalDate.now()
                Text(
                    (if (isNow) tr("Sada, ", "Now, ") else if (day == LocalDate.now()) tr("Danas, ", "Today, ") else "") + "%02d:%02d".format(tm / 60, tm % 60),
                    fontSize = 15.sp, fontWeight = if (isNow) FontWeight.Normal else FontWeight.SemiBold, color = if (isNow) Ds.sub else Ds.aText,
                )
                Icon(if (timeOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, Modifier.size(16.dp), tint = Ds.faint)
            }
            if (timeOpen) {
                HorizontalDivider(thickness = 1.dp, color = Ds.line)
                Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (day == LocalDate.now()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(0, 15, 30, 60, 120)) { d ->
                            val on = nowMin - tm == d
                            Box(
                                Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (on) Ds.aSoft else Ds.s3).clickable { setTm(nowMin - d) }.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    when (d) { 0 -> tr("Sada", "Now"); 60 -> tr("pre 1 h", "1 h ago"); 120 -> tr("pre 2 h", "2 h ago"); else -> tr("pre $d min", "$d min ago") },
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) Ds.aText else Ds.ink,
                                )
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                        TimePart("%02d".format(tm / 60), tr("Sat", "Hour"), { setTm(tm + 60) }, { setTm(tm - 60) }) { v -> setTm(minOf(23, v) * 60 + tm % 60) }
                        Text(":", Modifier.padding(bottom = 4.dp), fontSize = 40.sp, fontWeight = FontWeight.Light, color = Ds.faint)
                        TimePart("%02d".format(tm % 60), tr("Minut", "Minute"), { setTm(tm + 5) }, { setTm(tm - 5) }) { v -> setTm(tm / 60 * 60 + minOf(59, v)) }
                    }
                    Text(
                        if (day == LocalDate.now()) tr("Ne može kasnije od sada", "It can't be later than now") else tr("Vreme unosa za izabrani dan", "Time of the entry on the selected day"),
                        Modifier.fillMaxWidth(), fontSize = 13.sp, color = Ds.sub, textAlign = TextAlign.Center,
                    )
                }
            }
            HorizontalDivider(thickness = 1.dp, color = Ds.line)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable { remind = !remind }.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Podseti me svaki dan", "Remind me every day"), fontSize = 16.sp, color = Ds.ink)
                    Text(
                        tr("U %02d:%02d, dodaje se u Podsetnike".format(remindAt.hour, remindAt.minute), "At %02d:%02d, adds it to Reminders".format(remindAt.hour, remindAt.minute)),
                        Modifier.padding(top = 1.dp), fontSize = 13.sp, color = Ds.sub,
                    )
                }
                IosSwitch(remind)
            }
        }
        Box(
            Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(if (ok) Accent else Ds.s3)
                .clickable(enabled = ok) { onLog(text, kcal, prot, remind, remindAt, at) },
            contentAlignment = Alignment.Center,
        ) {
            Text(if (ok) tr("Unesi ${doseText(value, base.unit)}", "Log ${doseText(value, base.unit)}") else tr("Unesi", "Log"),
                fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (ok) Color.White else Ds.faint)
        }
    }
}

/** Sat ili minut: strelice gore/dole i broj koji moze da se upise. */
@Composable
private fun TimePart(value: String, desc: String, onUp: () -> Unit, onDown: () -> Unit, onType: (Int) -> Unit) {
    val style = TextStyle(fontFamily = Manrope, fontSize = 44.sp, fontWeight = FontWeight.Light, letterSpacing = (-1.32).sp, color = Ds.ink, textAlign = TextAlign.Center)
    var raw by remember { mutableStateOf<String?>(null) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp, 30.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onUp), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.KeyboardArrowUp, "$desc +", Modifier.size(20.dp), tint = Ds.sub)
        }
        BasicTextField(
            raw ?: value,
            { v -> val d = v.filter(Char::isDigit).takeLast(2); raw = d; d.toIntOrNull()?.let(onType) },
            Modifier.widthIn(min = 56.dp, max = 64.dp).onFocusChanged { if (!it.isFocused) raw = null }.dashedUnderline(Ds.s3),
            singleLine = true, textStyle = style, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(Accent),
        )
        Box(Modifier.size(56.dp, 30.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onDown), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.KeyboardArrowDown, "$desc −", Modifier.size(20.dp), tint = Ds.sub)
        }
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(Modifier.size(52.dp).clip(CircleShape).background(Ds.s3).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, Modifier.size(20.dp), tint = Ds.ink)
    }
}

/** Isprekidana linija ispod broja (2dp). */
private fun Modifier.dashedUnderline(color: Color): Modifier = drawBehind {
    val y = size.height - 1.dp.toPx()
    drawLine(
        color, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 2.dp.toPx(),
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
    )
}
