package com.ivy.widget.balance

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.annotation.Keep
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.domain.AppStarter
import com.nivy.domain.usecase.wallet.WalletBalanceUseCase
import com.nivy.domain.usecase.wallet.WalletIncomeExpenseUseCase
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.data.model.toCloseTimeRange
import com.nivy.widget.WidgetBase
import com.nivy.widget.balance.PrefsKey
import com.nivy.widget.balance.WalletBalanceWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

// Stays in com.ivy.widget.balance: launchers store this class name, and a new one would drop the widget on update.
@Keep
@AndroidEntryPoint
class WalletBalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    companion object {
        fun updateBroadcast(context: Context) {
            WidgetBase.updateBroadcast(context, WalletBalanceWidgetReceiver::class.java)
        }
    }

    override val glanceAppWidget: GlanceAppWidget = WalletBalanceWidget(
        getAppStarter = { appStarter }
    )
    private val coroutineScope = MainScope()

    @Inject
    lateinit var appStarter: AppStarter

    @Inject
    lateinit var walletBalanceUseCase: WalletBalanceUseCase

    @Inject
    lateinit var currencyRepository: CurrencyRepository

    @Inject
    lateinit var accountRepository: AccountRepository

    @Inject
    lateinit var walletIncomeExpenseUseCase: WalletIncomeExpenseUseCase

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var timeProvider: TimeProvider

    @Inject
    lateinit var timeConverter: TimeConverter

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateData(context)
    }

    private fun updateData(context: Context) {
        coroutineScope.launch {
            val appLocked = appPreferences.isAppLockEnabled()
            val currency = currencyRepository.getBaseCurrency().code
            val balance = walletBalanceUseCase.balance(baseCurrency = currency)
            val accounts = accountRepository.findAll()
            // This month with the user's start day: not the month the app happens to show, nor
            // the default start day a process started for the widget alone would have.
            val startDayOfMonth = appPreferences.startDayOfMonth()
            val period = TimePeriod.currentMonth(startDayOfMonth, timeProvider)
            val incomeExpense = walletIncomeExpenseUseCase.incomeExpense(
                baseCurrency = currency,
                accounts = accounts,
                range = period.toRange(
                    startDateOfMonth = startDayOfMonth,
                    timeConverter = timeConverter,
                    timeProvider = timeProvider,
                ).toCloseTimeRange()
            )

            GlanceAppWidgetManager(context).getGlanceIds(WalletBalanceWidget::class.java)
                .forEach {
                    updateAppWidgetState(
                        context,
                        PreferencesGlanceStateDefinition,
                        it
                    ) { pref ->
                        pref.toMutablePreferences().apply {
                            this[booleanPreferencesKey(PrefsKey.APP_LOCKED)] = appLocked
                            this[doublePreferencesKey(PrefsKey.BALANCE)] =
                                balance.toDouble()
                            this[stringPreferencesKey(PrefsKey.CURRENCY)] = currency
                            this[doublePreferencesKey(PrefsKey.INCOME)] =
                                incomeExpense.income.toDouble()
                            this[doublePreferencesKey(PrefsKey.EXPENSE)] =
                                incomeExpense.expense.toDouble()
                        }
                    }
                    glanceAppWidget.update(context, it)
                }
        }
    }
}
