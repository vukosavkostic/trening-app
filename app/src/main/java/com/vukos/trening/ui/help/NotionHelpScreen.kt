package com.vukos.trening.ui.help

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.TreningApp
import com.vukos.trening.ui.common.OutlinePill
import com.vukos.trening.ui.common.PrimaryPill
import com.vukos.trening.ui.theme.Success
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ------------------------------------------------------------------------------------------------
 * Vodic za povezivanje sa Notionom: koraci se listaju levo/desno, napredak je uvek vidljiv,
 * svaki korak ima jednu radnju (dugme vodi pravo na mesto), promptovi se kopiraju jednim dodirom,
 * a koraci koje aplikacija moze da proveri se sami stikliraju.
 * ---------------------------------------------------------------------------------------------- */

private const val URL_SIGNUP = "https://www.notion.so/signup"
private const val URL_INTEGRATIONS = "https://www.notion.so/profile/integrations"

/** Prompt za AI: pravi celu strukturu u Notionu (nazivi moraju biti tacni) i objasnjava token. */
val PROMPT_SETUP = """
Napravi mi u Notionu strukturu za moju aplikaciju za trening. Nazivi stranice, baza i kolona moraju biti TAČNO ovakvi (velika/mala slova i razmaci su bitni), jer ih aplikacija traži po imenu.

1) Napravi stranicu sa naslovom: Trening
2) Unutar te stranice napravi 6 baza (inline database) sa ovim nazivima i kolonama:

Exercises
- Name (title), Primary muscle (select), Secondary muscles (multi-select), Equipment (select), Category (select), Custom (checkbox), Image (files & media), Notes (text), AppId (text)

Routines
- Name (title), Days (multi-select: Pon, Uto, Sre, Čet, Pet, Sub, Ned), Exercises (relation → Exercises), Plan (text), Notes (text), AppId (text)

Workouts
- Name (title), Date (date), Routine (relation → Routines), Volume kg (number), Sets count (number), Duration min (number), Notes (text), AppId (text)

Sets
- Name (title), Workout (relation → Workouts), Exercise (relation → Exercises), Date (date), Set # (number), Kg (number), Reps (number), Order (number), Type (select: normal, warmup, drop, failure), AppId (text)

Body
- Name (title), Date (date), Weight kg (number), Waist cm (number), Chest cm (number), Arm cm (number), Thigh cm (number), Body fat % (number), Photos taken (checkbox), Notes (text), AppId (text)

Food
- Name (title), Date (date), Meal (select: Doručak, Ručak, Večera, Užina, Suplementi), Kcal (number), Protein g (number), AppId (text)

3) U bazi Workouts dodaj i prikaz (view) tipa Calendar po koloni Date.
4) Ne dodaji primere redova.

Na kraju mi potvrdi spisak baza i kolona, a zatim mi objasni korak po korak, jednostavno:
- kako da na notion.so/profile/integrations napravim Internal integraciju pod imenom "Trening app" i kopiram njen token (Internal Integration Secret, počinje sa ntn_),
- kako da stranicu Trening povežem sa tom integracijom (••• → Connections → Trening app),
- da token nalepim samo u aplikaciju Trening (Podešavanja → Notion) i da ga ne delim nigde drugde.
""".trimIndent()

