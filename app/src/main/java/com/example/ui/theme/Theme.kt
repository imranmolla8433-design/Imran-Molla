package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkNavyColorScheme = darkColorScheme(
    primary = RoyalBluePrimary,
    onPrimary = TextPrimaryWhite,
    primaryContainer = NavySurfaceElevated,
    onPrimaryContainer = CyanAccent,
    secondary = CyanAccent,
    onSecondary = NavyBackground,
    secondaryContainer = NavySurfaceHighlight,
    onSecondaryContainer = TextPrimaryWhite,
    tertiary = PaidEmerald,
    onTertiary = NavyBackground,
    tertiaryContainer = PaidEmeraldContainer,
    onTertiaryContainer = PaidEmeraldLight,
    error = DueCrimson,
    onError = TextPrimaryWhite,
    errorContainer = DueCrimsonContainer,
    onErrorContainer = DueCrimsonLight,
    background = NavyBackground,
    onBackground = TextPrimaryWhite,
    surface = NavySurface,
    onSurface = TextPrimaryWhite,
    surfaceVariant = NavySurfaceElevated,
    onSurfaceVariant = TextSecondarySlate,
    outline = NavyBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkNavyColorScheme,
        typography = Typography,
        content = content
    )
}
