package com.nivy.widget.balance

import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.domain.usecase.wallet.WalletBalanceUseCase
import com.nivy.domain.usecase.wallet.WalletIncomeExpenseUseCase
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.data.model.toCloseTimeRange
import javax.inject.Inject

/** What the balance widget shows: whether the app is locked, the balance, and this month's income and expenses. */
class WalletBalanceWidgetNumbers @Inject constructor(
    private val appPreferences: AppPreferences,
    private val currencyRepository: CurrencyRepository,
    private val walletBalanceUseCase: WalletBalanceUseCase,
    private val accountRepository: AccountRepository,
    private val walletIncomeExpenseUseCase: WalletIncomeExpenseUseCase,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) {
    data class Shown(
        val appLocked: Boolean,
        val currency: String,
        val balance: Double,
        val income: Double,
        val expense: Double,
    )

    suspend fun load(): Shown {
        val currency = currencyRepository.getBaseCurrency().code
        // This month with the person's start day: not the month the app happens to show, nor the
        // default start day a process started for the widget alone would have.
        val startDayOfMonth = appPreferences.startDayOfMonth()
        val month = TimePeriod.currentMonth(startDayOfMonth, timeProvider).toRange(
            startDateOfMonth = startDayOfMonth,
            timeConverter = timeConverter,
            timeProvider = timeProvider,
        ).toCloseTimeRange()
        val incomeExpense = walletIncomeExpenseUseCase.incomeExpense(
            baseCurrency = currency,
            accounts = accountRepository.findAll(),
            range = month,
        )
        return Shown(
            appLocked = appPreferences.isAppLockEnabled(),
            currency = currency,
            balance = walletBalanceUseCase.balance(baseCurrency = currency).toDouble(),
            income = incomeExpense.income.toDouble(),
            expense = incomeExpense.expense.toDouble(),
        )
    }
}
