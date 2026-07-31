package com.goyimatica.synaxismobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goyimatica.synaxismobile.core.FastRule
import com.goyimatica.synaxismobile.core.Feast
import com.goyimatica.synaxismobile.data.Saint
import com.goyimatica.synaxismobile.ui.Motion
import com.goyimatica.synaxismobile.ui.animColor
import com.goyimatica.synaxismobile.ui.animFloat
import com.goyimatica.synaxismobile.ui.theme.Syn

/* ---- a saint in a list -------------------------------------------------- */

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
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surface)
                .border(1.dp, c.rule, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Medallion(initial = saint.initial.toString())
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (!overline.isNullOrBlank()) {
                    Text(
                        text = overline.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.gold,
                    )
                    Spacer(Modifier.height(3.dp))
                }
                Text(
                    text = saint.display,
                    style = MaterialTheme.typography.titleLarge,
                    color = c.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val sub = listOf(saint.feastText(), saint.era)
                    .filter { it.isNotBlank() }
                    .joinToString("  ·  ")
                if (sub.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.dim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (bookmarked) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Bookmark,
                    contentDescription = "Bookmarked",
                    tint = c.gold,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/* ---- continue reading --------------------------------------------------- */

@Composable
fun ContinueCard(saint: Saint, progress: Float, onClick: () -> Unit) {
    val c = Syn.colors
    val p = animFloat(progress.coerceIn(0f, 1f), Motion.spatial(), "progress")
    Pressable(onClick = onClick, modifier = Modifier.width(232.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surface)
                .border(1.dp, c.rule, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Medallion(initial = saint.initial.toString(), size = 34.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = saint.display,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { p },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = c.gold,
                trackColor = c.rule,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${(p * 100).toInt()}% read",
                style = MaterialTheme.typography.labelSmall,
                color = c.faint,
            )
        }
    }
}

/* ---- the daily quote ---------------------------------------------------- */

@Composable
fun QuoteCard(text: String, by: String, copied: Boolean, onCopy: () -> Unit) {
    val c = Syn.colors
    val tint = animColor(if (copied) c.gold else c.faint, Motion.fade(), "copyTint")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .border(1.dp, c.rule, RoundedCornerShape(16.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OrthodoxCross(size = 16.dp, color = c.goldDim)
            Spacer(Modifier.width(8.dp))
            SectionLabel("A word for today")
            Spacer(Modifier.weight(1f))
            Pressable(onClick = onCopy, down = 0.9f) {
                Icon(
                    imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                    contentDescription = "Copy the quotation",
                    tint = tint,
                    modifier = Modifier.padding(6.dp).size(17.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            fontStyle = FontStyle.Italic,
            color = c.text,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "— $by",
            style = MaterialTheme.typography.labelLarge,
            color = c.gold,
        )
    }
}

/* ---- today's fast ------------------------------------------------------- */

@Composable
fun FastCard(
    civilLine: String,
    churchLine: String,
    season: String,
    rule: FastRule,
    onClick: () -> Unit,
) {
    val c = Syn.colors
    val edge = fastColor(rule.level)

    Pressable(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                /* THE FIX: the row is as tall as its tallest child, and the
                   stripe fills that height instead of inventing one. */
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(16.dp))
                .background(c.surface)
                .border(1.dp, c.rule, RoundedCornerShape(16.dp)),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(edge),
            )
            Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FastDot(rule.level, 8.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = fastWord(rule.level).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = edge,
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Open the calendar",
                        tint = c.faint,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = civilLine,
                    style = MaterialTheme.typography.titleLarge,
                    color = c.text,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = churchLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.dim,
                )
                if (season.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = season,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.faint,
                    )
                }

                if (rule.label.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = rule.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = c.text,
                    )
                }
                if (rule.eat.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "EAT",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.faint,
                            modifier = Modifier.width(52.dp),
                        )
                        Text(
                            text = rule.eat.first(),
                            style = MaterialTheme.typography.bodySmall,
                            color = c.dim,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (rule.avoid.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "NOT",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.faint,
                            modifier = Modifier.width(52.dp),
                        )
                        Text(
                            text = rule.avoid.first(),
                            style = MaterialTheme.typography.bodySmall,
                            color = c.dim,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/* ---- a feast ------------------------------------------------------------ */

@Composable
fun FeastCard(feast: Feast, modifier: Modifier = Modifier) {
    val c = Syn.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .background(if (feast.great) c.raised else c.surface)
            .border(1.dp, if (feast.great) c.goldDim.copy(alpha = 0.5f) else c.rule, RoundedCornerShape(14.dp)),
    ) {
        if (feast.great) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(c.gold),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(14.dp)) {
            if (feast.great) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OrthodoxCross(size = 13.dp, color = c.gold)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "GREAT FEAST",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.gold,
                    )
                }
                Spacer(Modifier.height(7.dp))
            }
            Text(
                text = feast.name,
                style = MaterialTheme.typography.headlineSmall,
                color = if (feast.great) c.gold else c.text,
            )
            if (feast.note.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = feast.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.dim,
                )
            }
        }
    }
}