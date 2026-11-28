package com.nivy.widget.transaction

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import com.nivy.base.di.AppCoroutineScope
import com.nivy.base.model.TransactionType
import com.nivy.base.refreshWidget
import com.nivy.domain.WidgetCornerRadius
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

/**
 * A widget whose buttons open a new income, expense or transfer: its [layout]'s icons, and their
 * labels too when [tappableLabels], in the corners the app's radius gives.
 */
abstract class AddTransactionWidgetBase(
    @LayoutRes private val layout: Int,
    private val tappableLabels: Boolean,
) : AppWidgetProvider() {

    @Inject
    lateinit var widgetClick: AddTransactionWidgetClick

    @Inject
    lateinit var cornerRadius: WidgetCornerRadius

    @Inject
    @AppCoroutineScope
    lateinit var appScope: CoroutineScope

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.refreshWidget(javaClass)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        drawWithCorners(appScope, cornerRadius, appWidgetIds) { appWidgetId, radius ->
            val rv = RemoteViews(context.packageName, layout)
            widgetClick.setClickListener(context, rv, R.id.ivIncome, TransactionType.INCOME)
            widgetClick.setClickListener(context, rv, R.id.ivExpense, TransactionType.EXPENSE)
            widgetClick.setClickListener(context, rv, R.id.ivTransfer, TransactionType.TRANSFER)
            if (tappableLabels) {
                widgetClick.setClickListener(context, rv, R.id.tvIncome, TransactionType.INCOME)
                widgetClick.setClickListener(context, rv, R.id.tvExpense, TransactionType.EXPENSE)
                widgetClick.setClickListener(context, rv, R.id.tvTransfer, TransactionType.TRANSFER)
            }
            rv.roundCorners(R.id.widgetBar, radius)
            appWidgetManager.updateAppWidget(appWidgetId, rv)
        }
    }
}
