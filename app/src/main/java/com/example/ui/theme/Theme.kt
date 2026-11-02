package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AthleticDarkColorScheme = darkColorScheme(
    primary = AthleticOrange,
    onPrimary = Color(0xFF0B0E14),
    primaryContainer = AthleticOrangeContainer,
    onPrimaryContainer = AthleticOrangeLight,
    secondary = NeonVoltGreen,
    onSecondary = Color(0xFF042217),
    secondaryContainer = NeonVoltContainer,
    onSecondaryContainer = NeonVoltGreen,
    tertiary = ElectricCyan,
    onTertiary = Color(0xFF002530),
    tertiaryContainer = ElectricCyanContainer,
    onTertiaryContainer = ElectricCyan,
    background = ObsidianBackground,
    onBackground = TextPrimaryWhite,
    surface = GraphiteSurface,
    onSurface = TextPrimaryWhite,
    surfaceVariant = GraphiteCard,
    onSurfaceVariant = TextSecondarySlate,
    outline = GraphiteBorder,
    error = CoralAlertRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AthleticDarkColorScheme,
        typography = Typography,
        content = content
    )
}
