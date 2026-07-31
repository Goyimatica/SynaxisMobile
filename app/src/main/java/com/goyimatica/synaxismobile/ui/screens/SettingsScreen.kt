package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goyimatica.synaxismobile.core.Cal
import com.goyimatica.synaxismobile.data.QuotesRepo
import com.goyimatica.synaxismobile.data.SaintsRepo
import com.goyimatica.synaxismobile.data.Store
import com.goyimatica.synaxismobile.data.WikiRepo
import com.goyimatica.synaxismobile.ui.CALENDAR_NAMES
import com.goyimatica.synaxismobile.ui.FACE_NAMES
import com.goyimatica.synaxismobile.ui.LEAD_NAMES
import com.goyimatica.synaxismobile.ui.PALETTE_NAMES
import com.goyimatica.synaxismobile.ui.SIZE_NAMES
import com.goyimatica.synaxismobile.ui.WEIGHT_NAMES
import com.goyimatica.synaxismobile.ui.components.HairRule
import com.goyimatica.synaxismobile.ui.components.SectionLabel
import com.goyimatica.synaxismobile.ui.components.SynChip
import com.goyimatica.synaxismobile.ui.theme.Syn
import com.goyimatica.synaxismobile.ui.theme.familyFor
import com.goyimatica.synaxismobile.ui.toFace
import com.goyimatica.synaxismobile.ui.toReading
import kotlinx.coroutines.launch

