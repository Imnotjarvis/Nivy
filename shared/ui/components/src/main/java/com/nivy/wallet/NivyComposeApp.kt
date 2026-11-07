package com.nivy.wallet

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
import com.nivy.base.model.Theme
import com.nivy.base.resource.AndroidResourceProvider
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.DeviceTimeProvider
import com.nivy.base.time.impl.StandardTimeConverter
import com.nivy.design.system.NivyMaterial3Theme
import com.nivy.design.utils.LocalScreenSize
import com.nivy.design.utils.StatusBarDarkText
import com.nivy.domain.RootScreen
import com.nivy.navigation.Navigation
import com.nivy.navigation.NavigationRoot
import com.nivy.ui.time.DateFormatState
import com.nivy.ui.time.LocalTimeConverter
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.ui.time.LocalTimeProvider
import com.nivy.ui.time.TimeFormatter
import com.nivy.ui.time.impl.AndroidDevicePreferences
import com.nivy.ui.time.impl.NivyTimeFormatter

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
 * The app's screens in the Nivy Material3 theme the user picked, with the status bar icons to
 * match. Also gives the screens the app's shared state, its time services and the screen size.
 */
@Composable
fun NivyTheme(
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
        NivyMaterial3Theme(
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
fun NivyComponentPreview(
    modifier: Modifier = Modifier,
    theme: Theme = Theme.LIGHT,
    content: @Composable BoxScope.() -> Unit
) {
    NivyPreview(
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

/**
 * The app's theme and screens' setting around a preview or a screenshot test, on [timeProvider]'s
 * clock: the device's, unless a test stops it so that dates read the same on every run.
 */
@Composable
fun NivyPreview(
    theme: Theme = Theme.LIGHT,
    timeProvider: TimeProvider = remember { DeviceTimeProvider() },
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    val context = LocalContext.current
    val timeConverter = remember(timeProvider) { StandardTimeConverter(timeProvider) }
    NivyTheme(
        theme = theme,
        periodState = remember(timeProvider) { PeriodState(timeProvider) },
        mainScreenState = remember { MainScreenState() },
        timeConverter = timeConverter,
        timeProvider = timeProvider,
        timeFormatter = NivyTimeFormatter(
            resourceProvider = AndroidResourceProvider(context),
            timeProvider = timeProvider,
            converter = timeConverter,
            devicePreferences = AndroidDevicePreferences(context),
            dateFormatState = DateFormatState(),
        ),
    ) {
        NavigationRoot(navigation = Navigation()) {
            content()
        }
    }
}
