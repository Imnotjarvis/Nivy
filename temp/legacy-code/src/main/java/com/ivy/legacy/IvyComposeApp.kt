package com.ivy.legacy

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import com.ivy.base.model.Theme
import com.ivy.base.legacy.appContext
import com.ivy.base.resource.AndroidResourceProvider
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.base.time.impl.DeviceTimeProvider
import com.ivy.base.time.impl.StandardTimeConverter
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.design.utils.StatusBarDarkText
import com.ivy.domain.RootScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.ui.time.TimeFormatter
import com.ivy.ui.time.impl.AndroidDevicePreferences
import com.ivy.ui.time.impl.IvyTimeFormatter

@Suppress("CompositionLocalAllowlist")
val LocalIvyWalletCtx = compositionLocalOf<IvyWalletCtx> { error("No IvyWalletCtx") }

@Suppress("CompositionLocalAllowlist")
@Deprecated("Used only for time migration to Instant. Never use it in new code!")
val LocalTimeConverter = compositionLocalOf<TimeConverter> { error("No LocalTimeConverter") }

@Suppress("CompositionLocalAllowlist")
@Deprecated("Used only for time migration to Instant. Never use it in new code!")
val LocalTimeProvider = compositionLocalOf<TimeProvider> { error("No LocalTimeProvider") }

@Suppress("CompositionLocalAllowlist")
@Deprecated("Used only for time migration to Instant. Never use it in new code!")
val LocalTimeFormatter = compositionLocalOf<TimeFormatter> { error("No LocalTimeFormatter") }

@Deprecated("Legacy code. Don't use it, please.")
@Composable
fun ivyWalletCtx(): IvyWalletCtx = LocalIvyWalletCtx.current

@Composable
fun rootScreen(): RootScreen = LocalContext.current as RootScreen

/** Whether [theme] is dark; AUTO follows the system setting. */
fun isDarkTheme(theme: Theme, systemDark: Boolean): Boolean = when (theme) {
    Theme.LIGHT -> false
    Theme.DARK, Theme.AMOLED_DARK -> true
    Theme.AUTO -> systemDark
}

/**
 * The app's screens in the Ivy Material3 theme the user picked, with the status bar icons to
 * match. Also provides the legacy app context and time services to the screens that still
 * read them from the composition.
 */
@Suppress("ModifierMissing") // a theme around whole screens, not a layout
@Composable
fun IvyWalletTheme(
    ivyContext: IvyWalletCtx,
    timeConverter: TimeConverter,
    timeProvider: TimeProvider,
    timeFormatter: TimeFormatter,
    includeSurface: Boolean = true,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    val dark = isDarkTheme(ivyContext.theme, isSystemInDarkTheme())
    CompositionLocalProvider(
        LocalIvyWalletCtx provides ivyContext,
        LocalTimeConverter provides timeConverter,
        LocalTimeProvider provides timeProvider,
        LocalTimeFormatter provides timeFormatter,
    ) {
        IvyMaterial3Theme(
            isTrueBlack = ivyContext.theme == Theme.AMOLED_DARK,
            dark = dark,
        ) {
            StatusBarDarkText(darkText = !dark)
            WrapWithSurface(includeSurface = includeSurface) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    ivyContext.screenWidth = with(LocalDensity.current) {
                        maxWidth.roundToPx()
                    }
                    ivyContext.screenHeight = with(LocalDensity.current) {
                        maxHeight.roundToPx()
                    }

                    content()
                }
            }
        }
    }
}

@Composable
private fun WrapWithSurface(
    includeSurface: Boolean,
    content: @Composable () -> Unit,
) {
    if (includeSurface) {
        Surface {
            content()
        }
    } else {
        content()
    }
}

@Composable
fun IvyWalletComponentPreview(
    theme: Theme = Theme.LIGHT,
    content: @Composable BoxScope.() -> Unit
) {
    IvyWalletPreview(
        theme = theme
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun IvyWalletPreview(
    theme: Theme = Theme.LIGHT,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    val context = LocalContext.current
    appContext = LocalView.current.context
    val ivyContext = remember { IvyWalletCtx() }
    ivyContext.switchTheme(theme)
    val timeProvider = remember { DeviceTimeProvider() }
    val timeConverter = remember { StandardTimeConverter(timeProvider) }
    IvyWalletTheme(
        ivyContext = ivyContext,
        timeConverter = timeConverter,
        timeProvider = timeProvider,
        timeFormatter = IvyTimeFormatter(
            resourceProvider = AndroidResourceProvider(context),
            timeProvider = timeProvider,
            converter = timeConverter,
            devicePreferences = AndroidDevicePreferences(context)
        ),
    ) {
        NavigationRoot(navigation = Navigation()) {
            content()
        }
    }
}
