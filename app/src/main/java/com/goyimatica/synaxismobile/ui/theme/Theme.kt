package com.goyimatica.synaxismobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/* Reading settings that every screen may need. Defaults match the web app's. */
data class ReadingPrefs(
    val face: ReadingFace = ReadingFace.CORMORANT,
    val sizeStep: Int = 3,          // 1..5, as html[data-size]
    val leadStep: Int = 2,          // 1..3
    val weight: Int = 400,          // 400 or 600
    val justify: Boolean = false,
    val dropCap: Boolean = true,
    val animations: Float = 1f,     // 1, .55 or 0
) {
    val fontSizeSp: Float
        get() = when (sizeStep) {
            1 -> 15f; 2 -> 16f; 3 -> 17f; 4 -> 18.5f; else -> 20.5f
        }

    val lineHeightSp: Float
        get() = fontSizeSp * when (leadStep) {
            1 -> 1.52f; 2 -> 1.74f; else -> 1.95f
        }
}

val LocalSynaxisColors = staticCompositionLocalOf { NightColors }
val LocalReadingPrefs = staticCompositionLocalOf { ReadingPrefs() }

/* Convenience so screens can write Syn.colors.gold instead of the local by name */
object Syn {
    val colors: SynaxisColors
        @Composable get() = LocalSynaxisColors.current
    val reading: ReadingPrefs
        @Composable get() = LocalReadingPrefs.current
}

@Composable
fun SynaxisTheme(
    palette: Palette? = null,
    reading: ReadingPrefs = ReadingPrefs(),
    content: @Composable () -> Unit,
) {
    val resolved = palette ?: if (isSystemInDarkTheme()) Palette.NIGHT else Palette.PARCHMENT
    val c = colorsFor(resolved)

    /* Material's own scheme is filled in from ours, so any stock component
       — a Slider, a Switch, a Snackbar — lands in the right colours without
       being restyled one at a time. */
    val scheme = if (c.isDark) {
        darkColorScheme(
            primary = c.gold, onPrimary = Color_onGold(c),
            secondary = c.goldDim, onSecondary = c.text,
            background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.raised, onSurfaceVariant = c.dim,
            outline = c.rule, outlineVariant = c.rule,
            error = c.blood, onError = c.text,
        )
    } else {
        lightColorScheme(
            primary = c.goldDim, onPrimary = c.surface,
            secondary = c.gold, onSecondary = c.text,
            background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.raised, onSurfaceVariant = c.dim,
            outline = c.rule, outlineVariant = c.rule,
            error = c.blood, onError = c.surface,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val context = LocalContext.current
        SideEffect {
            val window = (context as? Activity)?.window ?: return@SideEffect
            /* dark text on the light themes, light text on the dark ones */
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !c.isDark
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightNavigationBars = !c.isDark
        }
    }

    CompositionLocalProvider(
        LocalSynaxisColors provides c,
        LocalReadingPrefs provides reading,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = SynaxisTypography,
            content = content,
        )
    }
}

/* gold is light enough that black sits on it better than white, on every theme */
private fun Color_onGold(c: SynaxisColors) =
    if (c.isDark) androidx.compose.ui.graphics.Color(0xFF14100E) else androidx.compose.ui.graphics.Color(0xFF1A1512)