package com.vukos.trening.ui.help

import com.vukos.trening.i18n.tr

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
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
val PROMPT_SETUP: String get() = if (com.vukos.trening.i18n.Lang.en) PROMPT_SETUP_EN else PROMPT_SETUP_SR
val PROMPT_PLAN: String get() = if (com.vukos.trening.i18n.Lang.en) PROMPT_PLAN_EN else PROMPT_PLAN_SR

private val PROMPT_SETUP_SR = """
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
private val PROMPT_PLAN_SR = """
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


private val PROMPT_SETUP_EN = """
Create a Notion structure for my workout app. Page, database and column names must be EXACTLY as written below (case and spaces matter), because the app looks them up by name.

1) Create a page titled: Trening
2) Inside that page create 6 inline databases with these names and columns:

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

(Keep the select values exactly as written, including the Serbian ones - the app uses them internally.)

3) In the Workouts database add a Calendar view by the Date column.
4) Do not add example rows.

Finally confirm the list of databases and columns, then explain step by step, simply:
- how to create an Internal integration named "Trening app" at notion.so/profile/integrations and copy its token (Internal Integration Secret, starts with ntn_),
- how to connect the Trening page to that integration (••• → Connections → Trening app),
- that I should paste the token only into the Trening app (Settings → Notion) and never share it anywhere else.
""".trimIndent()

private val PROMPT_PLAN_EN = """
In my Notion, on the "Trening" page, in the "Routines" database, create a workout plan for me.

My goal: [e.g. hypertrophy / strength / fat loss]
Sessions per week: [e.g. 4]
Experience: [beginner / intermediate / advanced]
Equipment: [e.g. full gym / dumbbells only]
Limitations: [e.g. no squats because of my knee, or "none"]

Rules:
- Create a SEPARATE row in the Routines database for each workout.
- Name: short workout name (e.g. Push, Pull, Legs, Upper body).
- Plan: exercises, one per line, in the format: Exercise name SETSxREPS
  Example:
  Barbell Bench Press 4x6-8
  Incline Dumbbell Press 3x8-12
  Cable Fly 3x12-15
- If you want a starting weight, add it at the end of the line (e.g. "Barbell Squat 4x6-8 80kg"), otherwise leave it out.
- Use standard English exercise names (e.g. Barbell Squat, Romanian Deadlift, Lat Pulldown, Seated Cable Rows).
- Leave the AppId and Exercises columns empty - the app fills them in.
Finally, briefly explain the logic of the plan.
""".trimIndent()

private data class Step(
    val title: String,
    val short: String,
    val icon: ImageVector,
    val minutes: String,
)

private val STEPS get() = listOf(
    Step(tr("Zašto Notion?", "Why Notion?"), tr("Pregled", "Overview"), Icons.Filled.CloudDone, ""),
    Step(tr("Napravi Notion nalog", "Create a Notion account"), tr("Nalog", "Account"), Icons.Filled.PersonAdd, "1 min"),
    Step(tr("Napravi stranicu Trening", "Create the Trening page"), tr("Struktura", "Structure"), Icons.Filled.TableChart, "2–10 min"),
    Step(tr("Napravi ključ (token)", "Create a key (token)"), "Token", Icons.Filled.Key, "2 min"),
    Step(tr("Poveži stranicu sa ključem", "Connect the page to the key"), tr("Povezivanje", "Connect"), Icons.Filled.Link, "1 min"),
    Step(tr("Unesi ključ u aplikaciju", "Enter the key in the app"), tr("Aplikacija", "App"), Icons.Filled.Settings, "30 s"),
    Step(tr("Neka AI napravi trening", "Let AI build your workout"), "AI plan", Icons.Filled.AutoAwesome, "2 min"),
    Step(tr("Korisni saveti", "Useful tips"), tr("Saveti", "Tips"), Icons.Filled.Lightbulb, ""),
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
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, tr("Zatvori", "Close")) }
            Text(tr("Povezivanje sa Notionom", "Connect to Notion"), style = HelpBody.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp), modifier = Modifier.weight(1f))
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
                verticalArrangement = Arrangement.spacedBy(18.dp),
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
                Text(tr("Nazad", "Back"), style = MaterialTheme.typography.labelLarge)
            } else TextButton(onClick = onClose) { Text(tr("Preskoči", "Skip"), style = MaterialTheme.typography.labelLarge) }
            Spacer(Modifier.weight(1f))
            val last = pager.currentPage == STEPS.lastIndex
            PrimaryPill(
                if (last) tr("Gotovo", "Done") else if (pager.currentPage == 0) tr("Počni", "Start") else tr("Dalje", "Next"),
                { if (last) onClose() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                Modifier.width(150.dp),
            )
        }
    }
}

