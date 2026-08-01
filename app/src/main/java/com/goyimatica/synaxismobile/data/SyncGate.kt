package com.goyimatica.synaxismobile.data

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
 * V11: the user is asked first. A Material 3 alert dialog offers two ways to
 * take the missing lives - download everything now, or let it stream in the
 * background while the app is used. The dialog cannot be dismissed without
 * one of the two answers; force-stopping the app is the way to stop a
 * stream. Either way the fetching begins the instant the answer arrives.
 *
 * All observable fields are snapshot state, written from WikiRepo's IO
 * workers and read by the dialog, so the bar moves without anyone polling.
 */
object SyncGate {

    private var started = false

    /** The choice dialog is asking: now, or in the background? */
    var awaitingChoice by mutableStateOf(false)

    /** The progress dialog is up (a "download now" run). */
    var visible by mutableStateOf(false)

    /** A background stream is running; only a small pill is shown. */
    var background by mutableStateOf(false)

    var finished by mutableStateOf(false)
    var total by mutableIntStateOf(0)
    var done by mutableIntStateOf(0)

    /** Rough bytes the missing lives will cost, promised before we fetch. */
    var estimateBytes by mutableLongStateOf(0L)

    /** Seconds left, measured from the pace so far; 0 until we have a pace. */
    var etaSeconds by mutableLongStateOf(0L)

    private var choice: Int? = null   /* 0 = download now, 1 = background */
    private var startedAt = 0L

    /** 0f..1f, or -1f while we are still counting what is missing. */
    val fraction: Float
        get() = if (total <= 0) -1f else (done.toFloat() / total).coerceIn(0f, 1f)

    /** "12 of 40" for the ribbon and the pill. */
    val progressText: String
        get() = done.toString() + " of " + total

    fun chooseNow() {
        choice = 0
        awaitingChoice = false
    }

    fun chooseBackground() {
        choice = 1
        awaitingChoice = false
    }

    fun hide() {
        visible = false
        background = false
    }

    suspend fun runOnce(saints: List<Saint>) {
        if (started) return
        started = true

        awaitingChoice = false
        visible = false
        background = false
        finished = false
        done = 0
        total = 0
        estimateBytes = 0L
        etaSeconds = 0L
        choice = null

        val missing = WikiRepo.pending(saints)

        if (missing.isEmpty()) {
            finished = true
            visible = true
            delay(900)
            visible = false
            return
        }

        total = missing.size
        estimateBytes = WikiRepo.estimateBytes(missing)
        awaitingChoice = true

        /* The dialog is non-dismissable, so this loop ends only when one of
           the two buttons is pressed. Nothing is fetched until then. The
           fallback only exists for safety - no code clears awaitingChoice
           without also setting choice. */
        while (awaitingChoice) delay(60)
        val mode = choice ?: 1

        if (mode == 0) visible = true else background = true
        startedAt = SystemClock.elapsedRealtime()

        WikiRepo.syncAll(missing) { d, t ->
            done = d
            total = t
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            if (d > 0) {
                etaSeconds = (elapsed * (t - d) / d) / 1000L
            }
        }

        finished = true
        etaSeconds = 0L
        if (mode == 0) {
            delay(1400)
            visible = false
        } else {
            delay(600)
            background = false
        }
    }
}
