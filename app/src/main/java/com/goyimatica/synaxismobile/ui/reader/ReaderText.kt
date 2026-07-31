package com.goyimatica.synaxismobile.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goyimatica.synaxismobile.data.Mark
import com.goyimatica.synaxismobile.ui.theme.SelWash
import com.goyimatica.synaxismobile.ui.theme.Syn
import com.goyimatica.synaxismobile.ui.theme.familyFor

/**
 * The reading surface.
 *
 * One text, one layout, and everything drawn from it:
 *  - highlights, as a single Path per mark, so the spaces inside a highlight
 *    are highlighted too;
 *  - the selection wash, drawn the same way;
 *  - two handles you can drag, which snap outward to whole words;
 *  - a floating bar that exists only while a selection exists.
 *
 * There is deliberately no SelectionContainer. That is the component that hands
 * the text to Android's own selection machinery and its Copy/Share popup; since
 * we never use it, that popup can never appear over ours.
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
    val prefs = Syn.reading
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    /* A new life means the old offsets mean nothing. */
    LaunchedEffect(text) { state.clear() }

    val painted = remember(marks, text) { marks.valid(text.length) }

    val style = remember(prefs, c) {
        TextStyle(
            fontFamily = familyFor(prefs.face),
            fontSize = prefs.fontSizeSp.sp,
            lineHeight = prefs.lineHeightSp.sp,
            fontWeight = FontWeight(prefs.weight),
            color = c.text,
            textAlign = if (prefs.justify) TextAlign.Justify else TextAlign.Start,
        )
    }

    /* The drop cap is a span, not a separate composable - so every character
       index still lines up with the plain string the marks were made against. */
    val rendered: AnnotatedString = remember(text, prefs.dropCap, c) {
        if (!prefs.dropCap || text.isEmpty()) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                append(text)
                addStyle(
                    SpanStyle(
                        fontSize = (prefs.fontSizeSp * 2.1f).sp,
                        fontWeight = FontWeight.Medium,
                        color = c.gold,
                    ),
                    0,
                    1,
                )
            }
        }
    }

    val washAlpha = if (c.isDark) 0.30f else 0.38f
    var dragging by remember { mutableIntStateOf(0) }   // 0 none, 1 start, 2 end
    val grabPx = with(density) { 30.dp.toPx() }
    val handleR = with(density) { 7.dp.toPx() }

    Box(modifier.fillMaxWidth()) {
        Text(
            text = rendered,
            style = style,
            onTextLayout = { state.layout = it },
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val l = state.layout ?: return@drawBehind
                    val n = l.layoutInput.text.length

                    painted.forEach { m ->
                        if (m.end <= n) {
                            drawPath(
                                l.multiParagraph.getPathForRange(m.start, m.end),
                                markColor(c, m.color).copy(alpha = washAlpha),
                            )
                        }
                    }

                    if (state.active && state.end <= n) {
                        drawPath(
                            l.multiParagraph.getPathForRange(state.start, state.end),
                            SelWash,
                        )
                    }
                }
                /* Taps. A tap is a press and a release without travel, so a
                   scroll can never be read as a click on a highlight. */
                .pointerInput(text, painted) {
                    detectTapGestures(
                        onTap = { pos ->
                            val l = state.layout ?: return@detectTapGestures
                            if (state.active) {
                                state.clear()
                                return@detectTapGestures
                            }
                            val offset = l.getOffsetForPosition(pos)
                            painted.at(offset)?.let(onMarkClick)
                        },
                        onLongPress = { pos ->
                            val l = state.layout ?: return@detectTapGestures
                            state.selectWordAt(l.getOffsetForPosition(pos))
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                    )
                }
                /* Handle dragging is only wired up while something is selected,
                   so the reader scrolls normally the rest of the time. */
                .then(
                    if (!state.active) Modifier else Modifier.pointerInput(text, state.active) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                val l = state.layout
                                if (l == null) {
                                    dragging = 0
                                    return@detectDragGestures
                                }
                                val a = l.getCursorRect(state.start)
                                val b = l.getCursorRect(state.end)
                                val pa = Offset(a.left, a.bottom)
                                val pb = Offset(b.left, b.bottom)
                                dragging = when {
                                    (pos - pa).getDistance() <= grabPx -> 1
                                    (pos - pb).getDistance() <= grabPx -> 2
                                    else -> 0
                                }
                                /* A drag that began nowhere near a handle means
                                   the reader has moved on. Let it go. */
                                if (dragging == 0) state.clear()
                            },
                            onDrag = { change, _ ->
                                if (dragging == 0) return@detectDragGestures
                                change.consume()
                                val l = state.layout ?: return@detectDragGestures
                                val offset = l.getOffsetForPosition(change.position)
                                if (dragging == 1) state.dragStartTo(offset)
                                else state.dragEndTo(offset)
                            },
                            onDragEnd = { dragging = 0 },
                            onDragCancel = { dragging = 0 },
                        )
                    },
                ),
        )

        /* The handles, over the text rather than under it. */
        Canvas(Modifier.matchParentSize()) {
            val l = state.layout ?: return@Canvas
            if (!state.active) return@Canvas
            val n = l.layoutInput.text.length
            if (state.end > n) return@Canvas

            val a = l.getCursorRect(state.start)
            val b = l.getCursorRect(state.end)
            val stem = 1.6.dp.toPx()

            drawLine(c.gold, Offset(a.left, a.top), Offset(a.left, a.bottom), stem)
            drawCircle(c.gold, handleR, Offset(a.left, a.bottom + handleR))
            drawLine(c.gold, Offset(b.left, b.top), Offset(b.left, b.bottom), stem)
            drawCircle(c.gold, handleR, Offset(b.left, b.bottom + handleR))
        }

        /* The bar. Rendered from the selection, so it cannot outlive it. */
        val l = state.layout
        if (state.active && l != null && state.start < l.layoutInput.text.length) {
            val box = l.getBoundingBox(state.start)
            val above = with(density) { box.top.toDp() } - 56.dp
            val y = if (above < 4.dp) with(density) { box.bottom.toDp() } + 30.dp else above

            Box(
                Modifier.fillMaxWidth().offset(y = y),
                contentAlignment = Alignment.Center,
            ) {
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