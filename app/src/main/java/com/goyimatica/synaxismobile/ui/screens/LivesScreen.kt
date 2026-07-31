package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goyimatica.synaxismobile.data.Saint
import com.goyimatica.synaxismobile.data.SaintsRepo
import com.goyimatica.synaxismobile.data.Store
import com.goyimatica.synaxismobile.ui.components.EmptyNote
import com.goyimatica.synaxismobile.ui.components.SaintCard
import com.goyimatica.synaxismobile.ui.components.ScreenHeader
import com.goyimatica.synaxismobile.ui.components.SynChip
import com.goyimatica.synaxismobile.ui.theme.Syn

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LivesScreen(onOpenSaint: (String) -> Unit, onOpenSettings: () -> Unit) {
    val c = Syn.colors
    val library by Store.library.collectAsStateWithLifecycle()
    val settings by Store.settings.collectAsStateWithLifecycle()

    var era by remember { mutableStateOf<String?>(null) }
    var jurisdiction by remember { mutableStateOf<String?>(null) }

    val eras = remember { SaintsRepo.eras() }
    val jurisdictions = remember { SaintsRepo.jurisdictions() }

    val shown: List<Saint> = remember(era, jurisdiction, settings.showPending) {
        SaintsRepo
            .filter(era = era, jurisdiction = jurisdiction)
            .filter { settings.showPending || !it.pending }
    }
    val groups = remember(shown) { SaintsRepo.grouped(shown) }
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {

        item("head") {
            ScreenHeader(
                overline = "Lives",
                title = "The synaxarion",
                subtitle = shown.size.toString() + " of " + SaintsRepo.count + " lives",
                onSettings = onOpenSettings,
            )
        }

        item("eras") {
            Column {
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item("era-all") {
                        SynChip(
                            text = "All eras",
                            selected = era == null,
                            onClick = { era = null },
                        )
                    }
                    items(eras) { name ->
                        SynChip(
                            text = name,
                            selected = era == name,
                            onClick = { era = if (era == name) null else name },
                        )
                    }
                }
            }
        }

        item("jurisdictions") {
            Column {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item("j-all") {
                        SynChip(
                            text = "Everywhere",
                            selected = jurisdiction == null,
                            onClick = { jurisdiction = null },
                        )
                    }
                    items(jurisdictions) { name ->
                        SynChip(
                            text = name,
                            selected = jurisdiction == name,
                            onClick = {
                                jurisdiction = if (jurisdiction == name) null else name
                            },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        if (groups.isEmpty()) {
            item("empty") {
                EmptyNote("No lives match those filters.")
            }
        }

        groups.forEach { (letter, saints) ->
            stickyHeader(key = "h-" + letter) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(c.bg)
                        .padding(top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        letter.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = c.goldDim,
                    )
                    Spacer(Modifier.height(1.dp))
                }
            }
            items(saints, key = { it.id }) { saint ->
                SaintCard(
                    saint = saint,
                    onClick = { onOpenSaint(saint.id) },
                    bookmarked = library.isBookmarked(saint.id),
                )
            }
        }
    }
}