package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goyimatica.synaxismobile.data.QuotesRepo
import com.goyimatica.synaxismobile.data.SaintsRepo
import com.goyimatica.synaxismobile.data.Store
import com.goyimatica.synaxismobile.ui.components.ContinueCard
import com.goyimatica.synaxismobile.ui.components.FastCard
import com.goyimatica.synaxismobile.ui.components.FeastCard
import com.goyimatica.synaxismobile.ui.components.QuoteCard
import com.goyimatica.synaxismobile.ui.components.SaintCard
import com.goyimatica.synaxismobile.ui.components.ScreenHeader
import com.goyimatica.synaxismobile.ui.components.SectionLabel
import com.goyimatica.synaxismobile.ui.components.StatTile
import com.goyimatica.synaxismobile.ui.theme.Syn
import com.goyimatica.synaxismobile.ui.toCalStyle
import kotlinx.coroutines.delay
import java.time.LocalDate

/*
 * The homepage.
 *
 * One LazyColumn, one rhythm: a gold SectionLabel names each band, the cards
 * beneath it are all the same corner radius and the same border, and every
 * title is Cormorant while every caption is Inter. Sections that have nothing
 * in them are not drawn at all rather than drawn empty - a day with no great
 * feast should not have a heading announcing that there is no great feast.
 */
@Composable
fun TodayScreen(
    onOpenSaint: (String) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = Syn.colors
    val clipboard = LocalClipboardManager.current

    val library by Store.library.collectAsStateWithLifecycle()
    val settings by Store.settings.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    val facts = remember(today, settings.calendarStyle) {
        factsFor(today, settings.toCalStyle())
    }
    val quote = remember(today) { QuotesRepo.today() }

    val commemorated = remember(facts.churchKey, settings.showPending) {
        SaintsRepo.onFeast(facts.churchKey)
            .filter { settings.showPending || !it.pending }
    }

    val continuing = remember(library.recents) {
        library.recents
            .sortedByDescending { it.at }
            .filter { it.progress > 0.02f && it.progress < 0.985f }
            .take(8)
            .mapNotNull { r -> SaintsRepo.byId(r.id)?.let { s -> Pair(s, r.progress) } }
    }

    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        item(key = "header") {
            ScreenHeader(
                overline = "Synaxis",
                title = "Today",
                onSettings = onOpenSettings,
            )
        }

        /* ---- the day itself ---------------------------------------------- */

        item(key = "fast") {
            FastCard(
                civilLine = facts.civilLine,
                churchLine = facts.churchLine,
                season = facts.season,
                rule = facts.rule,
                onClick = onOpenCalendar,
            )
        }

        /* ---- feasts ------------------------------------------------------- */

        if (facts.feasts.isNotEmpty()) {
            item(key = "feasts-label") {
                Band(if (facts.feasts.size == 1) "Feast" else "Feasts")
            }
            items(
                count = facts.feasts.size,
                key = { i -> "feast-" + i },
            ) { i ->
                FeastCard(feast = facts.feasts[i])
            }
        }

        /* ---- the synaxarion of the day ------------------------------------ */

        if (commemorated.isNotEmpty()) {
            item(key = "saints-label") {
                Band("Commemorated today", trailing = commemorated.size.toString())
            }
            items(
                count = minOf(commemorated.size, 8),
                key = { i -> "saint-" + commemorated[i].id },
            ) { i ->
                val saint = commemorated[i]
                SaintCard(
                    saint = saint,
                    onClick = { onOpenSaint(saint.id) },
                    bookmarked = library.isBookmarked(saint.id),
                )
            }
        }

        /* ---- the word ------------------------------------------------------ */

        if (quote != null) {
            item(key = "quote-label") { Spacer(Modifier.height(2.dp)) }
            item(key = "quote") {
                QuoteCard(
                    text = quote.text,
                    by = quote.by,
                    copied = copied,
                    onCopy = {
                        clipboard.setText(
                            AnnotatedString(quote.text + "\n\n\u2014 " + quote.by)
                        )
                        copied = true
                    },
                )
            }
        }

        /* ---- unfinished lives ---------------------------------------------- */

        if (continuing.isNotEmpty()) {
            item(key = "continue-label") { Band("Continue reading") }
            item(key = "continue") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        count = continuing.size,
                        key = { i -> "cont-" + continuing[i].first.id },
                    ) { i ->
                        val (saint, progress) = continuing[i]
                        ContinueCard(
                            saint = saint,
                            progress = progress,
                            onClick = { onOpenSaint(saint.id) },
                        )
                    }
                }
            }
        }

        /* ---- the shelf ------------------------------------------------------ */

        item(key = "stats-label") { Band("The shelf") }
        item(key = "stats") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile(
                    value = SaintsRepo.count.toString(),
                    label = "Lives",
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = library.bookmarks.size.toString(),
                    label = "Bookmarks",
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = library.recents.size.toString(),
                    label = "Opened",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "tail") { Spacer(Modifier.height(70.dp)) }
    }
}

/* A band heading: the gold label on the left, an optional count on the right.
   Every section on this screen is introduced by one, which is most of what
   makes the page feel arranged rather than stacked. */
@Composable
private fun Band(text: String, trailing: String? = null) {
    val c = Syn.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, top = 10.dp, bottom = 2.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        SectionLabel(text)
        Spacer(Modifier.weight(1f))
        if (!trailing.isNullOrBlank()) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = c.faint,
            )
        }
    }
}