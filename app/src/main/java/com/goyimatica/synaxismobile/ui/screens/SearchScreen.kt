package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goyimatica.synaxismobile.data.SaintsRepo
import com.goyimatica.synaxismobile.data.Store
import com.goyimatica.synaxismobile.ui.components.EmptyNote
import com.goyimatica.synaxismobile.ui.components.SaintCard
import com.goyimatica.synaxismobile.ui.components.ScreenHeader
import com.goyimatica.synaxismobile.ui.components.SynChip
import com.goyimatica.synaxismobile.ui.pressScale
import com.goyimatica.synaxismobile.ui.rememberInteraction
import com.goyimatica.synaxismobile.ui.theme.Syn
import androidx.compose.foundation.clickable

@Composable
fun SearchScreen(onOpenSaint: (String) -> Unit, onOpenSettings: () -> Unit) {
    val c = Syn.colors
    val library by Store.library.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf<String?>(null) }

    val tags = remember { SaintsRepo.tags() }

    val results = remember(query, tag) {
        val base = if (query.isBlank()) {
            if (tag == null) emptyList() else SaintsRepo.filterAll(tag = tag)
        } else {
            SaintsRepo.search(query, limit = 80)
        }
        if (tag == null) base else base.filter { it.tags.contains(tag) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {

        item("head") {
            ScreenHeader(
                overline = "Search",
                title = "Find a life",
                subtitle = "By name, epithet, century or calling",
                onSettings = onOpenSettings,
            )
            Spacer(Modifier.height(14.dp))
        }

        item("field") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(c.surface)
                    .padding(horizontal = 15.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Search, "Search", tint = c.faint,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.height(1.dp))
                Box(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    if (query.isEmpty()) {
                        Text(
                            "Seraphim, martyr, 4th\u2026",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.faint,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        cursorBrush = SolidColor(c.gold),
                        textStyle = LocalTextStyle.current.merge(
                            MaterialTheme.typography.bodyMedium
                        ).copy(color = c.text),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    val press = rememberInteraction()
                    Icon(
                        Icons.Outlined.Close, "Clear", tint = c.dim,
                        modifier = Modifier
                            .size(18.dp)
                            .pressScale(press, down = 0.85f)
                            .clickable(
                                interactionSource = press,
                                indication = null,
                            ) { query = "" },
                    )
                }
            }
        }

        item("tags") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item("tag-all") {
                    SynChip(text = "Any calling", selected = tag == null, onClick = { tag = null })
                }
                items(tags) { name ->
                    SynChip(
                        text = name,
                        selected = tag == name,
                        onClick = { tag = if (tag == name) null else name },
                    )
                }
            }
        }

        if (results.isEmpty()) {
            item("empty") {
                Spacer(Modifier.height(10.dp))
                EmptyNote(
                    if (query.isBlank() && tag == null)
                        "Type a name, or choose a calling."
                    else
                        "Nothing matches that."
                )
            }
        } else {
            item("count") {
                Text(
                    results.size.toString() + (if (results.size == 1) " life" else " lives"),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.faint,
                )
            }
            items(results, key = { it.id }) { saint ->
                SaintCard(
                    saint = saint,
                    onClick = { onOpenSaint(saint.id) },
                    bookmarked = library.isBookmarked(saint.id),
                )
            }
        }
    }
}