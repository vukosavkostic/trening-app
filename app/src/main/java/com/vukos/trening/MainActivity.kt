package com.vukos.trening

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vukos.trening.ui.common.ComingSoon
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TreningTheme { AppRoot() } }
    }
}

@Composable
private fun AppRoot() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = Tab.entries.any { it.route == route }

    Scaffold(bottomBar = {
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
        NavHost(nav, startDestination = Tab.JOURNAL.route, modifier = Modifier.padding(pad)) {
            composable(Tab.JOURNAL.route) {
                ComingSoon("Dnevnik", "Kalendar treninga, ishrana po danima i uvidi.\nStiže u sledećim fazama.")
            }
            composable(Tab.ROUTINES.route) {
                ComingSoon("Planovi", "Rutine po danima u nedelji i \"Započni trening\".\nStiže u sledećoj fazi.")
            }
            composable(Tab.BODY.route) {
                ComingSoon("Telo", "Kilaža svake nedelje, slike na svake dve nedelje,\npodsetnici i grafikon.")
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
