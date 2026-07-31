package com.goyimatica.synaxismobile.ui.theme

import androidx.compose.ui.graphics.Color

/* The tokens from styles.css, unchanged. Gold is the whole identity of the app,
   so it is the one colour that does not vary between the four themes. */
val Gold = Color(0xFFC9A15B)
val GoldDim = Color(0xFF8A7040)
val Blood = Color(0xFF8E3126)

/* highlight fills — .30 alpha over whatever the page is */
val HlYellow = Color(0x4DC9A15B)
val HlGreen = Color(0x4D5E9C6A)
val HlBlue = Color(0x4D5B85B8)
val HlRed = Color(0x4DB5564A)

/* the selection wash, before a colour has been chosen */
val SelWash = Color(0x57C9A15B)

enum class Palette { NIGHT, MIDNIGHT, SEPIA, PARCHMENT }

/* Material3's ColorScheme has no slot for "the dim colour used for subtitles",
   nor for the four highlight fills, so the app carries its own alongside it. */
data class SynaxisColors(
    val palette: Palette,
    val bg: Color,
    val surface: Color,
    val raised: Color,
    val text: Color,
    val dim: Color,
    val faint: Color,
    val rule: Color,
    val gold: Color = Gold,
    val goldDim: Color = GoldDim,
    val blood: Color = Blood,
    val isDark: Boolean,
) {
    fun highlight(code: String): Color = when (code) {
        "g" -> HlGreen
        "b" -> HlBlue
        "r" -> HlRed
        else -> HlYellow
    }
}

val NightColors = SynaxisColors(
    palette = Palette.NIGHT,
    bg = Color(0xFF14100E),
    surface = Color(0xFF1B1613),
    raised = Color(0xFF221C18),
    text = Color(0xFFECE3D6),
    dim = Color(0xFFA79A8C),
    faint = Color(0xFF6E655C),
    rule = Color(0xFF2C2521),
    isDark = true,
)

val MidnightColors = SynaxisColors(
    palette = Palette.MIDNIGHT,
    bg = Color(0xFF0B0F14),
    surface = Color(0xFF11171F),
    raised = Color(0xFF171F29),
    text = Color(0xFFDFE6EE),
    dim = Color(0xFF93A0AE),
    faint = Color(0xFF5F6B78),
    rule = Color(0xFF1E2733),
    isDark = true,
)

val SepiaColors = SynaxisColors(
    palette = Palette.SEPIA,
    bg = Color(0xFFF3E9D8),
    surface = Color(0xFFFBF4E7),
    raised = Color(0xFFFFFAF0),
    text = Color(0xFF2E2620),
    dim = Color(0xFF6B5F52),
    faint = Color(0xFF938673),
    rule = Color(0xFFDECFB6),
    goldDim = Color(0xFF9A7C42),
    isDark = false,
)

val ParchmentColors = SynaxisColors(
    palette = Palette.PARCHMENT,
    bg = Color(0xFFFBF7EE),
    surface = Color(0xFFFFFFFF),
    raised = Color(0xFFFFFFFF),
    text = Color(0xFF241F1A),
    dim = Color(0xFF6A6157),
    faint = Color(0xFF988F83),
    rule = Color(0xFFE7E0D2),
    goldDim = Color(0xFF97793F),
    isDark = false,
)

fun colorsFor(p: Palette): SynaxisColors = when (p) {
    Palette.NIGHT -> NightColors
    Palette.MIDNIGHT -> MidnightColors
    Palette.SEPIA -> SepiaColors
    Palette.PARCHMENT -> ParchmentColors
}