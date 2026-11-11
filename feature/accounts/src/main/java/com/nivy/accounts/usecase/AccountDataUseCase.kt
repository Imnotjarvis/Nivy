package com.nivy.accounts.usecase

import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.getFromValue
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.account.AccountBalanceUseCase
import com.nivy.domain.usecase.account.AccountIncomeExpenseUseCase
import com.nivy.domain.usecase.exchange.BaseCurrencyRates
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.wallet.data.model.CreditCardUse
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.utils.nivyMaxTime
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
        accounts: ImmutableList<com.nivy.data.model.Account>,
        baseCurrency: String,
        range: ClosedTimeRange,
        includeTransfersInCalc: Boolean = false,
    ): ImmutableList<com.nivy.wallet.data.model.AccountData> =
        withContext(dispatchers.io) {
            val rates = BaseCurrencyRates(exchangeUseCase, AssetCode.unsafe(baseCurrency))
            accounts
                .map { acc -> accountData(acc, baseCurrency, rates, range, includeTransfersInCalc) }
                .toImmutableList()
        }

    private suspend fun accountData(
        acc: com.nivy.data.model.Account,
        baseCurrency: String,
        rates: BaseCurrencyRates,
        range: ClosedTimeRange,
        includeTransfersInCalc: Boolean,
    ): com.nivy.wallet.data.model.AccountData {
        val output = accountBalanceUseCase.balance(
            account = acc
        )
        val balance = output.balance

        val balanceBaseCurrency = if (acc.asset.code != baseCurrency) {
            rates.inBaseOrNull(balance.toDouble(), acc.asset)?.toBigDecimal()
        } else {
            null
        }

        val incomeExpensePair = accountIncomeExpenseUseCase.incomeExpense(
            account = acc,
            range = range,
            includeTransfersInCalc = includeTransfersInCalc
        )

        val creditCards = creditCardUses(acc)

        return com.nivy.wallet.data.model.AccountData(
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

    // A card's purchases and transfers are dated on the day it charges them: the ones ahead are what it owes.
    private suspend fun creditCardUses(
        acc: com.nivy.data.model.Account,
    ): ImmutableList<CreditCardUse> {
        val cards = creditCardRepository.findByAccount(acc.id)
        if (cards.isEmpty()) return persistentListOf()
        val pending = transactionRepository
            .findAllByAccountAndBetween(acc.id, timeProvider.utcNow(), nivyMaxTime())
        return cards.map { card ->
            CreditCardUse(
                card = card,
                used = pending.filter { it.metadata.creditCard == card.id }.sumOf { it.getFromValue().amount.value },
            )
        }.toImmutableList()
    }
}