private const val SPECIMEN =
    "Acquire the Spirit of Peace, and thousands around you will be saved. " +
        "The Lord seeks the heart filled to overflowing with love for God and " +
        "neighbour; this is the throne on which He loves to sit."

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val c = Syn.colors
    val scope = rememberCoroutineScope()
    val settings by Store.settings.collectAsStateWithLifecycle()
    val reading = settings.toReading()

    var syncing by remember { mutableStateOf(false) }
    var done by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var confirmErase by remember { mutableStateOf(false) }
    var confirmClearCache by remember { mutableStateOf(false) }

    /* WikiRepo counts the cache off the disk, so both of these suspend. A
       composable body cannot suspend and must never block, so they are held as
       state and refreshed whenever the cache could have changed. */
    var haveCount by remember { mutableIntStateOf(0) }
    var cacheKb by remember { mutableLongStateOf(0L) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh, syncing) {
        haveCount = WikiRepo.downloaded()
        cacheKb = WikiRepo.cacheBytes() / 1024L
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.ArrowBack, "Back", tint = c.dim,
                modifier = Modifier.size(23.dp).clickable { onBack() },
            )
            Spacer(Modifier.width(14.dp))
            Text("Settings", style = MaterialTheme.typography.headlineSmall, color = c.text)
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            /* ---------- reading ---------- */
            item("r-label") { SectionLabel("Reading") }

            item("specimen") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.surface)
                        .padding(18.dp),
                ) {
                    Text(
                        SPECIMEN,
                        fontFamily = familyFor(settings.toFace()),
                        fontSize = reading.fontSizeSp.sp,
                        lineHeight = reading.lineHeightSp.sp,
                        fontWeight = FontWeight(reading.weight),
                        textAlign = if (reading.justify) TextAlign.Justify else TextAlign.Start,
                        color = c.text,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "\u2014 St Seraphim of Sarov",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.goldDim,
                    )
                }
            }

            item("face") {
                ChipSetting("Typeface", FACE_NAMES, settings.face) { i ->
                    Store.update { it.copy(face = i) }
                }
            }
            item("size") {
                ChipSetting("Size", SIZE_NAMES, settings.sizeStep - 1) { i ->
                    Store.update { it.copy(sizeStep = i + 1) }
                }
            }
            item("lead") {
                ChipSetting("Line spacing", LEAD_NAMES, settings.leadStep - 1) { i ->
                    Store.update { it.copy(leadStep = i + 1) }
                }
            }
            item("weight") {
                ChipSetting(
                    "Weight",
                    WEIGHT_NAMES,
                    if (settings.weight >= 600) 1 else 0,
                ) { i ->
                    Store.update { it.copy(weight = if (i == 1) 600 else 400) }
                }
            }
            item("justify") {
                SwitchSetting(
                    "Justify the text",
                    "Even margins on both sides, as a printed book has.",
                    settings.justify,
                ) { v -> Store.update { it.copy(justify = v) } }
            }
            item("dropcap") {
                SwitchSetting(
                    "Drop capital",
                    "The first letter of a life set large, in gold.",
                    settings.dropCap,
                ) { v -> Store.update { it.copy(dropCap = v) } }
            }
            item("keep") {
                SwitchSetting(
                    "Keep the screen awake",
                    "While a life is open, the screen will not dim.",
                    settings.keepScreenOn,
                ) { v -> Store.update { it.copy(keepScreenOn = v) } }
            }

            /* ---------- appearance ---------- */
            item("a-label") { Spacer(Modifier.height(14.dp)); SectionLabel("Appearance") }
            item("palette") {
                ChipSetting("Palette", PALETTE_NAMES, settings.palette) { i ->
                    Store.update { it.copy(palette = i) }
                }
            }
            item("anim") {
                SwitchSetting(
                    "Animations",
                    "Fades and presses. Turn this off and everything happens at once.",
                    settings.animations,
                ) { v -> Store.update { it.copy(animations = v) } }
            }

            /* ---------- calendar ---------- */
            item("c-label") { Spacer(Modifier.height(14.dp)); SectionLabel("The calendar") }
            item("calstyle") {
                ChipSetting("Reckoning", CALENDAR_NAMES, settings.calendarStyle) { i ->
                    Store.update { it.copy(calendarStyle = i) }
                }
            }
            item("pending") {
                SwitchSetting(
                    "Show the not-yet-glorified",
                    "A few in the index are venerated locally but not yet glorified by a " +
                        "synod. They are marked as such when shown.",
                    settings.showPending,
                ) { v -> Store.update { it.copy(showPending = v) } }
            }

            /* ---------- offline ---------- */
            item("o-label") { Spacer(Modifier.height(14.dp)); SectionLabel("Offline") }
            item("wifi") {
                SwitchSetting(
                    "Sync on Wi-Fi only",
                    "Leave this on unless you are patient with your data.",
                    settings.syncOnWifiOnly,
                ) { v -> Store.update { it.copy(syncOnWifiOnly = v) } }
            }
            item("sync") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.surface)
                        .padding(16.dp),
                ) {
                    Text(
                        "Keep every life on this device",
                        style = MaterialTheme.typography.titleLarge,
                        color = c.text,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        if (syncing)
                            "Fetching \u2026 " + done + " of " +
                                (if (total > 0) total else SaintsRepo.count)
                        else
                            haveCount.toString() + " of " + SaintsRepo.count +
                                " lives are already downloaded \u00B7 " + cacheKb + " KB",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.dim,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SynChip(
                            text = if (syncing) "Working\u2026" else "Sync everything",
                            selected = syncing,
                            onClick = {
                                if (!syncing) {
                                    syncing = true
                                    done = 0
                                    total = 0
                                    scope.launch {
                                        WikiRepo.syncAll(SaintsRepo.all()) { n, all ->
                                            done = n
                                            total = all
                                        }
                                        syncing = false
                                        refresh++
                                    }
                                }
                            },
                        )
                        SynChip(
                            text = "Clear the cache",
                            selected = false,
                            onClick = { confirmClearCache = true },
                        )
                    }
                }
            }

            /* ---------- about ---------- */
            item("about") {
                Spacer(Modifier.height(20.dp))
                HairRule()
                Spacer(Modifier.height(16.dp))
                Column {
                    Text(
                        "Synaxis",
                        style = MaterialTheme.typography.titleLarge,
                        color = c.gold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        SaintsRepo.count.toString() + " lives \u00B7 " + QuotesRepo.count +
                            " sayings \u00B7 " + Cal.count() + " commemorations",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.dim,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Lives are fetched from OrthodoxWiki, and from Wikipedia where " +
                            "OrthodoxWiki is silent. Nothing here is written by a machine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.faint,
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Erase everything",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.blood,
                        modifier = Modifier.clickable { confirmErase = true },
                    )
                }
            }
        }
    }

    if (confirmClearCache) {
        AlertDialog(
            onDismissRequest = { confirmClearCache = false },
            containerColor = c.surface,
            title = { Text("Clear the downloaded lives?", color = c.text) },
            text = {
                Text(
                    "Your bookmarks, highlights and notes are kept. Only the downloaded " +
                        "text goes, and it can be fetched again.",
                    color = c.dim,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        WikiRepo.clear()
                        refresh++
                    }
                    confirmClearCache = false
                }) { Text("Clear", color = c.gold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearCache = false }) {
                    Text("Keep them", color = c.faint)
                }
            },
        )
    }

    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            containerColor = c.surface,
            title = { Text("Erase everything?", color = c.blood) },
            text = {
                Text(
                    "Bookmarks, highlights, notes, what you have read, the downloaded " +
                        "lives and every setting. This cannot be undone.",
                    color = c.dim,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    Store.eraseEverything()
                    scope.launch {
                        WikiRepo.clear()
                        refresh++
                    }
                    confirmErase = false
                }) { Text("Erase", color = c.blood) }
            },
            dismissButton = {
                TextButton(onClick = { confirmErase = false }) {
                    Text("Cancel", color = c.faint)
                }
            },
        )
    }
}

@Composable
private fun ChipSetting(
    label: String,
    options: List<String>,
    selected: Int,
    onPick: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        SectionLabel(label)
        Spacer(Modifier.height(9.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options.size) { i ->
                SynChip(
                    text = options[i],
                    selected = i == selected,
                    onClick = { onPick(i) },
                )
            }
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    note: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val c = Syn.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .clickable { onChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = c.text)
            if (note.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(note, style = MaterialTheme.typography.bodySmall, color = c.faint)
            }
        }
        Spacer(Modifier.width(14.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.bg,
                checkedTrackColor = c.gold,
                uncheckedThumbColor = c.faint,
                uncheckedTrackColor = c.surface,
                uncheckedBorderColor = c.rule,
            ),
        )
    }
}