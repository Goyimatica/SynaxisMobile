package com.goyimatica.synaxismobile.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goyimatica.synaxismobile.core.FastRule
import com.goyimatica.synaxismobile.core.Feast
import com.goyimatica.synaxismobile.data.Quote
import com.goyimatica.synaxismobile.data.Saint
import com.goyimatica.synaxismobile.ui.theme.Syn

/**
 * One life in a list. The second line is deliberately not the epithet alone:
 * on a list of 238 people, when they lived and where matters more for telling
 * two Johns apart than a title does.
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
    Pressable(onClick = onClick, modifier = modifier.fillMaxWidth(), contentPadding = 14) {
        Row(verticalAlignment = Alignment.Top) {
            Medallion(saint.initial)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                if (!overline.isNullOrBlank()) {
                    Text(overline.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.gold)
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    saint.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (saint.epithet.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        saint.epithet,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.dim,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val line = listOfNotNull(
                    saint.feastText().ifBlank { null },
                    saint.century?.let { it + " century" },
                    saint.jurisdiction.ifBlank { null },
                ).joinToString("  ·  ")
                if (line.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(line, style = MaterialTheme.typography.bodySmall, color = c.faint)
                }
                if (saint.tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    TagStrip(saint.tags)
                }
            }
            if (bookmarked) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Bookmark,
                    contentDescription = "Bookmarked",
                    tint = c.gold,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** A life you have already opened, with how far down you got. */
@Composable
fun ContinueCard(
    saint: Saint,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Syn.colors
    Pressable(onClick = onClick, modifier = modifier.fillMaxWidth(), contentPadding = 14) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Medallion(saint.initial, diameter = 38)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    saint.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                /* A two-box bar rather than a progress indicator: no animation
                   to fight, and it takes the theme's own colours. */
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(c.rule),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(c.goldDim),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                (progress.coerceIn(0f, 1f) * 100).toInt().toString() + "%",
                style = MaterialTheme.typography.labelSmall,
                color = c.faint,
            )
        }
    }
}

/** The daily saying. Tapping it draws another, which never repeats the one
 *  showing - the same behaviour the web card had. */
@Composable
fun QuoteCard(quote: Quote, onAnother: () -> Unit, modifier: Modifier = Modifier) {
    val c = Syn.colors
    Pressable(
        onClick = onAnother,
        modifier = modifier.fillMaxWidth(),
        background = c.raised,
        contentPadding = 20,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("A saying for today")
            Icon(
                Icons.Outlined.Refresh,
                contentDescription = "Another saying",
                tint = c.faint,
                modifier = Modifier.size(15.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = quote.text,
            style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic),
            color = c.text,
        )
        Spacer(Modifier.height(14.dp))
        Text("— " + quote.by, style = MaterialTheme.typography.bodySmall, color = c.goldDim)
    }
}

/** The plate: what the day asks for, and what may be eaten. */
@Composable
fun FastCard(fast: FastRule, modifier: Modifier = Modifier, compact: Boolean = false) {
    val c = Syn.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .background(c.surface)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FastDot(fast.level, size = 8)
            Spacer(Modifier.width(9.dp))
            Text(
                if (fast.label.isBlank()) fastWord(fast.level) else fast.label,
                style = MaterialTheme.typography.titleLarge,
                color = fastColor(fast.level),
            )
        }
        if (fast.detail.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(fast.detail, style = MaterialTheme.typography.bodyMedium, color = c.dim)
        }
        if (!compact) {
            if (fast.eat.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionLabel("Eat")
                Spacer(Modifier.height(7.dp))
                fast.eat.forEach { line ->
                    Row(Modifier.padding(bottom = 4.dp)) {
                        Text("·", style = MaterialTheme.typography.bodyMedium, color = c.goldDim)
                        Spacer(Modifier.width(9.dp))
                        Text(line, style = MaterialTheme.typography.bodyMedium, color = c.text)
                    }
                }
            }
            if (fast.avoid.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                SectionLabel("Set aside")
                Spacer(Modifier.height(7.dp))
                fast.avoid.forEach { line ->
                    Row(Modifier.padding(bottom = 4.dp)) {
                        Text("·", style = MaterialTheme.typography.bodyMedium, color = c.faint)
                        Spacer(Modifier.width(9.dp))
                        Text(line, style = MaterialTheme.typography.bodyMedium, color = c.dim)
                    }
                }
            }
        }
    }
}

/** A commemoration. Gold when it is one of the Great Feasts. */
@Composable
fun FeastCard(feast: Feast, modifier: Modifier = Modifier) {
    val c = Syn.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .background(if (feast.great) c.raised else c.surface)
            .padding(16.dp),
    ) {
        if (feast.great) {
            SectionLabel("A Great Feast")
            Spacer(Modifier.height(7.dp))
        }
        Text(
            feast.name,
            style = MaterialTheme.typography.headlineSmall,
            color = if (feast.great) c.gold else c.text,
        )
        if (feast.note.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            Text(feast.note, style = MaterialTheme.typography.bodyMedium, color = c.dim)
        }
    }
}