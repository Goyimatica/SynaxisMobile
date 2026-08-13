package com.goyimatica.synaxismobile.data

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
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
 * V14: the fetching itself runs in a foreground service (SyncService), so a
 * background stream keeps going - and keeps reporting itself in the status
 * bar - even when the app is sent to the background or the screen is off.
 * The service writes the same snapshot state the dialogs read, so nothing
 * in the UI has to know where the work is happening.
 *
 * All observable fields are snapshot state, written from the service's IO
 * workers and read by the dialog, so the bar moves without anyone polling.
 */
object SyncGate {

    private var appContext: Context? = null
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

    /** Actual download speed, bytes per second, from the bytes saved so far. */
    var speedBytes by mutableLongStateOf(0L)

    private var choice: Int? = null   /* 0 = download now, 1 = background */
    private var startedAt = 0L

    /** The entries a run is fetching; the service reads this when it wakes. */
    @Volatile
    var missing: List<Saint> = emptyList()

    /** 0f..1f, or -1f while we are still counting what is missing. */
    val fraction: Float
        get() = if (total <= 0) -1f else (done.toFloat() / total).coerceIn(0f, 1f)

    /** "12 of 40" for the ribbon and the pill. */
    val progressText: String
        get() = done.toString() + " of " + total

    /** "1.9 MB/s" once there is a pace to measure, "" before that. */
    val speedText: String
        get() {
            val b = speedBytes
            if (b <= 0L) return ""
            return when {
                b >= 1024L * 1024L -> (b.toDouble() / (1024.0 * 1024.0)).let { "%.1f".format(it) + " MB/s" }
                b >= 1024L -> (b / 1024L).toString() + " KB/s"
                else -> b.toString() + " B/s"
            }
        }

    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

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

    private fun startService() {
        val ctx = appContext ?: return
        runCatching {
            ContextCompat.startForegroundService(ctx, android.content.Intent(ctx, SyncService::class.java))
        }
    }

    /** Set by SyncService the moment it wakes up, so runOnce knows it is safe. */
    @Volatile
    private var servicePickedUp = false

    fun markServicePickedUp() {
        servicePickedUp = true
    }

    private fun resetServicePickedUp() {
        servicePickedUp = false
    }

    /* The active SyncService, when one is running the fetch. The progress
       callback touches it to move the status-bar bar; when the fetch runs
       in-process (the fallback) this stays null and nothing is notified. */
    @Volatile
    private var activeService: SyncService? = null

    fun attachService(s: SyncService) {
        activeService = s
    }

    fun detachService() {
        activeService = null
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
        speedBytes = 0L
        choice = null

        val pending = WikiRepo.pending(saints)

        if (pending.isEmpty()) {
            finished = true
            visible = true
            delay(900)
            visible = false
            return
        }

        total = pending.size
        estimateBytes = WikiRepo.estimateBytes(pending)
        missing = pending
        awaitingChoice = true

        /* The dialog is non-dismissable, so this loop ends only when one of
           the two buttons is pressed. Nothing is fetched until then. The
           fallback only exists for safety - no code clears awaitingChoice
           without also setting choice. */
        while (awaitingChoice) delay(60)
        val mode = choice ?: 1

        if (mode == 0) visible = true else background = true
        startedAt = SystemClock.elapsedRealtime()

        /* V14: the fetching happens in SyncService, so it survives the app
           being sent to the background. If the service does not wake up
           within a few seconds (which should never happen), fall back to
           fetching right here rather than leave the user staring at a
           dialog that never moves. The handshake means the sync can never
           run twice. */
        resetServicePickedUp()
        startService()
        var waited = 0
        while (!servicePickedUp && waited < 4000) {
            delay(50)
            waited += 50
        }
        if (!servicePickedUp) runInProcess()
    }

    /** Called by SyncService while it runs the fetch on this process's behalf. */
    suspend fun runInService() {
        markServicePickedUp()
        try {
            runInProcess()
        } finally {
            // nothing to release; the flags above are per-run state
        }
    }

    private suspend fun runInProcess() {
        WikiRepo.resetPulled()
        WikiRepo.syncAll(missing) { d, t ->
            done = d
            total = t
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            if (d > 0) {
                etaSeconds = (elapsed * (t - d) / d) / 1000L
            }
            val bytes = WikiRepo.pulledBytes()
            if (bytes > 0L && elapsed > 0L) {
                speedBytes = bytes * 1000L / elapsed
            }
            activeService?.update(d, t)
        }

        finished = true
        etaSeconds = 0L
        speedBytes = 0L
        activeService?.doneNotification(done, total)
        if (choice == 0) {
            delay(1400)
            visible = false
        } else {
            delay(600)
            background = false
        }
    }
}
