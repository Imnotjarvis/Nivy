package com.nivy.settings

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.AppLockDelay
import com.nivy.base.model.DateDisplayFormat
import com.nivy.base.model.Theme
import com.nivy.base.time.TimeProvider
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** The settings' option dialogs, each with an option chosen: in light and dark, in both color modes. */
@RunWith(TestParameterInjector::class)
class SettingsDialogsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot the date format dialog`() = shot {
        ChooseDateFormatModal(visible = true, selected = DateDisplayFormat.entries.first(), dismiss = {}, onSelect = {})
    }

    @Test
    fun `snapshot the lock delay dialog`() = shot {
        ChooseLockDelayModal(visible = true, selected = AppLockDelay.ONE_MINUTE, dismiss = {}, onSelect = {})
    }

    @Test
    fun `snapshot the corner radius dialog, matching the phone`() = shot {
        ChooseCornerRadiusModal(visible = true, dismiss = {}, onSet = {})
    }

    // The date format dialog shows today's date: on a stopped clock, one that stays the same.
    private fun shot(content: @Composable BoxWithConstraintsScope.() -> Unit) =
        snapshot(theme, minimal = colors == Colors.Minimal) {
            val dark = theme == PaparazziTheme.Dark
            NivyPreview(theme = if (dark) Theme.DARK else Theme.LIGHT, timeProvider = FixedTime) {
                content()
            }
        }
}

/** November 7, 2026, noon, in UTC. */
private object FixedTime : TimeProvider {
    private val now = LocalDateTime.of(2026, 11, 7, 12, 0)
    override fun getZoneId(): ZoneId = ZoneId.of("UTC")
    override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
    override fun localNow(): LocalDateTime = now
    override fun localDateNow(): LocalDate = now.toLocalDate()
    override fun localTimeNow(): LocalTime = now.toLocalTime()
}
