package com.goyimatica.synaxismobile.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.goyimatica.synaxismobile.data.Mark
import com.goyimatica.synaxismobile.ui.Motion
import com.goyimatica.synaxismobile.ui.animFloat
import com.goyimatica.synaxismobile.ui.theme.SelWash
import com.goyimatica.synaxismobile.ui.theme.Syn
import com.goyimatica.synaxismobile.ui.theme.familyFor

/**
 * The life itself.
 *
 * Highlights are painted underneath the glyphs with the layout's own path for
 * the range, which is why they cover the spaces between words rather than
 * breaking into separate boxes.
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

    val valid = remember(marks, text) { marks.valid(text.length) }

    val rendered = remember(text, prefs.dropCap, c.gold) {
        if (!prefs.dropCap || text.isEmpty()) {
            buildAnnotatedString { append(text) }
        } else {
            buildAnnotatedString {
                withStyle(SpanStyle(color = c.gold, fontSize = (prefs.fontSizeSp * 2.1f).sp)) {
                    append(text.substring(0, 1))
                }
                append(text.substring(1))
            }
        }
    }

    Box(modifier.fillMaxWidth()) {

        Text(
            text = rendered,
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val l = state.layout ?: return@drawBehind

                    valid.forEach { m ->
                        val s = m.start.coerceIn(0, text.length)
                        val e = m.end.coerceIn(s, text.length)
                        if (e > s) {
                            drawPath(
                                l.getPathForRange(s, e),
                                markColor(c, m.color).copy(alpha = if (c.isDark) 0.30f else 0.42f),
                            )
                        }
                    }

                    if (state.active) {
                        drawPath(
                            l.getPathForRange(state.start, state.end),
                            SelWash.copy(alpha = if (c.isDark) 0.34f else 0.26f),
                        )
                    }
                }
                .pointerInput(text, valid) {
                    detectTapGestures(
                        onLongPress = { pos ->
                            val l = state.layout ?: return@detectTapGestures
                            state.selectWordAt(l.getOffsetForPosition(pos))
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onTap = { pos ->
                            val l = state.layout ?: return@detectTapGestures
                            if (state.active) {
                                state.clear()
                                return@detectTapGestures
                            }
                            val hit = valid.at(l.getOffsetForPosition(pos))
                            if (hit != null) onMarkClick(hit)
                        },
                    )
                },
            fontFamily = familyFor(prefs.face),
            fontSize = prefs.fontSizeSp.sp,
            lineHeight = prefs.lineHeightSp.sp,
            fontWeight = FontWeight(prefs.weight),
            textAlign = if (prefs.justify) TextAlign.Justify else TextAlign.Start,
            color = c.text,
            onTextLayout = { state.layout = it },
        )

        /*  Handles and the bar exist only while something is selected. That is
            not tidiness - a drag detector left in the tree would consume the
            touch slop that the scroll needs.  */
        val layout = state.layout
        if (state.active && layout != null) {

            val startRect = layout.getCursorRect(state.start.coerceIn(0, text.length))
            val endRect = layout.getCursorRect(state.end.coerceIn(0, text.length))

            Handle(
                xPx = startRect.left,
                yPx = startRect.bottom,
                onDrag = { pos ->
                    state.dragStartTo(layout.getOffsetForPosition(pos))
                },
                onDown = { state.dragging = 1 },
                onUp = { state.dragging = 0 },
            )

            Handle(
                xPx = endRect.left,
                yPx = endRect.bottom,
                onDrag = { pos ->
                    state.dragEndTo(layout.getOffsetForPosition(pos))
                },
                onDown = { state.dragging = 2 },
                onUp = { state.dragging = 0 },
            )

            /*  The bar rides above the top of the selection and springs from
                line to line as the handles move.  */
            val barX by animFloat(startRect.left, Motion.spatial())
            val barY by animFloat(
                (startRect.top - with(density) { 58.dp.toPx() }).coerceAtLeast(0f),
                Motion.spatial(),
            )
            val fade by animFloat(if (state.dragging == 0) 1f else 0.35f, Motion.fade())

            Box(
                Modifier.offset(
                    x = with(density) { barX.toDp() } - 20.dp,
                    y = with(density) { barY.toDp() },
                ),
            ) {
                if (fade > 0.05f) {
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
}

@Composable
private fun Handle(
    xPx: Float,
    yPx: Float,
    onDrag: (Offset) -> Unit,
    onDown: () -> Unit,
    onUp: () -> Unit,
) {
    val c = Syn.colors
    val density = LocalDensity.current

    Box(
        Modifier
            .offset(
                x = with(density) { xPx.toDp() } - 9.dp,
                y = with(density) { yPx.toDp() } - 2.dp,
            )
            .size(18.dp)
            .clip(CircleShape)
            .background(c.gold)
            .border(2.dp, c.bg, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDown() },
                    onDragEnd = { onUp() },
                    onDragCancel = { onUp() },
                ) { change, _ ->
                    change.consume()
                    /*  The pointer's position within the text, not within the
                        handle - the handle sits below the line it belongs to,
                        so the y is pulled back up into the line.  */
                    onDrag(
                        Offset(
                            xPx + change.position.x - 9f,
                            yPx + change.position.y - 24f,
                        )
                    )
                }
            },
    )
}