package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.goyimatica.synaxismobile.ui.components.EmptyNote
import com.goyimatica.synaxismobile.ui.components.FastCard
import com.goyimatica.synaxismobile.ui.components.FeastCard
import com.goyimatica.synaxismobile.ui.components.QuoteCard
import com.goyimatica.synaxismobile.ui.components.SaintCard
import com.goyimatica.synaxismobile.ui.components.ScreenHeader
import com.goyimatica.synaxismobile.ui.components.SectionLabel
import com.goyimatica.synaxismobile.ui.toCalStyle
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun TodayScreen(
    onOpenSaint: (String) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val settings by Store.settings.collectAsStateWithLifecycle()
    val library by Store.library.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    val today = remember { LocalDate.now() }
    val facts = remember(today, settings.calendarStyle) {
        factsFor(today, settings.toCalStyle())
    }

    /*  The saying of the day. Days since the epoch, modulo the number of
        sayings - which means it is the same for the whole day, the same on
        every screen, and changes by itself at midnight. Nothing can re-roll
        it, which is the point of a daily saying.  */
    val quote = remember(today, QuotesRepo.count) {
        val all = QuotesRepo.all()
        if (all.isEmpty()) null
        else all[(today.toEpochDay().mod(all.size.toLong())).toInt()]
    }

    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    val commemorated = remember(facts.churchKey, settings.showPending) {
        SaintsRepo.onFeast(facts.churchKey)
            .filter { settings.showPending || !it.pending }
    }

    val continuing = remember(library.recents) {
        library.recents.take(3).mapNotNull { r -> SaintsRepo.byId(r.id)?.let { it to r } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("head") {
            ScreenHeader(
                overline = "Today",
                title = dayName(today),
                subtitle = facts.civilLine + "  \u00B7  " + facts.churchLine,
                onSettings = onOpenSettings,
            )
            Spacer(Modifier.height(10.dp))
        }

        if (quote != null) {
            item("quote") {
                QuoteCard(
                    text = quote.text,
                    by = quote.by,
                    copied = copied,
                    onCopy = {
                        clipboard.setText(
                            AnnotatedString(
                                "\u201C" + quote.text.trim() + "\u201D\n\u2014 " + quote.by
                            )
                        )
                        copied = true
                    },
                )
            }
        }

        item("fast") {
            FastCard(
                civilLine = facts.civilLine,
                churchLine = facts.churchLine,
                season = facts.season,
                rule = facts.rule,
                onClick = onOpenCalendar,
            )
        }

        if (facts.feasts.isNotEmpty()) {
            item("f-label") {
                Spacer(Modifier.height(6.dp))
                SectionLabel(if (facts.feasts.size == 1) "The feast" else "The feasts")
            }
            items(facts.feasts, key = { "f-" + it.name }) { feast ->
                FeastCard(feast)
            }
        }

        item("c-label") {
            Spacer(Modifier.height(6.dp))
            SectionLabel("Commemorated today")
        }
        if (commemorated.isEmpty()) {
            item("c-empty") {
                EmptyNote(
                    "No one in the index is commemorated on this day. " +
                        "Open the calendar to look either side of it."
                )
            }
        } else {
            items(commemorated, key = { "s-" + it.id }) { saint ->
                SaintCard(
                    saint = saint,
                    onClick = { onOpenSaint(saint.id) },
                    bookmarked = library.isBookmarked(saint.id),
                    overline = if (saint.pending) "Not yet glorified" else null,
                )
            }
        }

        if (continuing.isNotEmpty()) {
            item("r-label") {
                Spacer(Modifier.height(6.dp))
                SectionLabel("Where you left off")
            }
            items(continuing, key = { "r-" + it.first.id }) { (saint, recent) ->
                ContinueCard(
                    saint = saint,
                    progress = recent.progress,
                    onClick = { onOpenSaint(saint.id) },
                )
            }
        } else {
            item("r-empty") {
                Spacer(Modifier.height(6.dp))
                SectionLabel("Begin")
                Spacer(Modifier.height(10.dp))
                EmptyNote(
                    "Nothing read yet. Start with whoever is commemorated today \u2014 " +
                        "that is how the Church itself reads."
                )
            }
        }
    }
}