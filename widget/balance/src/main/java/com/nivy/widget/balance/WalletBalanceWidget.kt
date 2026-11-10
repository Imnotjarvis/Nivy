package com.nivy.widget.balance

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import com.nivy.base.model.TransactionType
import com.nivy.domain.AppStarter
import com.nivy.wallet.utils.formatAmount
import com.nivy.wallet.utils.shortenAmount
import kotlin.math.abs

object PrefsKey {
    const val APP_LOCKED = "appLocked"
    const val BALANCE = "balance_v2"
    const val CURRENCY = "currency"
    const val INCOME = "income_v2"
    const val EXPENSE = "expense_v2"
}

/** How the widget writes [amount]: as the app does, in [currency]'s format, shortened from 100k on. */
internal fun widgetAmount(amount: Double, currency: String): String = formatAmount(amount, currency, shorten = true)

/**
 * Income or expenses in the widget's chips: shortened from 1,000 on (12.35k), as the pie chart's
 * header does, so that they keep to one line; below, as the app writes amounts (80.50).
 */
internal fun chipAmount(amount: Double, currency: String): String =
    if (abs(amount) >= ChipShortenFrom) shortenAmount(amount) else formatAmount(amount, currency)

private const val ChipShortenFrom = 1_000

class WalletBalanceWidget(
    private val getAppStarter: () -> AppStarter,
) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val appLocked = prefs[booleanPreferencesKey(PrefsKey.APP_LOCKED)] ?: false
            val balance = prefs[doublePreferencesKey(PrefsKey.BALANCE)] ?: 0.00
            val currency = prefs[stringPreferencesKey(PrefsKey.CURRENCY)] ?: "USD"
            val income = prefs[doublePreferencesKey(PrefsKey.INCOME)] ?: 0.00
            val expense = prefs[doublePreferencesKey(PrefsKey.EXPENSE)] ?: 0.00

            WalletBalanceWidgetContent(
                appLocked = appLocked,
                balance = widgetAmount(balance, currency),
                currency = currency,
                income = chipAmount(income, currency),
                expense = chipAmount(expense, currency),
                // Activity actions, so a tap opens the app directly instead of going through a
                // broadcast receiver.
                onIncomeClick = actionStartActivity(
                    getAppStarter().addTransactionIntent(TransactionType.INCOME)
                ),
                onExpenseClick = actionStartActivity(
                    getAppStarter().addTransactionIntent(TransactionType.EXPENSE)
                ),
                onTransferClick = actionStartActivity(
                    getAppStarter().addTransactionIntent(TransactionType.TRANSFER)
                ),
                onWidgetClick = actionStartActivity(getAppStarter().homeIntent()),
            )
        }
    }
}
