package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
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
import com.goyimatica.synaxismobile.ui.components.EmptyNote
import com.goyimatica.synaxismobile.ui.components.SectionLabel
import com.goyimatica.synaxismobile.ui.reader.MarkSheet
import com.goyimatica.synaxismobile.ui.reader.hasFull
import com.goyimatica.synaxismobile.ui.reader.newMarkKey
import com.goyimatica.synaxismobile.ui.reader.words
import com.goyimatica.synaxismobile.ui.reader.NoteDialog
import com.goyimatica.synaxismobile.ui.reader.ReaderText
import com.goyimatica.synaxismobile.ui.reader.SelectionState
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

    val selection = remember(saintId) { SelectionState() }
    var sheetKey by remember(saintId) { mutableStateOf<String?>(null) }
    var noteKey by remember(saintId) { mutableStateOf<String?>(null) }
    var notingSelection by remember(saintId) { mutableStateOf(false) }

    val scroll = rememberScrollState()
    val marks = remember(library, saintId) { library.marksFor(saintId) }

    LaunchedEffect(saintId) {
        val s = saint ?: return@LaunchedEffect
        Store.touch(s.id)
        if (doc == null) {
            loading = true
            val fetched = WikiRepo.doc(s)
            doc = fetched
            failed = fetched == null || fetched.missing
            loading = false
        }
    }

    /* How far down the life you are, kept for the Continue cards. Written only
       when it has actually moved, so scrolling does not hammer the disk. */
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

        /* ---- the bar ---- */
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                Icons.Outlined.ArrowBack, "Back", tint = c.dim,
                modifier = Modifier.size(23.dp).clickable { onBack() },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Refresh, "Fetch the life again", tint = c.dim,
                    modifier = Modifier.size(20.dp).clickable {
                        scope.launch {
                            loading = true
                            val fresh = WikiRepo.doc(saint, force = true)
                            doc = fresh
                            failed = fresh == null || fresh.missing
                            loading = false
                        }
                    },
                )
                Spacer(Modifier.width(18.dp))
                Icon(
                    if (library.isBookmarked(saint.id)) Icons.Filled.Bookmark
                    else Icons.Outlined.BookmarkBorder,
                    "Save",
                    tint = if (library.isBookmarked(saint.id)) c.gold else c.dim,
                    modifier = Modifier.size(22.dp).clickable { Store.toggleBookmark(saint.id) },
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp),
        ) {
            /* ---- who ---- */
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

            val image = d?.image
            if (!image.isNullOrBlank()) {
                Spacer(Modifier.height(20.dp))
                AsyncImage(
                    model = image,
                    contentDescription = saint.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.78f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.surface),
                )
            }

            /* ---- in brief ---- */
            if (d != null && d.hasFull && d.intro.isNotBlank()) {
                Spacer(Modifier.height(24.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.surface)
                        .padding(17.dp),
                ) {
                    SectionLabel("In brief")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        d.intro,
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = c.dim,
                    )
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
                    Column {
                        Text(
                            "No life has been downloaded for this saint yet. Tap the arrows " +
                                "above to try again, or sync everything at once from Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.faint,
                        )
                    }
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

            /* ---- where it came from ---- */
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

    /* ---- the sheet, for a highlight that already exists ---- */
    val open = sheetKey?.let { k -> marks.firstOrNull { it.key == k } }
    if (open != null) {
        MarkSheet(
            mark = open,
            onRecolour = { code -> Store.editMark(saint.id, open.key, color = code, note = open.note) },
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

    /* ---- a note on an existing highlight ---- */
    val noting = noteKey?.let { k -> marks.firstOrNull { it.key == k } }
    if (noting != null) {
        NoteDialog(
            initial = noting.note,
            quoted = noting.text,
            onSave = { written ->
                Store.editMark(saint.id, noting.key, color = noting.color, note = written)
                noteKey = null
            },
            onDismiss = { noteKey = null },
        )
    }

    /* ---- a note on something just selected: highlight it and annotate it in
            one movement, which is how anybody actually reads ---- */
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