@Composable
private fun StepHeader(page: Int, done: Boolean) {
    val s = STEPS[page]
    Column(Modifier.padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (page == 0) tr("Vodič", "Guide") else tr("Korak $page od ${STEPS.size - 2}", "Step $page of ${STEPS.size - 2}") + if (s.minutes.isNotEmpty()) "  ·  ${s.minutes}" else "",
                style = HelpSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
            )
            if (done) {
                Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp), tint = Success)
                Text(tr("  Završeno", "  Done"), style = HelpSmall, color = Success)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(s.title, style = HelpTitle)
    }
}

/* ------------------------------- Stranice ------------------------------- */

@Composable
private fun OverviewPage(done: Map<Int, Boolean>, onJump: (Int) -> Unit) {
    Body(tr("Notion je besplatan „digitalni notes“. Aplikacija u njega sama šalje sve što uneseš, pa imaš:", "Notion is a free “digital notebook”. The app sends everything you log there automatically, so you get:"))
    Benefit(Icons.Filled.CloudDone, tr("Kopiju u oblaku", "A cloud copy"), tr("Novi telefon? Jedan klik i podaci su vraćeni.", "New phone? One tap and your data is back."))
    Benefit(Icons.Filled.TableChart, tr("Pregled na računaru", "A view on your computer"), tr("Treninzi u kalendaru, tabele, filteri, grafikoni.", "Workouts in a calendar, tables, filters, charts."))
    Benefit(Icons.Filled.AutoAwesome, tr("AI trenera", "An AI coach"), tr("AI ti napravi plan u Notionu, a ti ga ovde uvučeš jednim dodirom.", "AI builds a plan in Notion and you pull it in here with one tap."))
    Divider()
    Text(tr("Koraci", "Steps"), style = HelpH2)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer)) {
        STEPS.drop(1).dropLast(1).forEachIndexed { k, s ->
            val i = k + 1
            Row(
                Modifier.fillMaxWidth().clickable { onJump(i) }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NumberDot(i, done[i] == true)
                Spacer(Modifier.width(12.dp))
                Text(s.title, Modifier.weight(1f), style = HelpBody)
                Text(s.minutes, style = HelpSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Tip(tr("Ukupno oko 10 minuta. Možeš da staneš bilo kad. Vodič uvek otvaraš iz Podešavanja.", "About 10 minutes in total. You can stop anytime. Open the guide again from Settings."))
}

@Composable
private fun AccountPage() {
    Body(tr("Ako već koristiš Notion, preskoči ovaj korak.", "If you already use Notion, skip this step."))
    Steps(
        tr("Otvori notion.so i izaberi „Sign up“.", "Open notion.so and choose “Sign up”."),
        tr("Prijavi se Google nalogom ili mejlom (besplatno, bez kartice).", "Sign up with Google or email (free, no card needed)."),
        tr("Na pitanje „How are you planning to use Notion?“ izaberi „For myself“.", "When asked “How are you planning to use Notion?”, choose “For myself”."),
    )
    HelpButton(tr("Otvori notion.so", "Open notion.so"), openUri(URL_SIGNUP), icon = Icons.AutoMirrored.Filled.OpenInNew)
    Tip(tr("Notion ima aplikaciju za telefon i računar. Korisna je za pregled, ali nije obavezna.", "Notion has phone and desktop apps. They are handy for browsing, but not required."))
}

@Composable
private fun StructurePage() {
    var mode by remember { mutableIntStateOf(0) }
    Body(tr("Aplikacija traži stranicu „Trening“ sa 6 tabela (baza). Najlakše je da ih napravi AI.", "The app looks for a page called “Trening” with 6 tables (databases). The easiest way is to let AI create them."))
    com.vukos.trening.ui.common.SegmentedToggle(listOf(tr("Sa AI-jem", "With AI"), tr("Ručno", "Manually")), mode, { mode = it })
    Small(if (mode == 0) tr("Preporuka: AI napravi sve za oko 2 minuta.", "Recommended: AI sets everything up in about 2 minutes.") else tr("Oko 10 minuta kucanja u Notionu.", "About 10 minutes of typing in Notion."))
    NotionPageMock()
    if (mode == 0) {
        Steps(
            tr("Kopiraj prompt ispod.", "Copy the prompt below."),
            tr("Nalepi ga u AI koji ima pristup tvom Notionu: Notion AI (ugrađen u Notion), Claude sa Notion konektorom ili ChatGPT sa Notion konektorom.", "Paste it into an AI that can access your Notion: Notion AI (built into Notion), Claude with the Notion connector, or ChatGPT with the Notion connector."),
            tr("AI pravi stranicu i tabele, a na kraju ti objasni kako da napraviš ključ (sledeći korak).", "AI creates the page and tables, then explains how to create the key (next step)."),
        )
        PromptBox(PROMPT_SETUP)
    } else {
        Steps(
            tr("U Notionu napravi novu stranicu i nazovi je tačno: Trening", "In Notion create a new page and name it exactly: Trening"),
            tr("Unutar nje ukucaj /database i izaberi „Database - Inline“. Napravi 6 tabela: Exercises, Routines, Workouts, Sets, Body, Food.", "Inside it type /database and choose “Database - Inline”. Create 6 tables: Exercises, Routines, Workouts, Sets, Body, Food."),
            tr("U svakoj tabeli dodaj kolone tačno kao u prompt-u (naziv i tip). Najbrže je da prompt pročitaš kao spisak.", "In each table add the columns exactly as in the prompt (name and type). Use the prompt as your checklist."),
        )
        PromptBox(PROMPT_SETUP)
    }
    Warn(tr("Nazivi moraju biti tačni (npr. „Weight kg“, ne „Weight“). Aplikacija ih traži po imenu.", "Names must be exact (e.g. “Weight kg”, not “Weight”). The app looks them up by name."))
}

@Composable
private fun TokenPage() {
    Body(tr("Ključ (token) je „lozinka“ kojom aplikacija sme da piše u tvoju stranicu Trening. Nigde drugde nema pristup.", "The key (token) is a “password” that lets the app write to your Trening page. It has no access anywhere else."))
    Steps(
        tr("Otvori stranicu integracija (dugme ispod) i izaberi „+ New integration“.", "Open the integrations page (button below) and choose “+ New integration”."),
        tr("Za Name upiši Trening app, za Type izaberi Internal, izaberi svoj workspace i sačuvaj.", "Set Name to Trening app and Type to Internal, pick your workspace and save."),
        tr("Otvori karticu „Configuration“ i kod „Internal Integration Secret“ dodirni Show, pa Copy.", "Open the “Configuration” tab and next to “Internal Integration Secret” tap Show, then Copy."),
    )
    IntegrationMock()
    HelpButton(tr("Otvori Notion integracije", "Open Notion integrations"), openUri(URL_INTEGRATIONS), icon = Icons.AutoMirrored.Filled.OpenInNew)
    Warn(tr("Token počinje sa ntn_. Čuvaj ga kao lozinku: ne šalji ga u chat, mejl ni poruke. Ako procuri, na istoj strani izaberi „Refresh“.", "The token starts with ntn_. Treat it like a password: never send it in chats, email or messages. If it leaks, choose “Refresh” on the same page."))
}

@Composable
private fun ConnectPage() {
    Body(tr("Integracija vidi samo stranice koje joj ti dozvoliš. Dozvoli joj stranicu Trening:", "The integration only sees pages you allow. Allow the Trening page:"))
    Steps(
        tr("U Notionu otvori stranicu Trening.", "In Notion open the Trening page."),
        tr("Gore desno dodirni ••• (tri tačke).", "Tap ••• (three dots) in the top right."),
        tr("Izaberi „Connections“ (ili „Add connections“), potraži „Trening app“ i potvrdi.", "Choose “Connections” (or “Add connections”), search for “Trening app” and confirm."),
    )
    ConnectionsMock()
    Tip(tr("Tabele unutar stranice Trening automatski dobijaju istu dozvolu.", "Tables inside the Trening page get the same access automatically."))
}

@Composable
private fun AppPage(connected: Boolean, onOpenSettings: () -> Unit) {
    Body(tr("Poslednji korak: nalepi token u aplikaciju.", "Last step: paste the token into the app."))
    Steps(
        tr("Otvori Podešavanja (dugme ispod) → sekcija Notion.", "Open Settings (button below) → Notion."),
        tr("Nalepi token u polje „Notion token“.", "Paste the token into the “Notion token” field."),
        tr("Dodirni „Sačuvaj i poveži“. Aplikacija sama pronađe stranicu Trening i svih 6 tabela.", "Tap “Save & connect”. The app finds the Trening page and all 6 tables by itself."),
    )
    AppSettingsMock(connected)
    if (connected) StatusOk(tr("Povezano! Sve što uneseš šalje se u Notion automatski.", "Connected! Everything you log is sent to Notion automatically."))
    else HelpButton(tr("Otvori Podešavanja", "Open Settings"), onOpenSettings, primary = true)
    Tip(tr("Token se čuva šifrovan samo na ovom telefonu. Ako nešto ne radi: proveri korak 4 (Connections) i tačne nazive tabela.", "The token is stored encrypted on this phone only. If something fails: check step 4 (Connections) and the exact table names."))
}

@Composable
private fun AiPlanPage(connected: Boolean, onOpenNotionPlans: () -> Unit) {
    Body(tr("Ne moraš sam da kucaš planove. Reci AI-ju šta želiš, on popuni tabelu Routines, a ti plan uvučeš ovde.", "No need to type plans yourself. Tell AI what you want, it fills the Routines table, and you pull the plan in here."))
    Steps(
        tr("Kopiraj prompt i u zagradama upiši svoj cilj, broj treninga i opremu.", "Copy the prompt and fill in your goal, sessions per week and equipment in the brackets."),
        tr("Nalepi ga u AI koji ima pristup Notionu (Notion AI, Claude ili ChatGPT sa Notion konektorom).", "Paste it into an AI with Notion access (Notion AI, Claude or ChatGPT with the Notion connector)."),
        tr("U aplikaciji otvori Trening → ikona oblaka (Notion planovi) i dodirni „Dodaj“ pored plana. Plan je odmah spreman za „Započni trening“.", "In the app open Workout → cloud icon (Notion plans) and tap “Add” next to the plan. The plan is ready for “Start workout” right away."),
    )
    PlanMock()
    PromptBox(PROMPT_PLAN)
    HelpButton(tr("Otvori Notion planove", "Open Notion plans"), onOpenNotionPlans, primary = true, enabled = connected)
    if (!connected) Small(tr("Prvo poveži Notion (korak 5).", "Connect Notion first (step 5)."))
    Tip(tr("Format jednog reda: „Barbell Squat 4x6-8 80kg“. Redom: naziv, setovi x ponavljanja (opseg ili broj) i, po želji, kilaža. Nepoznat naziv vežbe postaje tvoja nova vežba.", "One line format: “Barbell Squat 4x6-8 80kg”. In order: name, sets x reps (range or number) and optional weight. Unknown exercise names become your own exercises."))
}

@Composable
private fun TipsPage() {
    Benefit(Icons.Filled.TableChart, tr("Kalendar treninga", "Workout calendar"), tr("U tabeli Workouts otvori prikaz Calendar. Tu vidiš svaki trening po danima.", "Open the Calendar view in the Workouts table. It shows every workout by day."))
    Benefit(Icons.Filled.FitnessCenter, tr("Svi setovi na jednom mestu", "Every set in one place"), tr("Tabela Sets: filtriraj po vežbi i vidi svaki set ikad (kg, ponavljanja, tip).", "Sets table: filter by exercise and see every set ever (kg, reps, type)."))
    Benefit(Icons.Filled.AutoAwesome, tr("Pitaj AI o napretku", "Ask AI about your progress"), tr("„Pogledaj moje Workouts i Sets za poslednji mesec i predloži izmene plana.“", "“Look at my Workouts and Sets for the last month and suggest plan changes.”"))
    Benefit(Icons.Filled.CloudDone, tr("Novi telefon", "New phone"), tr("Podešavanja → „Vrati podatke iz Notiona“. Slike tela nisu u Notionu. Za njih koristi Rezervnu kopiju.", "Settings → “Restore from Notion”. Body photos are not in Notion. Use Backup for them."))
    Benefit(Icons.Filled.Lightbulb, tr("Suplementi i ishrana", "Supplements and food"), tr("U tabeli Food postavi filter Meal = Suplementi. Tu je sve što si uzimao.", "In the Food table set the filter Meal = Suplementi. It lists everything you have taken."))
    Warn(tr("Izmene koje napraviš direktno u Notionu se ne vraćaju u aplikaciju (osim uvoza planova). Aplikacija je „glavna“, Notion je kopija i pregled.", "Changes made directly in Notion do not flow back into the app (except plan imports). The app is the source of truth, Notion is a copy and a view."))
}

/* ------------------------------- Gradivni elementi ------------------------------- */

/* Tipografija vodica: regular font, veci prored, svetao tekst - mirno i citljivo. */
private val HelpTitle = androidx.compose.ui.text.TextStyle(fontFamily = com.vukos.trening.ui.theme.Manrope, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp)
private val HelpH2 = androidx.compose.ui.text.TextStyle(fontFamily = com.vukos.trening.ui.theme.Manrope, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp)
private val HelpBody = androidx.compose.ui.text.TextStyle(fontFamily = com.vukos.trening.ui.theme.Manrope, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp)
private val HelpSmall = androidx.compose.ui.text.TextStyle(fontFamily = com.vukos.trening.ui.theme.Manrope, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp)

@Composable
private fun Body(text: String) = Text(text, style = HelpBody, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f))

@Composable
private fun Small(text: String) = Text(text, style = HelpSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))

@Composable
private fun NumberDot(n: Int, done: Boolean) {
    Box(
        Modifier.size(26.dp).clip(CircleShape)
            .then(if (done) Modifier.background(Success) else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = Color.Black)
        else Text("$n", style = HelpSmall)
    }
}

/** Numerisani koraci kao obican tekst ("1.", "2.") - bez sarenih krugova. */
@Composable
private fun Steps(vararg items: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { i, t ->
            Row {
                Text("${i + 1}.", style = HelpBody, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(26.dp))
                Text(t, style = HelpBody, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f))
            }
        }
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp).size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = HelpBody.copy(fontWeight = FontWeight.SemiBold))
            Text(text, style = HelpSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Napomene u tonu aplikacije: neutralna kartica, samo ikona nosi boju. */
@Composable
private fun Tip(text: String) = Callout(Icons.Outlined.Lightbulb, text, MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun Warn(text: String) = Callout(Icons.Outlined.Info, text, MaterialTheme.colorScheme.primary)

@Composable
private fun StatusOk(text: String) = Callout(Icons.Filled.CheckCircle, text, Success)

@Composable
private fun Callout(icon: ImageVector, text: String, tint: Color) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.padding(top = 1.dp).size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = HelpSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
    }
}

