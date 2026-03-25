package com.eatwhat.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeoOrange,
    secondary = NeoBlue,
    tertiary = NeoGreen,
    background = NeoBlack,
    surface = NeoBlack,
    onPrimary = NeoWhite,
    onSecondary = NeoWhite,
    onBackground = NeoWhite,
    onSurface = NeoWhite
)

private val LightColorScheme = lightColorScheme(
    primary = NeoOrange,
    secondary = NeoBlue,
    tertiary = NeoGreen,
    background = NeoYellow,
    surface = NeoWhite,
    onPrimary = NeoBlack,
    onSecondary = NeoBlack,
    onBackground = NeoBlack,
    onSurface = NeoBlack
)

@Composable
fun EatWhatTheme(
    darkTheme: Boolean = false, // Forced to false for consistent Neo-Brutalism look
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
