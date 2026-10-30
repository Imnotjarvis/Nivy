package com.ivy.design.system

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.ivy.design.system.colors.Black
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.Green
import com.ivy.design.system.colors.GreenDark
import com.ivy.design.system.colors.GreenLight
import com.ivy.design.system.colors.IvyDark
import com.ivy.design.system.colors.IvyLight
import com.ivy.design.system.colors.MediumBlack
import com.ivy.design.system.colors.MediumWhite
import com.ivy.design.system.colors.Orange
import com.ivy.design.system.colors.OrangeDark
import com.ivy.design.system.colors.OrangeLight
import com.ivy.design.system.colors.Purple
import com.ivy.design.system.colors.Red
import com.ivy.design.system.colors.RedDark
import com.ivy.design.system.colors.RedLight
import com.ivy.design.system.colors.TrueBlack
import com.ivy.design.system.colors.White
import com.ivy.design.system.shapes.IvyShapes
import com.ivy.design.system.typography.IvyTypography

// How the Ivy design maps onto Material3 color roles:
//   surface, background   the screen background ("pure")
//   onSurface             text and icons on it ("pureInverse")
//   surfaceVariant        cards, chips and input backgrounds ("medium")
//   onSurfaceVariant      secondary text and icons (gray)
//   inverseSurface        the opposite theme's surfaceVariant ("mediumInverse")
//   primary               Ivy purple; secondary green; tertiary orange; error red
//   secondaryContainer    the theme's soft green ("green1")
@Composable
fun IvyMaterial3Theme(
    isTrueBlack: Boolean,
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (dark) ivyDarkColorScheme(isTrueBlack) else ivyLightColorScheme(),
        typography = IvyTypography,
        shapes = IvyShapes,
        content = content,
    )
}

// Built with lightColorScheme/darkColorScheme so no role is left Color.Unspecified:
// Material3 1.3+ blends surfaceContainer into TopAppBar/Switch colors, and an unspecified
// role there yields an invalid color space ("Invalid ID") and crashes drawing.
private fun ivyLightColorScheme(): ColorScheme = lightColorScheme(
    primary = Purple,
    onPrimary = White,
    primaryContainer = IvyLight,
    onPrimaryContainer = IvyDark,
    inversePrimary = IvyLight,
    secondary = Green,
    onSecondary = White,
    secondaryContainer = GreenLight,
    onSecondaryContainer = GreenDark,
    tertiary = Orange,
    onTertiary = White,
    tertiaryContainer = OrangeLight,
    onTertiaryContainer = OrangeDark,

    error = Red,
    onError = White,
    errorContainer = RedLight,
    onErrorContainer = RedDark,

    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = MediumWhite,
    onSurfaceVariant = Gray,
    surfaceTint = Black,
    inverseSurface = MediumBlack,
    inverseOnSurface = White,

    outline = Gray,
    outlineVariant = MediumWhite,
    scrim = MediumBlack.copy(alpha = 0.8f),

    surfaceBright = White,
    surfaceDim = White,
    surfaceContainer = White,
    surfaceContainerHigh = White,
    surfaceContainerHighest = White,
    surfaceContainerLow = White,
    surfaceContainerLowest = White,
)

private fun ivyDarkColorScheme(isTrueBlack: Boolean): ColorScheme = darkColorScheme(
    primary = Purple,
    onPrimary = White,
    primaryContainer = IvyDark,
    onPrimaryContainer = IvyLight,
    inversePrimary = IvyDark,
    secondary = Green,
    onSecondary = White,
    secondaryContainer = GreenDark,
    onSecondaryContainer = GreenLight,
    tertiary = Orange,
    onTertiary = White,
    tertiaryContainer = OrangeDark,
    onTertiaryContainer = OrangeLight,

    error = Red,
    onError = White,
    errorContainer = RedDark,
    onErrorContainer = RedLight,

    background = darkSurface(isTrueBlack),
    onBackground = White,
    surface = darkSurface(isTrueBlack),
    onSurface = White,
    surfaceVariant = MediumBlack,
    onSurfaceVariant = Gray,
    surfaceTint = White,
    inverseSurface = MediumWhite,
    inverseOnSurface = darkSurface(isTrueBlack),

    outline = Gray,
    outlineVariant = MediumBlack,
    scrim = MediumWhite.copy(alpha = 0.8f),

    surfaceBright = darkSurface(isTrueBlack),
    surfaceDim = darkSurface(isTrueBlack),
    surfaceContainer = darkSurface(isTrueBlack),
    surfaceContainerHigh = darkSurface(isTrueBlack),
    surfaceContainerHighest = darkSurface(isTrueBlack),
    surfaceContainerLow = darkSurface(isTrueBlack),
    surfaceContainerLowest = darkSurface(isTrueBlack),
)

private fun darkSurface(isTrueBlack: Boolean) = if (isTrueBlack) TrueBlack else Black
