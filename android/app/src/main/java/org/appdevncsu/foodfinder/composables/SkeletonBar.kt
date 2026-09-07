package org.appdevncsu.foodfinder.composables

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private const val SkeletonPulseDurationMillis = 600
private const val SkeletonPulseMinAlpha = 0.25f
private const val SkeletonPulseMaxAlpha = 0.65f
private val SkeletonBaseColor = Color.Gray
internal val SkeletonBarShape = RoundedCornerShape(4.dp)

@Composable
fun SkeletonBar(modifier: Modifier = Modifier, shape: Shape = SkeletonBarShape) {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = SkeletonPulseMinAlpha,
        targetValue = SkeletonPulseMaxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SkeletonPulseDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonPulse"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(SkeletonBaseColor.copy(alpha = pulse))
    )
}
