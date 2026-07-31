package com.goyimatica.synaxismobile.ui.reader

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextLayoutResult

/**
 * Two integers and a layout. That is the whole selection.
 *
 * Everything here refuses to write a value that has not changed, because a
 * write is a recomposition and a recomposition of a long life is a measurable
 * amount of work. A drag across a paragraph produces one write per character
 * crossed, not one per pointer sample.
 */
@Stable
class SelectionState {

    var layout: TextLayoutResult? by mutableStateOf(null)

    var start by mutableIntStateOf(-1)
        private set

    var end by mutableIntStateOf(-1)
        private set

    /** 0 nothing, 1 the left handle, 2 the right. Kept so the reader can hide
     *  the action bar while a handle is under the finger. */
    var dragging by mutableIntStateOf(0)

    val active: Boolean get() = start >= 0 && end > start

    val length: Int get() = if (active) end - start else 0

    val anchorStart: Int get() = start
    val anchorEnd: Int get() = end

    fun clear() {
        if (start != -1) start = -1
        if (end != -1) end = -1
        if (dragging != 0) dragging = 0
    }

    fun select(a: Int, b: Int) {
        val lo = minOf(a, b)
        val hi = maxOf(a, b)
        if (hi <= lo) return
        if (start != lo) start = lo
        if (end != hi) end = hi
    }

    /** A long press takes the whole word under the finger, never a bare caret. */
    fun selectWordAt(offset: Int) {
        val l = layout ?: return
        val safe = offset.coerceIn(0, l.layoutInput.text.length)
        val word = l.getWordBoundary(safe)
        if (word.end > word.start) {
            select(word.start, word.end)
        } else {
            select(safe, (safe + 1).coerceAtMost(l.layoutInput.text.length))
        }
    }

    /**
     * The left handle. It may never cross the right one - it stops one
     * character short, which is what stops the selection flickering out of
     * existence mid-drag and taking the action bar with it.
     */
    fun dragStartTo(offset: Int) {
        if (!active) return
        val wanted = offset.coerceAtMost(end - 1).coerceAtLeast(0)
        if (wanted != start) start = wanted
    }

    fun dragEndTo(offset: Int) {
        if (!active) return
        val limit = layout?.layoutInput?.text?.length ?: return
        val wanted = offset.coerceAtLeast(start + 1).coerceAtMost(limit)
        if (wanted != end) end = wanted
    }

    fun textOf(source: String): String {
        if (!active) return ""
        val a = start.coerceIn(0, source.length)
        val b = end.coerceIn(a, source.length)
        return source.substring(a, b)
    }
}