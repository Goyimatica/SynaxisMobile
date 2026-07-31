package com.goyimatica.synaxismobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goyimatica.synaxismobile.core.FastRule
import com.goyimatica.synaxismobile.core.Feast
import com.goyimatica.synaxismobile.data.Saint
import com.goyimatica.synaxismobile.ui.Motion
import com.goyimatica.synaxismobile.ui.animFloat
import com.goyimatica.synaxismobile.ui.theme.Syn

@Composable
fun SaintCard(
    saint: Saint,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bookmarked: Boolean = false,
    overline: String? = null,
) {
    val c = Syn.colors
    Pressable(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surface)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Medallion(saint.initial)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                if (!overline.isNullOrBlank()) {
                    Text(
                        overline.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.goldDim,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    saint.display,
                    style = MaterialTheme.typography.titleLarge,
                    color = c.text,
                )
                val under = listOfNotNull(
                    saint.feastText().ifBlank { null },
                    saint.era.ifBlank { null },
                ).joinToString("  \u00B7  ")
                if (under.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(under, style = MaterialTheme.typography.bodySmall, color = c.faint)
                }
            }
            if (bookmarked) {
                Spacer(Modifier.width(10.dp))
                Icon(
                    Icons.Filled.Bookmark, null, tint = c.gold,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
fun ContinueCard(saint: Saint, progress: Float, onClick: () -> Unit) {
    val c = Syn.colors
    val shown by animFloat(progress.coerceIn(0f, 1f), Motion.size())

    Pressable(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surface)
                .padding(16.dp),
        ) {
            Text("CONTINUE", style = MaterialTheme.typography.labelSmall, color = c.goldDim)
            Spacer(Modifier.height(9.dp))
            Text(saint.display, style = MaterialTheme.typography.titleLarge, color = c.text)
            Spacer(Modifier.height(13.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(c.raised),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(shown)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(c.gold),
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                (shown * 100).toInt().toString() + "% read",
                style = MaterialTheme.typography.labelSmall,
                color = c.faint,
            )
        }
    }
}

/**
 * One saying, fixed for the day. Tapping copies it with the attribution -
 * it does not shuffle, because a daily saying that can be re-rolled is not a
 * daily saying.
 */
@Composable
fun QuoteCard(text: String, by: String, copied: Boolean, onCopy: () -> Unit) {
    val c = Syn.colors
    val hint by animFloat(if (copied) 1f else 0f, Motion.fade())

    Pressable(onClick = onCopy, modifier = Modifier.fillMaxWidth(), down = 0.985f) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.surface)
                .border(1.dp, c.rule, RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "A SAYING FOR TODAY",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.gold,
                )
                Icon(
                    Icons.Outlined.ContentCopy, "Copy", tint = c.faint,
                    modifier = Modifier.size(15.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "\u201C" + text.trim() + "\u201D",
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                color = c.text,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "\u2014 " + by,
                style = MaterialTheme.typography.labelLarge,
                color = c.goldDim,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (copied) "Copied." else "Tap to copy",
                style = MaterialTheme.typography.labelSmall,
                color = if (copied) c.gold.copy(alpha = 0.4f + 0.6f * hint) else c.faint,
            )
        }
    }
}

/**
 * The morning card. Everything a fasting day actually needs to say, and a
 * severity stripe down the edge so it can be read at a glance.
 */
@Composable
fun FastCard(
    civilLine: String,
    churchLine: String,
    season: String,
    rule: FastRule,
    onClick: () -> Unit,
) {
    val c = Syn.colors
    val accent = fastColor(rule.level)

    Pressable(onClick = onClick, modifier = Modifier.fillMaxWidth(), down = 0.985f) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.surface),
        ) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(accent))
            Column(Modifier.padding(18.dp)) {
                Text(
                    civilLine.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.gold,
                )
                Spacer(Modifier.height(8.dp))
                Text(rule.label, style = MaterialTheme.typography.headlineSmall, color = c.text)

                Spacer(Modifier.height(6.dp))
                Text(
                    listOfNotNull(
                        churchLine.ifBlank { null },
                        season.ifBlank { null },
                    ).joinToString("  \u00B7  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.faint,
                )

                if (rule.detail.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(rule.detail, style = MaterialTheme.typography.bodyMedium, color = c.dim)
                }

                if (rule.eat.isNotBlank() || rule.avoid.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    HairRule()
                    Spacer(Modifier.height(14.dp))
                    if (rule.eat.isNotBlank()) {
                        FastLine("EAT", rule.eat, accent)
                    }
                    if (rule.avoid.isNotBlank()) {
                        if (rule.eat.isNotBlank()) Spacer(Modifier.height(10.dp))
                        FastLine("AVOID", rule.avoid, c.blood)
                    }
                }
            }
        }
    }
}

@Composable
private fun FastLine(label: String, body: String, dot: androidx.compose.ui.graphics.Color) {
    val c = Syn.colors
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = c.faint)
            Spacer(Modifier.height(3.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = c.text)
        }
    }
}

@Composable
fun FeastCard(feast: Feast, modifier: Modifier = Modifier) {
    val c = Syn.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (feast.great) c.raised else c.surface)
            .border(
                1.dp,
                if (feast.great) c.goldDim else c.rule,
                RoundedCornerShape(14.dp),
            )
            .padding(16.dp),
    ) {
        if (feast.great) {
            Text("A GREAT FEAST", style = MaterialTheme.typography.labelSmall, color = c.gold)
            Spacer(Modifier.height(8.dp))
        }
        Text(
            feast.name,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = if (feast.great) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = c.text,
        )
        if (feast.note.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(feast.note, style = MaterialTheme.typography.bodySmall, color = c.dim)
        }
    }
}