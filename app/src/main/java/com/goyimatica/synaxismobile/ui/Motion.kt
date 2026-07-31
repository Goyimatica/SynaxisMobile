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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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

/* Settings → Animations: Full, Reduced, None. */
val animationsOn: Boolean
    @Composable get() = Syn.reading.animations > 0f

private val motionScale: Float
    @Composable get() = Syn.reading.animations.coerceIn(0f, 1f)

/*
 * These three return State, not a bare value, so every call site can use the
 * delegate form:
 *
 *     val alpha by animFloat(if (open) 1f else 0f)
 *
 * With animations turned off they return a State that always holds the
 * target, which snaps instantly and allocates nothing per frame.
 */
@Composable
fun animFloat(
    target: Float,
    spec: AnimationSpec<Float> = Motion.quick(),
    label: String = "float",
): State<Float> =
    if (!animationsOn) rememberUpdatedState(target)
    else animateFloatAsState(targetValue = target, animationSpec = spec, label = label)

@Composable
fun animDp(
    target: Dp,
    spec: AnimationSpec<Dp> = Motion.size(),
    label: String = "dp",
): State<Dp> =
    if (!animationsOn) rememberUpdatedState(target)
    else animateDpAsState(targetValue = target, animationSpec = spec, label = label)

@Composable
fun animColor(
    target: Color,
    spec: AnimationSpec<Color> = Motion.fade(),
    label: String = "colour",
): State<Color> =
    if (!animationsOn) rememberUpdatedState(target)
    else animateColorAsState(targetValue = target, animationSpec = spec, label = label)

@Composable
fun rememberInteraction(): MutableInteractionSource =
    remember { MutableInteractionSource() }

/*
 * The press. The scale is read inside the graphicsLayer lambda, which means
 * it is read during the draw phase - a press redraws one layer and never
 * invalidates composition or layout. That is what keeps a list at full rate
 * while something on it is being pressed.
 */
@Composable
fun Modifier.pressScale(
    interaction: InteractionSource,
    down: Float = 0.972f,
): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val amount = 1f - ((1f - down) * motionScale)
    val scale by animFloat(if (pressed) amount else 1f, Motion.quick(), "press")
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/* A one-shot entrance for a card or a row: fades and lifts eight dp. */
@Composable
fun Modifier.appearance(progress: Float): Modifier {
    val p = progress.coerceIn(0f, 1f)
    return this.graphicsLayer {
        alpha = p
        translationY = (1f - p) * 8.dp.toPx()
    }
}