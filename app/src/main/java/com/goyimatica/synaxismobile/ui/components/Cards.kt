package com.goyimatica.synaxismobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.goyimatica.synaxismobile.ui.theme.Syn

/**
 * A saint in a list. One tap opens the life; the medallion carries the
 * initial so a list of forty scrolls without forty network images.
 */
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
            Medallion(initial = saint.initial.toString(), size = 42.dp)
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
                    saint.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = c.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val second = listOfNotNull(
                    saint.feastText().ifBlank { null },
                    saint.epithet.ifBlank { null },
                ).joinToString("  \u00B7  ")
                if (second.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        second,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.faint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (bookmarked) {
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(c.gold),
                )
            }
        }
    }
}

/** Where you left off. The gold rule under the name is how far you read. */
@Composable
fun ContinueCard(saint: Saint, progress: Float, onClick: () -> Unit) {
    val c = Syn.colors
    val p = progress.coerceIn(0f, 1f)

    Pressable(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surface)
                .padding(16.dp),
        ) {
            Text(
                "CONTINUE READING",
                style = MaterialTheme.typography.labelSmall,
                color = c.goldDim,
            )
            Spacer(Modifier.height(9.dp))
            Text(saint.name, style = MaterialTheme.typography.titleLarge, color = c.text)
            if (saint.epithet.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    saint.epithet,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.faint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(13.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.rule),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(if (p < 0.02f) 0.02f else p)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(c.gold),
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                (p * 100f).toInt().toString() + "% read",
                style = MaterialTheme.typography.labelSmall,
                color = c.faint,
            )
        }
    }
}

/**
 * The saying of the day. It does not change when tapped - tapping copies it,
 * with the attribution, and says so for a moment.
 */
@Composable
fun QuoteCard(text: String, by: String, copied: Boolean, onCopy: () -> Unit) {
    val c = Syn.colors

    Pressable(onClick = onCopy, modifier = Modifier.fillMaxWidth(), down = 0.99f) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.surface)
                .border(1.dp, c.rule, RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Text(
                "\u201C" + text.trim() + "\u201D",
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                color = c.text,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "\u2014 " + by,
                    style = MaterialTheme.typography.labelLarge,
                    color = c.goldDim,
                )
                Text(
                    if (copied) "Copied" else "Tap to copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (copied) c.gold else c.faint,
                )
            }
        }
    }
}

/**
 * Today's fast, with the date on it.
 *
 * The stripe down the left is the severity, so the card can be read at a
 * glance from across the room: nothing, dairy, fish, oil, xerophagy, strict.
 * What may be eaten and what may not are lists in the model, and are set as
 * lists here.
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
    val tone = fastColor(rule.level)

    Pressable(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.surface),
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(intrinsicHeightGuess)
                    .background(tone),
            )
            Column(Modifier.weight(1f).padding(17.dp)) {

                Text(
                    civilLine.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.goldDim,
                )
                Spacer(Modifier.height(6.dp))
                Text(churchLine, style = MaterialTheme.typography.bodySmall, color = c.faint)

                Spacer(Modifier.height(13.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FastDot(rule.level, size = 9.dp)
                    Spacer(Modifier.width(9.dp))
                    Text(
                        rule.label,
                        style = MaterialTheme.typography.titleLarge,
                        color = c.text,
                    )
                }

                if (season.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(season, style = MaterialTheme.typography.bodySmall, color = c.dim)
                }

                if (rule.detail.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(rule.detail, style = MaterialTheme.typography.bodyMedium, color = c.dim)
                }

                if (rule.eat.isNotEmpty() || rule.avoid.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.rule))
                    Spacer(Modifier.height(13.dp))
                }

                if (rule.eat.isNotEmpty()) {
                    FastLine("EAT", rule.eat, c.gold)
                }
                if (rule.eat.isNotEmpty() && rule.avoid.isNotEmpty()) {
                    Spacer(Modifier.height(9.dp))
                }
                if (rule.avoid.isNotEmpty()) {
                    FastLine("AVOID", rule.avoid, c.blood)
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "Open the calendar",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.faint,
                )
            }
        }
    }
}

/** The stripe has no content of its own, so it borrows a sensible height. */
private val intrinsicHeightGuess = 1000.dp

@Composable
private fun FastLine(label: String, items: List<String>, tone: androidx.compose.ui.graphics.Color) {
    val c = Syn.colors
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = tone,
            modifier = Modifier.width(52.dp),
        )
        Text(
            items.joinToString(" \u00B7 "),
            style = MaterialTheme.typography.bodySmall,
            color = c.dim,
            modifier = Modifier.weight(1f),
        )
    }
}

/** A feast, in the calendar and under today's date. */
@Composable
fun FeastCard(feast: Feast, modifier: Modifier = Modifier) {
    val c = Syn.colors

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(if (feast.great) c.raised else c.surface)
            .then(
                if (feast.great) Modifier.border(1.dp, c.goldDim, RoundedCornerShape(13.dp))
                else Modifier
            )
            .padding(15.dp),
    ) {
        if (feast.great) {
            Text(
                "GREAT FEAST",
                style = MaterialTheme.typography.labelSmall,
                color = c.gold,
            )
            Spacer(Modifier.height(7.dp))
        }
        Text(
            feast.name,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = if (feast.great) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = c.text,
        )
        if (feast.note.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(feast.note, style = MaterialTheme.typography.bodySmall, color = c.dim)
        }
    }
}