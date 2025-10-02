package com.simon.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp

private val Orange = Color(0xFFFF9500)
private val Green = Color(0xFF4CD964)
private val DarkGreen = Color(0xFF2E7D32)
private val LightGreen = Color(0xFF66BB6A)
private val Blue = Color(0xFF007AFF)

@Composable
fun AudioVisualizer(
    isConnected: Boolean,
    isUserSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val innerCircleTargetColor = if (!isConnected) Orange else Green

    val innerCircleColor by animateColorAsState(
        targetValue = innerCircleTargetColor,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "inner_color"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "speaking_breath")
    val breathingProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val outerRadius = size.minDimension / 3
            val innerRadius = outerRadius * 0.65f

            val innerColor = if (isUserSpeaking && isConnected) {
                lerp(DarkGreen, LightGreen, breathingProgress)
            } else {
                innerCircleColor
            }

            drawGlowEffect(
                isConnected = isConnected,
                isUserSpeaking = isUserSpeaking,
                innerColor = innerColor,
                breathingProgress = breathingProgress,
                radius = outerRadius * 1.3f
            )

            drawOuterBlueRing(radius = outerRadius)
            drawInnerStatusCircle(color = innerColor, radius = innerRadius)
        }
    }
}

private fun DrawScope.drawGlowEffect(
    isConnected: Boolean,
    isUserSpeaking: Boolean,
    innerColor: Color,
    breathingProgress: Float,
    radius: Float
) {
    if (!isConnected) return

    val glowColor = if (isUserSpeaking) {
        lerp(Green, LightGreen, breathingProgress)
    } else {
        innerColor
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                glowColor.copy(alpha = 0.2f),
                glowColor.copy(alpha = 0.05f),
                Color.Transparent
            ),
            radius = radius,
            center = center
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawOuterBlueRing(radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Blue.copy(alpha = 0.7f),
                Blue.copy(alpha = 0.9f)
            ),
            radius = radius,
            center = center
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawInnerStatusCircle(color: Color, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color,
                color.copy(alpha = 0.95f),
                color.copy(alpha = 0.9f)
            ),
            radius = radius,
            center = center
        ),
        radius = radius,
        center = center
    )
}