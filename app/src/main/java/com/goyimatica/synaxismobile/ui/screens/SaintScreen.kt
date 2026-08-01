package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.goyimatica.synaxismobile.data.Doc
import com.goyimatica.synaxismobile.data.Mark
import com.goyimatica.synaxismobile.data.SaintsRepo
import com.goyimatica.synaxismobile.data.Store
import com.goyimatica.synaxismobile.data.WikiRepo
import com.goyimatica.synaxismobile.ui.Motion
import com.goyimatica.synaxismobile.ui.animFloat
import com.goyimatica.synaxismobile.ui.components.EmptyNote
import com.goyimatica.synaxismobile.ui.components.SectionLabel
import com.goyimatica.synaxismobile.ui.pressScale
import com.goyimatica.synaxismobile.ui.rememberInteraction
import com.goyimatica.synaxismobile.ui.reader.MarkSheet
import com.goyimatica.synaxismobile.ui.reader.NoteDialog
import com.goyimatica.synaxismobile.ui.reader.ReaderText
import com.goyimatica.synaxismobile.ui.reader.SelectionState
import com.goyimatica.synaxismobile.ui.reader.hasFull
import com.goyimatica.synaxismobile.ui.reader.newMarkKey
import com.goyimatica.synaxismobile.ui.reader.words
import com.goyimatica.synaxismobile.ui.theme.Syn
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SaintScreen(saintId: String, onBack: () -> Unit) {
    val c = Syn.colors
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val library by Store.library.collectAsStateWithLifecycle()
    val saint = remember(saintId) { SaintsRepo.byId(saintId) }

    var doc by remember(saintId) { mutableStateOf<Doc?>(WikiRepo.cached(saintId)) }
    var loading by remember(saintId) { mutableStateOf(doc == null) }
    var failed by remember(saintId) { mutableStateOf(false) }
    var briefOpen by remember(saintId) { mutableStateOf(false) }

    val selection = remember(saintId) { SelectionState() }
    var sheetKey by remember(saintId) { mutableStateOf<String?>(null) }
    var noteKey by remember(saintId) { mutableStateOf<String?>(null) }
    var notingSelection by remember(saintId) { mutableStateOf(false) }

    val scroll = rememberScrollState()
    val marks = remember(library, saintId) { library.marksFor(saintId) }

    LaunchedEffect(saintId) {
        val s = saint ?: return@LaunchedEffect
        Store.touch(s.id)
        val fetched = WikiRepo.doc(s)
        doc = fetched ?: doc
        failed = fetched == null || fetched.missing
        loading = false
    }

    LaunchedEffect(saintId, scroll) {
        var last = -1f
        snapshotFlow { scroll.value to scroll.maxValue }.collect { (v, max) ->
            if (max > 0) {
                val p = (v.toFloat() / max).coerceIn(0f, 1f)
                if (last < 0f || abs(p - last) > 0.02f) {
                    last = p
                    Store.setProgress(saintId, p)
                }
            }
        }
    }

    if (saint == null) {
        EmptyNote("That life is not in the index.")
        return
    }

    val d = doc
    val body = remember(d) {
        when {
            d == null -> ""
            d.hasFull -> d.full
            else -> d.intro
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val backPress = rememberInteraction()
            Icon(
                Icons.Outlined.ArrowBack,
                "Back",
                tint = c.dim,
                modifier = Modifier
                    .size(23.dp)
                    .pressScale(backPress, down = 0.85f)
                    .clickable(interactionSource = backPress, indication = null, onClick = onBack),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                val refreshPress = rememberInteraction()
                Icon(
                    Icons.Outlined.Refresh,
                    "Fetch the life again",
                    tint = c.dim,
                    modifier = Modifier
                        .size(20.dp)
                        .pressScale(refreshPress, down = 0.85f)
                        .clickable(
                            interactionSource = refreshPress,
                            indication = null,
                        ) {
                            scope.launch {
                                loading = true
                                val fresh = WikiRepo.doc(saint, force = true)
                                doc = fresh ?: doc
                                failed = fresh == null || fresh.missing
                                loading = false
                            }
                        },
                )

                Spacer(Modifier.width(18.dp))

                val savePress = rememberInteraction()
                Icon(
                    if (library.isBookmarked(saint.id)) Icons.Filled.Bookmark
                    else Icons.Outlined.BookmarkBorder,
                    "Save",
                    tint = if (library.isBookmarked(saint.id)) c.gold else c.dim,
                    modifier = Modifier
                        .size(22.dp)
                        .pressScale(savePress, down = 0.82f)
                        .clickable(interactionSource = savePress, indication = null) {
                            Store.toggleBookmark(saint.id)
                        },
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp),
        ) {
            Text(saint.name, style = MaterialTheme.typography.displayMedium, color = c.text)
            if (saint.epithet.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(saint.epithet, style = MaterialTheme.typography.bodyMedium, color = c.dim)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                listOfNotNull(
                    saint.feastText().ifBlank { null },
                    saint.era.ifBlank { null },
                    saint.jurisdiction.ifBlank { null },
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = c.faint,
            )

            /* ---- the icon ----
               V7.1: the reader gets the original file, not the thumbnail.
               Coil downsamples it to the frame while decoding, so the memory
               cost is the same and the detail is not. */
            val image = d?.imageFull.orEmpty().ifBlank { d?.image.orEmpty() }
            if (image.isNotBlank()) {
                Spacer(Modifier.height(22.dp))
                IconFrame(url = image, label = saint.name)
            }

            /* ---- in brief, closed until asked ---- */
            if (d != null && d.hasFull && d.intro.isNotBlank()) {
                Spacer(Modifier.height(24.dp))
                val turn by animFloat(if (briefOpen) 90f else 0f, Motion.spatial())
                val press = rememberInteraction()

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.surface)
                        .border(1.dp, c.rule, RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = press,
                            indication = null,
                        ) { briefOpen = !briefOpen }
                        .padding(17.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SectionLabel("In brief")
                        Icon(
                            Icons.Outlined.ChevronRight,
                            if (briefOpen) "Close" else "Open",
                            tint = c.goldDim,
                            modifier = Modifier.size(19.dp).rotate(turn),
                        )
                    }

                    AnimatedVisibility(
                        visible = briefOpen,
                        enter = fadeIn(Motion.fade()) + expandVertically(Motion.size()),
                        exit = fadeOut(Motion.fade()) + shrinkVertically(Motion.size()),
                    ) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                d.intro,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic,
                                ),
                                color = c.dim,
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = !briefOpen,
                        enter = fadeIn(Motion.fade()),
                        exit = fadeOut(Motion.fade()),
                    ) {
                        Column {
                            Spacer(Modifier.height(9.dp))
                            Text(
                                d.intro.take(88).trimEnd() + "…",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.faint,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            /* ---- the life ---- */
            Spacer(Modifier.height(28.dp))
            SectionLabel(if (d != null && d.hasFull) "The life" else "In brief")
            Spacer(Modifier.height(14.dp))

            when {
                loading && body.isBlank() -> {
                    Text(
                        "Fetching the life…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.faint,
                    )
                }

                body.isBlank() || failed -> {
                    Text(
                        "No life has been downloaded for this entry yet. Tap the arrows above to try again, or sync everything at once from Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.faint,
                    )
                }

                else -> {
                    ReaderText(
                        text = body,
                        marks = marks,
                        state = selection,
                        onMarkClick = { m -> sheetKey = m.key },
                        onHighlight = { code ->
                            val chosen = selection.textOf(body)
                            if (chosen.isNotBlank()) {
                                Store.addMark(
                                    saintId = saint.id,
                                    mark = Mark(
                                        key = newMarkKey(),
                                        start = selection.start,
                                        end = selection.end,
                                        color = code,
                                        at = System.currentTimeMillis(),
                                        text = chosen,
                                        note = "",
                                    ),
                                )
                            }
                            selection.clear()
                        },
                        onNote = { notingSelection = true },
                        onCopy = {
                            val chosen = selection.textOf(body)
                            if (chosen.isNotBlank()) {
                                clipboard.setText(AnnotatedString(chosen))
                            }
                            selection.clear()
                        },
                    )
                }
            }

            if (d != null && !d.missing) {
                Spacer(Modifier.height(30.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.rule))
                Spacer(Modifier.height(16.dp))
                Text(
                    (if (d.fromOrthodoxWiki) "From OrthodoxWiki" else "From Wikipedia") +
                        " · " + d.words + " words" +
                        (if (marks.isNotEmpty()) " · " + marks.size + " highlighted" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.faint,
                )
            }

            Spacer(Modifier.height(90.dp))
        }
    }

    val open = sheetKey?.let { k -> marks.firstOrNull { it.key == k } }
    if (open != null) {
        MarkSheet(
            mark = open,
            onRecolour = { code ->
                Store.editMark(saint.id, open.key, color = code, note = open.note.orEmpty())
            },
            onNote = {
                noteKey = open.key
                sheetKey = null
            },
            onCopy = {
                clipboard.setText(AnnotatedString(open.text))
                sheetKey = null
            },
            onDelete = {
                Store.dropMark(saint.id, open.key)
                sheetKey = null
            },
            onDismiss = { sheetKey = null },
        )
    }

    val noting = noteKey?.let { k -> marks.firstOrNull { it.key == k } }
    if (noting != null) {
        NoteDialog(
            initial = noting.note.orEmpty(),
            quoted = noting.text,
            onSave = { written ->
                Store.editMark(saint.id, noting.key, color = noting.color, note = written)
                noteKey = null
            },
            onDismiss = { noteKey = null },
        )
    }

    if (notingSelection) {
        val chosen = selection.textOf(body)
        NoteDialog(
            initial = "",
            quoted = chosen,
            onSave = { written ->
                if (chosen.isNotBlank()) {
                    Store.addMark(
                        saintId = saint.id,
                        mark = Mark(
                            key = newMarkKey(),
                            start = selection.start,
                            end = selection.end,
                            color = "y",
                            at = System.currentTimeMillis(),
                            text = chosen,
                            note = written,
                        ),
                    )
                }
                notingSelection = false
                selection.clear()
            },
            onDismiss = {
                notingSelection = false
                selection.clear()
            },
        )
    }
}

/* ---- the icon ----------------------------------------------------------
 * Height-led, ratio-measured, centred.
 */
@Composable
private fun IconFrame(url: String, label: String) {
    val c = Syn.colors
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val cap = (screenHeight * 0.34f).coerceIn(190.dp, 300.dp)

    var ratio by remember(url) { mutableFloatStateOf(0.80f) }

    val outer = RoundedCornerShape(14.dp)
    val inner = RoundedCornerShape(9.dp)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(cap)
                .aspectRatio(ratio, matchHeightConstraintsFirst = true)
                .clip(outer)
                .background(c.raised)
                .border(1.dp, c.goldDim.copy(alpha = 0.45f), outer)
                .padding(5.dp),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = url,
                contentDescription = label,
                contentScale = ContentScale.Fit,
                onSuccess = { state ->
                    val w = state.painter.intrinsicSize.width
                    val h = state.painter.intrinsicSize.height
                    if (w > 0f && h > 0f) {
                        ratio = (w / h).coerceIn(0.62f, 1.60f)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(inner)
                    .background(c.surface)
                    .border(1.dp, c.rule, inner),
            )
        }
    }
}