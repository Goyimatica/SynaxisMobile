package com.goyimatica.synaxismobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goyimatica.synaxismobile.core.FastLevel
import com.goyimatica.synaxismobile.ui.theme.Syn

/**
 * The eight-pointed Russian cross, drawn.
 *
 * Three bars: the titulus that carried the inscription, the crossbar, and the
 * suppedaneum - the footrest, which slants. Its raised end is on the viewer's
 * LEFT, the side of the thief who repented and was promised paradise; the
 * lowered end points to the other. Getting that backwards inverts the meaning,
 * which is why it is written down here.
 */
@Composable
fun OrthodoxCross(
    modifier: Modifier = Modifier,
    color: Color = Syn.colors.gold,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val t = (w * 0.115f).coerceAtLeast(1.4f)   // bar thickness
        val cx = w / 2f

        // upright
        drawRect(color, Offset(cx - t / 2f, 0f), Size(t, h))

        // titulus - short, high
        val ty = h * 0.15f
        drawRect(color, Offset(cx - w * 0.20f, ty), Size(w * 0.40f, t * 0.85f))

        // crossbar - the long one
        val my = h * 0.35f
        drawRect(color, Offset(cx - w * 0.40f, my), Size(w * 0.80f, t))

        // suppedaneum - raised on the viewer's left
        val fy = h * 0.72f
        val dx = w * 0.26f
        val rise = h * 0.075f
        val foot = Path().apply {
            moveTo(cx - dx, fy - rise)
            lineTo(cx + dx, fy + rise)
            lineTo(cx + dx, fy + rise + t)
            lineTo(cx - dx, fy - rise + t)
            close()
        }
        drawPath(foot, color)
    }
}

/** The kicker + title + subtitle every screen opens with. No back arrow: these
 *  are tabs, not pushed screens, and a back arrow on a home screen is a lie. */
@Composable
fun ScreenHeader(
    kicker: String,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    val c = Syn.colors
    Column(modifier.fillMaxWidth()) {
        Text(kicker.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.gold)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge, color = c.text)
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.dim)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Syn.colors.faint,
        modifier = modifier,
    )
}

@Composable
fun HairRule(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Syn.colors.rule),
    )
}

/**
 * A card that answers to a press. The scale is small on purpose - 2.5% is felt
 * rather than seen - and it is switched off entirely when the animations
 * setting is off, which some people need rather than merely prefer.
 */
@Composable
fun Pressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    background: Color = Syn.colors.surface,
    outlined: Boolean = false,
    contentPadding: Int = 16,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Syn.colors
    val lively = Syn.reading.animations > 0f
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && lively) 0.975f else 1f,
        animationSpec = tween(durationMillis = if (lively) 110 else 0),
        label = "press",
    )

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(background)
            .then(if (outlined) Modifier.border(1.dp, c.rule, shape) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(contentPadding.dp),
        content = content,
    )
}

/** A filter pill. Gold when chosen, a hairline when not. */
@Composable
fun SynChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Syn.colors
    val lively = Syn.reading.animations > 0f
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && lively) 0.94f else 1f,
        animationSpec = tween(durationMillis = if (lively) 100 else 0),
        label = "chip",
    )
    val shape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(if (selected) c.gold else Color.Transparent)
            .border(1.dp, if (selected) c.gold else c.rule, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) (if (c.isDark) Color(0xFF14100E) else Color(0xFFFFFDF8)) else c.dim,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

/**
 * One colour per degree of fasting, used by the calendar grid and the dots.
 * Deliberately not six shades of one hue: at a glance you need to tell a fish
 * day from an oil day without reading anything.
 */
@Composable
fun fastColor(level: FastLevel): Color {
    val c = Syn.colors
    return when (level) {
        FastLevel.NONE -> c.faint
        FastLevel.DAIRY -> Color(0xFF7FA86B)
        FastLevel.FISH -> Color(0xFF5B85B8)
        FastLevel.OIL -> c.gold
        FastLevel.XEROPHAGY -> Color(0xFFB5794A)
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
fun FastDot(level: FastLevel, modifier: Modifier = Modifier, size: Int = 6) {
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(fastColor(level)),
    )
}

/** The round letter that stands in for a portrait until one is downloaded. */
@Composable
fun Medallion(letter: Char, modifier: Modifier = Modifier, diameter: Int = 44) {
    val c = Syn.colors
    Box(
        modifier
            .size(diameter.dp)
            .clip(CircleShape)
            .background(c.raised)
            .border(1.dp, c.rule, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letter.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = c.goldDim,
        )
    }
}

@Composable
fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OrthodoxCross(Modifier.size(22.dp, 34.dp), Syn.colors.faint)
        Spacer(Modifier.height(14.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Syn.colors.faint)
    }
}

/** A row of small tag pills, used on the saint cards. */
@Composable
fun TagStrip(tags: List<String>, modifier: Modifier = Modifier, max: Int = 3) {
    val c = Syn.colors
    if (tags.isEmpty()) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        tags.take(max).forEach { t ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(c.raised)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
                Text(t, style = MaterialTheme.typography.labelSmall, color = c.dim)
            }
        }
        if (tags.size > max) {
            Spacer(Modifier.width(2.dp))
            Text("+" + (tags.size - max), style = MaterialTheme.typography.labelSmall, color = c.faint)
        }
    }
}