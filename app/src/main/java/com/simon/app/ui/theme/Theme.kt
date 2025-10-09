package com.simon.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val DarkBackground = Color(0xFF0F0F1E)
val MediumBackground = Color(0xFF1A1A2E)
val WhiteAlpha70 = Color(0xB3FFFFFF)

private val DarkColorScheme = darkColorScheme(
    background = DarkBackground,
    surface = MediumBackground,
    onBackground = WhiteAlpha70,
    onSurface = WhiteAlpha70
)

@Composable
fun SimonTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
