package com.nivy.domain.usecase.planned

import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Transaction
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import com.nivy.wallet.domain.pure.exchange.ExchangeTrnArgument
import com.nivy.wallet.domain.pure.exchange.exchangeInBaseCurrency
import com.nivy.wallet.domain.pure.transaction.expenses
import com.nivy.wallet.domain.pure.transaction.incomes
import com.nivy.wallet.domain.pure.transaction.sumTrns
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

class DueTransactionsInfoUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val exchangeUseCase: ExchangeUseCase,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun info(
        range: ClosedTimeRange,
        baseCurrency: String,
        dueFilter: (dueDate: LocalDate, dateNow: LocalDate) -> Boolean,
    ): DueTransactionsInfo = withContext(dispatchers.io) {
        val dateNow = timeProvider.localDateNow()
        val dueTrns = transactionRepository.findAllDueToBetween(range.from, range.to).filter {
            dueFilter(with(timeConverter) { it.time.toLocalDate() }, dateNow)
        }

        // We have due transactions in different currencies
        val exchangeArg = ExchangeTrnArgument(
            baseCurrency = baseCurrency,
            exchange = exchangeUseCase::exchange,
        )

        DueTransactionsInfo(
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
}

data class DueTransactionsInfo(
    val dueIncomeExpense: IncomeExpensePair,
    val dueTrns: List<Transaction>
)
