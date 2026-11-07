package com.nivy.ui.testing

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.nivy.design.system.NivyMaterial3Theme
import com.nivy.design.system.colors.ColorMode
import org.junit.Before
import org.junit.Rule
import java.util.Locale

open class PaparazziScreenshotTest {

    @Before
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6_PRO,
        showSystemUi = true,
        maxPercentDifference = 0.005
    )

    /** Snapshots [content] in [theme], and in minimal colors when [minimal] is true. */
    protected fun snapshot(theme: PaparazziTheme, minimal: Boolean = false, content: @Composable () -> Unit) {
        ColorMode.minimal = minimal
        try {
            paparazzi.snapshot {
                NivyMaterial3Theme(
                    dark = when (theme) {
                        PaparazziTheme.Light -> false
                        PaparazziTheme.Dark -> true
                    },
                    isTrueBlack = false
                ) {
                    content()
                }
            }
        } finally {
            ColorMode.minimal = false
        }
    }
}

enum class PaparazziTheme {
    Light, Dark
}
