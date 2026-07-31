package com.goyimatica.synaxismobile.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.goyimatica.synaxismobile.data.QuotesRepo
import com.goyimatica.synaxismobile.data.SaintsRepo
import com.goyimatica.synaxismobile.data.Store
import com.goyimatica.synaxismobile.data.WikiRepo
import com.goyimatica.synaxismobile.ui.components.OrthodoxCross
import com.goyimatica.synaxismobile.ui.screens.CalendarScreen
import com.goyimatica.synaxismobile.ui.screens.LibraryScreen
import com.goyimatica.synaxismobile.ui.screens.LivesScreen
import com.goyimatica.synaxismobile.ui.screens.SaintScreen
import com.goyimatica.synaxismobile.ui.screens.SearchScreen
import com.goyimatica.synaxismobile.ui.screens.SettingsScreen
import com.goyimatica.synaxismobile.ui.screens.TodayScreen
import com.goyimatica.synaxismobile.ui.theme.Syn
import com.goyimatica.synaxismobile.ui.theme.SynaxisTheme

@Composable
fun SynaxisApp() {
    val settings by Store.settings.collectAsStateWithLifecycle()

    SynaxisTheme(palette = settings.toPalette(), reading = settings.toReading()) {
        val context = LocalContext.current
        var ready by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            WikiRepo.init(context)
            Store.init(context)
            SaintsRepo.load(context)
            QuotesRepo.load(context)
            ready = true
        }

        Surface(Modifier.fillMaxSize(), color = Syn.colors.bg) {
            if (ready) Shell() else Splash()
        }
    }
}

@Composable
private fun Splash() {
    val c = Syn.colors
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OrthodoxCross(size = 56.dp)
            Spacer(Modifier.height(20.dp))
            Text("SYNAXIS", style = MaterialTheme.typography.labelLarge, color = c.goldDim)
        }
    }
}

@Composable
private fun Shell() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route ?: Routes.TODAY
    val onTab = Routes.TABS.any { it.route == current }

    val openSaint: (String) -> Unit = { id -> nav.navigate(Routes.saint(id)) }
    val openSettings: () -> Unit = { nav.navigate(Routes.SETTINGS) }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            NavHost(
                navController = nav,
                startDestination = Routes.TODAY,
                enterTransition = { fadeIn(tween(180)) },
                exitTransition = { fadeOut(tween(140)) },
            ) {
                composable(Routes.TODAY) {
                    TodayScreen(
                        onOpenSaint = openSaint,
                        onOpenCalendar = { nav.navigate(Routes.CALENDAR) },
                        onOpenSettings = openSettings,
                    )
                }
                composable(Routes.LIVES) {
                    LivesScreen(onOpenSaint = openSaint, onOpenSettings = openSettings)
                }
                composable(Routes.CALENDAR) {
                    CalendarScreen(onOpenSaint = openSaint, onOpenSettings = openSettings)
                }
                composable(Routes.SEARCH) {
                    SearchScreen(onOpenSaint = openSaint, onOpenSettings = openSettings)
                }
                composable(Routes.LIBRARY) {
                    LibraryScreen(onOpenSaint = openSaint, onOpenSettings = openSettings)
                }

                composable(
                    route = Routes.SAINT,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    enterTransition = {
                        slideInHorizontally(tween(260)) { it / 6 } + fadeIn(tween(200))
                    },
                    popExitTransition = {
                        slideOutHorizontally(tween(220)) { it / 6 } + fadeOut(tween(160))
                    },
                ) { backStack ->
                    SaintScreen(
                        saintId = backStack.arguments?.getString("id").orEmpty(),
                        onBack = { nav.popBackStack() },
                    )
                }

                composable(
                    route = Routes.SETTINGS,
                    enterTransition = {
                        slideInHorizontally(tween(260)) { it / 6 } + fadeIn(tween(200))
                    },
                    popExitTransition = {
                        slideOutHorizontally(tween(220)) { it / 6 } + fadeOut(tween(160))
                    },
                ) {
                    SettingsScreen(onBack = { nav.popBackStack() })
                }
            }
        }

        if (onTab) {
            BottomBar(current = current, onSelect = { route ->
                if (route != current) {
                    nav.navigate(route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            })
        }
    }
}

@Composable
private fun BottomBar(current: String, onSelect: (String) -> Unit) {
    val c = Syn.colors

    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.rule))
        Row(
            Modifier
                .fillMaxWidth()
                .background(c.surface)
                .navigationBarsPadding()
                .padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Routes.TABS.forEach { tab ->
                val chosen = current == tab.route
                val press = rememberInteraction()
                val tint by animColor(if (chosen) c.gold else c.faint)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .pressScale(press, down = 0.9f)
                        .clickable(
                            interactionSource = press,
                            indication = null,
                        ) { onSelect(tab.route) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    if (tab.icon == null) {
                        OrthodoxCross(size = 21.dp, color = tint)
                    } else {
                        Icon(tab.icon, tab.label, tint = tint, modifier = Modifier.size(21.dp))
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(tab.label, style = MaterialTheme.typography.labelSmall, color = tint)
                }
            }
        }
    }
}