/** Prompt za AI: pravi plan treninga u bazi Routines, u formatu koji aplikacija razume. */
val PROMPT_PLAN = """
U mom Notionu, na stranici "Trening", u bazi "Routines" napravi mi plan treninga.

Moj cilj: [npr. hipertrofija / snaga / mršavljenje]
Koliko puta nedeljno: [npr. 4]
Iskustvo: [početnik / srednji / napredni]
Oprema: [npr. cela teretana / samo bučice]
Ograničenja: [npr. bez čučnja zbog kolena, ili "nema"]

Pravila:
- Za svaki trening napravi POSEBAN red u bazi Routines.
- Name: kratak naziv treninga (npr. Push, Pull, Noge, Gornji deo).
- Plan: vežbe, jedna po redu, u formatu: Naziv vežbe SETOVIxPONAVLJANJA
  Primer:
  Barbell Bench Press 4x6-8
  Incline Dumbbell Press 3x8-12
  Cable Fly 3x12-15
- Ako želiš početnu kilažu, dodaj je na kraj reda (npr. "Barbell Squat 4x6-8 80kg"), inače je izostavi.
- Nazive vežbi piši na engleskom, standardnim nazivima (npr. Barbell Squat, Romanian Deadlift, Lat Pulldown, Seated Cable Rows).
- Kolone AppId i Exercises ostavi prazne - njih popunjava aplikacija.
Na kraju mi ukratko objasni logiku plana.
""".trimIndent()

private data class Step(
    val title: String,
    val short: String,
    val icon: ImageVector,
    val minutes: String,
)

private val STEPS = listOf(
    Step("Zašto Notion?", "Pregled", Icons.Filled.CloudDone, ""),
    Step("Napravi Notion nalog", "Nalog", Icons.Filled.PersonAdd, "1 min"),
    Step("Napravi stranicu Trening", "Struktura", Icons.Filled.TableChart, "2–10 min"),
    Step("Napravi ključ (token)", "Token", Icons.Filled.Key, "2 min"),
    Step("Poveži stranicu sa ključem", "Povezivanje", Icons.Filled.Link, "1 min"),
    Step("Unesi ključ u aplikaciju", "Aplikacija", Icons.Filled.Settings, "30 s"),
    Step("Neka AI napravi trening", "AI plan", Icons.Filled.AutoAwesome, "2 min"),
    Step("Korisni saveti", "Saveti", Icons.Filled.Lightbulb, ""),
)

@Composable
fun NotionHelpScreen(onClose: () -> Unit, onOpenSettings: () -> Unit, onOpenNotionPlans: () -> Unit) {
    val pager = rememberPagerState { STEPS.size }
    val scope = rememberCoroutineScope()
    val app = TreningApp.instance
    val routines by remember { app.db.routines().observeAll() }.collectAsStateWithLifecycle(emptyList())
    // Koraci koje aplikacija sama proverava.
    val connected = app.settings.notionConfigured
    val planImported = routines.any { it.notionPageId != null }
    val autoDone = mapOf(3 to connected, 4 to connected, 5 to connected, 6 to planImported, 2 to connected, 1 to connected)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        // Zaglavlje: zatvori, naslov, korak N od M, segmentirani napredak (dodir = skok na korak).
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Zatvori") }
            Text("Povezivanje sa Notionom", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${pager.currentPage + 1}/${STEPS.size}", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            STEPS.indices.forEach { i ->
                val c by animateColorAsState(
                    when {
                        i == pager.currentPage -> MaterialTheme.colorScheme.primary
                        autoDone[i] == true || i < pager.currentPage -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        else -> MaterialTheme.colorScheme.surfaceContainerHighest
                    }, label = "seg",
                )
                Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(c).clickable { scope.launch { pager.animateScrollToPage(i) } })
            }
        }

        HorizontalPager(pager, Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 0.dp)) { page ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                StepHeader(page, autoDone[page] == true)
                when (page) {
                    0 -> OverviewPage(autoDone) { i -> scope.launch { pager.animateScrollToPage(i) } }
                    1 -> AccountPage()
                    2 -> StructurePage()
                    3 -> TokenPage()
                    4 -> ConnectPage()
                    5 -> AppPage(connected, onOpenSettings)
                    6 -> AiPlanPage(connected, onOpenNotionPlans)
                    7 -> TipsPage()
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // Navigacija: Nazad / Dalje (na kraju: Gotovo).
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (pager.currentPage > 0) TextButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) {
                Text("Nazad", style = MaterialTheme.typography.labelLarge)
            } else TextButton(onClick = onClose) { Text("Preskoči", style = MaterialTheme.typography.labelLarge) }
            Spacer(Modifier.weight(1f))
            val last = pager.currentPage == STEPS.lastIndex
            PrimaryPill(
                if (last) "Gotovo" else if (pager.currentPage == 0) "Počni" else "Dalje",
                { if (last) onClose() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                Modifier.width(150.dp),
            )
        }
    }
}

