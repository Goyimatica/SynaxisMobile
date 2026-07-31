package com.goyimatica.synaxismobile.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goyimatica.synaxismobile.data.Mark
import com.goyimatica.synaxismobile.ui.theme.Syn
import com.goyimatica.synaxismobile.ui.theme.familyFor
import kotlin.math.abs
import kotlin.math.roundToInt

/** `== Anything ==` on a line of its own, at any depth. */
private val HEADING = Regex("(?m)^[ \\t]*(={2,6})[ \\t]*(.+?)[ \\t]*(={2,6})[ \\t]*$")

/**
 * The life, set as text, with highlights under it and a selection over it.
 *
 * Everything is one `Text`. That matters: the marks are character offsets, so
 * splitting the life into paragraphs or sections would mean translating every
 * offset on every draw. One string, one layout, one set of coordinates.
 */
@Composable
fun ReaderText(
    text: String,
    marks: List<Mark>,
    state: SelectionState,
    modifier: Modifier = Modifier,
    onMarkClick: (Mark) -> Unit,
    onHighlight: (String) -> Unit,
    onNote: () -> Unit,
    onCopy: () -> Unit,
) {
    val c = Syn.colors
    val reading = Syn.reading
    val density = LocalDensity.current

    val headingSize = (reading.fontSizeSp * 1.16f).sp
    val bodySize = reading.fontSizeSp.sp

    /* The string is rebuilt only when the text, the marks, the selection or
       the reading settings actually change. */
    val annotated: AnnotatedString = remember(text, marks, state.start, state.end, state.active, reading) {
        buildAnnotatedString {
            append(text)

            // Headings: the markers vanish, the words become a heading.
            HEADING.findAll(text).forEach { m ->
                val open = m.groups[1] ?: return@forEach
                val words = m.groups[2] ?: return@forEach
                val close = m.groups[3] ?: return@forEach

                addStyle(
                    SpanStyle(color = Color.Transparent, fontSize = 0.1f.sp),
                    open.range.first,
                    open.range.last + 1,
                )
                addStyle(
                    SpanStyle(color = Color.Transparent, fontSize = 0.1f.sp),
                    close.range.first,
                    close.range.last + 1,
                )
                addStyle(
                    SpanStyle(
                        fontSize = headingSize,
                        fontWeight = FontWeight.SemiBold,
                        color = c.gold,
                        letterSpacing = 0.4.sp,
                    ),
                    words.range.first,
                    words.range.last + 1,
                )
            }

            // A drop capital, if it is wanted and the life starts with a letter.
            if (reading.dropCap && text.firstOrNull()?.isLetter() == true) {
                addStyle(
                    SpanStyle(
                        fontSize = (reading.fontSizeSp * 2.1f).sp,
                        color = c.gold,
                        fontWeight = FontWeight.Medium,
                    ),
                    0,
                    1,
                )
            }

            // Highlights, oldest first so a newer one paints over an older.
            marks.sortedBy { it.at }.forEach { mark ->
                val a = mark.start.coerceIn(0, text.length)
                val b = mark.end.coerceIn(a, text.length)
                if (b > a) {
                    addStyle(SpanStyle(background = markColor(c, mark.color)), a, b)
                }
            }

            // The live selection sits above everything.
            if (state.active && state.end > state.start) {
                addStyle(
                    SpanStyle(background = c.gold.copy(alpha = 0.30f)),
                    state.start.coerceIn(0, text.length),
                    state.end.coerceIn(0, text.length),
                )
            }
        }
    }

    Box(modifier.fillMaxWidth()) {

        Text(
            text = annotated,
            color = c.text,
            fontFamily = familyFor(reading.face),
            fontSize = bodySize,
            lineHeight = reading.lineHeightSp.sp,
            fontWeight = FontWeight(reading.weight),
            textAlign = if (reading.justify) TextAlign.Justify else TextAlign.Start,
            onTextLayout = { state.layout = it },
            modifier = Modifier
                .fillMaxWidth()
                /* Tap and long press. This detector is always present and it
                   does not consume drags, so the page still scrolls. */
                .pointerInput(text, marks) {
                    detectTapGestures(
                        onTap = { pos ->
                            val layout = state.layout
                            if (layout == null) {
                                state.clear()
                                return@detectTapGestures
                            }
                            val offset = layout.getOffsetForPosition(pos)
                            val hit = marks.firstOrNull { offset >= it.start && offset < it.end }
                            when {
                                state.active -> state.clear()
                                hit != null -> onMarkClick(hit)
                                else -> Unit
                            }
                        },
                        onLongPress = { pos ->
                            val layout = state.layout ?: return@detectTapGestures
                            state.selectWordAt(layout.getOffsetForPosition(pos))
                        },
                    )
                }
                /* Handle dragging is attached ONLY while a selection exists.
                   detectDragGestures consumes the touch slop, which would
                   otherwise kill the scroll of the whole page. */
                .then(
                    if (!state.active) Modifier else Modifier.pointerInput(state.active, text) {
                        var draggingStart = false
                        detectDragGestures(
                            onDragStart = { pos ->
                                val layout = state.layout
                                if (layout != null) {
                                    val here = layout.getOffsetForPosition(pos)
                                    draggingStart =
                                        abs(here - state.start) <= abs(here - state.end)
                                }
                            },
                            onDrag = { change, _ ->
                                val layout = state.layout ?: return@detectDragGestures
                                change.consume()
                                val here = layout.getOffsetForPosition(change.position)
                                if (draggingStart) state.dragStartTo(here)
                                else state.dragEndTo(here)
                            },
                        )
                    }
                ),
        )

        /* The two handles. Drawn, not composed, so they cost nothing. */
        val layout = state.layout
        if (state.active && layout != null && state.end > state.start) {
            Canvas(Modifier.fillMaxWidth()) {
                runCatching {
                    val a = layout.getBoundingBox(state.start.coerceIn(0, text.length - 1))
                    val b = layout.getBoundingBox((state.end - 1).coerceIn(0, text.length - 1))
                    val r = 6.dp.toPx()
                    drawCircle(c.gold, r, Offset(a.left, a.bottom + r * 0.6f))
                    drawCircle(c.gold, r, Offset(b.right, b.bottom + r * 0.6f))
                }
            }

            /* The bar, placed just above the first line of the selection and
               nudged back on screen if the selection starts at the very top. */
            val place = runCatching {
                layout.getBoundingBox(state.start.coerceIn(0, text.length - 1))
            }.getOrNull()

            val dx = with(density) { ((place?.left ?: 0f) - 40.dp.toPx()).coerceAtLeast(0f) }
            val dy = with(density) { ((place?.top ?: 0f) - 54.dp.toPx()).coerceAtLeast(0f) }

            Box(Modifier.offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }) {
                SelectionBar(
                    onHighlight = onHighlight,
                    onNote = onNote,
                    onCopy = onCopy,
                    onDismiss = { state.clear() },
                )
            }
        }
    }
}