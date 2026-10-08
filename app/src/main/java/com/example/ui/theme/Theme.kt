package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = GamePurple,
    onPrimary = Color.White,
    secondary = GameAccent,
    onSecondary = Color.White,
    background = LavenderBg,
    onBackground = GameTextDark,
    surface = Color.White,
    onSurface = GameTextDark,
    surfaceVariant = LavenderCard,
    onSurfaceVariant = GameTextDark
)

@Composable
fun FaceShooterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
