package com.ivy.widget.transaction

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.nivy.base.model.TransactionType
import com.nivy.base.refreshWidget
import com.nivy.widget.transaction.AddTransactionWidgetClick
import com.nivy.base.di.AppCoroutineScope
import com.nivy.domain.WidgetCornerRadius
import com.nivy.widget.transaction.drawWithCorners
import com.nivy.widget.transaction.roundCorners
import kotlinx.coroutines.CoroutineScope
import com.nivy.widget.transaction.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Stays in com.ivy.widget.transaction: launchers store this class name, and a new one would drop the widget on update.
@AndroidEntryPoint
class AddTransactionWidgetCompact : AppWidgetProvider() {

    @Inject
    lateinit var widgetClick: AddTransactionWidgetClick

    @Inject
    lateinit var cornerRadius: WidgetCornerRadius

    @Inject
    @AppCoroutineScope
    lateinit var appScope: CoroutineScope

    companion object {
        fun updateBroadcast(context: Context) {
            context.refreshWidget(AddTransactionWidgetCompact::class.java)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        updateBroadcast(context)
    }

    // --------------------------- </BROADCASTS> ----------------------------------------------------
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        drawWithCorners(appScope, cornerRadius, appWidgetIds) { appWidgetId, radius ->
            updateAppWidget(context, appWidgetManager, appWidgetId, radius)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        cornerRadiusDp: Float?,
    ) {
        val rv = RemoteViews(context.packageName, R.layout.widget_add_transaction_compact)
        widgetClick.setClickListener(context, rv, R.id.ivIncome, TransactionType.INCOME)
        widgetClick.setClickListener(context, rv, R.id.ivExpense, TransactionType.EXPENSE)
        widgetClick.setClickListener(context, rv, R.id.ivTransfer, TransactionType.TRANSFER)

        rv.roundCorners(R.id.widgetBar, cornerRadiusDp)

        appWidgetManager.updateAppWidget(appWidgetId, rv)
    }
}
