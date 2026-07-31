package com.goyimatica.synaxismobile.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Display
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import coil3.compose.setSingletonImageLoaderFactory
import com.goyimatica.synaxismobile.data.Images

/* Reading settings that every screen may need. */
data class ReadingPrefs(
    val face: ReadingFace = ReadingFace.CORMORANT,
    val sizeStep: Int = 3,          // 1..5
    val leadStep: Int = 2,          // 1..3
    val weight: Int = 500,          // 400, 500 or 600
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

object Syn {
    val colors: SynaxisColors
        @Composable get() = LocalSynaxisColors.current
    val reading: ReadingPrefs
        @Composable get() = LocalReadingPrefs.current
}

/* ---- the display mode ---------------------------------------------------
 * Android hands out 60Hz by default. Ask for the fastest mode the panel has
 * at the resolution we are already running, and ask again if it changes.
 */
private fun Context.activity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Suppress("DEPRECATION")
private fun Activity.displayCompat(): Display? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else windowManager.defaultDisplay

@Composable
fun MaxRefreshRate() {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    DisposableEffect(config) {
        val activity = context.activity()
        val display = activity?.displayCompat()
        if (activity != null && display != null) {
            val current = display.mode
            val best = display.supportedModes
                .filter {
                    it.physicalWidth == current.physicalWidth &&
                        it.physicalHeight == current.physicalHeight
                }
                .maxByOrNull { it.refreshRate }
            if (best != null) {
                val attrs = activity.window.attributes
                attrs.preferredDisplayModeId = best.modeId
                attrs.preferredRefreshRate = best.refreshRate
                activity.window.attributes = attrs
            }
        }
        onDispose { }
    }
}

@Composable
fun SynaxisTheme(
    palette: Palette? = null,
    reading: ReadingPrefs = ReadingPrefs(),
    content: @Composable () -> Unit,
) {
    val resolved = palette ?: if (isSystemInDarkTheme()) Palette.NIGHT else Palette.PARCHMENT
    val c = colorsFor(resolved)

    /* every AsyncImage in the app goes through our loader: real User-Agent,
       real disk cache, crossfade off (we animate it ourselves) */
    setSingletonImageLoaderFactory { ctx -> Images.loader(ctx) }

    MaxRefreshRate()

    val scheme = if (c.isDark) {
        darkColorScheme(
            primary = c.gold, onPrimary = onGold(c),
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
            val window = context.activity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !c.isDark
            controller.isAppearanceLightNavigationBars = !c.isDark
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

/* gold is light enough that near-black sits on it better than white */
private fun onGold(c: SynaxisColors) =
    if (c.isDark) Color(0xFF14100E) else Color(0xFF1A1512)