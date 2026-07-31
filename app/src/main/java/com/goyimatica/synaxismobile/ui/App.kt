package com.goyimatica.synaxismobile.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.goyimatica.synaxismobile.ui.components.HairRule
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
    val context = LocalContext.current
    val settings by Store.settings.collectAsStateWithLifecycle()
    var ready by remember { mutableStateOf(false) }

    /* Everything the app needs before it can draw a single screen. Store first,
       because the theme is read from it - starting in the wrong palette and
       flipping a frame later is worse than waiting a moment. */
    LaunchedEffect(Unit) {
        WikiRepo.init(context)
        Store.init(context)
        SaintsRepo.load(context)
        QuotesRepo.load(context)
        ready = true
    }

    SynaxisTheme(
        palette = settings.toPalette(),
        reading = settings.toReading(),
    ) {
        if (ready) Shell() else Splash()
    }
}

@Composable
private fun Splash() {
    val c = Syn.colors
    val lively = Syn.reading.animations > 0f
    val breath = rememberInfiniteTransition(label = "splash")
    val alpha by breath.animateFloat(
        initialValue = if (lively) 0.45f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )

    Box(
        Modifier.fillMaxSize().background(c.bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OrthodoxCross(
                modifier = Modifier.size(46.dp, 72.dp).alpha(alpha),
                color = c.gold,
            )
            Spacer(Modifier.height(22.dp))
            Text("SYNAXIS", style = MaterialTheme.typography.labelMedium, color = c.goldDim)
        }
    }
}

@Composable
private fun Shell() {
    val c = Syn.colors
    val lively = Syn.reading.animations > 0f
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val onTab = TABS.any { it.route == route }

    val open: (String) -> Unit = { id -> nav.navigate(Routes.saint(id)) }

    Scaffold(
        containerColor = c.bg,
        bottomBar = {
            if (onTab) {
                BottomBar(current = route) { target ->
                    if (target != route) {
                        nav.navigate(target) {
                            /* Tabs do not stack. Coming back to Today from four
                               tabs deep should leave, not walk backwards. */
                            popUpTo(Routes.TODAY) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        },
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(pad),
            enterTransition = { fadeIn(tween(if (lively) 170 else 0)) },
            exitTransition = { fadeOut(tween(if (lively) 130 else 0)) },
            popEnterTransition = { fadeIn(tween(if (lively) 170 else 0)) },
            popExitTransition = { fadeOut(tween(if (lively) 130 else 0)) },
        ) {
            composable(Routes.TODAY) { TodayScreen(onOpenSaint = open) }
            composable(Routes.LIVES) { LivesScreen(onOpenSaint = open) }
            composable(Routes.CALENDAR) { CalendarScreen(onOpenSaint = open) }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onOpenSaint = open,
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.SEARCH) { SearchScreen(onOpenSaint = open) }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { nav.popBackStack() })
            }
            composable(
                route = Routes.SAINT,
                arguments = listOf(navArgument(Routes.SAINT_ARG) { type = NavType.StringType }),
            ) { backStack ->
                val id = Routes.decode(backStack.arguments?.getString(Routes.SAINT_ARG))
                SaintScreen(saintId = id, onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun BottomBar(current: String?, onSelect: (String) -> Unit) {
    val c = Syn.colors
    Column(Modifier.fillMaxWidth().background(c.surface)) {
        HairRule()
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(60.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TABS.forEach { tab ->
                val selected = current == tab.route
                val tint = if (selected) c.gold else c.faint
                val interaction = remember { MutableInteractionSource() }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = { onSelect(tab.route) },
                        )
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (tab.icon == null) {
                        OrthodoxCross(Modifier.size(14.dp, 21.dp), tint)
                    } else {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                    )
                }
            }
        }
    }
}