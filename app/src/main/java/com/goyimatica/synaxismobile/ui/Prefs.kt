package com.goyimatica.synaxismobile.ui

import com.goyimatica.synaxismobile.core.CalStyle
import com.goyimatica.synaxismobile.data.Settings
import com.goyimatica.synaxismobile.ui.theme.Palette
import com.goyimatica.synaxismobile.ui.theme.ReadingFace
import com.goyimatica.synaxismobile.ui.theme.ReadingPrefs

val PALETTE_NAMES = listOf("Night", "Midnight", "Sepia", "Parchment")
val FACE_NAMES = listOf("Cormorant", "Noto Serif", "Inter")
val SIZE_NAMES = listOf("Small", "Medium", "Comfortable", "Large", "Largest")
val LEAD_NAMES = listOf("Tight", "Normal", "Airy")
val WEIGHT_NAMES = listOf("Regular", "Medium", "Semibold")
val WEIGHT_VALUES = listOf(400, 500, 600)
val CALENDAR_NAMES = listOf("Julian (old)", "Revised (new)")

fun Settings.toPalette(): Palette =
    Palette.entries.getOrElse(palette) { Palette.NIGHT }

fun Settings.toFace(): ReadingFace =
    ReadingFace.entries.getOrElse(face) { ReadingFace.CORMORANT }

fun Settings.toReading(): ReadingPrefs = ReadingPrefs(
    face = toFace(),
    sizeStep = sizeStep.coerceIn(1, 5),
    leadStep = leadStep.coerceIn(1, 3),
    weight = weight,
    justify = justify,
    dropCap = dropCap,
    animations = if (animations) 1f else 0f,
)

fun Settings.toCalStyle(): CalStyle =
    if (calendarStyle == 1) CalStyle.REVISED else CalStyle.JULIAN

fun CalStyle.toStored(): Int = if (shift > 0L) 0 else 1

/** Which chip is lit for the current weight. */
fun weightIndex(weight: Int): Int = when {
    weight >= 600 -> 2
    weight >= 500 -> 1
    else -> 0
}