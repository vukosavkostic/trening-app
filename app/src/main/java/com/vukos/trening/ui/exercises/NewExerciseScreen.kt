package com.vukos.trening.ui.exercises

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vukos.trening.TreningApp
import com.vukos.trening.data.Exercise
import com.vukos.trening.data.equipmentLabels
import com.vukos.trening.data.muscleLabels
import com.vukos.trening.data.newId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewExerciseScreen(onDone: (String?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf("chest") }
    var equipment by rememberSaveable { mutableStateOf("dumbbell") }
    var photo by remember { mutableStateOf<Uri?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { photo = it }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Nova vežba") },
            navigationIcon = { IconButton(onClick = { onDone(null) }) { Icon(Icons.Filled.Close, "Otkaži") } },
            actions = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    scope.launch {
                        val id = "custom_" + newId()
                        val image = photo?.let { copyPhoto(context, it, id) }
                        TreningApp.instance.db.exercises().upsert(
                            Exercise(
                                id = id, name = name.trim(), primaryMuscle = muscle, equipment = equipment,
                                isCustom = true, customImage = image, dirty = true,
                            )
                        )
                        onDone(id)
                    }
                }) { Text("Sačuvaj") }
            },
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).consumeWindowInsets(pad).imePadding().fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Naziv") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Text("Glavni mišić", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                muscleLabels.forEach { (key, label) ->
                    FilterChip(selected = muscle == key, onClick = { muscle = key }, label = { Text(label) })
                }
            }
            Text("Oprema", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                equipmentLabels.forEach { (key, label) ->
                    FilterChip(selected = equipment == key, onClick = { equipment = key }, label = { Text(label) })
                }
            }
            Text("Slika (opciono)", style = MaterialTheme.typography.titleSmall)
            Box(
                Modifier.size(140.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable {
                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (photo == null) Icon(Icons.Filled.AddPhotoAlternate, "Izaberi sliku", Modifier.size(40.dp))
                else AsyncImage(photo, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
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
