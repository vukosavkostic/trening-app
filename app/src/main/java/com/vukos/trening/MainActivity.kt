package com.vukos.trening

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
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.vukos.trening.ui.common.ComingSoon
import com.vukos.trening.ui.routines.NotionRoutinesScreen
import com.vukos.trening.ui.routines.RoutineEditScreen
import com.vukos.trening.ui.routines.RoutinesScreen
import com.vukos.trening.ui.settings.SettingsScreen
import java.time.LocalDate
import com.vukos.trening.ui.exercises.ExerciseDetailScreen
import com.vukos.trening.ui.exercises.ExerciseListScreen
import com.vukos.trening.ui.exercises.NewExerciseScreen
import com.vukos.trening.ui.theme.TreningTheme

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    JOURNAL("journal", "Dnevnik", Icons.Filled.MenuBook),
    ROUTINES("routines", "Planovi", Icons.Filled.ViewList),
    EXERCISES("exercises", "Vežbe", Icons.Filled.FitnessCenter),
    BODY("body", "Telo", Icons.Filled.MonitorWeight),
}

class MainActivity : ComponentActivity() {
    /** Notifikacija moze traziti otvaranje ekrana za merenje. */
    private val openRequest = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openRequest.value = intent?.getStringExtra(Reminders.EXTRA_OPEN)
        setContent { TreningTheme { AppRoot(openRequest.value) { openRequest.value = null } } }
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
    fun openCheckIn(day: LocalDate = LocalDate.now()) = nav.navigate("checkin/${day.toEpochDay()}")

    // Dozvola za notifikacije (Android 13+), trazi se jednom pri pokretanju.
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(openRequest) {
        if (openRequest == Reminders.OPEN_CHECKIN) { openCheckIn(); onOpenHandled() }
    }

    Scaffold(topBar = {
        // Crvena / narandzasta traka za merenje, na svim glavnim tabovima.
        if (showBar) Column { CheckInBanner(due) { openCheckIn() } }
    }, bottomBar = {
        if (showBar) NavigationBar {
            Tab.entries.forEach { tab ->
                NavigationBarItem(
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
        NavHost(nav, startDestination = Tab.JOURNAL.route, modifier = Modifier.padding(pad).consumeWindowInsets(pad)) {
            composable(Tab.JOURNAL.route) {
                ComingSoon("Dnevnik", "Kalendar treninga, ishrana po danima i uvidi.\nStiže u sledećim fazama.")
            }
            composable(Tab.ROUTINES.route) {
                RoutinesScreen(
                    onOpen = { nav.navigate("routine/$it") },
                    onNew = { nav.navigate("routine/new") },
                    onNotion = { nav.navigate("notion_routines") },
                    onSettings = { nav.navigate("settings") },
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
                    onBack = { nav.popBackStack() },
                    onSettings = { nav.navigate("settings") },
                    onOpenRoutine = { nav.navigate("routine/$it") },
                )
            }
            composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
            composable(Tab.BODY.route) {
                BodyScreen(
                    onOpenDay = { nav.navigate("body_day/${it.toEpochDay()}") },
                    onNew = { openCheckIn() },
                    onOpenChart = { nav.navigate("body_chart") },
                )
            }
            composable("body_day/{day}") { e ->
                val day = LocalDate.ofEpochDay(e.arguments!!.getString("day")!!.toLong())
                BodyDayScreen(day, onBack = { nav.popBackStack() }, onEdit = { openCheckIn(day) })
            }
            composable("body_chart") {
                WeightChartScreen(
                    onBack = { nav.popBackStack() },
                    onOpenDay = { nav.navigate("body_day/${it.toEpochDay()}") },
                )
            }
            composable("checkin/{day}") { e ->
                CheckInScreen(
                    LocalDate.ofEpochDay(e.arguments!!.getString("day")!!.toLong()),
                    onDone = { nav.popBackStack() },
                )
            }
            composable(Tab.EXERCISES.route) {
                ExerciseListScreen(
                    selectionMode = false,
                    onOpen = { nav.navigate("exercise/$it") },
                    onNew = { nav.navigate("exercise_new") },
                )
            }
            composable("exercise/{id}") { e ->
                ExerciseDetailScreen(e.arguments?.getString("id")!!, onBack = { nav.popBackStack() })
            }
            composable("exercise_new") {
                NewExerciseScreen(onDone = { nav.popBackStack() })
            }
            // Picker: vraca izabrane ID-jeve prethodnom ekranu preko savedStateHandle["picked"].
            composable("picker") {
                ExerciseListScreen(
                    selectionMode = true,
                    onOpen = {},
                    onNew = { nav.navigate("exercise_new") },
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
