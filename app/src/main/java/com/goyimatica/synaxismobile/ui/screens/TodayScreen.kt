package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goyimatica.synaxismobile.core.Cal
import com.goyimatica.synaxismobile.data.QuotesRepo
import com.goyimatica.synaxismobile.data.SaintsRepo
import com.goyimatica.synaxismobile.data.Store
import com.goyimatica.synaxismobile.ui.components.ContinueCard
import com.goyimatica.synaxismobile.ui.components.FastCard
import com.goyimatica.synaxismobile.ui.components.FeastCard
import com.goyimatica.synaxismobile.ui.components.Pressable
import com.goyimatica.synaxismobile.ui.components.QuoteCard
import com.goyimatica.synaxismobile.ui.components.SaintCard
import com.goyimatica.synaxismobile.ui.components.ScreenHeader
import com.goyimatica.synaxismobile.ui.components.SectionLabel
import com.goyimatica.synaxismobile.ui.theme.Syn
import com.goyimatica.synaxismobile.ui.toCalStyle
import java.time.LocalDate

@Composable
fun TodayScreen(onOpenSaint: (String) -> Unit) {
    val c = Syn.colors
    val settings by Store.settings.collectAsStateWithLifecycle()
    val library by Store.library.collectAsStateWithLifecycle()
    val style = settings.toCalStyle()

    val today = remember { LocalDate.now() }
    val info = remember(today, style) { Cal.dayInfo(today, style) }

    /* The saying is seeded by the date, so it is the same all day - but tapping
       the card draws a different one, which does not disturb tomorrow's. */
    var quote by remember { mutableStateOf(QuotesRepo.today()) }

    val commemorated = remember(info.churchKey) { SaintsRepo.onFeast(info.churchKey) }

    val recents = remember(library.recents) {
        library.recents.mapNotNull { r -> SaintsRepo.byId(r.id)?.let { s -> s to r } }.take(6)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp, end = 18.dp, top = 26.dp, bottom = 34.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item("header") {
            ScreenHeader(
                kicker = "Synaxis",
                title = Cal.fmt(today),
                subtitle = Cal.churchLine(today, style),
            )
            Spacer(Modifier.height(6.dp))
        }

        quote?.let { q ->
            item("quote") {
                QuoteCard(quote = q, onAnother = { quote = QuotesRepo.another(q) })
            }
        }

        if (info.feasts.isNotEmpty()) {
            item("feasts-label") {
                Spacer(Modifier.height(6.dp))
                SectionLabel(if (info.feasts.size == 1) "Today the Church keeps" else "Today the Church keeps")
            }
            items(info.feasts, key = { it.name }) { f -> FeastCard(f) }
        }

        item("fast-label") {
            Spacer(Modifier.height(6.dp))
            SectionLabel("The fast")
        }
        item("fast") { FastCard(info.fast) }

        if (commemorated.isNotEmpty()) {
            item("saints-label") {
                Spacer(Modifier.height(6.dp))
                SectionLabel(
                    if (commemorated.size == 1) "One saint is commemorated"
                    else commemorated.size.toString() + " saints are commemorated",
                )
            }
            items(commemorated, key = { "c-" + it.id }) { s ->
                SaintCard(
                    saint = s,
                    onClick = { onOpenSaint(s.id) },
                    bookmarked = library.isBookmarked(s.id),
                )
            }
        }

        if (recents.isNotEmpty()) {
            item("recent-label") {
                Spacer(Modifier.height(6.dp))
                SectionLabel("Continue reading")
            }
            items(recents, key = { "r-" + it.first.id }) { (saint, recent) ->
                ContinueCard(
                    saint = saint,
                    progress = recent.progress,
                    onClick = { onOpenSaint(saint.id) },
                )
            }
        } else {
            item("start") {
                Spacer(Modifier.height(6.dp))
                val first = commemorated.firstOrNull() ?: SaintsRepo.all().firstOrNull()
                Pressable(
                    onClick = { first?.let { onOpenSaint(it.id) } },
                    modifier = Modifier.fillMaxWidth(),
                    background = c.raised,
                    contentPadding = 20,
                ) {
                    SectionLabel("Begin")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Start your journey through the saints — their lives, and what they taught.",
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.text,
                    )
                    if (first != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Open the life of " + first.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.goldDim,
                        )
                    }
                }
            }
        }

        item("foot") {
            Spacer(Modifier.height(18.dp))
            Column(Modifier.fillMaxWidth()) {
                Text(
                    SaintsRepo.count.toString() + " lives · " + Cal.count() +
                        " commemorations · Pascha " +
                        Cal.fmt(com.goyimatica.synaxismobile.core.Pascha.of(today.year)),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.faint,
                )
            }
        }
    }
}