/** Dugme vodica (zaobljen pravougaonik, kao na referenci). */
@Composable
private fun HelpButton(text: String, onClick: () -> Unit, primary: Boolean = false, icon: ImageVector? = null, enabled: Boolean = true) {
    val shape = RoundedCornerShape(12.dp)
    val content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = HelpBody.copy(fontWeight = FontWeight.SemiBold))
    }
    if (primary) androidx.compose.material3.Button(onClick, Modifier.fillMaxWidth().height(52.dp), enabled = enabled, shape = shape, content = content)
    else androidx.compose.material3.OutlinedButton(
        onClick, Modifier.fillMaxWidth().height(52.dp), enabled = enabled, shape = shape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        content = content,
    )
}

/** Prompt: skracen prikaz + "Prikazi vise", i dugme za kopiranje ("Kopirano" 2 s). */
@Composable
private fun PromptBox(prompt: String, collapsed: Boolean = true) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(2000); copied = false } }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(tr("Prompt za AI", "Prompt for AI"), style = HelpH2)
        // Ceo prompt je odmah vidljiv.
        Text(prompt, style = HelpSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
        if (copied) StatusOk(tr("Kopirano. Nalepi ga u AI chat.", "Copied. Paste it into your AI chat."))
        else HelpButton(tr("Kopiraj prompt", "Copy prompt"), { clipboard.setText(AnnotatedString(prompt)); copied = true }, primary = true, icon = Icons.Filled.ContentCopy)
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(14.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PaperBg).padding(14.dp),
            content = content,
        )
    }
}

