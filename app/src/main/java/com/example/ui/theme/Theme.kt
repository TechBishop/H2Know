package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkMaterialColors = darkColorScheme(
    primary = DarkH2KnowPalette.brand,
    onPrimary = Color(0xFF06131F),
    secondary = DarkH2KnowPalette.safe,
    tertiary = DarkH2KnowPalette.caution,
    error = DarkH2KnowPalette.danger,
    background = DarkH2KnowPalette.surface0,
    onBackground = DarkH2KnowPalette.ink,
    surface = DarkH2KnowPalette.surface1,
    onSurface = DarkH2KnowPalette.ink,
    surfaceVariant = DarkH2KnowPalette.surface2,
    onSurfaceVariant = DarkH2KnowPalette.ink2,
    outline = DarkH2KnowPalette.border
)

private val LightMaterialColors = lightColorScheme(
    primary = LightH2KnowPalette.brand,
    onPrimary = Color.White,
    secondary = LightH2KnowPalette.safe,
    tertiary = LightH2KnowPalette.caution,
    error = LightH2KnowPalette.danger,
    background = LightH2KnowPalette.surface0,
    onBackground = LightH2KnowPalette.ink,
    surface = LightH2KnowPalette.surface1,
    onSurface = LightH2KnowPalette.ink,
    surfaceVariant = LightH2KnowPalette.surface2,
    onSurfaceVariant = LightH2KnowPalette.ink2,
    outline = LightH2KnowPalette.border
)

@Composable
fun H2KnowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkH2KnowPalette else LightH2KnowPalette
    val colorScheme = if (darkTheme) DarkMaterialColors else LightMaterialColors

    CompositionLocalProvider(LocalH2KnowPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object H2KnowThemeTokens {
    val colors: H2KnowPalette
        @Composable get() = LocalH2KnowPalette.current
}
