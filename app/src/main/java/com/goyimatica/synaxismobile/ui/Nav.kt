package com.goyimatica.synaxismobile.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

data class Tab(val route: String, val label: String, val icon: ImageVector?)

object Routes {
    const val TODAY = "today"
    const val LIVES = "lives"
    const val CALENDAR = "calendar"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val SAINT = "saint/{id}"

    fun saint(id: String) = "saint/" + id

    /** Today's icon is null on purpose - the shell draws the cross there. */
    val TABS = listOf(
        Tab(TODAY, "Today", null),
        Tab(LIVES, "Lives", Icons.Outlined.MenuBook),
        Tab(CALENDAR, "Calendar", Icons.Outlined.CalendarMonth),
        Tab(SEARCH, "Search", Icons.Outlined.Search),
        Tab(LIBRARY, "Library", Icons.Outlined.BookmarkBorder),
    )
}