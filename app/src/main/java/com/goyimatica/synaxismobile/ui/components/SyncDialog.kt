package com.goyimatica.synaxismobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.goyimatica.synaxismobile.data.SyncGate
import com.goyimatica.synaxismobile.ui.Motion
import com.goyimatica.synaxismobile.ui.animFloat
import com.goyimatica.synaxismobile.ui.theme.Syn

/**
 * The download that greets you, as the current Material alert dialogs.
 *
 * V11: the user is asked first. The first dialog offers two ways to take the
 * missing lives - download everything now, or let it stream in the
 * background - and cannot be dismissed without choosing one of them. The
 * second reports the run: how many lives, how much data, and how long is
 * left. Force-stopping the app stops a stream; that is the escape hatch,
 * and the dialog says so.
 */
@Composable
fun SyncDialog() {
    when {
        SyncGate.awaitingChoice -> ChoiceDialog()
        SyncGate.visible -> ProgressDialog()
        SyncGate.background -> StreamingPill()
    }
}

private val dialogProperties = DialogProperties(
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
)

private fun humanBytes(bytes: Long): String {
    if (bytes <= 0L) return ""
    val mb = (bytes + 512L * 1024L) / (1024L * 1024L)
    return if (mb < 1L) "a moment of data" else "about " + mb + " MB"
}

private fun etaText(seconds: Long): String {
    if (seconds <= 0L) return ""
    val min = (seconds + 59L) / 60L
    return if (min < 1L) "under a minute left" else if (min == 1L) "about a minute left" else "about " + min + " minutes left"
}

/** "Download now" vs "stream in the background". One of them is required. */
@Composable
private fun ChoiceDialog() {
    val c = Syn.colors
    val count = SyncGate.total
    val bytes = humanBytes(SyncGate.estimateBytes)

    AlertDialog(
        onDismissRequest = { /* cannot be dismissed without a choice */ },
        properties = dialogProperties,
        shape = RoundedCornerShape(24.dp),
        containerColor = c.surface,
        title = {
            Text(
                text = "Download the lives?",
                style = MaterialTheme.typography.headlineSmall,
                color = c.text,
            )
        },
        text = {
            Column {
                Text(
                    text = count.toString() + " lives are missing" +
                        (if (bytes.isBlank()) "" else " - " + bytes) + ". " +
                        "Download them all now at full speed, or let them stream " +
                        "in the background while you read?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.dim,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "You can stop a stream at any moment by closing the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.faint,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { SyncGate.chooseNow() }) {
                Text("Download now", color = c.gold)
            }
        },
        dismissButton = {
            TextButton(onClick = { SyncGate.chooseBackground() }) {
                Text("Stream in background", color = c.dim)
            }
        },
    )
}

/** The run itself: a bar, a count, the data used and the time left. */
@Composable
private fun ProgressDialog() {
    val c = Syn.colors
    val fraction = SyncGate.fraction
    val target = if (fraction < 0f) 0f else fraction
    val filled by animFloat(target, Motion.spatial(), "sync")
    val finished = SyncGate.finished

    AlertDialog(
        onDismissRequest = { if (finished) SyncGate.hide() },
        properties = dialogProperties,
        shape = RoundedCornerShape(24.dp),
        containerColor = c.surface,
        title = {
            Text(
                text = when {
                    finished && SyncGate.total == 0 -> "Everything is here"
                    finished -> "The lives are downloaded"
                    else -> "Downloading the lives"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = c.text,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when {
                        finished && SyncGate.total == 0 ->
                            "Every life is already on this phone."
                        finished ->
                            SyncGate.total.toString() + " fetched. They are yours offline now."
                        else ->
                            SyncGate.progressText +
                                (if (SyncGate.etaSeconds > 0)
                                    " \u00B7 " + etaText(SyncGate.etaSeconds) else "")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.dim,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(c.rule),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (filled < 0.015f) 0.015f else filled)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(c.gold),
                    )
                }

                if (!finished) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "At full speed. Force-stop the app to stop it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.faint,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
        confirmButton = {
            if (finished) {
                TextButton(onClick = { SyncGate.hide() }) {
                    Text("Begin", color = c.gold)
                }
            }
        },
    )
}

/** One small pill while a background stream is running. */
@Composable
private fun StreamingPill() {
    val c = Syn.colors
    val filled by animFloat(SyncGate.fraction.coerceAtLeast(0f), Motion.fade(), "pill")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 10.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(c.surface.copy(alpha = 0.95f))
                .border(1.dp, c.goldDim.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(c.rule),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(filled.coerceIn(0.05f, 1f))
                        .height(3.dp)
                        .background(c.gold),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = SyncGate.progressText +
                    (if (SyncGate.etaSeconds > 0) " \u00B7 " + etaText(SyncGate.etaSeconds) else "") +
                    " \u00B7 force-stop to stop",
                style = MaterialTheme.typography.labelMedium,
                color = c.dim,
            )
        }
    }
}
