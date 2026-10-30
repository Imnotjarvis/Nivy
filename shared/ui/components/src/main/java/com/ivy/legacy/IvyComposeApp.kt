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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import com.ivy.base.model.Theme
import com.ivy.base.resource.AndroidResourceProvider
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.base.time.impl.DeviceTimeProvider
import com.ivy.base.time.impl.StandardTimeConverter
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.design.utils.LocalScreenSize
import com.ivy.design.utils.StatusBarDarkText
import com.ivy.domain.RootScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider
import com.ivy.ui.time.TimeFormatter
import com.ivy.ui.time.impl.AndroidDevicePreferences
import com.ivy.ui.time.impl.IvyTimeFormatter

val LocalPeriodState = staticCompositionLocalOf<PeriodState> { error("No PeriodState") }

val LocalMainScreenState = staticCompositionLocalOf<MainScreenState> { error("No MainScreenState") }

@Composable
@ReadOnlyComposable
fun rootScreen(): RootScreen = LocalContext.current as RootScreen

/** Whether [theme] is dark; AUTO follows the system setting. */
fun isDarkTheme(theme: Theme, systemDark: Boolean): Boolean = when (theme) {
    Theme.LIGHT -> false
    Theme.DARK, Theme.AMOLED_DARK -> true
    Theme.AUTO -> systemDark
}

/**
 * The app's screens in the Ivy Material3 theme the user picked, with the status bar icons to
 * match. Also gives the screens the app's shared state, its time services and the screen size.
 */
@Composable
fun IvyWalletTheme(
    theme: Theme,
    periodState: PeriodState,
    mainScreenState: MainScreenState,
    timeConverter: TimeConverter,
    timeProvider: TimeProvider,
    timeFormatter: TimeFormatter,
    modifier: Modifier = Modifier,
    includeSurface: Boolean = true,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    val dark = isDarkTheme(theme, isSystemInDarkTheme())
    CompositionLocalProvider(
        LocalPeriodState provides periodState,
        LocalMainScreenState provides mainScreenState,
        LocalTimeConverter provides timeConverter,
        LocalTimeProvider provides timeProvider,
        LocalTimeFormatter provides timeFormatter,
    ) {
        IvyMaterial3Theme(
            isTrueBlack = theme == Theme.AMOLED_DARK,
            dark = dark,
        ) {
            StatusBarDarkText(darkText = !dark)
            WrapWithSurface(includeSurface = includeSurface) {
                BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                    val screenSize = IntSize(constraints.maxWidth, constraints.maxHeight)
                    CompositionLocalProvider(LocalScreenSize provides screenSize) {
                        content()
                    }
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
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    } else {
        content()
    }
}

@Composable
fun IvyWalletComponentPreview(
    modifier: Modifier = Modifier,
    theme: Theme = Theme.LIGHT,
    content: @Composable BoxScope.() -> Unit
) {
    IvyWalletPreview(
        theme = theme
    ) {
        Box(
            modifier = modifier
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
    val timeProvider = remember { DeviceTimeProvider() }
    val timeConverter = remember { StandardTimeConverter(timeProvider) }
    IvyWalletTheme(
        theme = theme,
        periodState = remember { PeriodState(timeProvider) },
        mainScreenState = remember { MainScreenState() },
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
