package com.goyimatica.synaxismobile.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/*
 * The download that happens when the app opens.
 *
 * Process-scoped on purpose. `started` is an ordinary field on a singleton,
 * so it survives every configuration change, every navigation, and every
 * recomposition, and resets only when Android actually kills the process.
 * That is what makes this "once per launch" rather than "once per screen".
 *
 * All four observable fields are snapshot state, written from WikiRepo's IO
 * workers and read by the dialog, so the bar moves without anyone polling.
 */
object SyncGate {

    private var started = false

    var visible by mutableStateOf(false)
    var finished by mutableStateOf(false)
    var total by mutableIntStateOf(0)
    var done by mutableIntStateOf(0)

    /** 0f..1f, or -1f while we are still counting what is missing. */
    val fraction: Float
        get() = if (total <= 0) -1f else (done.toFloat() / total).coerceIn(0f, 1f)

    fun hide() {
        visible = false
    }

    suspend fun runOnce(saints: List<Saint>) {
        if (started) return
        started = true

        visible = true
        finished = false
        done = 0
        total = 0

        val missing = WikiRepo.pending(saints)

        if (missing.isEmpty()) {
            finished = true
            delay(1100)
            visible = false
            return
        }

        total = missing.size
        WikiRepo.syncAll(missing) { d, t ->
            done = d
            total = t
        }

        finished = true
        delay(1500)
        visible = false
    }
}