package com.vukos.trening.ui.exercises

import com.vukos.trening.i18n.tr

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.vukos.trening.TreningApp
import com.vukos.trening.data.Exercise
import com.vukos.trening.data.ExerciseSearch
import com.vukos.trening.data.MuscleGroup
import com.vukos.trening.data.TrackType
import com.vukos.trening.data.equipmentLabels
import com.vukos.trening.data.muscleLabels
import com.vukos.trening.data.newId
import com.vukos.trening.ui.journal.DsField
import com.vukos.trening.ui.theme.Accent
import com.vukos.trening.ui.theme.Ds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Grupe za izbor glavnog misica (dizajn v2.1). */
private enum class PickGroup(private val sr: String, private val en: String, val muscles: List<String>) {
    PUSH("Guranje", "Push", listOf("chest", "shoulders", "triceps")),
    PULL("Vučenje", "Pull", listOf("lats", "middle back", "traps", "biceps", "forearms", "neck")),
    LEGS("Noge", "Legs", listOf("quadriceps", "hamstrings", "glutes", "calves", "adductors", "abductors")),
    CORE("Trup", "Core", listOf("abdominals", "lower back"));

    val label: String get() = tr(sr, en)
}

private val EQUIP_MAIN = listOf("barbell", "dumbbell", "cable", "machine", "body only", "bands")
private val EQUIP_MORE = listOf("e-z curl bar", "kettlebells", "medicine ball", "exercise ball", "foam roll", "other")

private fun groupFor(m: MuscleGroup?): PickGroup = when (m) {
    MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS -> PickGroup.PULL
    MuscleGroup.LEGS, MuscleGroup.GLUTES -> PickGroup.LEGS
    MuscleGroup.ABS -> PickGroup.CORE
    else -> PickGroup.PUSH
}

private fun label(m: String) = (muscleLabels[m] ?: m).replaceFirstChar { it.uppercase() }
private fun equipLabel(e: String) = (equipmentLabels[e] ?: e).replaceFirstChar { it.uppercase() }

