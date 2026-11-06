package com.ivy.domain.usecase.planned

import com.ivy.base.threading.DispatchersProvider
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.AccountId
import com.ivy.data.model.Transaction
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.usecase.exchange.ExchangeUseCase
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import com.ivy.wallet.domain.pure.exchange.ExchangeTrnArgument
import com.ivy.wallet.domain.pure.exchange.exchangeInBaseCurrency
import com.ivy.wallet.domain.pure.transaction.expenses
import com.ivy.wallet.domain.pure.transaction.incomes
import com.ivy.wallet.domain.pure.transaction.sumTrns
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

class DueTransactionsInfoUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
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
            getAccount = { accountId -> accountRepository.findById(AccountId(accountId)) }
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
