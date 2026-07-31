package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goyimatica.synaxismobile.data.Saint
import com.goyimatica.synaxismobile.data.SaintsRepo
import com.goyimatica.synaxismobile.data.Store
import com.goyimatica.synaxismobile.ui.components.SaintCard
import com.goyimatica.synaxismobile.ui.components.ScreenHeader
import com.goyimatica.synaxismobile.ui.components.SynChip
import com.goyimatica.synaxismobile.ui.theme.Syn

private const val ALL = "All"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LivesScreen(onOpenSaint: (String) -> Unit, onOpenSettings: () -> Unit) {
    val c = Syn.colors
    val settings by Store.settings.collectAsStateWithLifecycle()
    val library by Store.library.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var era by remember { mutableStateOf(ALL) }
    var jurisdiction by remember { mutableStateOf(ALL) }

    val everyone = remember { SaintsRepo.all() }

    val eras = remember(everyone) {
        listOf(ALL) + everyone.map { it.era }.filter { it.isNotBlank() }.distinct()
    }
    val jurisdictions = remember(everyone) {
        listOf(ALL) + everyone.map { it.jurisdiction }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val shown: List<Saint> = remember(query, era, jurisdiction, settings.showPending, everyone) {
        val q = query.trim()
        everyone.asSequence()
            .filter { settings.showPending || !it.pending }
            .filter { era == ALL || it.era == era }
            .filter { jurisdiction == ALL || it.jurisdiction == jurisdiction }
            .filter { q.isBlank() || it.haystack.contains(q, ignoreCase = true) }
            .toList()
    }

    /*  Grouped by first letter when we are browsing, ungrouped when searching -
        headings over a list of four results are noise.  */
    val grouped = remember(shown, query) {
        if (query.isNotBlank()) emptyMap()
        else shown.groupBy { it.initial }.toSortedMap()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item("head") {
            ScreenHeader(
                overline = "Lives",
                title = "The saints",
                subtitle = shown.size.toString() + " of " + SaintsRepo.count,
                onSettings = onOpenSettings,
            )
            Spacer(Modifier.height(16.dp))
        }

        item("search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("Search a name, a place, an age", color = c.faint) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = c.faint) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Outlined.Close, "Clear", tint = c.faint,
                            modifier = Modifier.padding(4.dp).let { m ->
                                m.then(Modifier)
                            },
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.goldDim,
                    unfocusedBorderColor = c.rule,
                    focusedTextColor = c.text,
                    unfocusedTextColor = c.text,
                    cursorColor = c.gold,
                ),
            )
            Spacer(Modifier.height(4.dp))
        }

        item("eras") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(eras, key = { "e-" + it }) { name ->
                    SynChip(text = name, selected = era == name, onClick = {
                        era = if (era == name) ALL else name
                    })
                }
            }
        }

        item("juris") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(jurisdictions, key = { "j-" + it }) { name ->
                    SynChip(text = name, selected = jurisdiction == name, onClick = {
                        jurisdiction = if (jurisdiction == name) ALL else name
                    })
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        if (shown.isEmpty()) {
            item("empty") {
                com.goyimatica.synaxismobile.ui.components.EmptyNote(
                    "No one in the index answers to that."
                )
            }
        } else if (grouped.isEmpty()) {
            items(shown, key = { it.id }) { s ->
                SaintCard(
                    saint = s,
                    onClick = { onOpenSaint(s.id) },
                    bookmarked = library.isBookmarked(s.id),
                )
            }
        } else {
            grouped.forEach { (letter, people) ->
                stickyHeader("h-" + letter) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(c.bg)
                            .padding(vertical = 8.dp),
                    ) {
                        Text(
                            letter,
                            style = MaterialTheme.typography.labelLarge,
                            color = c.goldDim,
                        )
                    }
                }
                items(people, key = { it.id }) { s ->
                    SaintCard(
                        saint = s,
                        onClick = { onOpenSaint(s.id) },
                        bookmarked = library.isBookmarked(s.id),
                    )
                }
            }
        }
    }
}