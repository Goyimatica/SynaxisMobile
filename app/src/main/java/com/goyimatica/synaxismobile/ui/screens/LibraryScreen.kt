package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goyimatica.synaxismobile.data.Mark
import com.goyimatica.synaxismobile.data.Saint
import com.goyimatica.synaxismobile.data.SaintsRepo
import com.goyimatica.synaxismobile.data.Store
import com.goyimatica.synaxismobile.ui.components.ContinueCard
import com.goyimatica.synaxismobile.ui.components.EmptyNote
import com.goyimatica.synaxismobile.ui.components.SaintCard
import com.goyimatica.synaxismobile.ui.components.SectionLabel
import com.goyimatica.synaxismobile.ui.components.SynChip
import com.goyimatica.synaxismobile.ui.reader.markColor
import com.goyimatica.synaxismobile.ui.reader.summary
import com.goyimatica.synaxismobile.ui.theme.Syn

@Composable
fun LibraryScreen(onOpenSaint: (String) -> Unit, onOpenSettings: () -> Unit) {
    val c = Syn.colors
    val library by Store.library.collectAsStateWithLifecycle()
    var view by remember { mutableStateOf("Saved") }

    val saved = remember(library.bookmarks) {
        library.bookmarks.mapNotNull { SaintsRepo.byId(it) }
    }
    val recents = remember(library.recents) {
        library.recents.mapNotNull { r -> SaintsRepo.byId(r.id)?.let { s -> s to r } }
    }

    /* Walked over the index rather than over the store, so the marks come back
       already attached to the saint they belong to and already in the index's
       own order. Two hundred and thirty-eight lookups is nothing. */
    val marked: List<Pair<Saint, Mark>> = remember(library) {
        SaintsRepo.all().flatMap { s -> library.marksFor(s.id).map { m -> s to m } }
            .sortedByDescending { it.second.at }
    }
    val noted = remember(marked) { marked.filter { it.second.note.isNotBlank() } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item("header") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "LIBRARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.gold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Your library",
                        style = MaterialTheme.typography.headlineLarge,
                        color = c.text,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        saved.size.toString() + " saved · " + library.markCount +
                            " highlights · " + library.noteCount + " notes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.dim,
                    )
                }
                Icon(
                    Icons.Outlined.Settings,
                    "Settings",
                    tint = c.dim,
                    modifier = Modifier
                        .size(23.dp)
                        .clickable { onOpenSettings() },
                )
            }
            Spacer(Modifier.height(18.dp))
        }

        item("views") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Saved", "Highlights", "Notes", "Recent"), key = { it }) { name ->
                    SynChip(text = name, selected = view == name, onClick = { view = name })
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        when (view) {
            "Saved" -> {
                if (saved.isEmpty()) {
                    item("e1") { EmptyNote("Nothing saved yet.") }
                } else {
                    items(saved, key = { "b-" + it.id }) { s ->
                        SaintCard(saint = s, onClick = { onOpenSaint(s.id) }, bookmarked = true)
                    }
                }
            }

            "Highlights" -> {
                if (marked.isEmpty()) {
                    item("e2") { EmptyNote("No highlights yet. Press and hold a line as you read.") }
                } else {
                    items(marked, key = { "m-" + it.second.key }) { (saint, mark) ->
                        MarkRow(saint = saint, mark = mark, onClick = { onOpenSaint(saint.id) })
                    }
                }
            }

            "Notes" -> {
                if (noted.isEmpty()) {
                    item("e3") { EmptyNote("No notes yet.") }
                } else {
                    items(noted, key = { "n-" + it.second.key }) { (saint, mark) ->
                        MarkRow(saint = saint, mark = mark, onClick = { onOpenSaint(saint.id) })
                    }
                }
            }

            else -> {
                if (recents.isEmpty()) {
                    item("e4") { EmptyNote("Nothing read yet.") }
                } else {
                    item("clear") {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Text(
                                "Clear",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.goldDim,
                                modifier = Modifier.clickable { Store.clearRecents() },
                            )
                        }
                    }
                    items(recents, key = { "r-" + it.first.id }) { (saint, recent) ->
                        ContinueCard(
                            saint = saint,
                            progress = recent.progress,
                            onClick = { onOpenSaint(saint.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkRow(saint: Saint, mark: Mark, onClick: () -> Unit) {
    val c = Syn.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .clickable { onClick() }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(markColor(c, mark.color)),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                saint.name,
                style = MaterialTheme.typography.labelLarge,
                color = c.gold,
                modifier = Modifier.weight(1f),
            )
            Text(mark.summary(), style = MaterialTheme.typography.labelSmall, color = c.faint)
        }
        Spacer(Modifier.height(11.dp))
        Text(
            "“" + mark.text.trim() + "”",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = c.text,
        )
        if (mark.note.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.rule))
            Spacer(Modifier.height(12.dp))
            SectionLabel("Your note")
            Spacer(Modifier.height(6.dp))
            Text(mark.note, style = MaterialTheme.typography.bodyMedium, color = c.dim)
        }
    }
}