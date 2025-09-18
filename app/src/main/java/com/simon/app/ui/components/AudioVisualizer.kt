package com.simon.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.simon.app.ui.theme.*
import kotlin.math.sin

@Composable
fun AudioVisualizer(
    isConnected: Boolean,
    isUserSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    // Define colors
    val orange = Color(0xFFFF9500)
    val green = Color(0xFF4CD964)
    val darkGreen = Color(0xFF2E7D32)
    val lightGreen = Color(0xFF66BB6A)

    // Determine inner circle color based on state
    val innerCircleTargetColor = when {
        !isConnected -> orange  // Initializing
        else -> green  // Ready and listening (or assistant speaking)
    }

    // Animate base color transitions
    val innerCircleColor by animateColorAsState(
        targetValue = innerCircleTargetColor,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "inner_color"
    )

    // Smooth breathing animation for user speaking
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
            val centerX = size.width / 2
            val centerY = size.height / 2
            val outerRadius = size.minDimension / 3
            val innerRadius = outerRadius * 0.65f

            // Draw subtle glow effect
            if (isConnected) {
                val glowColor = if (isUserSpeaking) {
                    lerp(green, lightGreen, breathingProgress)
                } else {
                    innerCircleColor
                }

                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.2f),
                            glowColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        radius = outerRadius * 1.3f,
                        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                    ),
                    radius = outerRadius * 1.3f,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                )
            }

            // Draw outer filled circle (blue background)
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        Blue.copy(alpha = 0.7f),
                        Blue.copy(alpha = 0.9f)
                    ),
                    radius = outerRadius,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                ),
                radius = outerRadius,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )

            // Determine final inner circle color with smooth transitions
            val finalInnerColor = if (isUserSpeaking && isConnected) {
                // Smooth color interpolation using sine wave for natural breathing effect
                val smoothProgress = (sin(breathingProgress * Math.PI) * 0.5 + 0.5).toFloat()
                lerp(darkGreen, lightGreen, smoothProgress)
            } else {
                innerCircleColor
            }

            // Draw inner circle with gradient
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        finalInnerColor,
                        finalInnerColor.copy(alpha = 0.95f),
                        finalInnerColor.copy(alpha = 0.9f)
                    ),
                    radius = innerRadius,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                ),
                radius = innerRadius,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )
        }
    }
}