/**
 * Nova vezba (dizajn v2.1): naziv (sa predlozima postojecih), glavni misic po grupi, oprema, nacin logovanja,
 * sporedni misici i pregled reda. [onUse] bira postojecu vezbu umesto nove, [onDone] vraca ID nove (ili null).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewExerciseScreen(
    initialName: String = "",
    muscleHint: MuscleGroup? = null,
    selectionNumber: Int? = null,
    onUse: (String) -> Unit = {},
    onDone: (String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val all by remember { TreningApp.instance.db.exercises().observeAll() }.collectAsStateWithLifecycle(emptyList())
    var name by rememberSaveable { mutableStateOf(initialName) }
    var group by rememberSaveable { mutableStateOf(groupFor(muscleHint)) }
    var muscle by rememberSaveable { mutableStateOf<String?>(null) }
    var equipment by rememberSaveable { mutableStateOf<String?>(null) }
    var moreEquip by rememberSaveable { mutableStateOf(false) }
    var track by rememberSaveable { mutableStateOf(TrackType.WEIGHT) }
    var secOpen by rememberSaveable { mutableStateOf(false) }
    val secondary = remember { mutableStateListOf<String>() }
    var photo by remember { mutableStateOf<Uri?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { photo = it }
    val n = name.trim()
    // "Vec postoji?": do 3 vezbe sa najvise zajednickih reci (od 3+ slova).
    val similar = remember(n, all) {
        val toks = ExerciseSearch.normalize(n).split(' ').filter { it.length >= 3 }
        if (toks.isEmpty()) emptyList() else all.map { e -> e to toks.count { ExerciseSearch.normalize(e.name).contains(it) } }
            .filter { it.second > 0 }.sortedByDescending { it.second }.take(3).map { it.first }
    }
    val ok = n.isNotEmpty() && muscle != null && equipment != null
    val missing = listOfNotNull(
        tr("naziv", "a name").takeIf { n.isEmpty() }, tr("glavni mišić", "a primary muscle").takeIf { muscle == null },
        tr("opremu", "equipment").takeIf { equipment == null },
    )
    val showMore = moreEquip || equipment in EQUIP_MORE

    fun save() {
        if (!ok) return
        scope.launch {
            val id = "custom_" + newId()
            val image = photo?.let { copyPhoto(context, it, id) }
            TreningApp.instance.db.exercises().upsert(
                Exercise(
                    id = id, name = n, primaryMuscle = muscle!!, equipment = equipment!!, secondaryMuscles = secondary.joinToString(","),
                    trackType = track, isCustom = true, customImage = image, dirty = true,
                )
            )
            onDone(id)
        }
    }

    Column(
        Modifier.fillMaxSize().background(Ds.bg).statusBarsPadding().imePadding().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Otkaži", "Cancel"), Modifier.clip(RoundedCornerShape(8.dp)).clickable { onDone(null) }.padding(4.dp), fontSize = 17.sp, color = Ds.sub)
            Text(tr("Nova vežba", "New exercise"), Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink, textAlign = TextAlign.Center)
            Text(tr("Sačuvaj", "Save"), Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = ok) { save() }.padding(4.dp),
                fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (ok) Ds.aText else Ds.faint)
        }
        DsField(name, { name = it.take(60) }, tr("Naziv vežbe", "Exercise name"), null, height = 56.dp, radius = 18.dp)
        if (similar.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tr("Već postoji?", "Already in the library?"), Modifier.padding(horizontal = 4.dp), fontSize = 13.sp, color = Ds.sub)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Ds.s1)) {
                similar.forEachIndexed { i, e ->
                    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        com.vukos.trening.ui.common.ExerciseThumb(e, 36.dp)
                        Column(Modifier.weight(1f)) {
                            Text(e.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ds.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            MuscleEquip(label(e.primaryMuscle), equipLabel(e.equipment))
                        }
                        Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(Ds.aSoft).clickable { onUse(e.id) }.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center) {
                            Text(tr("Izaberi", "Use"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText)
                        }
                    }
                    if (i < similar.size - 1) HorizontalDivider(thickness = 1.dp, color = Ds.line)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHead(tr("Glavni mišić", "Primary muscle"), muscle?.let { label(it) } ?: tr("Izaberi", "Pick one"), muscle != null)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Ds.s1).padding(12.dp)) {
                Segments(PickGroup.entries.map { it.label }, group.ordinal, Ds.bg) { group = PickGroup.entries[it] }
                FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.muscles.forEach { m ->
                        Chip(label(m), muscle == m) { muscle = m; secondary.remove(m) }
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHead(tr("Oprema", "Equipment"), equipment?.let { equipLabel(it) } ?: tr("Izaberi", "Pick one"), equipment != null)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (EQUIP_MAIN + if (showMore) EQUIP_MORE else emptyList()).forEach { e ->
                    Chip(equipLabel(e), equipment == e) {
                        track = when {
                            e == "body only" -> TrackType.REPS
                            equipment == "body only" && track == TrackType.REPS -> TrackType.WEIGHT
                            else -> track
                        }
                        equipment = e
                    }
                }
                Row(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).clickable { moreEquip = !showMore }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (showMore) tr("Manje", "Less") else tr("Još ${EQUIP_MORE.size}", "${EQUIP_MORE.size} more"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ds.aText)
                    Icon(if (showMore) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, Modifier.size(15.dp), tint = Ds.aText)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            com.vukos.trening.ui.common.Kicker(tr("Setovi se loguju kao", "Log sets as"), Ds.sub, Modifier.padding(horizontal = 4.dp))
            val tracks = listOf(TrackType.WEIGHT, TrackType.REPS, TrackType.TIME)
            Segments(listOf(tr("kg × ponav.", "kg × reps"), tr("Samo ponav.", "Reps only"), tr("Vreme", "Time")), tracks.indexOf(track), Ds.s1) { track = tracks[it] }
            Text(
                when (track) {
                    TrackType.REPS -> tr("Svaki set beleži samo ponavljanja. Za sklekove, zgibove i propadanja.", "Each set logs reps only. Good for push-ups, pull-ups and dips.")
                    TrackType.TIME -> tr("Svaki set beleži sekunde. Za plank i izdržaje.", "Each set logs seconds. Good for planks and holds.")
                    else -> tr("Svaki set beleži kg i ponavljanja.", "Each set logs kg and reps.")
                },
                Modifier.padding(horizontal = 4.dp), fontSize = 13.sp, lineHeight = 19.sp, color = Ds.sub,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { secOpen = !secOpen }.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically) {
                com.vukos.trening.ui.common.Kicker(tr("Sporedni mišići", "Secondary muscles"), Ds.sub, Modifier.weight(1f))
                Text(if (secondary.isEmpty()) tr("Opciono", "Optional") else "${secondary.size}", fontSize = 13.sp, color = Ds.sub)
                Icon(if (secOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, Modifier.padding(start = 4.dp).size(14.dp), tint = Ds.sub)
            }
            if (secOpen) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PickGroup.entries.flatMap { it.muscles }.filter { it != muscle }.forEach { m ->
                        val on = m in secondary
                        Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Ds.aSoft else Ds.s1)
                            .clickable { if (on) secondary.remove(m) else secondary.add(m) }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                            Text(label(m), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (on) Ds.aText else Ds.ink)
                        }
                    }
                }
                Text(tr("Svaki set se računa i kao pola seta za ove mišiće u Setovima po mišićima.", "Each set also counts as half a set for these muscles in Sets per muscle."),
                    Modifier.padding(horizontal = 4.dp), fontSize = 13.sp, lineHeight = 19.sp, color = Ds.sub)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            com.vukos.trening.ui.common.Kicker(tr("Pregled", "Preview"), Ds.sub, Modifier.padding(horizontal = 4.dp))
            Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(RoundedCornerShape(20.dp)).background(Ds.s1).padding(start = 12.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Ds.aSoft).clickable {
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }, contentAlignment = Alignment.Center) {
                    if (photo != null) AsyncImage(photo, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else if (n.isEmpty()) Icon(Icons.Filled.AddPhotoAlternate, tr("Izaberi sliku", "Choose image"), Modifier.size(22.dp), tint = Ds.aText)
                    else Text(initials(n), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ds.aText)
                }
                Column(Modifier.weight(1f)) {
                    Text(n.ifEmpty { tr("Naziv vežbe", "Exercise name") }, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp,
                        color = if (n.isEmpty()) Ds.faint else Ds.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MuscleEquip(muscle?.let { label(it) } ?: tr("Mišić", "Muscle"), equipment?.let { equipLabel(it) } ?: tr("Oprema", "Equipment"))
                        Text(tr("Moja", "Yours"), Modifier.clip(RoundedCornerShape(5.dp)).background(Ds.aSoft).padding(horizontal = 6.dp),
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp, color = Ds.aText)
                    }
                }
                if (selectionNumber != null) Box(Modifier.size(30.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                    Text("$selectionNumber", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Text(
                if (missing.isEmpty()) tr("Čuva se u tvoju biblioteku i odmah je izabrana.", "Saved to your library and selected right away.")
                else tr("Još treba: ", "Still needed: ") + missing.joinToString(", "),
                Modifier.padding(horizontal = 4.dp), fontSize = 13.sp, lineHeight = 19.sp, color = if (missing.isEmpty()) Ds.sub else Ds.aText,
            )
        }
    }
}

private fun initials(s: String) = s.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

@Composable
private fun SectionHead(title: String, value: String, set: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.Bottom) {
        com.vukos.trening.ui.common.Kicker(title, Ds.sub, Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = if (set) Ds.ink else Ds.faint, maxLines = 1)
    }
}

@Composable
internal fun MuscleEquip(muscle: String, equipment: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(muscle, fontSize = 13.sp, color = Ds.sub, maxLines = 1)
        Text(equipment, Modifier.clip(RoundedCornerShape(5.dp)).background(Ds.s3).padding(horizontal = 6.dp),
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp, color = Ds.sub, maxLines = 1)
    }
}

@Composable
private fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(if (on) Ds.ink else Color.Transparent)
            .border(1.dp, if (on) Ds.ink else Ds.s3, RoundedCornerShape(19.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (on) Ds.bg else Ds.ink, maxLines = 1) }
}

@Composable
private fun Segments(options: List<String>, selected: Int, track: Color, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(track).padding(3.dp)) {
        options.forEachIndexed { i, o ->
            Box(Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(9.dp)).background(if (i == selected) Ds.s3 else Color.Transparent).clickable { onPick(i) },
                contentAlignment = Alignment.Center) {
                Text(o, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (i == selected) Ds.ink else Ds.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Kopira izabranu sliku u interno skladiste (URI iz galerije ne vazi zauvek). */
private suspend fun copyPhoto(context: Context, uri: Uri, id: String): String? = withContext(Dispatchers.IO) {
    runCatching {
        val dir = File(context.filesDir, "exercise_images").apply { mkdirs() }
        val out = File(dir, "$id.jpg")
        context.contentResolver.openInputStream(uri)!!.use { input -> out.outputStream().use { input.copyTo(it) } }
        Uri.fromFile(out).toString()
    }.getOrNull()
}
