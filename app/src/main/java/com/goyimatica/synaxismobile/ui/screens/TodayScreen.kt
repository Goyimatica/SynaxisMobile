package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.goyimatica.synaxismobile.ui.components.Pressable
import com.goyimatica.synaxismobile.ui.components.QuoteCard
import com.goyimatica.synaxismobile.ui.components.SaintCard
import com.goyimatica.synaxismobile.ui.components.ScreenHeader
import com.goyimatica.synaxismobile.ui.components.SectionLabel
import com.goyimatica.synaxismobile.ui.theme.Syn
import com.goyimatica.synaxismobile.ui.toCalStyle
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun TodayScreen(
    onOpenSaint: (String) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val library by Store.library.collectAsStateWithLifecycle()
    val settings by Store.settings.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    /*
     * Midnight, without a clock and without a timer.
     *
     * The effect is keyed on the day it is showing. It sleeps exactly as long
     * as is left of that day, sets the new one, and is thereby re-keyed to
     * sleep through the next. A suspended coroutine is free, so the page turns
     * itself over at 00:00 whether you are watching it or not.
     */
    var day by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(day) {
        val untilMidnight = Duration
            .between(LocalDateTime.now(), day.plusDays(1).atStartOfDay())
            .toMillis()
            .coerceIn(1_000L, 26L * 60L * 60L * 1000L)
        delay(untilMidnight)
        day = LocalDate.now()
    }

    val facts = remember(day, settings.calendarStyle) {
        factsFor(day, settings.toCalStyle())
    }
    /* V14: the quote follows the user's reckoning - the church date of the
       civil day, which is what the saints and feasts below also follow. */
    val quote = remember(facts.churchDate) { QuotesRepo.forDay(facts.churchDate) }

    var copied by remember(day) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }

    val commemorated = remember(facts.churchKey, settings.showPending) {
        SaintsRepo.commemoratedFor(facts.churchKey, settings.showPending)
    }

    /*
     * V8: the article behind today's fast.
     *
     * The rule's own words first - "Dormition Fast", "Great Lent" - then the
     * season, then plain "Fasting", which every day of the year can answer to.
     * If the harvester has not been run yet all three miss, the row is not
     * drawn, and nothing else on the screen notices.
     */
    val fastTopic = remember(facts.rule.label, facts.season, SaintsRepo.count) {
        SaintsRepo.topicFor(facts.rule.label)
            ?: SaintsRepo.topicFor(facts.season)
            ?: SaintsRepo.topicFor("Fasting")
    }

    val continuing = remember(library.recents) {
        library.recents
            .sortedByDescending { it.at }
            .take(8)
            .mapNotNull { r -> SaintsRepo.byId(r.id)?.let { s -> s to r.progress } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 0.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        item("head") {
            ScreenHeader(
                overline = "Synaxis",
                title = "Today",
                subtitle = facts.civilLine,
                onSettings = onOpenSettings,
            )
        }

        /* The word for the day, above everything, turning over at midnight. */
        if (quote != null) {
            item("quote") {
                QuoteCard(
                    text = quote.text,
                    by = quote.by,
                    copied = copied,
                    onCopy = {
                        clipboard.setText(
                            AnnotatedString("\u201C" + quote.text.trim() + "\u201D\n\u2014 " + quote.by)
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

        if (fastTopic != null) {
            item("fast-read") {
                ReadAbout(
                    label = "Read about " + fastTopic.name,
                    onClick = { onOpenSaint(fastTopic.id) },
                )
            }
        }

        if (facts.feasts.isNotEmpty()) {
            item("feast-label") {
                Spacer(Modifier.height(4.dp))
                SectionLabel(if (facts.feasts.size == 1) "The feast" else "The feasts")
            }
            items(facts.feasts, key = { "f-" + it.name }) { feast ->
                /* V8: a feast is a door. If an article exists for it, the card
                   opens it; if not, it is the plain card it always was. */
                val topic = SaintsRepo.topicFor(feast.name)
                if (topic == null) {
                    FeastCard(feast = feast)
                } else {
                    Pressable(
                        onClick = { onOpenSaint(topic.id) },
                        modifier = Modifier.fillMaxWidth(),
                        down = 0.985f,
                    ) {
                        FeastCard(feast = feast)
                    }
                }
            }
        }

        item("saints-label") {
            Spacer(Modifier.height(4.dp))
            SectionLabel(
                if (commemorated.isEmpty()) "Commemorated today"
                else commemorated.size.toString() + " commemorated today"
            )
        }

        if (commemorated.isEmpty()) {
            item("saints-empty") {
                EmptyNote("No life in the index is appointed to this day.")
            }
        } else {
            items(commemorated, key = { "s-" + it.id }) { saint ->
                SaintCard(
                    saint = saint,
                    onClick = { onOpenSaint(saint.id) },
                    bookmarked = library.isBookmarked(saint.id),
                )
            }
        }

        if (continuing.isNotEmpty()) {
            item("continue-label") {
                Spacer(Modifier.height(4.dp))
                SectionLabel("Where you left off")
            }
            item("continue") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(continuing, key = { "c-" + it.first.id }) { pair ->
                        ContinueCard(
                            saint = pair.first,
                            progress = pair.second,
                            onClick = { onOpenSaint(pair.first.id) },
                        )
                    }
                }
            }
        }

        item("tail") {
            Spacer(Modifier.height(8.dp))
            Text(
                text = SaintsRepo.saintCount.toString() + " lives \u00B7 " +
                    SaintsRepo.subjects().size + " feasts and subjects \u00B7 " +
                    QuotesRepo.count + " sayings",
                style = MaterialTheme.typography.labelSmall,
                color = Syn.colors.faint,
            )
        }
    }
}

/** One quiet row that says a thing can be read, and opens it. */
@Composable
private fun ReadAbout(label: String, onClick: () -> Unit) {
    val c = Syn.colors
    Pressable(onClick = onClick, modifier = Modifier.fillMaxWidth(), down = 0.985f) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.surface)
                .border(1.dp, c.rule, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = c.text)
            Text("Read", style = MaterialTheme.typography.labelLarge, color = c.gold)
        }
    }
}