package com.goyimatica.synaxismobile.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.goyimatica.synaxismobile.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/*
 * V14: the launch download, out of the activity and into the foreground.
 *
 * A "stream in the background" choice used to die the moment the user left
 * the app, because the work ran on the activity's process scope. This
 * service hosts the fetch as a foreground service: Android keeps a dataSync
 * foreground service alive while the screen is off and the app is buried,
 * and the progress bar lives in the status bar where the user can see it.
 *
 * Force-stopping the app is still the way to stop a stream - a foreground
 * service cannot be swiped away and is the documented escape hatch the
 * choice dialog promises.
 */
class SyncService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        SyncGate.attachService(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        /* The handshake: tell SyncGate the service is alive so it never
           falls back to running the same sync twice. */
        SyncGate.markServicePickedUp()

        startForegroundCompat()

        if (job?.isActive != true) {
            job = scope.launch {
                SyncGate.runInService()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        SyncGate.detachService()
        scope.cancel()
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val notification = buildNotification(0, SyncGate.total)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /* Notification updates are cheap but not free; the sync ticks a hundred
       times a minute, so coalesce to about two per second. */
    private var lastNotifyAt = 0L

    /** Called by SyncGate.runInService on every progress tick. */
    fun update(done: Int, total: Int) {
        if (total == 0) return
        val now = System.currentTimeMillis()
        if (now - lastNotifyAt < 500L && done < total) return
        lastNotifyAt = now
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { manager.notify(NOTIFICATION_ID, buildNotification(done, total)) }
    }

    fun doneNotification(done: Int, total: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { manager.notify(NOTIFICATION_ID, buildNotification(done, total, finished = true)) }
    }

    private fun buildNotification(done: Int, total: Int, finished: Boolean = false): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val text = if (finished) {
            "Every life is on this phone."
        } else {
            buildString {
                append(SyncGate.progressText)
                val speed = SyncGate.speedText
                if (speed.isNotBlank()) append(" \u00B7 ").append(speed)
                if (SyncGate.etaSeconds > 0L) {
                    val min = (SyncGate.etaSeconds + 59L) / 60L
                    append(" \u00B7 ").append(
                        if (min < 1L) "under a minute left"
                        else if (min == 1L) "about a minute left"
                        else "about " + min + " minutes left"
                    )
                }
            }
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(if (finished) "Synaxis is ready" else "Downloading the lives")
            .setContentText(text)
            .setStyle(
                if (finished) NotificationCompat.BigTextStyle().bigText(text)
                else NotificationCompat.BigTextStyle().bigText(text).setBigContentTitle("Downloading the lives")
            )
            .setProgress(total, done, total == 0)
            .setOnlyAlertOnce(true)
            .setOngoing(!finished)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Downloads",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Downloading the lives of the saints"
            setShowBadge(false)
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "synaxis-sync"
        private const val NOTIFICATION_ID = 7
    }
}
