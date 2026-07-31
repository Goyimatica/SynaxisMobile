package com.goyimatica.synaxismobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goyimatica.synaxismobile.data.SyncGate
import com.goyimatica.synaxismobile.ui.Motion
import com.goyimatica.synaxismobile.ui.animFloat
import com.goyimatica.synaxismobile.ui.theme.Syn

/**
 * The download that greets you.
 *
 * Shown once per launch by SyncGate. It can be dismissed at any moment and
 * the fetching carries on regardless - the gate owns the work, this only
 * reports on it.
 */
@Composable
fun SyncDialog() {
    if (!SyncGate.visible) return

    val c = Syn.colors
    val fraction = SyncGate.fraction
    val target = if (fraction < 0f) 0f else fraction
    val filled by animFloat(target, Motion.spatial(), "sync")

    Dialog(
        onDismissRequest = { SyncGate.hide() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface)
                .border(1.dp, c.goldDim.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OrthodoxCross(size = 30.dp)
            Spacer(Modifier.height(18.dp))

            Text(
                text = when {
                    SyncGate.finished && SyncGate.total == 0 -> "Everything is here"
                    SyncGate.finished -> "The lives are downloaded"
                    SyncGate.total == 0 -> "Looking for new lives"
                    else -> "Downloading the lives"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = c.text,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = when {
                    SyncGate.finished && SyncGate.total == 0 ->
                        "Every life is already on this phone."
                    SyncGate.finished ->
                        SyncGate.total.toString() + " fetched. They are yours offline now."
                    SyncGate.total == 0 ->
                        "Checking which lives this phone is missing\u2026"
                    else ->
                        SyncGate.done.toString() + " of " + SyncGate.total +
                            " \u00B7 twelve at a time"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = c.dim,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))

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

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Pressable(onClick = { SyncGate.hide() }, down = 0.93f) {
                    Text(
                        text = if (SyncGate.finished) "Begin" else "Read while it downloads",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.gold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.width(1.dp))
            }
        }
    }
}

/** Kept for callers that want the bar without the dialog around it. */
@Composable
fun SyncRibbon() {
    if (!SyncGate.visible || SyncGate.total <= 0) return
    val c = Syn.colors
    val filled by animFloat(SyncGate.fraction.coerceAtLeast(0f), Motion.spatial(), "ribbon")
    AnimatedVisibility(visible = true, enter = fadeIn(Motion.fade()), exit = fadeOut(Motion.fade())) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(c.rule),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(filled)
                    .height(2.dp)
                    .background(c.gold),
            )
        }
    }
}