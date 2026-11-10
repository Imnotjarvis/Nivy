package com.ivy.widget.balance

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.annotation.Keep
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.nivy.base.refreshWidget
import com.nivy.domain.AppStarter
import com.nivy.widget.balance.WalletBalanceWidget
import com.nivy.widget.balance.WalletBalanceWidgetWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Stays in com.ivy.widget.balance: launchers store this class name, and a new one would drop the widget on update.
@Keep
@AndroidEntryPoint
class WalletBalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    companion object {
        fun updateBroadcast(context: Context) {
            context.refreshWidget(WalletBalanceWidgetReceiver::class.java)
        }
    }

    override val glanceAppWidget: GlanceAppWidget = WalletBalanceWidget(
        getAppStarter = { appStarter }
    )

    @Inject
    lateinit var appStarter: AppStarter

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // Draws the numbers it last read; the worker reads them again and redraws.
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WalletBalanceWidgetWorker.enqueue(context)
    }
}