@Composable
private fun StepHeader(page: Int, done: Boolean) {
    val s = STEPS[page]
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(s.icon, null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(if (page == 0) "VODIČ" else "KORAK $page" + if (s.minutes.isNotEmpty()) " · ${s.minutes}" else "",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(s.title, style = MaterialTheme.typography.headlineSmall)
        }
        if (done) Icon(Icons.Filled.CheckCircle, "Završeno", tint = Success)
    }
}

/* ------------------------------- Stranice ------------------------------- */

@Composable
private fun OverviewPage(done: Map<Int, Boolean>, onJump: (Int) -> Unit) {
    Body("Notion je besplatan „digitalni notes“. Aplikacija u njega sama šalje sve što uneseš, pa imaš:")
    Benefit(Icons.Filled.CloudDone, "Kopiju u oblaku", "Novi telefon? Jedan klik i podaci su vraćeni.")
    Benefit(Icons.Filled.TableChart, "Pregled na računaru", "Treninzi u kalendaru, tabele, filteri, grafikoni.")
    Benefit(Icons.Filled.AutoAwesome, "AI trenera", "AI ti napravi plan u Notionu, a ti ga ovde uvučeš jednim dodirom.")
    Spacer(Modifier.height(4.dp))
    Text("Koraci", style = MaterialTheme.typography.titleMedium)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer)) {
        STEPS.drop(1).dropLast(1).forEachIndexed { k, s ->
            val i = k + 1
            Row(
                Modifier.fillMaxWidth().clickable { onJump(i) }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NumberDot(i, done[i] == true)
                Spacer(Modifier.width(12.dp))
                Text(s.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Text(s.minutes, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Tip("Ukupno oko 10 minuta. Možeš da staneš bilo kad - vodič uvek otvaraš iz Podešavanja (Uputstvo).")
}

@Composable
private fun AccountPage() {
    Body("Ako već koristiš Notion, preskoči ovaj korak.")
    Steps(
        "Otvori notion.so i izaberi „Sign up“.",
        "Prijavi se Google nalogom ili mejlom (besplatno, bez kartice).",
        "Na pitanje „How are you planning to use Notion?“ izaberi „For myself“.",
    )
    OutlinePill("Otvori notion.so", openUri(URL_SIGNUP), Modifier.fillMaxWidth(), Icons.AutoMirrored.Filled.OpenInNew)
    Tip("Notion ima i aplikaciju za telefon i računar - korisno za pregled, ali nije obavezna.")
}

@Composable
private fun StructurePage() {
    var mode by remember { mutableIntStateOf(0) }
    Body("Aplikacija traži stranicu „Trening“ sa 6 tabela (baza). Najlakše je da ih napravi AI.")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        listOf("Sa AI-jem (preporuka)", "Ručno").forEachIndexed { i, l ->
            SegmentedButton(selected = mode == i, onClick = { mode = i }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(l) }
        }
    }
    NotionPageMock()
    if (mode == 0) {
        Steps(
            "Kopiraj prompt ispod.",
            "Nalepi ga u AI koji ima pristup tvom Notionu: Notion AI (dugme ✦ u Notionu), Claude sa Notion konektorom ili ChatGPT sa Notion konektorom.",
            "AI pravi stranicu i tabele, a na kraju ti objasni kako da napraviš ključ (sledeći korak).",
        )
        PromptBox(PROMPT_SETUP)
    } else {
        Steps(
            "U Notionu napravi novu stranicu i nazovi je tačno: Trening",
            "Unutar nje ukucaj /database i izaberi „Database - Inline“. Napravi 6 tabela: Exercises, Routines, Workouts, Sets, Body, Food.",
            "U svakoj tabeli dodaj kolone tačno kao u prompt-u (naziv i tip). Najbrže je da prompt pročitaš kao spisak.",
        )
        PromptBox(PROMPT_SETUP, collapsed = true)
    }
    Warn("Nazivi moraju biti tačni (npr. „Weight kg“, ne „Weight“). Aplikacija ih traži po imenu.")
}

@Composable
private fun TokenPage() {
    Body("Ključ (token) je „lozinka“ kojom aplikacija sme da piše u tvoju stranicu Trening - i ništa drugo.")
    Steps(
        "Otvori stranicu integracija (dugme ispod) i izaberi „+ New integration“.",
        "Name: Trening app · Type: Internal · izaberi svoj workspace · Save.",
        "Otvori karticu „Configuration“ i kod „Internal Integration Secret“ dodirni Show, pa Copy.",
    )
    IntegrationMock()
    OutlinePill("Otvori Notion integracije", openUri(URL_INTEGRATIONS), Modifier.fillMaxWidth(), Icons.AutoMirrored.Filled.OpenInNew)
    Warn("Token počinje sa ntn_. Čuvaj ga kao lozinku: ne šalji ga u chat, mejl ni poruke. Ako procuri, na istoj strani izaberi „Refresh“.")
}

@Composable
private fun ConnectPage() {
    Body("Integracija vidi samo stranice koje joj ti dozvoliš. Dozvoli joj stranicu Trening:")
    Steps(
        "U Notionu otvori stranicu Trening.",
        "Gore desno dodirni ••• (tri tačke).",
        "Izaberi „Connections“ (ili „Add connections“), potraži „Trening app“ i potvrdi.",
    )
    ConnectionsMock()
    Tip("Tabele unutar stranice Trening automatski dobijaju istu dozvolu.")
}

@Composable
private fun AppPage(connected: Boolean, onOpenSettings: () -> Unit) {
    Body("Poslednji korak: nalepi token u aplikaciju.")
    Steps(
        "Otvori Podešavanja (dugme ispod) → sekcija Notion.",
        "Nalepi token u polje „Notion token“.",
        "Dodirni „Sačuvaj i poveži“. Aplikacija sama pronađe stranicu Trening i svih 6 tabela.",
    )
    AppSettingsMock(connected)
    if (connected) StatusOk("Povezano! Sve što uneseš šalje se u Notion automatski.")
    else PrimaryPill("Otvori Podešavanja", onOpenSettings, Modifier.fillMaxWidth(), Icons.Filled.Settings)
    Tip("Token se čuva šifrovan samo na ovom telefonu. Ako nešto ne radi: proveri korak 4 (Connections) i tačne nazive tabela.")
}

@Composable
private fun AiPlanPage(connected: Boolean, onOpenNotionPlans: () -> Unit) {
    Body("Ne moraš sam da kucaš planove. Reci AI-ju šta želiš, on popuni tabelu Routines, a ti plan uvučeš ovde.")
    Steps(
        "Kopiraj prompt i u zagradama upiši svoj cilj, broj treninga i opremu.",
        "Nalepi ga u AI koji ima pristup Notionu (Notion AI, Claude ili ChatGPT sa Notion konektorom).",
        "U aplikaciji: Trening → ☁ (Notion planovi) → „Dodaj“ pored plana. Gotovo - plan je spreman za „Započni trening“.",
    )
    PlanMock()
    PromptBox(PROMPT_PLAN)
    PrimaryPill("Otvori Notion planove", onOpenNotionPlans, Modifier.fillMaxWidth(), Icons.Filled.FitnessCenter, enabled = connected)
    if (!connected) Text("Prvo poveži Notion (korak 5).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Tip("Format jednog reda: „Barbell Squat 4x6-8 80kg“ - naziv, setovi x ponavljanja (opseg ili broj) i opciono kilaža. Nepoznat naziv vežbe postaje tvoja nova vežba.")
}

@Composable
private fun TipsPage() {
    Benefit(Icons.Filled.TableChart, "Kalendar treninga", "U tabeli Workouts otvori prikaz Calendar - vidiš svaki trening po danima.")
    Benefit(Icons.Filled.FitnessCenter, "Svi setovi na jednom mestu", "Tabela Sets: filtriraj po vežbi i vidi svaki set ikad (kg, ponavljanja, tip).")
    Benefit(Icons.Filled.AutoAwesome, "Pitaj AI o napretku", "„Pogledaj moje Workouts i Sets za poslednji mesec i predloži izmene plana.“")
    Benefit(Icons.Filled.CloudDone, "Novi telefon", "Podešavanja → „Vrati podatke iz Notiona“. Slike tela nisu u Notionu - za njih koristi Rezervnu kopiju.")
    Benefit(Icons.Filled.Lightbulb, "Suplementi i ishrana", "Tabela Food, filter Meal = Suplementi - sve što si uzimao.")
    Warn("Izmene koje napraviš direktno u Notionu se ne vraćaju u aplikaciju (osim uvoza planova). Aplikacija je „glavna“, Notion je kopija i pregled.")
}

/* ------------------------------- Gradivni elementi ------------------------------- */

@Composable
private fun Body(text: String) = Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun NumberDot(n: Int, done: Boolean) {
    Box(
        Modifier.size(28.dp).clip(CircleShape).background(if (done) Success else MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Filled.CheckCircle, null, Modifier.size(28.dp), tint = Color.White)
        else Text("$n", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Steps(vararg items: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { i, t ->
            Row {
                Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Text(t, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 1.dp))
            }
        }
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, text: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(14.dp)) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Tip(text: String) = Callout(Icons.Filled.Lightbulb, text, MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.primary)

@Composable
private fun Warn(text: String) = Callout(Icons.Filled.Warning, text, Color(0xFF3A2A10), Color(0xFFF5B83D))

@Composable
private fun StatusOk(text: String) = Callout(Icons.Filled.CheckCircle, text, Success.copy(alpha = 0.15f), Success)

@Composable
private fun Callout(icon: ImageVector, text: String, bg: Color, tint: Color) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).padding(14.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Prompt sa dugmetom za kopiranje (prikazuje "Kopirano ✓" na 2 s). */
@Composable
private fun PromptBox(prompt: String, collapsed: Boolean = false) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(!collapsed) }
    LaunchedEffect(copied) { if (copied) { delay(2000); copied = false } }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Text("  Prompt za AI", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Skupi" else "Prikaži") }
        }
        Text(
            if (expanded) prompt else prompt.lines().take(3).joinToString("\n") + "…",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 17.sp),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Box(Modifier.padding(12.dp)) {
            if (copied) StatusOk("Kopirano - nalepi u AI chat.")
            else PrimaryPill("Kopiraj prompt", { clipboard.setText(AnnotatedString(prompt)); copied = true },
                Modifier.fillMaxWidth(), Icons.Filled.ContentCopy)
        }
    }
}

@Composable
private fun openUri(url: String): () -> Unit {
    val h = LocalUriHandler.current
    return { runCatching { h.openUri(url) } }
}

/* ------------------------------- Ilustracije (crtane u aplikaciji) ------------------------------- */

private val PaperBg = Color(0xFFFFFFFF)
private val PaperInk = Color(0xFF37352F)
private val PaperMuted = Color(0xFF9B9A97)
private val PaperLine = Color(0xFFEDECE9)

@Composable
private fun Illustration(content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), Color(0xFF2B1A3A))))
            .padding(18.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PaperBg).padding(14.dp),
            content = content,
        )
    }
}

