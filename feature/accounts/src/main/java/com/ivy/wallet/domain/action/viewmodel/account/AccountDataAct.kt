package com.ivy.wallet.domain.action.viewmodel.account

import arrow.core.toOption
import com.ivy.base.model.AccountType
import com.ivy.wallet.domain.action.account.CalcAccBalanceAct
import com.ivy.wallet.domain.action.account.CalcAccIncomeExpenseAct
import com.ivy.wallet.domain.action.exchange.ExchangeAct
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.exchange.ExchangeData
import com.ivy.wallet.utils.ivyMaxTime
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccountDataAct @Inject constructor(
    private val exchangeAct: ExchangeAct,
    private val calcAccBalanceAct: CalcAccBalanceAct,
    private val calcAccIncomeExpenseAct: CalcAccIncomeExpenseAct
) {
    suspend operator fun invoke(input: Input): ImmutableList<com.ivy.wallet.data.model.AccountData> =
        withContext(Dispatchers.IO) {
            input.accounts.map { acc -> accountData(acc, input) }.toImmutableList()
        }

    private suspend fun accountData(
        acc: com.ivy.data.model.Account,
        input: Input,
    ): com.ivy.wallet.data.model.AccountData {
        val balance = calcAccBalanceAct(
            CalcAccBalanceAct.Input(
                account = acc
            )
        ).balance

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

        val creditUsed = if (acc.type == AccountType.CREDIT_CARD) {
            // The balance with the purchases dated on their future charge dates.
            val balanceWithPendingCharges = calcAccBalanceAct(
                CalcAccBalanceAct.Input(
                    account = acc,
                    range = ClosedTimeRange.to(ivyMaxTime())
                )
            ).balance
            maxOf(0.0, -balanceWithPendingCharges.toDouble())
        } else {
            null
        }

        return com.ivy.wallet.data.model.AccountData(
            account = acc,
            balance = balance.toDouble(),
            balanceBaseCurrency = balanceBaseCurrency?.toDouble(),
            monthlyIncome = incomeExpensePair.income.toDouble(),
            monthlyExpenses = incomeExpensePair.expense.toDouble(),
            creditUsed = creditUsed,
        )
    }

    data class Input(
        val accounts: ImmutableList<com.ivy.data.model.Account>,
        val baseCurrency: String,
        val range: ClosedTimeRange,
        val includeTransfersInCalc: Boolean = false
    )
}
