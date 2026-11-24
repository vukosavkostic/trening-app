package com.vukos.trening.ui.routines

import com.vukos.trening.i18n.tr
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.notion.ImportResult
import com.vukos.trening.notion.NotionRoutine
import com.vukos.trening.notion.NotionRoutines
import kotlinx.coroutines.launch

/**
 * "Notion treninzi": planovi iz Notion baze Routines.
 * Plan koji nije u aplikaciji ima dugme Dodaj; dodat plan (AppId postoji lokalno) ima oznaku Dodato.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotionRoutinesScreen(onHelp: () -> Unit, onBack: () -> Unit, onSettings: () -> Unit, onOpenRoutine: (String) -> Unit) {
    val app = TreningApp.instance
    val scope = rememberCoroutineScope()
    val local by remember { app.db.routines().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val localIds = local.map { it.id }.toSet()
    var items by remember { mutableStateOf<List<NotionRoutine>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var importing by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Pair<String, ImportResult>?>(null) }

    LaunchedEffect(reload) {
        if (!app.settings.notionConfigured) { error = "notoken"; return@LaunchedEffect }
        error = null; items = null
        runCatching { NotionRoutines.list(app.settings) }
            .onSuccess { items = it }
            .onFailure { error = it.message ?: tr("Greška pri učitavanju.", "Loading failed.") }
    }

    val D = com.vukos.trening.ui.theme.Ds
    val notConnected = error == "notoken"
    LazyColumn(
        Modifier.fillMaxSize().background(D.bg).statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Circle(Icons.Filled.ChevronLeft, tr("Nazad", "Back"), onBack)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Circle(Icons.AutoMirrored.Filled.HelpOutline, tr("Uputstvo", "Guide"), onHelp)
                    if (!notConnected) Circle(Icons.Filled.Refresh, tr("Osveži", "Refresh"), enabled = items != null || error != null) { reload++ }
                }
            }
        }
        item {
            Column {
                Text(tr("Notion treninzi", "Notion workouts"), fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.68).sp, color = D.ink)
                Text(
                    when {
                        notConnected -> tr("Planovi koje napišeš u Notionu se pojavljuju ovde", "Plans you write in Notion appear here")
                        items == null && error == null -> tr("Osvežavam…", "Refreshing…")
                        else -> tr("Iz baze Routines", "From the Routines database")
                    },
                    Modifier.padding(top = 2.dp), fontSize = 15.sp, color = D.sub,
                )
            }
        }
        when {
            notConnected -> item {
                Column(
                    Modifier.padding(top = 4.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(D.s1).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column {
                        Text(tr("Nije povezano", "Not connected"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = D.aText)
                        Text(tr("Notion nije povezan", "Notion is not connected"), Modifier.padding(top = 6.dp), fontSize = 26.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.52).sp, color = D.ink)
                        Text(
                            tr("Uz Notion imaš rezervnu kopiju u oblaku, pregled na računaru, a AI može da ti napravi plan treninga koji ovde uvučeš jednim klikom.", "With Notion you get a cloud copy, a view on your computer, and AI can build a workout plan you pull in here with one tap."),
                            Modifier.padding(top = 8.dp), fontSize = 15.sp, lineHeight = 22.sp, color = D.sub,
                        )
                    }
                    Wide(tr("Uputstvo korak po korak", "Step-by-step guide"), com.vukos.trening.ui.theme.Accent, Color.White, 56.dp, onHelp)
                    Wide(tr("Već imam token", "I already have a token"), Color.Transparent, D.ink, 44.dp, onSettings)
                }
            }
            error != null -> item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(D.s1).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(tr("Ne mogu da učitam planove iz Notiona.", "Could not load plans from Notion."), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = D.ink)
                    Text(error!!, fontSize = 14.sp, color = D.sub)
                    val guide = error?.contains("Trening") == true || error?.contains("401") == true
                    Wide(if (guide) tr("Pogledaj uputstvo", "See the guide") else tr("Pokušaj ponovo", "Try again"), D.s3, D.ink, 48.dp) {
                        if (guide) onHelp() else reload++
                    }
                }
            }
            items == null -> item {
                Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = com.vukos.trening.ui.theme.Accent)
                }
            }
            items!!.isEmpty() -> item {
                Text(
                    tr("U Notion bazi Routines još nema planova.", "There are no plans in the Notion Routines database yet."),
                    Modifier.padding(4.dp), fontSize = 15.sp, color = D.sub,
                )
            }
            else -> items(items!!, key = { it.pageId }) { nr ->
                val added = nr.appId != null && nr.appId in localIds
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(D.s1)
                        .alpha(if (importing != null && importing != nr.pageId) 0.6f else 1f).padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(nr.name, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.19).sp, color = D.ink)
                            Text(tr("${nr.lines.size} vežbi", "${nr.lines.size} exercises") + (if (added) tr(" · u Mojim planovima", " · in My plans") else ""),
                                Modifier.padding(top = 2.dp), fontSize = 13.sp, color = D.sub)
                        }
                        when {
                            added -> Row(
                                Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).clickable { onOpenRoutine(nr.appId!!) }.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp), tint = D.green)
                                Text(tr("Dodato", "Added"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = D.green)
                            }
                            nr.lines.isNotEmpty() -> {
                                val busy = importing == nr.pageId
                                Box(
                                    Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (busy) D.s3 else D.aSoft)
                                        .clickable(enabled = importing == null) {
                                            importing = nr.pageId
                                            scope.launch {
                                                runCatching { NotionRoutines.import(app.settings, nr) }
                                                    .onSuccess { r ->
                                                        result = nr.name to r
                                                        items = items?.map { if (it.pageId == nr.pageId) it.copy(appId = r.routineId) else it }
                                                    }
                                                    .onFailure { error = tr("Dodavanje nije uspelo: ${it.message}", "Adding failed: ${it.message}") }
                                                importing = null
                                            }
                                        }.padding(horizontal = 18.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(if (busy) tr("Dodajem…", "Adding…") else tr("Dodaj", "Add"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                        color = if (busy) D.sub else D.aText)
                                }
                            }
                        }
                    }
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(top = 12.dp), thickness = 1.dp, color = D.line)
                    nr.lines.take(6).forEach { l ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(l.name, Modifier.weight(1f), fontSize = 14.sp, color = D.ink)
                            Text(
                                "${l.sets} × ${l.repsMin ?: ""}${if (l.repsMax != null && l.repsMax != l.repsMin) "–${l.repsMax}" else ""}" + (l.kg?.let { " · $it kg" } ?: ""),
                                fontSize = 14.sp, color = D.sub, maxLines = 1,
                            )
                        }
                        androidx.compose.material3.HorizontalDivider(thickness = 1.dp, color = D.line)
                    }
                    if (nr.lines.size > 6) Text(tr("… još ${nr.lines.size - 6}", "… ${nr.lines.size - 6} more"), Modifier.padding(top = 9.dp), fontSize = 14.sp, color = D.sub)
                    if (nr.lines.isEmpty()) Text(tr("Nema vežbi (lista na stranici ili polje Plan).", "No exercises (list on the page or the Plan field)."),
                        Modifier.padding(top = 10.dp), fontSize = 14.sp, lineHeight = 20.sp, color = D.danger)
                }
            }
        }
        if (!notConnected) item {
            Text(
                buildAnnotatedString {
                    append(tr("Napiši plan u Notion bazi Routines, jedna vežba po redu, na primer ", "Write a plan in the Notion Routines database, one exercise per line, for example "))
                    withStyle(SpanStyle(color = D.ink, fontFamily = FontFamily.Monospace, fontSize = 12.sp)) { append("Dumbbell Bench Press 3x8-12 [20kg]") }
                    append(tr(". Dodirni osveži da se pojavi ovde.", ". Tap refresh to see it here."))
                },
                Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp), fontSize = 13.sp, lineHeight = 20.sp, color = D.sub,
            )
        }
    }

    result?.let { (name, r) ->
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { result = null },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = D.sheet,
        ) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 34.dp)) {
                Text(tr("Dodato u Moje planove", "Added to My plans"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = D.green)
                Text(name, Modifier.padding(top = 6.dp), fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.52).sp, color = D.ink)
                Text(tr("Uparenih vežbi: ${r.matched}", "Matched exercises: ${r.matched}"), Modifier.padding(top = 6.dp), fontSize = 15.sp, color = D.sub)
                if (r.created.isNotEmpty()) Column(
                    Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(D.s1).padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(tr("Nisu pronađene u bazi, pa su dodate kao tvoje vežbe:", "Not found in the database, added as your own exercises:"),
                        fontSize = 14.sp, lineHeight = 20.sp, color = D.sub)
                    r.created.forEach { Text(it, Modifier.padding(top = 8.dp), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = D.ink) }
                }
                Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Wide(tr("Otvori plan", "Open plan"), com.vukos.trening.ui.theme.Accent, Color.White, 56.dp) { result = null; onOpenRoutine(r.routineId) }
                    Wide("OK", Color.Transparent, D.ink, 44.dp) { result = null }
                }
            }
        }
    }
}

@Composable
private fun Circle(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) = Circle(icon, desc, true, onClick)

@Composable
private fun Circle(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    val D = com.vukos.trening.ui.theme.Ds
    Box(Modifier.size(40.dp).alpha(if (enabled) 1f else 0.5f).clip(CircleShape).background(D.s1).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center) { Icon(icon, desc, Modifier.size(19.dp), tint = D.ink) }
}

@Composable
private fun Wide(text: String, bg: Color, ink: Color, height: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, fontSize = if (height >= 56.dp) 17.sp else 15.sp, fontWeight = FontWeight.SemiBold, color = ink)
    }
}
