package com.goyimatica.synaxismobile.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goyimatica.synaxismobile.ui.theme.Syn

/*
 * Four specs, used everywhere, so the whole app moves with one hand.
 *
 *   quick    - a press, a tick, a colour. Critically damped, very stiff.
 *   spatial  - anything that travels: a sheet, a row sliding in.
 *   size     - anything that grows: the In Brief card opening.
 *   fade     - opacity only, where a spring would look like a flicker.
 */
object Motion {
    fun <T> quick(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1600f)

    fun <T> spatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.85f, stiffness = 380f)

    fun <T> size(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 520f)

    fun <T> fade(): FiniteAnimationSpec<T> =
        tween(durationMillis = 150, easing = LinearOutSlowInEasing)
}

/* Settings → Animations: Full, Reduced, None. At None every wrapper below
   snaps to its target, so "off" costs nothing rather than animating to zero. */
val animationsOn: Boolean
    @Composable get() = Syn.reading.animations > 0f

private val scale: Float
    @Composable get() = Syn.reading.animations.coerceIn(0f, 1f)

@Composable
fun animFloat(
    target: Float,
    spec: AnimationSpec<Float> = Motion.quick(),
    label: String = "float",
): Float =
    if (!animationsOn) target
    else animateFloatAsState(targetValue = target, animationSpec = spec, label = label).value

@Composable
fun animDp(
    target: Dp,
    spec: AnimationSpec<Dp> = Motion.size(),
    label: String = "dp",
): Dp =
    if (!animationsOn) target
    else animateDpAsState(targetValue = target, animationSpec = spec, label = label).value

@Composable
fun animColor(
    target: Color,
    spec: AnimationSpec<Color> = Motion.fade(),
    label: String = "colour",
): Color =
    if (!animationsOn) target
    else animateColorAsState(targetValue = target, animationSpec = spec, label = label).value

@Composable
fun rememberInteraction(): MutableInteractionSource =
    remember { MutableInteractionSource() }

/*
 * The press. Reads the interaction source, animates a scale, and applies it
 * in a graphicsLayer lambda - the lambda form, so the value is read during
 * the draw phase and a press never invalidates composition or layout.
 */
@Composable
fun Modifier.pressScale(
    interaction: InteractionSource,
    down: Float = 0.972f,
): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val amount = 1f - ((1f - down) * scale)
    val target = if (pressed) amount else 1f
    val s = animFloat(target, Motion.quick(), "press")
    return this.graphicsLayer {
        scaleX = s
        scaleY = s
    }
}

/* A one-shot entrance for a card or a row: fades and lifts eight dp.
   `index` staggers a list by twenty milliseconds a row, capped at six rows
   so a long list never feels like it is loading in slow motion. */
@Composable
fun Modifier.appearance(progress: Float): Modifier {
    val p = progress.coerceIn(0f, 1f)
    return this.graphicsLayer {
        alpha = p
        translationY = (1f - p) * 8.dp.toPx()
    }
}