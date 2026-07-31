package com.goyimatica.synaxismobile.ui.reader

import com.goyimatica.synaxismobile.data.Doc
import java.util.UUID

/**
 * Whether this document carries a real life or only its opening. Compared
 * against the intro rather than tested for emptiness, because some fetches
 * come back with the full text set to the summary and nothing more - and a
 * screen that promises "The life" and delivers two sentences is a small lie.
 */
val Doc.hasFull: Boolean
    get() = full.trim().length > intro.trim().length + 40

val Doc.words: Int
    get() = (if (hasFull) full else intro)
        .trim()
        .split(Regex("\\s+"))
        .count { it.isNotBlank() }

/** A key for a new mark. Random rather than positional, so re-reading a life
 *  and marking the same sentence twice makes two marks, not one collision. */
fun newMarkKey(): String = "m" + UUID.randomUUID().toString().take(10)