@Composable
private fun MockText(t: String, bold: Boolean = false, color: Color = PaperInk, size: Int = 13) =
    Text(t, color = color, fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)

@Composable
private fun NotionPageMock() = Illustration {
    MockText("🏋️  Trening", bold = true, size = 18)
    Spacer(Modifier.height(8.dp))
    listOf("Exercises", "Routines", "Workouts", "Sets", "Body", "Food").forEach {
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TableChart, null, Modifier.size(14.dp), tint = PaperMuted)
            Spacer(Modifier.width(8.dp))
            MockText(it)
            Spacer(Modifier.weight(1f))
            Box(Modifier.width(60.dp).height(6.dp).clip(CircleShape).background(PaperLine))
        }
    }
}

@Composable
private fun IntegrationMock() = Illustration {
    MockText("New integration", bold = true, size = 15)
    Spacer(Modifier.height(8.dp))
    MockField("Name", "Trening app")
    MockField("Type", "Internal")
    Spacer(Modifier.height(8.dp))
    MockText("Internal Integration Secret", color = PaperMuted, size = 11)
    Row(Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(6.dp)).background(PaperLine).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        MockText("ntn_••••••••••••••••", size = 12)
        Spacer(Modifier.weight(1f))
        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF2383E2)).padding(horizontal = 8.dp, vertical = 3.dp)) {
            MockText("Copy", color = Color.White, size = 11, bold = true)
        }
    }
}

