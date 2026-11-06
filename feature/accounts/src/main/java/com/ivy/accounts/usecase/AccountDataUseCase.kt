package com.ivy.accounts.usecase

import arrow.core.toOption
import com.ivy.base.threading.DispatchersProvider
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Expense
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.usecase.account.AccountBalanceUseCase
import com.ivy.domain.usecase.account.AccountIncomeExpenseUseCase
import com.ivy.domain.usecase.exchange.ExchangeUseCase
import com.ivy.wallet.data.model.CreditCardUse
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.exchange.ExchangeData
import com.ivy.wallet.utils.ivyMaxTime
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AccountDataUseCase @Inject constructor(
    private val exchangeUseCase: ExchangeUseCase,
    private val accountBalanceUseCase: AccountBalanceUseCase,
    private val accountIncomeExpenseUseCase: AccountIncomeExpenseUseCase,
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun accountsData(
        accounts: ImmutableList<com.ivy.data.model.Account>,
        baseCurrency: String,
        range: ClosedTimeRange,
        includeTransfersInCalc: Boolean = false,
    ): ImmutableList<com.ivy.wallet.data.model.AccountData> =
        withContext(dispatchers.io) {
            accounts.map { acc -> accountData(acc, baseCurrency, range, includeTransfersInCalc) }.toImmutableList()
        }

    private suspend fun accountData(
        acc: com.ivy.data.model.Account,
        baseCurrency: String,
        range: ClosedTimeRange,
        includeTransfersInCalc: Boolean,
    ): com.ivy.wallet.data.model.AccountData {
        val output = accountBalanceUseCase.balance(
            account = acc
        )
        val balance = output.balance

        val balanceBaseCurrency = if (acc.asset.code != baseCurrency) {
            exchangeUseCase.exchange(
                data = ExchangeData(
                    baseCurrency = baseCurrency,
                    fromCurrency = acc.asset.code.toOption()
                ),
                amount = balance
            ).getOrNull()
        } else {
            null
        }

        val incomeExpensePair = accountIncomeExpenseUseCase.incomeExpense(
            account = acc,
            range = range,
            includeTransfersInCalc = includeTransfersInCalc
        ).incomeExpensePair

        val creditCards = creditCardUses(acc)

        return com.ivy.wallet.data.model.AccountData(
            account = acc,
            balance = balance.toDouble(),
            balanceBaseCurrency = balanceBaseCurrency?.toDouble(),
            monthlyIncome = incomeExpensePair.income.toDouble(),
            monthlyExpenses = incomeExpensePair.expense.toDouble(),
            creditCards = creditCards,
            gain = output.gain?.toDouble(),
            cash = output.cash?.toDouble(),
        )
    }

    // A card's purchases are dated on the day it charges them: the ones ahead are what it owes.
    private suspend fun creditCardUses(
        acc: com.ivy.data.model.Account,
    ): ImmutableList<CreditCardUse> {
        val cards = creditCardRepository.findByAccount(acc.id)
        if (cards.isEmpty()) return persistentListOf()
        val pending = transactionRepository
            .findAllByAccountAndBetween(acc.id, timeProvider.utcNow(), ivyMaxTime())
            .filterIsInstance<Expense>()
        return cards.map { card ->
            CreditCardUse(
                card = card,
                used = pending.filter { it.metadata.creditCard == card.id }.sumOf { it.value.amount.value },
            )
        }.toImmutableList()
    }
}
