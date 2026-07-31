package com.goyimatica.synaxismobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goyimatica.synaxismobile.core.FastLevel
import com.goyimatica.synaxismobile.ui.Motion
import com.goyimatica.synaxismobile.ui.animColor
import com.goyimatica.synaxismobile.ui.animDp
import com.goyimatica.synaxismobile.ui.pressScale
import com.goyimatica.synaxismobile.ui.rememberInteraction
import com.goyimatica.synaxismobile.ui.theme.Syn

/**
 * The Russian cross, drawn rather than shipped as a vector, so it takes the
 * palette's gold and any size without a second asset.
 *
 * Three bars: the titulus above, the crossbar, and the slanted footrest whose
 * left end - Christ's right, the side of the repentant thief - is the high
 * one. Coordinates are in a hundred-unit square and scaled.
 */
@Composable
fun OrthodoxCross(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = Syn.colors.gold,
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 100f
        fun rect(x: Float, y: Float, w: Float, h: Float) =
            drawRect(color, Offset(x * s, y * s), Size(w * s, h * s))

        rect(48.5f, 20f, 5f, 66f)   // the shaft
        rect(41f, 30f, 20f, 4.5f)   // the titulus
        rect(31f, 44f, 40f, 5f)     // the crossbar

        val foot = Path().apply {
            moveTo(35f * s, 62f * s)
            lineTo(67f * s, 70f * s)
            lineTo(67f * s, 75f * s)
            lineTo(35f * s, 67f * s)
            close()
        }
        drawPath(foot, color)
    }
}

/**
 * Every tab opens with this, which is why the gear lives here rather than in
 * one screen. `trailing` is for anything a particular screen needs beside it.
 */
@Composable
fun ScreenHeader(
    overline: String,
    title: String,
    subtitle: String? = null,
    onSettings: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = Syn.colors
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(overline.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.gold)
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge, color = c.text)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.dim)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            trailing?.invoke()
            if (onSettings != null) {
                if (trailing != null) Spacer(Modifier.width(16.dp))
                val press = rememberInteraction()
                Icon(
                    Icons.Outlined.Settings,
                    "Settings",
                    tint = c.dim,
                    modifier = Modifier
                        .size(23.dp)
                        .pressScale(press, down = 0.86f)
                        .clickable(
                            interactionSource = press,
                            indication = null,
                            onClick = onSettings,
                        ),
                )
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Syn.colors.faint,
    )
}

@Composable
fun HairRule(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Syn.colors.rule))
}

/** The one press in the app. Anything tappable that is not an icon uses it. */
@Composable
fun Pressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    down: Float = 0.972f,
    content: @Composable () -> Unit,
) {
    val press = rememberInteraction()
    Box(
        modifier
            .pressScale(press, down)
            .clickable(interactionSource = press, indication = null, onClick = onClick),
    ) { content() }
}

/**
 * A chip that changes shape as well as colour when it is chosen. The corner
 * radius travelling from a full round to something squarer is the whole trick
 * behind the new Material buttons, and a spring on a Dp gets it exactly.
 */
@Composable
fun SynChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Syn.colors
    val press = rememberInteraction()

    val radius by animDp(if (selected) 11.dp else 22.dp, Motion.spatial())
    val fill by animColor(if (selected) c.gold else c.surface)
    val ink by animColor(if (selected) c.bg else c.dim)
    val edge by animColor(if (selected) c.gold else c.rule)
    val shape = RoundedCornerShape(radius)

    Box(
        modifier
            .pressScale(press, down = 0.94f)
            .clip(shape)
            .background(fill)
            .border(1.dp, edge, shape)
            .clickable(interactionSource = press, indication = null, onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = ink)
    }
}

// ---------------------------------------------------------------- fasting

@Composable
fun fastColor(level: FastLevel): Color {
    val c = Syn.colors
    return when (level) {
        FastLevel.NONE -> c.faint
        FastLevel.DAIRY -> Color(0xFF8FB08A)
        FastLevel.FISH -> Color(0xFF6E9BC5)
        FastLevel.OIL -> c.gold
        FastLevel.XEROPHAGY -> Color(0xFFC98B4B)
        FastLevel.STRICT -> c.blood
    }
}

fun fastWord(level: FastLevel): String = when (level) {
    FastLevel.NONE -> "Fast-free"
    FastLevel.DAIRY -> "Dairy allowed"
    FastLevel.FISH -> "Fish allowed"
    FastLevel.OIL -> "Wine and oil"
    FastLevel.XEROPHAGY -> "Xerophagy"
    FastLevel.STRICT -> "Strict fast"
}

@Composable
fun FastDot(level: FastLevel, size: Dp = 9.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(fastColor(level)),
    )
}

/** The gold ring with an initial in it, standing in for an icon we have not
 *  downloaded yet. */
@Composable
fun Medallion(initial: String, size: Dp = 44.dp) {
    val c = Syn.colors
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(c.raised)
            .border(1.dp, c.goldDim, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initial.take(1).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            color = c.goldDim,
        )
    }
}

@Composable
fun EmptyNote(text: String) {
    Box(
        Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = Syn.colors.faint,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun TagStrip(tags: List<String>, modifier: Modifier = Modifier) {
    if (tags.isEmpty()) return
    val c = Syn.colors
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(tags, key = { it }) { t ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(c.raised)
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text(t, style = MaterialTheme.typography.labelSmall, color = c.faint)
            }
        }
    }
}