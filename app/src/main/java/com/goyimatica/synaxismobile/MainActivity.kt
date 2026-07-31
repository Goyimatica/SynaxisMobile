package com.goyimatica.synaxismobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goyimatica.synaxismobile.data.Store
import com.goyimatica.synaxismobile.ui.SynaxisApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            /* "Keep the screen on while reading" is a setting, so it is honoured
               at the very top where the window actually lives. */
            val settings by Store.settings.collectAsStateWithLifecycle()
            val view = LocalView.current
            LaunchedEffect(settings.keepScreenOn) {
                view.keepScreenOn = settings.keepScreenOn
            }
            SynaxisApp()
        }
    }
}