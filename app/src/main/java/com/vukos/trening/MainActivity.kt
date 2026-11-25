package com.vukos.trening

import com.vukos.trening.i18n.tr

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vukos.trening.data.BodyRepo
import com.vukos.trening.reminders.Reminders
import com.vukos.trening.ui.body.BodyDayScreen
import com.vukos.trening.ui.body.BodyScreen
import com.vukos.trening.ui.body.WeightChartScreen
import com.vukos.trening.ui.body.CheckInBanner
import com.vukos.trening.ui.body.CheckInScreen
import com.vukos.trening.data.WorkoutRepo
import com.vukos.trening.ui.workout.ActiveWorkoutScreen
import com.vukos.trening.ui.workout.WorkoutDetailScreen
import com.vukos.trening.ui.journal.JournalScreen
import kotlinx.coroutines.launch
import com.vukos.trening.ui.routines.NotionRoutinesScreen
import com.vukos.trening.ui.routines.RoutineEditScreen
import com.vukos.trening.ui.routines.RoutinesScreen
import com.vukos.trening.ui.settings.SettingsScreen
import java.time.LocalDate
import com.vukos.trening.ui.exercises.ExerciseDetailScreen
import com.vukos.trening.ui.exercises.ExerciseListScreen
import com.vukos.trening.ui.exercises.NewExerciseScreen
import com.vukos.trening.ui.theme.TreningTheme

private enum class Tab(val route: String, private val sr: String, private val en: String, val icon: ImageVector) {
    JOURNAL("journal", "Dnevnik", "Journal", Icons.Filled.MenuBook),
    ROUTINES("routines", "Trening", "Train", Icons.Filled.FitnessCenter),
    BODY("body", "Profil", "Profile", Icons.Filled.Person);

    val label: String get() = tr(sr, en)
}

