package com.goyimatica.synaxismobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goyimatica.synaxismobile.core.FastLevel
import com.goyimatica.synaxismobile.ui.Motion
import com.goyimatica.synaxismobile.ui.animColor
import com.goyimatica.synaxismobile.ui.pressScale
import com.goyimatica.synaxismobile.ui.rememberInteraction
import com.goyimatica.synaxismobile.ui.theme.Syn

/* ---- the cross ---------------------------------------------------------- */

@Composable
fun OrthodoxCross(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = Syn.colors.gold,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        fun x(v: Float) = w * v / 100f
        fun y(v: Float) = h * v / 100f

        val path = Path().apply {
            // upright
            moveTo(x(46f), y(8f)); lineTo(x(54f), y(8f))
            lineTo(x(54f), y(94f)); lineTo(x(46f), y(94f)); close()
            // titulus
            moveTo(x(32f), y(20f)); lineTo(x(68f), y(20f))
            lineTo(x(68f), y(27f)); lineTo(x(32f), y(27f)); close()
            // crossbar
            moveTo(x(16f), y(38f)); lineTo(x(84f), y(38f))
            lineTo(x(84f), y(47f)); lineTo(x(16f), y(47f)); close()
            // suppedaneum - high on the left, low on the right
            moveTo(x(24f), y(66f)); lineTo(x(76f), y(76f))
            lineTo(x(76f), y(84f)); lineTo(x(24f), y(74f)); close()
        }
        drawPath(path, color)
    }
}

/* ---- the header --------------------------------------------------------- */

@Composable
fun ScreenHeader(
    overline: String,
    title: String,
    subtitle: String? = null,
    onSettings: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = Syn.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                if (overline.isNotBlank()) {
                    Text(
                        text = overline.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = c.gold,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = c.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.dim,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                trailing?.invoke()
                if (onSettings != null) {
                    val press = rememberInteraction()
                    Box(
                        modifier = Modifier
                            .pressScale(press)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = press,
                                indication = null,
                                onClick = onSettings,
                            )
                            .padding(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = c.dim,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        HairRule()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Syn.colors.faint,
        modifier = modifier,
    )
}

@Composable
fun HairRule(modifier: Modifier = Modifier, color: Color = Syn.colors.rule) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

/* ---- a pressable surface ------------------------------------------------ */

@Composable
fun Pressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    down: Float = 0.972f,
    content: @Composable () -> Unit,
) {
    val press: MutableInteractionSource = rememberInteraction()
    Box(
        modifier = modifier
            .pressScale(press, down)
            .clickable(
                interactionSource = press,
                indication = null,
                onClick = onClick,
            ),
    ) {
        content()
    }
}

/* ---- a filter chip ------------------------------------------------------ */

@Composable
fun SynChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Syn.colors
    val bg = animColor(if (selected) c.gold else c.surface, Motion.fade(), "chipBg")
    val fg = animColor(if (selected) c.bg else c.dim, Motion.fade(), "chipFg")
    val edge = animColor(if (selected) c.gold else c.rule, Motion.fade(), "chipEdge")

    Pressable(onClick = onClick, modifier = modifier, down = 0.94f) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(bg)
                .border(1.dp, edge, RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/* ---- the fast, in one colour and one word ------------------------------- */

@Composable
fun fastColor(level: FastLevel): Color {
    val c = Syn.colors
    return when (level) {
        FastLevel.NONE -> Color(0xFF5E9C6A)
        FastLevel.DAIRY -> Color(0xFF8FA84F)
        FastLevel.FISH -> Color(0xFF5B85B8)
        FastLevel.OIL -> c.gold
        FastLevel.XEROPHAGY -> Color(0xFFB5773A)
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
fun FastDot(level: FastLevel, size: Dp = 8.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(fastColor(level)),
    )
}

/* ---- the medallion ------------------------------------------------------ */

@Composable
fun Medallion(initial: String, size: Dp = 44.dp) {
    val c = Syn.colors
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(c.raised)
            .border(1.dp, c.goldDim.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = c.gold,
        )
    }
}

@Composable
fun EmptyNote(text: String) {
    val c = Syn.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OrthodoxCross(size = 26.dp, color = c.faint)
        Spacer(Modifier.height(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = c.faint,
        )
    }
}

@Composable
fun TagStrip(tags: List<String>, modifier: Modifier = Modifier) {
    if (tags.isEmpty()) return
    val c = Syn.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tags.take(3).forEach { t ->
            Text(
                text = t,
                style = MaterialTheme.typography.labelSmall,
                color = c.dim,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(c.raised)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}