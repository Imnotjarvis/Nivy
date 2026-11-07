package com.nivy.widget.balance

import android.content.Context
import android.icu.text.DecimalFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.nivy.wallet.utils.shortenAmount
import kotlin.math.abs

const val THOUSAND = 1000

object PrefsKey {
    const val APP_LOCKED = "appLocked"
    const val BALANCE = "balance_v2"
    const val CURRENCY = "currency"
    const val INCOME = "income_v2"
    const val EXPENSE = "expense_v2"
}

class WalletBalanceWidget(
    private val getAppStarter: () -> AppStarter,
) : GlanceAppWidget() {
    @Composable
    fun formatBalance(balance: Double): String {
        val formattedBalance = remember(balance) {
            if (abs(balance) < THOUSAND) {
                DecimalFormat("###,###.##").format(balance)
            } else {
                shortenAmount(balance)
            }
        }
        return formattedBalance
    }

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
                balance = formatBalance(balance),
                currency = currency,
                income = shortenAmount(income),
                expense = shortenAmount(expense),
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
