package com.nivy.widget.balance

import android.content.Intent
import android.util.TypedValue
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.appwidget.action.actionStartActivity
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams.RenderingMode
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * The balance widget as a launcher draws it: its RemoteViews, inflated, on a home screen's
 * background. It has one look of its own, whatever the app's theme or color mode.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class WalletBalanceWidgetPaparazziTest {
    // A launcher inflates the framework's views; RemoteViews refuse AppCompat's.
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6_PRO,
        renderingMode = RenderingMode.SHRINK,
        appCompatEnabled = false,
        maxPercentDifference = 0.005,
    )

    @Before
    fun english() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `snapshot the balance widget`() {
        val context = paparazzi.context
        val open = actionStartActivity(Intent())
        val views = runBlocking {
            GlanceRemoteViews().compose(context, DpSize(WidthDp.dp, HeightDp.dp)) {
                WalletBalanceWidgetContent(
                    appLocked = false,
                    balance = widgetAmount(2_419.5, "EUR"),
                    currency = "EUR",
                    income = chipAmount(2_500.0, "EUR"),
                    expense = chipAmount(80.5, "EUR"),
                    onIncomeClick = open,
                    onExpenseClick = open,
                    onTransferClick = open,
                    onWidgetClick = open,
                )
            }.remoteViews
        }
        val homeScreen = FrameLayout(context).apply {
            setBackgroundColor(Wallpaper)
            val margin = dp(MarginDp)
            setPadding(margin, margin, margin, margin)
        }
        homeScreen.addView(views.apply(context, homeScreen), FrameLayout.LayoutParams(dp(WidthDp), dp(HeightDp)))
        paparazzi.snapshot(homeScreen)
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        paparazzi.context.resources.displayMetrics,
    ).toInt()

    private companion object {
        // A 4 by 2 widget on a phone's home screen.
        const val WidthDp = 360
        const val HeightDp = 220
        const val MarginDp = 16
        const val Wallpaper = 0xFF3A4A5C.toInt()
    }
}
