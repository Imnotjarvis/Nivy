package com.ivy.domain.usecase.transaction

import com.ivy.base.threading.DispatchersProvider
import com.ivy.base.time.TimeConverter
import com.ivy.data.model.AccountId
import com.ivy.data.model.Transaction
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.TagRepository
import com.ivy.domain.usecase.exchange.ExchangeUseCase
import com.ivy.wallet.domain.data.TransactionHistoryItem
import com.ivy.wallet.domain.pure.transaction.transactionsWithDateDividers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TransactionsWithDateDividersUseCase @Inject constructor(
    private val exchangeUseCase: ExchangeUseCase,
    private val tagRepository: TagRepository,
    private val accountRepository: AccountRepository,
    private val timeConverter: TimeConverter,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun withDateDividers(
        baseCurrency: String,
        transactions: List<Transaction>,
    ): List<TransactionHistoryItem> = withContext(dispatchers.io) {
        transactionsWithDateDividers(
            transactions = transactions,
            baseCurrencyCode = baseCurrency,
            timeConverter = timeConverter,
            getTags = { tagIds -> tagRepository.findByIds(tagIds) },
            getAccount = { accountId -> accountRepository.findById(AccountId(accountId)) },
            exchange = exchangeUseCase::exchange
        )
    }
}
