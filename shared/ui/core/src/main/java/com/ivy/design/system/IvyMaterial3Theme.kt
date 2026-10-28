package com.ivy.design.system

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.ivy.design.system.colors.IvyColors

@Composable
fun IvyMaterial3Theme(
    isTrueBlack: Boolean,
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (dark) ivyDarkColorScheme(isTrueBlack) else ivyLightColorScheme(),
        content = content,
    )
}

// Built with lightColorScheme/darkColorScheme so no role is left Color.Unspecified:
// Material3 1.3+ blends surfaceContainer into TopAppBar/Switch colors, and an unspecified
// role there yields an invalid color space ("Invalid ID") and crashes drawing.
private fun ivyLightColorScheme(): ColorScheme = lightColorScheme(
    primary = IvyColors.Purple.primary,
    onPrimary = IvyColors.White,
    primaryContainer = IvyColors.Purple.light,
    onPrimaryContainer = IvyColors.White,
    inversePrimary = IvyColors.Purple.dark,
    secondary = IvyColors.Green.primary,
    onSecondary = IvyColors.White,
    secondaryContainer = IvyColors.Green.light,
    onSecondaryContainer = IvyColors.White,
    tertiary = IvyColors.Green.primary,
    onTertiary = IvyColors.White,
    tertiaryContainer = IvyColors.Green.light,
    onTertiaryContainer = IvyColors.White,

    error = IvyColors.Red.primary,
    onError = IvyColors.White,
    errorContainer = IvyColors.Red.light,
    onErrorContainer = IvyColors.White,

    background = IvyColors.White,
    onBackground = IvyColors.Black,
    surface = IvyColors.White,
    onSurface = IvyColors.Black,
    surfaceVariant = IvyColors.ExtraLightGray,
    onSurfaceVariant = IvyColors.Black,
    surfaceTint = IvyColors.Black,
    inverseSurface = IvyColors.DarkGray,
    inverseOnSurface = IvyColors.White,

    outline = IvyColors.Gray,
    outlineVariant = IvyColors.DarkGray,
    scrim = IvyColors.ExtraDarkGray.copy(alpha = 0.8f),

    surfaceBright = IvyColors.White,
    surfaceDim = IvyColors.White,
    surfaceContainer = IvyColors.White,
    surfaceContainerHigh = IvyColors.White,
    surfaceContainerHighest = IvyColors.White,
    surfaceContainerLow = IvyColors.White,
    surfaceContainerLowest = IvyColors.White,
)

private fun ivyDarkColorScheme(isTrueBlack: Boolean): ColorScheme = darkColorScheme(
    primary = IvyColors.Purple.primary,
    onPrimary = IvyColors.White,
    primaryContainer = IvyColors.Purple.light,
    onPrimaryContainer = IvyColors.White,
    inversePrimary = IvyColors.Purple.dark,
    secondary = IvyColors.Green.primary,
    onSecondary = IvyColors.White,
    secondaryContainer = IvyColors.Green.light,
    onSecondaryContainer = IvyColors.White,
    tertiary = IvyColors.Green.primary,
    onTertiary = IvyColors.White,
    tertiaryContainer = IvyColors.Green.light,
    onTertiaryContainer = IvyColors.White,

    error = IvyColors.Red.primary,
    onError = IvyColors.White,
    errorContainer = IvyColors.Red.light,
    onErrorContainer = IvyColors.White,

    background = if (isTrueBlack) IvyColors.TrueBlack else IvyColors.Black,
    onBackground = IvyColors.White,
    surface = if (isTrueBlack) IvyColors.TrueBlack else IvyColors.Black,
    onSurface = IvyColors.White,
    surfaceVariant = IvyColors.ExtraDarkGray,
    onSurfaceVariant = IvyColors.White,
    surfaceTint = IvyColors.White,
    inverseSurface = IvyColors.LightGray,
    inverseOnSurface = if (isTrueBlack) IvyColors.TrueBlack else IvyColors.Black,

    outline = IvyColors.Gray,
    outlineVariant = IvyColors.LightGray,
    scrim = IvyColors.ExtraLightGray.copy(alpha = 0.8f),

    surfaceBright = darkSurface(isTrueBlack),
    surfaceDim = darkSurface(isTrueBlack),
    surfaceContainer = darkSurface(isTrueBlack),
    surfaceContainerHigh = darkSurface(isTrueBlack),
    surfaceContainerHighest = darkSurface(isTrueBlack),
    surfaceContainerLow = darkSurface(isTrueBlack),
    surfaceContainerLowest = darkSurface(isTrueBlack),
)

private fun darkSurface(isTrueBlack: Boolean) = if (isTrueBlack) IvyColors.TrueBlack else IvyColors.Black
