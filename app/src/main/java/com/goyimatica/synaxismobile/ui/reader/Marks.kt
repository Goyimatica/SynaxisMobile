package com.goyimatica.synaxismobile.ui.reader

import androidx.compose.ui.graphics.Color
import com.goyimatica.synaxismobile.data.Mark
import com.goyimatica.synaxismobile.ui.theme.SynaxisColors

/* The four colours, in the order they appear on the bar. Stored as one letter
   each, exactly as the web app stored them, so a library exported from the
   website and dropped into the phone would still make sense. */
val MARK_CODES: List<String> = listOf("y", "g", "b", "r")

fun markName(code: String): String = when (code) {
    "g" -> "Green"
    "b" -> "Blue"
    "r" -> "Red"
    else -> "Yellow"
}

fun markColor(colors: SynaxisColors, code: String): Color = colors.highlight(code)

/**
 * The mark under a character offset. When two overlap - and they will, because
 * nobody highlights tidily - the shorter one wins, since that is the one the
 * finger was almost certainly aiming at.
 */
fun List<Mark>.at(offset: Int): Mark? =
    filter { offset >= it.start && offset < it.end }
        .minByOrNull { it.end - it.start }

/**
 * Marks clipped to a text of this length. A life can be re-fetched and come
 * back a paragraph longer; rather than silently painting the wrong words, a
 * mark that no longer fits is dropped from the painting (it stays in storage,
 * so nothing is lost if the original text comes back).
 */
fun List<Mark>.valid(length: Int): List<Mark> =
    filter { it.start >= 0 && it.end > it.start && it.end <= length }
        .sortedBy { it.start }

fun List<Mark>.without(key: String): List<Mark> = filter { it.key != key }

/** A short, readable stamp for a mark in the Library: "Yellow · 14 words". */
fun Mark.summary(): String {
    val words = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    return markName(color) + " · " + words + if (words == 1) " word" else " words"
}