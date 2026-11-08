package com.nivy.wallet.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.accessibility.AccessibilityRenderExtension
import com.nivy.design.system.colors.Nivy
import com.nivy.ui.R
import com.nivy.wallet.NivyPreview
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * What TalkBack reads on the shared controls: each one's label, role and state, beside it. The amount
 * keypad reads its layout setting from Hilt, which screenshot tests don't have, so it isn't here.
 */
class ControlsAccessibilityPaparazziTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6_PRO,
        renderExtensions = setOf(AccessibilityRenderExtension()),
        maxPercentDifference = 0.005,
    )

    @Before
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `snapshot the tabs, the + button and a two-option switch`() {
        paparazzi.snapshot {
            NivyPreview {
                Column(Modifier.padding(top = 48.dp)) {
                    Row(Modifier.padding(16.dp)) {
                        SegmentedButton(label = "Borrow", selected = true) {}
                        SegmentedButton(label = "Lend", selected = false) {}
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth().height(96.dp)) {
                        TabsBottomBar(
                            start = { Tab(R.drawable.ic_home, "Home", selected = true) },
                            end = { Tab(R.drawable.ic_accounts, "Accounts", selected = false) },
                        )
                    }
                }
                BottomBarFab(backgroundColor = Nivy, contentDescription = "Add") {}
            }
        }
    }

    @Composable
    private fun RowScope.Tab(@DrawableRes icon: Int, name: String, selected: Boolean) =
        BottomBarTab(icon = icon, name = name, selected = selected, selectedColor = Nivy) {}
}
