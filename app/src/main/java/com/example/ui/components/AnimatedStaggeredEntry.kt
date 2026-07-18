package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

@Composable
fun AnimatedStaggeredEntry(
    index: Int,
    delayUnitMs: Int = 70,
    durationMs: Int = 450,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val translationY = remember { Animatable(30f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * delayUnitMs).toLong())
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs, easing = EaseOutQuart)
            )
        }
        launch {
            translationY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = durationMs, easing = EaseOutQuart)
            )
        }
    }

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha.value
            this.translationY = translationY.value
        }
    ) {
        content()
    }
}
