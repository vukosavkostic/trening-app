package com.vukos.trening.ui.body

import com.vukos.trening.i18n.tr

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.vukos.trening.TreningApp
import com.vukos.trening.data.BodyPhoto
import com.vukos.trening.data.BodyRepo
import com.vukos.trening.data.CheckInSchedule
import com.vukos.trening.data.Pose
import com.vukos.trening.data.SecurePhotoStore
import com.vukos.trening.reminders.Reminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Ekran merenja: kilaza + tri slike (spreda, bok, ledja) za izabrani dan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(initialDay: LocalDate, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = TreningApp.instance.db.body()
    SecureWindow()

    var day by remember { mutableStateOf(initialDay) }
    var weightText by remember { mutableStateOf("") }
    var existingPhotos by remember { mutableStateOf<Map<String, BodyPhoto>>(emptyMap()) }
    val measureText = remember { androidx.compose.runtime.mutableStateListOf("", "", "", "", "") }
    var showMeasures by remember { mutableStateOf(false) }
    val newPhotos = remember { mutableStateMapOf<Pose, Uri>() }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var actionFor by remember { mutableStateOf<Pose?>(null) }
    var viewing by remember { mutableStateOf<Pose?>(null) }

    // Ucitaj postojece merenje za izabrani dan.
    LaunchedEffect(day) {
        val e = dao.getByDay(day.toEpochDay())
        weightText = e?.weightKg?.let { it.kg().removeSuffix(" kg") } ?: weightText.takeIf { e == null } ?: ""
        existingPhotos = e?.let { dao.photosFor(it.id).associateBy { p -> p.pose } } ?: emptyMap()
        val m = e?.let { com.vukos.trening.data.Measurements.of(it) } ?: com.vukos.trening.data.Measurements()
        com.vukos.trening.data.Measurements.FIELDS.forEachIndexed { i, f -> measureText[i] = f.third(m)?.let { fmtNum(it) } ?: "" }
        if (!m.isEmpty) showMeasures = true
        newPhotos.clear()
    }

    val checkInDay = CheckInSchedule.currentCheckInDay(day)
    val photoWeek = checkInDay != null && CheckInSchedule.isPhotoDay(checkInDay)

    // Kamera: privremeni fajl u cache/capture, posle cuvanja se brise.
    var pendingCapture by remember { mutableStateOf<Pair<Pose, Uri>?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val (pose, uri) = pendingCapture ?: return@rememberLauncherForActivityResult
        if (ok) newPhotos[pose] = uri
        pendingCapture = null
    }
    var pickFor by remember { mutableStateOf<Pose?>(null) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val pose = pickFor
        if (uri != null && pose != null) newPhotos[pose] = uri
        pickFor = null
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(tr("Merenje", "Check-in")) },
            navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.Filled.Close, tr("Zatvori", "Close")) } },
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).consumeWindowInsets(pad).imePadding().fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { showDate = true }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                com.vukos.trening.ui.common.IconBadge(Icons.Filled.CalendarMonth, size = 40.dp)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(day.srLong().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                    Text(day.srRelative(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(tr("Promeni", "Change"), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }

            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6) },
                label = { Text(tr("Kilaža", "Weight")) },
                placeholder = { Text(tr("npr. 82,4", "e.g. 82.4"), style = MaterialTheme.typography.headlineMedium) },
                suffix = { Text("kg", style = MaterialTheme.typography.titleLarge) },
                textStyle = MaterialTheme.typography.headlineMedium,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Mere (opciono): obimi u cm i % masti.
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { showMeasures = !showMeasures }.padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Mere tela", "Body measurements"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(if (showMeasures) tr("Sakrij", "Hide") else tr("Dodaj (opciono)", "Add (optional)"), color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge)
                }
                if (showMeasures) {
                    Spacer(Modifier.height(10.dp))
                    com.vukos.trening.data.Measurements.FIELDS.chunked(2).forEachIndexed { row, pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                            pair.forEachIndexed { j, f ->
                                val i = row * 2 + j
                                OutlinedTextField(
                                    value = measureText[i],
                                    onValueChange = { v -> measureText[i] = v.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(5) },
                                    label = { Text(f.first) }, suffix = { Text(f.second) }, singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f),
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Column {
                Text(
                    if (photoWeek) tr("Slike (ove sedmice obavezno)", "Photos (due this week)") else tr("Slike (opciono ove sedmice)", "Photos (optional this week)"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                com.vukos.trening.ui.common.LockNote(tr("Šifrovano, samo u aplikaciji. Ne ide u galeriju ni u Notion.", "Encrypted, only inside the app. Never goes to the gallery or Notion."))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pose.entries.forEach { pose ->
                    PhotoSlot(
                        pose = pose,
                        newUri = newPhotos[pose],
                        existing = existingPhotos[pose.key],
                        modifier = Modifier.weight(1f),
                        onClick = { actionFor = pose },
                    )
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                enabled = !saving,
                onClick = {
                    val kg = parseKg(weightText)
                    if (weightText.isNotBlank() && kg == null) { error = tr("Kilaža nije ispravna.", "Weight is not a valid number."); return@Button }
                    val nums = measureText.map { it.trim().replace(',', '.').takeIf { t -> t.isNotEmpty() }?.toDoubleOrNull() }
                    if (measureText.indices.any { measureText[it].isNotBlank() && nums[it] == null }) {
                        error = tr("Neka mera nije ispravan broj.", "A measurement is not a valid number."); return@Button
                    }
                    val measures = com.vukos.trening.data.Measurements(nums[0], nums[1], nums[2], nums[3], nums[4])
                    if (kg == null && newPhotos.isEmpty() && existingPhotos.isEmpty() && measures.isEmpty) {
                        error = tr("Unesi kilažu, mere ili bar jednu sliku.", "Enter weight, measurements or at least one photo."); return@Button
                    }
                    saving = true
                    scope.launch {
                        runCatching { BodyRepo.save(context, day, kg, newPhotos.toMap(), measures) }
                            .onSuccess {
                                clearCaptures(context)
                                Reminders.cancelNotification(context)
                                onDone()
                            }
                            .onFailure { error = tr("Greška pri čuvanju: ${it.message}", "Error while saving: ${it.message}"); saving = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
            ) { Text(if (saving) tr("Čuvam…", "Saving…") else tr("Sačuvaj merenje", "Save check-in"), style = MaterialTheme.typography.labelLarge) }
        }
    }

    actionFor?.let { pose ->
        val hasPhoto = newPhotos[pose] != null || existingPhotos[pose.key] != null
        AlertDialog(
            onDismissRequest = { actionFor = null },
            title = { Text(tr("Slika: ${pose.label}", "Photo: ${pose.label}")) },
            text = {
                Column {
                    if (hasPhoto) TextButton(onClick = { actionFor = null; viewing = pose }) { Text(tr("Pogledaj", "View")) }
                    TextButton(onClick = {
                        actionFor = null
                        val uri = captureUri(context, pose)
                        pendingCapture = pose to uri
                        takePicture.launch(uri)
                    }) { Text(if (hasPhoto) tr("Slikaj ponovo", "Retake") else tr("Slikaj kamerom", "Take photo")) }
                    TextButton(onClick = {
                        actionFor = null
                        pickFor = pose
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(tr("Izaberi iz galerije", "Choose from gallery")) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { actionFor = null }) { Text(tr("Otkaži", "Cancel")) } },
        )
    }

    viewing?.let { pose ->
        Dialog(onDismissRequest = { viewing = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black).clickable { viewing = null }) {
                PhotoContent(newPhotos[pose], existingPhotos[pose.key], ContentScale.Fit, Modifier.fillMaxSize())
            }
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        day = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(tr("Otkaži", "Cancel")) } },
        ) { DatePicker(state) }
    }
}

@Composable
private fun PhotoSlot(pose: Pose, newUri: Uri?, existing: BodyPhoto?, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (newUri == null && existing == null) {
                Icon(Icons.Filled.AddAPhoto, tr("Dodaj sliku", "Add photo"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                PhotoContent(newUri, existing, ContentScale.Crop, Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(pose.label, style = MaterialTheme.typography.labelLarge)
    }
}

private fun captureUri(context: Context, pose: Pose): Uri {
    val dir = File(context.cacheDir, "capture").apply { mkdirs() }
    val file = File(dir, "${pose.key}_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
}

private fun clearCaptures(context: Context) {
    File(context.cacheDir, "capture").listFiles()?.forEach { it.delete() }
}
