package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goyimatica.synaxismobile.data.Doc
import com.goyimatica.synaxismobile.data.SaintsRepo
import com.goyimatica.synaxismobile.data.Store
import com.goyimatica.synaxismobile.data.WikiRepo
import com.goyimatica.synaxismobile.ui.components.EmptyNote
import com.goyimatica.synaxismobile.ui.components.Medallion
import com.goyimatica.synaxismobile.ui.components.SectionLabel
import com.goyimatica.synaxismobile.ui.theme.Syn

/* Part 4 replaces this whole file with the reader: selection, highlighting,
   notes, and the progress that ContinueCard is already drawing. */
@Composable
fun SaintScreen(saintId: String, onBack: () -> Unit) {
    val c = Syn.colors
    val library by Store.library.collectAsStateWithLifecycle()
    val saint = remember(saintId) { SaintsRepo.byId(saintId) }

    var doc by remember(saintId) { mutableStateOf<Doc?>(WikiRepo.cached(saintId)) }
    var loading by remember(saintId) { mutableStateOf(doc == null) }

    LaunchedEffect(saintId) {
        val s = saint ?: return@LaunchedEffect
        Store.touch(s.id)
        if (doc == null) {
            loading = true
            doc = WikiRepo.doc(s)
            loading = false
        }
    }

    if (saint == null) {
        EmptyNote("That life is not in the index.")
        return
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                Icons.Outlined.ArrowBack, "Back", tint = c.dim,
                modifier = Modifier.size(23.dp).clickable { onBack() },
            )
            Icon(
                if (library.isBookmarked(saint.id)) Icons.Filled.Bookmark
                else Icons.Outlined.BookmarkBorder,
                "Save",
                tint = if (library.isBookmarked(saint.id)) c.gold else c.dim,
                modifier = Modifier.size(22.dp).clickable { Store.toggleBookmark(saint.id) },
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Medallion(saint.initial, diameter = 56)
            Spacer(Modifier.height(16.dp))
            Text(saint.name, style = MaterialTheme.typography.displayMedium, color = c.text)
            if (saint.epithet.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(saint.epithet, style = MaterialTheme.typography.bodyMedium, color = c.dim)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                listOfNotNull(
                    saint.feastText().ifBlank { null },
                    saint.era.ifBlank { null },
                    saint.jurisdiction.ifBlank { null },
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = c.faint,
            )

            Spacer(Modifier.height(26.dp))
            SectionLabel("In brief")
            Spacer(Modifier.height(10.dp))
            val d = doc
            Text(
                when {
                    loading -> "Fetching the life…"
                    d == null || d.missing -> "No life has been downloaded for this saint yet."
                    else -> d.intro
                },
                style = MaterialTheme.typography.bodyLarge,
                color = c.text,
            )

            Spacer(Modifier.height(28.dp))
            Box(Modifier.fillMaxWidth()) {
                Text(
                    "The full life, the highlighter and the notes arrive in Part 4.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.faint,
                )
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}