package com.simon.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Blue,
    secondary = Purple,
    tertiary = LightBlue,
    background = DarkBackground,
    surface = MediumBackground,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = WhiteAlpha70,
    onSurface = WhiteAlpha70
)

@Composable
fun SimonTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set system bar appearance for dark theme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            // Make status bar transparent and draw content behind it
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}