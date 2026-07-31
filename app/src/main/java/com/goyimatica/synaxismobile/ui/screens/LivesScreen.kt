package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
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
import com.goyimatica.synaxismobile.ui.components.SectionLabel
import com.goyimatica.synaxismobile.ui.components.SynChip
import com.goyimatica.synaxismobile.ui.theme.Syn

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LivesScreen(onOpenSaint: (String) -> Unit) {
    val c = Syn.colors
    val library by Store.library.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var era by remember { mutableStateOf<String?>(null) }
    var jurisdiction by remember { mutableStateOf<String?>(null) }
    var tag by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    /* Built once from the data, so no filter can offer an empty result set. */
    val eras = remember { SaintsRepo.eras() }
    val jurisdictions = remember { SaintsRepo.jurisdictions() }
    val tags = remember { SaintsRepo.tags() }

    val results = remember(query, era, jurisdiction, tag) {
        SaintsRepo.filter(query = query, era = era, jurisdiction = jurisdiction, tag = tag)
    }
    val groups = remember(results) { SaintsRepo.grouped(results) }
    val active = listOfNotNull(era, jurisdiction, tag).size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item("header") {
            ScreenHeader(
                kicker = "The lives",
                title = "Lives of the Saints",
                subtitle = SaintsRepo.count.toString() +
                    " lives, from the Patriarchs to the saints of our own century.",
            )
            Spacer(Modifier.height(16.dp))
        }

        item("search") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surface)
                        .border(1.dp, c.rule, RoundedCornerShape(12.dp))
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Search, null, tint = c.faint, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                "Search a name, a place, an age",
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
                            Icons.Outlined.Close,
                            "Clear",
                            tint = c.dim,
                            modifier = Modifier.size(16.dp).clickable { query = "" },
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(43.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active > 0) c.gold else c.surface)
                        .border(1.dp, if (active > 0) c.gold else c.rule, RoundedCornerShape(12.dp))
                        .clickable { showFilters = !showFilters },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.FilterList,
                        "Filters",
                        tint = if (active > 0) c.bg else c.dim,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }

        if (showFilters) {
            item("f-era") {
                FilterRow("Era", eras, era) { era = if (era == it) null else it }
            }
            item("f-jur") {
                FilterRow("Church", jurisdictions, jurisdiction) {
                    jurisdiction = if (jurisdiction == it) null else it
                }
            }
            item("f-tag") {
                FilterRow("Kind", tags, tag) { tag = if (tag == it) null else it }
            }
        }

        item("count") {
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    when (results.size) {
                        SaintsRepo.count -> "All " + results.size + " lives"
                        1 -> "One life"
                        else -> results.size.toString() + " lives"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = c.faint,
                )
                if (active > 0 || query.isNotEmpty()) {
                    Text(
                        "Clear",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.goldDim,
                        modifier = Modifier.clickable {
                            query = ""; era = null; jurisdiction = null; tag = null
                        },
                    )
                }
            }
        }

        if (results.isEmpty()) {
            item("empty") { EmptyNote("No life answers to that.") }
        }

        groups.forEach { (letter, list) ->
            stickyHeader(key = "h-" + letter) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(c.bg)
                        .padding(top = 12.dp, bottom = 8.dp),
                ) {
                    Text(
                        letter.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = c.goldDim,
                    )
                }
            }
            items(list, key = { it.id }) { s ->
                SaintCard(
                    saint = s,
                    onClick = { onOpenSaint(s.id) },
                    bookmarked = library.isBookmarked(s.id),
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    label: String,
    options: List<String>,
    selected: String?,
    onPick: (String) -> Unit,
) {
    if (options.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        SectionLabel(label)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options, key = { it }) { option ->
                SynChip(
                    text = option,
                    selected = selected == option,
                    onClick = { onPick(option) },
                )
            }
        }
    }
}