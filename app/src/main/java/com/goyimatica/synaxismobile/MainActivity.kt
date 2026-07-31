package com.goyimatica.synaxismobile

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goyimatica.synaxismobile.data.Store
import com.goyimatica.synaxismobile.ui.SynaxisApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askForTheFastestMode()

        setContent {
            val settings by Store.settings.collectAsStateWithLifecycle()
            val view = LocalView.current
            SideEffect { view.keepScreenOn = settings.keepScreenOn }
            SynaxisApp()
        }
    }

    /**
     * A window gets sixty hertz unless it says otherwise. Every spring in the
     * app is computed per frame, so on a 120 Hz panel this is the difference
     * between motion that looks smooth and motion that looks nearly smooth.
     */
    @Suppress("DEPRECATION")
    private fun askForTheFastestMode() {
        val d = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display
        else windowManager.defaultDisplay
        val best = d?.supportedModes?.maxByOrNull { it.refreshRate } ?: return
        val lp = window.attributes
        lp.preferredDisplayModeId = best.modeId
        window.attributes = lp
    }
}