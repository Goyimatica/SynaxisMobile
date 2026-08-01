package com.goyimatica.synaxismobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.goyimatica.synaxismobile.R

/* The built-in faces, under their legal resource names. */
val Cormorant = FontFamily(
    Font(R.font.cormorant_regular, FontWeight.Normal),
    Font(R.font.cormorant_medium, FontWeight.Medium),
    Font(R.font.cormorant_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_italic, FontWeight.Normal, FontStyle.Italic),
)

val NotoSerif = FontFamily(
    Font(R.font.noto_serif_regular, FontWeight.Normal),
    Font(R.font.noto_serif_medium, FontWeight.Medium),
    Font(R.font.noto_serif_semibold, FontWeight.SemiBold),
    Font(R.font.noto_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

/* The full Outfit range: the app ships all nine weights, so headings can
   sit at SemiBold and Black without Android synthesising a fake bold. */
val Outfit = FontFamily(
    Font(R.font.outfit_thin, FontWeight.Thin),
    Font(R.font.outfit_extralight, FontWeight.ExtraLight),
    Font(R.font.outfit_light, FontWeight.Light),
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
    Font(R.font.outfit_extrabold, FontWeight.ExtraBold),
    Font(R.font.outfit_black, FontWeight.Black),
)

enum class ReadingFace { CORMORANT, NOTO, OUTFIT }

fun familyFor(f: ReadingFace): FontFamily = when (f) {
    ReadingFace.CORMORANT -> Cormorant
    ReadingFace.NOTO -> NotoSerif
    ReadingFace.OUTFIT -> Outfit
}

/**
 * A bundled face by its display name, so Settings can list the faces that
 * came with the app in the same chips as the downloaded ones. The stored
 * setting is the name string, so a face like "Outfit" resolves here before
 * any downloaded family of the same name is even consulted.
 */
fun bundledFamily(name: String?): FontFamily? = when (name) {
    "Cormorant" -> Cormorant
    "Noto Serif" -> NotoSerif
    "Outfit" -> Outfit
    else -> null
}

/** What the app is set in when nothing has been downloaded. */
val DefaultUiFamily: FontFamily = NotoSerif

/*
 * V8: one family, fifteen styles.
 *
 * Every style below takes the same `family`. The hierarchy is carried by
 * weight, size and letter-spacing alone, which is how a book does it. Pass a
 * downloaded family in and the entire interface changes voice at once, with
 * nothing left behind in the old one.
 */
fun synaxisTypography(family: FontFamily = DefaultUiFamily): Typography = Typography(
    displayLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 42.sp, lineHeight = 49.sp),
    displayMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 41.sp),
    displaySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 29.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 37.sp),
    headlineMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 27.sp),
    titleLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 23.sp),
    titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 15.5.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 13.5.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, letterSpacing = 1.0.sp),
    labelSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, letterSpacing = 1.2.sp),
)

/** Kept so nothing that still imports the old constant fails to compile. */
val SynaxisTypography: Typography = synaxisTypography()