@Composable
private fun MockText(t: String, bold: Boolean = false, color: Color = PaperInk, size: Int = 13) =
    Text(t, color = color, fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)

@Composable
private fun NotionPageMock() = Illustration {
    MockText("Trening", bold = true, size = 18)
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
        MockText("Trening", bold = true, size = 15)
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(14.dp)
    ) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(14.dp)) {
            Text("Notion token", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)).padding(10.dp)) {
                Text("ntn_••••••••••", style = MaterialTheme.typography.bodyMedium)
            }
            Box(Modifier.fillMaxWidth().clip(CircleShape).background(if (connected) Success else MaterialTheme.colorScheme.primary).padding(10.dp),
                contentAlignment = Alignment.Center) {
                Text(if (connected) tr("Povezano", "Connected") else tr("Sačuvaj i poveži", "Save & connect"), color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun PlanMock() = Illustration {
    MockText("Routines", bold = true, size = 15)
    Spacer(Modifier.height(6.dp))
    listOf("Push" to "Barbell Bench Press 4x6-8 …", "Pull" to "Lat Pulldown 4x8-12 …", tr("Noge", "Legs") to "Barbell Squat 4x6-8 …").forEach { (n, p) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Box(Modifier.width(56.dp)) { MockText(n, bold = true, size = 12) }
            MockText(p, color = PaperMuted, size = 12)
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        MockText(tr("u aplikaciji →", "in the app →"), color = PaperMuted, size = 12)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.clip(CircleShape).background(Color(0xFFFF5A1F)).padding(horizontal = 12.dp, vertical = 4.dp)) {
            MockText(tr("Dodaj", "Add"), color = Color.White, bold = true, size = 12)
        }
        Spacer(Modifier.width(6.dp))
        MockText(tr("Dodato", "Added"), color = Color(0xFF2E9E5B), bold = true, size = 12)
    }
}
