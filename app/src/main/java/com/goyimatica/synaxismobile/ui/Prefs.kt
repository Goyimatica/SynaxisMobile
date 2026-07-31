package com.goyimatica.synaxismobile.ui

import com.goyimatica.synaxismobile.core.CalStyle
import com.goyimatica.synaxismobile.data.Settings
import com.goyimatica.synaxismobile.ui.theme.Palette
import com.goyimatica.synaxismobile.ui.theme.ReadingFace
import com.goyimatica.synaxismobile.ui.theme.ReadingPrefs

/*
 * Settings are stored as plain integers so a renamed enum constant can never
 * orphan somebody's preferences. This file is the only place that knows which
 * integer means which enum, and the lists below are the only place the Settings
 * screen gets its labels from - so the two cannot disagree.
 */

val PALETTE_NAMES = listOf("Night", "Midnight", "Sepia", "Parchment")
val FACE_NAMES = listOf("Cormorant", "Noto Serif", "Inter")
val SIZE_NAMES = listOf("Smallest", "Small", "Medium", "Large", "Largest")
val LEAD_NAMES = listOf("Tight", "Comfortable", "Airy")
val WEIGHT_NAMES = listOf("Regular", "Medium")
val CALENDAR_NAMES = listOf("Julian · Old Calendar", "Revised Julian · New Calendar")

fun Settings.toPalette(): Palette = when (palette) {
    1 -> Palette.MIDNIGHT
    2 -> Palette.SEPIA
    3 -> Palette.PARCHMENT
    else -> Palette.NIGHT
}

fun Settings.toFace(): ReadingFace = when (face) {
    1 -> ReadingFace.NOTO
    2 -> ReadingFace.SANS
    else -> ReadingFace.CORMORANT
}

/* The reader's own settings, assembled. `animations` is a switch in Settings
   and a multiplier in ReadingPrefs, because the reader scales its durations
   by it rather than branching on a boolean in twenty places. */
fun Settings.toReading(): ReadingPrefs = ReadingPrefs(
    face = toFace(),
    sizeStep = sizeStep.coerceIn(1, 5),
    leadStep = leadStep.coerceIn(1, 3),
    weight = if (weight >= 600) 600 else 400,
    justify = justify,
    dropCap = dropCap,
    animations = if (animations) 1f else 0f,
)

fun Settings.toCalStyle(): CalStyle =
    if (calendarStyle == 1) CalStyle.REVISED else CalStyle.JULIAN

fun CalStyle.toStored(): Int = if (this == CalStyle.REVISED) 1 else 0