package com.ivy.wallet.domain.action.viewmodel.home

import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Transaction
import com.ivy.wallet.domain.action.account.AccountByIdAct
import com.ivy.wallet.domain.action.exchange.ExchangeAct
import com.ivy.wallet.domain.action.transaction.DueTrnsAct
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import com.ivy.wallet.domain.pure.exchange.ExchangeTrnArgument
import com.ivy.wallet.domain.pure.exchange.exchangeInBaseCurrency
import com.ivy.wallet.domain.pure.transaction.expenses
import com.ivy.wallet.domain.pure.transaction.incomes
import com.ivy.wallet.domain.pure.transaction.sumTrns
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DueTrnsInfoAct @Inject constructor(
    private val dueTrnsAct: DueTrnsAct,
    private val accountByIdAct: AccountByIdAct,
    private val exchangeAct: ExchangeAct,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) {
    suspend operator fun invoke(input: Input): Output = withContext(Dispatchers.IO) {
        val dateNow = timeProvider.localDateNow()
        val dueTrns = dueTrnsAct(input.range).filter {
            input.dueFilter(with(timeConverter) { it.time.toLocalDate() }, dateNow)
        }

        // We have due transactions in different currencies
        val exchangeArg = ExchangeTrnArgument(
            baseCurrency = input.baseCurrency,
            exchange = exchangeAct::convert,
            getAccount = { accountId -> accountByIdAct(accountId) }
        )

        Output(
            dueIncomeExpense = IncomeExpensePair(
                income = sumTrns(
                    incomes(dueTrns),
                    ::exchangeInBaseCurrency,
                    exchangeArg
                ),
                expense = sumTrns(
                    expenses(dueTrns),
                    ::exchangeInBaseCurrency,
                    exchangeArg
                )
            ),
            dueTrns = dueTrns
        )
    }

    data class Input(
        val range: ClosedTimeRange,
        val baseCurrency: String,
        val dueFilter: (dueDate: LocalDate, dateNow: LocalDate) -> Boolean
    )

    data class Output(
        val dueIncomeExpense: IncomeExpensePair,
        val dueTrns: List<Transaction>
    )
}
