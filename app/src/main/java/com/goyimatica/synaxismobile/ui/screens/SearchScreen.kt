package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goyimatica.synaxismobile.data.SaintsRepo
import com.goyimatica.synaxismobile.data.Store
import com.goyimatica.synaxismobile.ui.components.EmptyNote
import com.goyimatica.synaxismobile.ui.components.SaintCard
import com.goyimatica.synaxismobile.ui.components.ScreenHeader
import com.goyimatica.synaxismobile.ui.theme.Syn

@Composable
fun SearchScreen(onOpenSaint: (String) -> Unit) {
    val c = Syn.colors
    val library by Store.library.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val results = remember(query) {
        if (query.isBlank()) emptyList() else SaintsRepo.search(query)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item("header") {
            ScreenHeader(
                kicker = "Search",
                title = "Find a saint",
                subtitle = "Names, epithets, places, centuries and titles are all searched.",
            )
            Spacer(Modifier.height(16.dp))
        }
        item("field") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.surface)
                    .border(1.dp, c.rule, RoundedCornerShape(12.dp))
                    .padding(horizontal = 13.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, null, tint = c.faint, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Nicholas, Athos, hieromartyr, Alaska…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.faint,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.text),
                        cursorBrush = SolidColor(c.gold),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Outlined.Close, "Clear", tint = c.dim,
                        modifier = Modifier.size(16.dp).clickable { query = "" },
                    )
                }
            }
        }

        if (query.isBlank()) {
            item("idle") { EmptyNote("Type a name.") }
        } else if (results.isEmpty()) {
            item("none") { EmptyNote("Nobody by that name here.") }
        } else {
            items(results, key = { it.id }) { s ->
                SaintCard(
                    saint = s,
                    onClick = { onOpenSaint(s.id) },
                    bookmarked = library.isBookmarked(s.id),
                )
            }
        }
    }
}