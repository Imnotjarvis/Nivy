package com.nivy.settings

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.domain.RootScreen
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** The top of the Settings screen: in light and dark, in both color modes. */
@OptIn(ExperimentalFoundationApi::class)
@RunWith(TestParameterInjector::class)
class SettingsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot the top of settings`() {
        val dark = theme == PaparazziTheme.Dark
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(theme = if (dark) Theme.DARK else Theme.LIGHT) {
                CompositionLocalProvider(LocalContext provides ActivityStandIn(LocalContext.current)) {
                    UI(
                        currencyCode = "EUR",
                        theme = if (dark) Theme.DARK else Theme.LIGHT,
                        onSwitchTheme = {},
                        lockApp = false,
                        onSetCurrency = {},
                    )
                }
            }
        }
    }
}

/** The activity as Settings reads it: only its version shows, fixed so the screenshot stays put. */
private class ActivityStandIn(base: Context) : ContextWrapper(base), RootScreen {
    override val buildVersionName = "2026.11.10"
    override val buildVersionCode = 1_503_360

    override fun shareCSVFile(fileUri: Uri) = Unit

    override fun shareZipFile(fileUri: Uri) = Unit

    override fun <T> pinWidget(widget: Class<T>) = Unit

    override fun pickDate(
        initialDate: LocalDate?,
        minDate: LocalDate?,
        maxDate: LocalDate?,
        onPicked: (LocalDate) -> Unit,
    ) = Unit

    override fun createNewFile(fileName: String, onCreated: (Uri) -> Unit) = Unit

    override fun openFile(onOpened: (Uri) -> Unit) = Unit
}
