package com.goyimatica.synaxismobile.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.animateColorAsState
import com.goyimatica.synaxismobile.ui.theme.Syn

/**
 * Four springs, and nothing else in the app is allowed to invent a fifth.
 *
 * They are springs rather than durations on purpose: a spring can be given a
 * new target mid-flight and will carry its velocity into it, so a tap during
 * an animation never produces the little stutter a tween does.
 */
object Motion {

    /** Chips, presses, small colour changes. Quick, barely overshoots. */
    fun <T> quick(): SpringSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = 900f)

    /** Cards moving, sheets sliding, the calendar grid. A little life in it. */
    fun <T> spatial(): SpringSpec<T> =
        spring(dampingRatio = 0.78f, stiffness = 380f)

    /** Anything that changes height. Never overshoots - overshooting height
     *  reflows the text underneath and looks like a fault. */
    fun <T> size(): SpringSpec<T> =
        spring(dampingRatio = 1f, stiffness = 300f)

    /** Opacity. Critically damped, slowish, reads as a fade rather than a pop. */
    fun <T> fade(): SpringSpec<T> =
        spring(dampingRatio = 1f, stiffness = 260f)
}

/** True when the user has left animations on. */
val animationsOn: Boolean
    @Composable get() = Syn.reading.animations > 0f

@Composable
private fun <T> honour(spec: SpringSpec<T>): AnimationSpec<T> =
    if (animationsOn) spec else snap()

@Composable
fun animFloat(target: Float, spec: SpringSpec<Float> = Motion.quick()): State<Float> =
    animateFloatAsState(targetValue = target, animationSpec = honour(spec), label = "f")

@Composable
fun animDp(target: Dp, spec: SpringSpec<Dp> = Motion.spatial()): State<Dp> =
    animateDpAsState(targetValue = target, animationSpec = honour(spec), label = "dp")

@Composable
fun animColor(target: Color, spec: SpringSpec<Color> = Motion.quick()): State<Color> =
    animateColorAsState(targetValue = target, animationSpec = honour(spec), label = "c")

/**
 * The press. Everything tappable in the app gets this and nothing else, which
 * is what makes a set of unrelated widgets feel like one piece of software.
 *
 * Scale rather than a ripple: a ripple has to draw and clip, this is a layer
 * transform the GPU does for free, and it survives being interrupted.
 */
@Composable
fun Modifier.pressScale(
    interaction: MutableInteractionSource,
    down: Float = 0.972f,
): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animFloat(if (pressed) down else 1f, Motion.quick())
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun rememberInteraction(): MutableInteractionSource =
    remember { MutableInteractionSource() }