package com.goyimatica.synaxismobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.goyimatica.synaxismobile.R

/* The ten faces, under their legal resource names. Android resolves weights
   itself, so FontWeight.Medium on a Cormorant style picks the medium file —
   no synthetic bolding, which is what made the web build look wrong for so long. */
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

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
)

/* which face the reader uses — the html[data-font] setting, as an enum */
enum class ReadingFace { CORMORANT, NOTO, SANS }

fun familyFor(f: ReadingFace): FontFamily = when (f) {
    ReadingFace.CORMORANT -> Cormorant
    ReadingFace.NOTO -> NotoSerif
    ReadingFace.SANS -> Inter
}

/* Chrome — bars, tabs, buttons — is Inter. Display type is Cormorant.
   Body text inside a life is built separately, from the user's settings. */
val SynaxisTypography = Typography(
    displayLarge = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 46.sp),
    displayMedium = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineLarge = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.Medium, fontSize = 23.sp, lineHeight = 29.sp),
    headlineSmall = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = NotoSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, letterSpacing = 0.9.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 10.5.sp, letterSpacing = 1.1.sp),
)