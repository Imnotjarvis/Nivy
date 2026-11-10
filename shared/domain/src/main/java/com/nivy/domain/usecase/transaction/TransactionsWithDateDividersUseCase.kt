package com.nivy.domain.usecase.transaction

import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeConverter
import com.nivy.data.model.Transaction
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.repository.TagRepository
import com.nivy.domain.usecase.exchange.BaseCurrencyRates
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.wallet.domain.data.TransactionHistoryItem
import com.nivy.wallet.domain.pure.transaction.transactionsWithDateDividers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TransactionsWithDateDividersUseCase @Inject constructor(
    private val exchangeUseCase: ExchangeUseCase,
    private val tagRepository: TagRepository,
    private val timeConverter: TimeConverter,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun withDateDividers(
        baseCurrency: String,
        transactions: List<Transaction>,
    ): List<TransactionHistoryItem> = withContext(dispatchers.io) {
        transactionsWithDateDividers(
            transactions = transactions,
            timeConverter = timeConverter,
            rates = BaseCurrencyRates(exchangeUseCase, AssetCode.unsafe(baseCurrency)),
            getTags = { tagIds -> tagRepository.findByIds(tagIds) },
        )
    }
}