@Composable
private fun MockField(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        MockText(label, color = PaperMuted, size = 12)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f).border(BorderStroke(1.dp, PaperLine), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
            MockText(value, size = 12)
        }
    }
}

@Composable
private fun ConnectionsMock() = Illustration {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MockText("🏋️  Trening", bold = true, size = 15)
        Spacer(Modifier.weight(1f))
        Box(Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp)) { MockText("•••", bold = true) }
    }
    Spacer(Modifier.height(10.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, PaperLine, RoundedCornerShape(10.dp)).padding(vertical = 4.dp)) {
        listOf("Copy link", "Connections  ›", "Move to").forEach { item ->
            val hl = item.startsWith("Connections")
            Row(
                Modifier.fillMaxWidth().background(if (hl) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) { MockText(item, bold = hl) }
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp), tint = Color(0xFF2E9E5B))
        Spacer(Modifier.width(6.dp))
        MockText("Trening app", bold = true)
    }
}

@Composable
private fun AppSettingsMock(connected: Boolean) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), Color(0xFF2B1A3A)))).padding(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(14.dp)) {
            Text("Notion token", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)).padding(10.dp)) {
                Text("ntn_••••••••••", style = MaterialTheme.typography.bodyMedium)
            }
            Box(Modifier.fillMaxWidth().clip(CircleShape).background(if (connected) Success else MaterialTheme.colorScheme.primary).padding(10.dp),
                contentAlignment = Alignment.Center) {
                Text(if (connected) "Povezano ✓" else "Sačuvaj i poveži", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun PlanMock() = Illustration {
    MockText("Routines", bold = true, size = 15)
    Spacer(Modifier.height(6.dp))
    listOf("Push" to "Barbell Bench Press 4x6-8 …", "Pull" to "Lat Pulldown 4x8-12 …", "Noge" to "Barbell Squat 4x6-8 …").forEach { (n, p) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Box(Modifier.width(56.dp)) { MockText(n, bold = true, size = 12) }
            MockText(p, color = PaperMuted, size = 12)
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        MockText("u aplikaciji →", color = PaperMuted, size = 12)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.clip(CircleShape).background(Color(0xFFFF5A1F)).padding(horizontal = 12.dp, vertical = 4.dp)) {
            MockText("Dodaj", color = Color.White, bold = true, size = 12)
        }
        Spacer(Modifier.width(6.dp))
        MockText("→ Dodato ✓", color = Color(0xFF2E9E5B), bold = true, size = 12)
    }
}