class MainActivity : ComponentActivity() {
    /** Notifikacija moze traziti otvaranje ekrana za merenje. */
    private val openRequest = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openRequest.value = intent?.getStringExtra(Reminders.EXTRA_OPEN)
        setContent {
            TreningTheme {
                // Ikone statusne trake prate izabranu temu (ne samo sistemsku).
                val dark = com.vukos.trening.ui.theme.isDarkTheme()
                androidx.compose.runtime.DisposableEffect(dark) {
                    val t = android.graphics.Color.TRANSPARENT
                    enableEdgeToEdge(
                        statusBarStyle = if (dark) androidx.activity.SystemBarStyle.dark(t) else androidx.activity.SystemBarStyle.light(t, t),
                        navigationBarStyle = if (dark) androidx.activity.SystemBarStyle.dark(t) else androidx.activity.SystemBarStyle.light(t, t),
                    )
                    onDispose {}
                }
                AppRoot(openRequest.value) { openRequest.value = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openRequest.value = intent.getStringExtra(Reminders.EXTRA_OPEN)
    }
}

@Composable
private fun AppRoot(openRequest: String?, onOpenHandled: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = Tab.entries.any { it.route == route }
    val due by remember { BodyRepo.observeDue() }.collectAsStateWithLifecycle(null)
    val db = TreningApp.instance.db
    val active by remember { db.workouts().observeActive() }.collectAsStateWithLifecycle(null)
    val scope = rememberCoroutineScope()
    fun startWorkout(routineId: String?) = scope.launch {
        val id = if (routineId != null) WorkoutRepo.startFromRoutine(routineId) else WorkoutRepo.startEmpty()
        if (nav.currentDestination?.route != "workout/{id}") nav.navigate("workout/$id")
    }
    fun openCheckIn(day: LocalDate = LocalDate.now()) = nav.navigate("checkin/${day.toEpochDay()}")

    // Dozvola za notifikacije (Android 13+), trazi se jednom pri pokretanju.
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    // Trening u toku se uvek otvara preko celog ekrana dok se ne zavrsi (npr. posle zatvaranja aplikacije).
    LaunchedEffect(active?.id) {
        val a = active ?: return@LaunchedEffect
        if (nav.currentDestination?.route != "workout/{id}") nav.navigate("workout/${a.id}")
    }
    LaunchedEffect(openRequest) {
        if (openRequest == Reminders.OPEN_CHECKIN) { openCheckIn(); onOpenHandled() }
    }

    Scaffold(topBar = {
        // Crvena / narandzasta traka za merenje, na glavnim tabovima osim Profila
        // (Profil vec ima veliku karticu "Danas je merenje" sa istim dugmetom).
        if (showBar && route != Tab.BODY.route) CheckInBanner(due) { openCheckIn() }
    }, bottomBar = {
        if (showBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
            Tab.entries.forEach { tab ->
                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    selected = route == tab.route,
                    onClick = {
                        nav.navigate(tab.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(tab.icon, null) },
                    label = { Text(tab.label) },
                )
            }
        }
    }) { pad ->
        NavHost(nav, startDestination = Tab.ROUTINES.route, modifier = Modifier.padding(pad).consumeWindowInsets(pad)) {
            composable(Tab.JOURNAL.route) {
                JournalScreen(
                    onOpenWorkout = { nav.navigate("workout_detail/$it") },
                    onOpenBodyDay = { nav.navigate("body_day/${it.toEpochDay()}") },
                    onOpenStats = { nav.navigate("stats") },
                    onOpenSuppReminders = { nav.navigate("supp_reminders") },
                )
            }
            composable("supp_reminders") { com.vukos.trening.ui.journal.SupplementRemindersScreen(onBack = { nav.popBackStack() }) }
            composable("notion_help") {
                com.vukos.trening.ui.help.NotionHelpScreen(
                    onClose = { nav.popBackStack() },
                    onOpenSettings = { nav.navigate("settings_notion") },
                    // Vodic se zatvara; ako je otvoren sa Notion treninga, samo se vraca tamo.
                    onOpenNotionPlans = {
                        nav.popBackStack()
                        if (nav.currentDestination?.route != "notion_routines") nav.navigate("notion_routines")
                    },
                )
            }
            composable("workout/{id}") { e ->
                ActiveWorkoutScreen(
                    workoutId = e.arguments!!.getString("id")!!,
                    savedState = e.savedStateHandle,
                    onPickExercises = { nav.navigate("picker") },
                    onOpenExercise = { nav.navigate("exercise/$it") },
                    // "Sačuvaj u dnevnik": Dnevnik sa danasnjim danom (novi trening je na vrhu).
                    onFinished = {
                        nav.popBackStack()
                        nav.navigate(Tab.JOURNAL.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onClose = { nav.popBackStack() },
                )
            }
            composable("workout_detail/{id}") { e ->
                WorkoutDetailScreen(
                    e.arguments!!.getString("id")!!,
                    onBack = { nav.popBackStack() },
                    onOpenExercise = { nav.navigate("exercise/$it") },
                )
            }
            composable(Tab.ROUTINES.route) {
                RoutinesScreen(
                    onOpen = { nav.navigate("routine/$it") },
                    onNew = { nav.navigate("routine/new") },
                    onNotion = { nav.navigate("notion_routines") },
                    onSettings = { nav.navigate("settings") },
                    onStart = { startWorkout(it) },
                    onOpenStats = { nav.navigate("stats") },
                    onNotionGuide = { nav.navigate("notion_help") },
                )
            }
            composable("routine/{id}") { e ->
                val id = e.arguments!!.getString("id")!!.takeIf { it != "new" }
                RoutineEditScreen(
                    routineId = id,
                    savedState = e.savedStateHandle,
                    onPickExercises = { nav.navigate("picker") },
                    onDone = { nav.popBackStack() },
                )
            }
            composable("notion_routines") {
                NotionRoutinesScreen(
                    onHelp = { nav.navigate("notion_help") },
                    onBack = { nav.popBackStack() },
                    onSettings = { nav.navigate("settings_notion") },
                    onOpenRoutine = { nav.navigate("routine/$it") },
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onNotion = { nav.navigate("settings_notion") },
                    onBackup = { nav.navigate("settings_backup") },
                    onGoals = { nav.navigate("settings_goals") },
                    onReminders = { nav.navigate("supp_reminders") },
                    onHelp = { nav.navigate("notion_help") },
                )
            }
            composable("settings_notion") {
                com.vukos.trening.ui.settings.NotionSettingsScreen(onBack = { nav.popBackStack() }, onHelp = { nav.navigate("notion_help") })
            }
            composable("settings_backup") { com.vukos.trening.ui.settings.BackupScreen(onBack = { nav.popBackStack() }) }
            composable("settings_goals") { com.vukos.trening.ui.settings.GoalsScreen(onBack = { nav.popBackStack() }) }
            composable(Tab.BODY.route) {
                BodyScreen(
                    onOpenDay = { nav.navigate("body_day/${it.toEpochDay()}") },
                    onNew = { openCheckIn() },
                    onOpenChart = { nav.navigate("body_chart") },
                    onSettings = { nav.navigate("settings") },
                    onCompare = { nav.navigate("body_compare") },
                )
            }
            composable("stats") { com.vukos.trening.ui.stats.StatsScreen(onBack = { nav.popBackStack() }) }
            composable("body_compare?after={after}", arguments = listOf(androidx.navigation.navArgument("after") { defaultValue = "" })) { e ->
                com.vukos.trening.ui.body.BodyCompareScreen(onBack = { nav.popBackStack() }, afterId = e.arguments?.getString("after")?.takeIf { it.isNotEmpty() })
            }
            composable("body_day/{day}") { e ->
                val day = LocalDate.ofEpochDay(e.arguments!!.getString("day")!!.toLong())
                BodyDayScreen(day, onBack = { nav.popBackStack() }, onEdit = { openCheckIn(day) })
            }
            composable("body_chart") {
                WeightChartScreen(
                    onBack = { nav.popBackStack() },
                    onOpenDay = { nav.navigate("body_day/${it.toEpochDay()}") },
                    onCompare = { nav.navigate("body_compare?after=$it") },
                )
            }
            composable("checkin/{day}") { e ->
                CheckInScreen(
                    LocalDate.ofEpochDay(e.arguments!!.getString("day")!!.toLong()),
                    onDone = { nav.popBackStack() },
                )
            }
            composable("exercise/{id}") { e ->
                ExerciseDetailScreen(
                    e.arguments?.getString("id")!!,
                    onBack = { nav.popBackStack() },
                    onOpenWorkout = { nav.navigate("workout_detail/$it") },
                )
            }
            composable(
                "exercise_new?name={name}&group={group}&num={num}",
                arguments = listOf(
                    androidx.navigation.navArgument("name") { defaultValue = "" },
                    androidx.navigation.navArgument("group") { defaultValue = "" },
                    androidx.navigation.navArgument("num") { defaultValue = 0; type = androidx.navigation.NavType.IntType },
                ),
            ) { e ->
                val args = e.arguments!!
                // Iz izbora vezbi: nova (ili postojeca "Izaberi") vezba se vraca izboru i odmah je izabrana.
                fun back(id: String?) {
                    if (id != null) nav.previousBackStackEntry?.savedStateHandle?.set("created", id)
                    nav.popBackStack()
                }
                NewExerciseScreen(
                    initialName = args.getString("name").orEmpty(),
                    muscleHint = args.getString("group")?.takeIf { it.isNotEmpty() }?.let { com.vukos.trening.data.MuscleGroup.valueOf(it) },
                    selectionNumber = args.getInt("num").takeIf { it > 0 },
                    onUse = { back(it) },
                    onDone = { back(it) },
                )
            }
            // Picker: vraca izabrane ID-jeve prethodnom ekranu preko savedStateHandle["picked"].
            composable("picker") { e ->
                val created by e.savedStateHandle.getStateFlow<String?>("created", null).collectAsStateWithLifecycle()
                com.vukos.trening.ui.exercises.ChooseExercisesScreen(
                    created = created,
                    onCreatedHandled = { e.savedStateHandle["created"] = null },
                    onNew = { name, group, num ->
                        nav.navigate("exercise_new?name=${android.net.Uri.encode(name)}&group=${group?.name ?: ""}&num=$num")
                    },
                    onConfirm = { ids ->
                        nav.previousBackStackEntry?.savedStateHandle?.set("picked", ArrayList(ids))
                        nav.popBackStack()
                    },
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
