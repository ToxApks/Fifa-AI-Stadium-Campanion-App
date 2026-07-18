package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import com.example.data.AppState

private val FIFAStadiumColorScheme = darkColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    tertiary = TertiaryColor,
    background = DeepNavyBg,
    surface = CardGlassBg,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = DarkText,
    onSurface = DarkText
)

private val FIFAStadiumLightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    secondary = Color(0xFF131315),
    tertiary = TertiaryColor,
    background = Color(0xFFF3F4F6),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF131315),
    onSurface = Color(0xFF131315)
)

// In High Contrast accessibility mode, we transition to pure high-contrast black and neon borders for maximum legibility.
private val HighContrastColorScheme = darkColorScheme(
    primary = Color(0xFF00FFFF), // High contrast neon cyan
    secondary = Color(0xFFFFFF00), // High contrast yellow
    tertiary = Color(0xFF00FF00), // Pure green
    background = Color(0xFF000000), // Absolute black for OLED and screen-reader contrast
    surface = Color(0xFF111111),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val isHighContrast = AppState.highContrastMode.collectAsState().value
    val isDarkMode = AppState.isDarkMode.collectAsState().value
    val colors = if (isHighContrast) {
        HighContrastColorScheme
    } else if (isDarkMode) {
        FIFAStadiumColorScheme
    } else {
        FIFAStadiumLightColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
