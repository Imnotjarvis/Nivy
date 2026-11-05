package com.ivy.wallet.domain.action.viewmodel.account

import arrow.core.toOption
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Expense
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.wallet.data.model.CreditCardUse
import com.ivy.wallet.domain.action.account.CalcAccBalanceAct
import com.ivy.wallet.domain.action.account.CalcAccIncomeExpenseAct
import com.ivy.wallet.domain.action.exchange.ExchangeAct
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.exchange.ExchangeData
import com.ivy.wallet.utils.ivyMaxTime
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccountDataAct @Inject constructor(
    private val exchangeAct: ExchangeAct,
    private val calcAccBalanceAct: CalcAccBalanceAct,
    private val calcAccIncomeExpenseAct: CalcAccIncomeExpenseAct,
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(input: Input): ImmutableList<com.ivy.wallet.data.model.AccountData> =
        withContext(Dispatchers.IO) {
            input.accounts.map { acc -> accountData(acc, input) }.toImmutableList()
        }

    private suspend fun accountData(
        acc: com.ivy.data.model.Account,
        input: Input,
    ): com.ivy.wallet.data.model.AccountData {
        val output = calcAccBalanceAct(
            CalcAccBalanceAct.Input(
                account = acc
            )
        )
        val balance = output.balance

        val balanceBaseCurrency = if (acc.asset.code != input.baseCurrency) {
            exchangeAct(
                ExchangeAct.Input(
                    data = ExchangeData(
                        baseCurrency = input.baseCurrency,
                        fromCurrency = acc.asset.code.toOption()
                    ),
                    amount = balance
                )
            ).getOrNull()
        } else {
            null
        }

        val incomeExpensePair = calcAccIncomeExpenseAct(
            CalcAccIncomeExpenseAct.Input(
                account = acc,
                range = input.range,
                includeTransfersInCalc = input.includeTransfersInCalc
            )
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

    data class Input(
        val accounts: ImmutableList<com.ivy.data.model.Account>,
        val baseCurrency: String,
        val range: ClosedTimeRange,
        val includeTransfersInCalc: Boolean = false
    )
}
