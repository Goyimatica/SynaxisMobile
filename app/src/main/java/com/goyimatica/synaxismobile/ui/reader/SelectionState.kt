package com.goyimatica.synaxismobile.ui.reader

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextLayoutResult

@Stable
class SelectionState {

    /** The last layout of the reader's text. Everything else needs it. */
    var layout by mutableStateOf<TextLayoutResult?>(null)

    var start by mutableIntStateOf(-1)
    var end by mutableIntStateOf(-1)

    /* The word the long press landed on. The edges may travel, but neither may
       cross this, which is what stops a selection turning inside out. */
    var anchorStart by mutableIntStateOf(-1)
    var anchorEnd by mutableIntStateOf(-1)

    val active: Boolean get() = start >= 0 && end > start

    val length: Int get() = layout?.layoutInput?.text?.length ?: 0

    fun clear() {
        start = -1
        end = -1
        anchorStart = -1
        anchorEnd = -1
    }

    /** A long press selects the word beneath it. */
    fun selectWordAt(offset: Int) {
        val l = layout ?: return
        val n = l.layoutInput.text.length
        if (n == 0) return
        val o = offset.coerceIn(0, n - 1)
        val word = l.getWordBoundary(o)
        var a = word.start
        var b = word.end
        if (b <= a) {
            a = o
            b = (o + 1).coerceAtMost(n)
        }
        start = a
        end = b
        anchorStart = a
        anchorEnd = b
    }

    /** Select an exact range - used when a saved highlight is reopened. */
    fun select(a: Int, b: Int) {
        val n = length
        if (n == 0) return
        start = a.coerceIn(0, n - 1)
        end = b.coerceIn(start + 1, n)
        anchorStart = start
        anchorEnd = end
    }

    /** The leading handle, snapped outward to the start of its word. */
    fun dragStartTo(offset: Int) {
        val l = layout ?: return
        if (anchorEnd < 0) return
        val n = l.layoutInput.text.length
        val o = offset.coerceIn(0, n - 1)
        val word = l.getWordBoundary(o)
        val a = minOf(word.start, o)
        start = a.coerceIn(0, anchorEnd - 1)
    }

    /** The trailing handle, snapped outward to the end of its word. */
    fun dragEndTo(offset: Int) {
        val l = layout ?: return
        if (anchorStart < 0) return
        val n = l.layoutInput.text.length
        val o = offset.coerceIn(0, n)
        val word = l.getWordBoundary(o.coerceAtMost(n - 1))
        val b = maxOf(word.end, o)
        end = b.coerceIn(anchorStart + 1, n)
    }

    fun textOf(source: String): String {
        if (!active) return ""
        val a = start.coerceIn(0, source.length)
        val b = end.coerceIn(a, source.length)
        return source.substring(a, b)
    }
}