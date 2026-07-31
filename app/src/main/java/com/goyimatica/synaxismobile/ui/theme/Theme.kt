package com.goyimatica.synaxismobile.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
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

data class ReadingPrefs(
    val face: ReadingFace = ReadingFace.NOTO,
    val sizeStep: Int = 3,
    val leadStep: Int = 2,
    val weight: Int = 600,
    val justify: Boolean = false,
    val dropCap: Boolean = true,
    val animations: Float = 1f,
) {
    val fontSizeSp: Float
        get() = when (sizeStep) {
            1 -> 16.5f; 2 -> 17.75f; 3 -> 19f; 4 -> 20.5f; else -> 22.5f
        }

    val lineHeightSp: Float
        get() = fontSizeSp * when (leadStep) {
            1 -> 1.52f; 2 -> 1.72f; else -> 1.92f
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

            /*
             * V7.1 - the navigation bar.
             *
             * enableEdgeToEdge installs a contrast scrim behind the gesture
             * pill, picked from the system's light/dark setting rather than
             * from our palette, which is why the bottom bar never matched the
             * app while the status bar always did. Both bars are made truly
             * transparent and the scrim is switched off, so what shows through
             * is the surface our own bottom bar already draws.
             */
            window.statusBarColor = AndroidColor.TRANSPARENT
            window.navigationBarColor = AndroidColor.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }

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

private fun onGold(c: SynaxisColors) =
    if (c.isDark) Color(0xFF14100E) else Color(0xFF1A1512)