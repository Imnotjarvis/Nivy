package com.nivy.design.system

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.nivy.design.system.colors.BackgroundDark
import com.nivy.design.system.colors.BackgroundLight
import com.nivy.design.system.colors.Black
import com.nivy.design.system.colors.ColorMode
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.GreenDark
import com.nivy.design.system.colors.GreenLight
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.NivyDark
import com.nivy.design.system.colors.NivyLight
import com.nivy.design.system.colors.MediumBlack
import com.nivy.design.system.colors.MediumWhite
import com.nivy.design.system.colors.Orange
import com.nivy.design.system.colors.OrangeDark
import com.nivy.design.system.colors.OrangeLight
import com.nivy.design.system.colors.Palette
import com.nivy.design.system.colors.TrueBlack
import com.nivy.design.system.colors.White
import com.nivy.design.system.colors.readableOn
import com.nivy.design.system.colors.wallpaperColors
import com.nivy.design.system.shapes.AppCornerRadius
import com.nivy.design.system.shapes.nivyShapes
import com.nivy.design.system.typography.NivyTypography

// How the Nivy design maps onto Material3 color roles:
//   background            the screen background, behind everything
//   surface               modal, sheet and component backgrounds ("pure")
//   onSurface             text and icons on it ("pureInverse")
//   surfaceVariant        cards, chips and input backgrounds ("medium")
//   onSurfaceVariant      secondary text and icons (gray)
//   inverseSurface        the opposite theme's surfaceVariant ("mediumInverse")
//   primary               the palette's accent (Nivy's purple); secondary its second color,
//                         tertiary its third (green and orange in Nivy's); error red
//   secondaryContainer    the theme's soft green ("green1")
@Composable
fun NivyMaterial3Theme(
    isTrueBlack: Boolean,
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Before anything below draws: minimal colors draw with the theme's text color.
    ColorMode.dark = dark
    // The wallpaper's colors, for its palette and its row in the Colors dialog, where Android has
    // them. A new wallpaper restarts the activity, and with it this.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        ColorMode.wallpaper = remember(context) {
            wallpaperColors(dynamicLightColorScheme(context), dynamicDarkColorScheme(context))
        }
    }
    val colorScheme = if (dark) nivyDarkColorScheme(isTrueBlack) else nivyLightColorScheme()
    MaterialTheme(
        colorScheme = if (ColorMode.minimal) colorScheme.monochrome() else colorScheme,
        typography = NivyTypography,
        shapes = nivyShapes(AppCornerRadius.value),
        content = content,
    )
}

// Built with lightColorScheme/darkColorScheme so no role is left Color.Unspecified:
// Material3 1.3+ blends surfaceContainer into TopAppBar/Switch colors, and an unspecified
// role there yields an invalid color space ("Invalid ID") and crashes drawing.
private fun nivyLightColorScheme(): ColorScheme = lightColorScheme(
    primary = Nivy,
    // White, or black on a light accent: the wallpaper's on dark themes.
    onPrimary = readableOn(Nivy),
    primaryContainer = NivyLight,
    onPrimaryContainer = NivyDark,
    inversePrimary = NivyLight,
    secondary = Green,
    onSecondary = White,
    secondaryContainer = GreenLight,
    onSecondaryContainer = GreenDark,
    tertiary = Orange,
    onTertiary = White,
    tertiaryContainer = OrangeLight,
    onTertiaryContainer = OrangeDark,

    // Errors are warnings: red also in minimal colors.
    error = Palette.Red,
    onError = White,
    errorContainer = Palette.RedLight,
    onErrorContainer = Palette.RedDark,

    background = BackgroundLight,
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

private fun nivyDarkColorScheme(isTrueBlack: Boolean): ColorScheme = darkColorScheme(
    primary = Nivy,
    // White, or black on a light accent: the wallpaper's on dark themes.
    onPrimary = readableOn(Nivy),
    primaryContainer = NivyDark,
    onPrimaryContainer = NivyLight,
    inversePrimary = NivyDark,
    secondary = Green,
    onSecondary = White,
    secondaryContainer = GreenDark,
    onSecondaryContainer = GreenLight,
    tertiary = Orange,
    onTertiary = White,
    tertiaryContainer = OrangeDark,
    onTertiaryContainer = OrangeLight,

    // Errors are warnings: red also in minimal colors.
    error = Palette.Red,
    onError = White,
    errorContainer = Palette.RedDark,
    onErrorContainer = Palette.RedLight,

    background = if (isTrueBlack) TrueBlack else BackgroundDark,
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

// Minimal colors, as Material's monochrome scheme: the primary roles are the text color (the hues
// turn into it), with the background's color on them; the containers are the cards' grey, with
// the text color on them. Errors stay red.
private fun ColorScheme.monochrome(): ColorScheme = copy(
    onPrimary = surface,
    onSecondary = surface,
    onTertiary = surface,
    primaryContainer = surfaceVariant,
    onPrimaryContainer = onSurface,
    secondaryContainer = surfaceVariant,
    onSecondaryContainer = onSurface,
    tertiaryContainer = surfaceVariant,
    onTertiaryContainer = onSurface,
    inversePrimary = inverseOnSurface,
)
