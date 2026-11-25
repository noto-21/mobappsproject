package com.group6.whereit.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light theme: forest green primary, periwinkle secondary, light lavender background
private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = GreenPrimaryDark,
    secondary = Periwinkle,
    onSecondary = Color.White,
    secondaryContainer = PeriwinkleContainer,
    onSecondaryContainer = PeriwinkleOnContainer,
    tertiary = Periwinkle,
    onTertiary = Color.White,
    tertiaryContainer = PeriwinkleContainer,
    onTertiaryContainer = PeriwinkleOnContainer,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = BackgroundLight,
    onSurface = TextPrimary,
    surfaceVariant = GreenContainer,
    onSurfaceVariant = TextSecondary,
    // lavender family so sheets, menus and fields sit nicely on the lavender background
    surfaceContainerLowest = SurfaceWhite,
    surfaceContainerLow = Color(0xFFF0EDFA),
    surfaceContainer = Color(0xFFEBE8F7),
    surfaceContainerHigh = Color(0xFFE6E3F5),
    surfaceContainerHighest = SurfaceWhite, // item cards: white on lavender
    outline = Color(0xFF8A948E),
    outlineVariant = DividerSubtle
)

// Dark theme: sage green primary, soft periwinkle secondary, charcoal with a green undertone
private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimaryNight,
    onPrimary = GreenOnPrimaryNight,
    primaryContainer = GreenContainerNight,
    onPrimaryContainer = GreenOnContainerNight,
    secondary = PeriwinkleNight,
    onSecondary = PeriwinkleOnNight,
    secondaryContainer = PeriwinkleContainerNight,
    onSecondaryContainer = PeriwinkleOnContainerNight,
    tertiary = PeriwinkleNight,
    onTertiary = PeriwinkleOnNight,
    tertiaryContainer = PeriwinkleContainerNight,
    onTertiaryContainer = PeriwinkleOnContainerNight,
    background = BackgroundNight,
    onBackground = TextPrimaryNight,
    surface = BackgroundNight,
    onSurface = TextPrimaryNight,
    surfaceVariant = DividerNight,
    onSurfaceVariant = TextSecondaryNight,
    surfaceContainerLowest = Color(0xFF0D100E),
    surfaceContainerLow = Color(0xFF181C19),
    surfaceContainer = Color(0xFF1C211E),
    surfaceContainerHigh = Color(0xFF232925),
    surfaceContainerHighest = Color(0xFF2A302C), // item cards
    outline = Color(0xFF6B756F),
    outlineVariant = DividerNight
)

@Composable
fun WhereItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic colour (Android 12+) pulls colours from the phone's wallpaper and
    // overrides the brand colours, so it's off. The parameter is kept so existing
    // calls still compile; it is ignored.
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
