package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goyimatica.synaxismobile.data.SaintsRepo
import com.goyimatica.synaxismobile.data.Store
import com.goyimatica.synaxismobile.ui.components.ContinueCard
import com.goyimatica.synaxismobile.ui.components.EmptyNote
import com.goyimatica.synaxismobile.ui.components.SaintCard
import com.goyimatica.synaxismobile.ui.components.ScreenHeader
import com.goyimatica.synaxismobile.ui.components.SectionLabel
import com.goyimatica.synaxismobile.ui.theme.Syn

/* Bookmarks and recents work now. Highlights and notes appear here in Part 4,
   once the reader exists to make them. */
@Composable
fun LibraryScreen(onOpenSaint: (String) -> Unit) {
    val c = Syn.colors
    val library by Store.library.collectAsStateWithLifecycle()

    val saved = remember(library.bookmarks) {
        library.bookmarks.mapNotNull { SaintsRepo.byId(it) }
    }
    val recents = remember(library.recents) {
        library.recents.mapNotNull { r -> SaintsRepo.byId(r.id)?.let { s -> s to r } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item("header") {
            ScreenHeader(
                kicker = "Library",
                title = "Your library",
                subtitle = saved.size.toString() + " saved · " +
                    library.markCount + " highlights · " + library.noteCount + " notes",
            )
            Spacer(Modifier.height(16.dp))
        }

        if (saved.isEmpty() && recents.isEmpty()) {
            item("empty") { EmptyNote("Nothing saved yet.") }
        }

        if (saved.isNotEmpty()) {
            item("saved-label") { SectionLabel("Saved") }
            items(saved, key = { "b-" + it.id }) { s ->
                SaintCard(saint = s, onClick = { onOpenSaint(s.id) }, bookmarked = true)
            }
        }

        if (recents.isNotEmpty()) {
            item("recent-label") {
                Spacer(Modifier.height(8.dp))
                SectionLabel("Recently read")
            }
            items(recents, key = { "r-" + it.first.id }) { (saint, recent) ->
                ContinueCard(
                    saint = saint,
                    progress = recent.progress,
                    onClick = { onOpenSaint(saint.id) },
                )
            }
        }

        item("soon") {
            Spacer(Modifier.height(16.dp))
            Text(
                "Highlights and notes gather here once you start marking up the lives.",
                style = MaterialTheme.typography.bodySmall,
                color = c.faint,
            )
        }
    }
}