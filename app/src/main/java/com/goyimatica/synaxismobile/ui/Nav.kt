package com.goyimatica.synaxismobile.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val TODAY = "today"
    const val LIVES = "lives"
    const val CALENDAR = "calendar"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val SETTINGS = "settings"

    const val SAINT_ARG = "id"
    const val SAINT = "saint/{id}"

    /* A handful of ids carry characters a route would swallow, so they are
       encoded going in and decoded coming out. */
    fun saint(id: String): String = "saint/" + URLEncoder.encode(id, "UTF-8")

    fun decode(raw: String?): String =
        if (raw.isNullOrBlank()) "" else URLDecoder.decode(raw, "UTF-8")
}

/** A tab. A null icon means the Russian cross, which is drawn rather than
 *  loaded - no Material glyph is the right shape. */
data class Tab(val route: String, val label: String, val icon: ImageVector?)

val TABS: List<Tab> = listOf(
    Tab(Routes.TODAY, "Today", null),
    Tab(Routes.LIVES, "Lives", Icons.Outlined.MenuBook),
    Tab(Routes.CALENDAR, "Calendar", Icons.Outlined.CalendarMonth),
    Tab(Routes.LIBRARY, "Library", Icons.Outlined.BookmarkBorder),
    Tab(Routes.SEARCH, "Search", Icons.Outlined.Search),
)