package com.ivy.wallet.domain.action.transaction

import com.ivy.base.time.TimeConverter
import com.ivy.data.model.AccountId
import com.ivy.data.model.Transaction
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.TagRepository
import com.ivy.wallet.domain.pure.transaction.transactionsWithDateDividers
import com.ivy.wallet.domain.action.exchange.ExchangeAct
import com.ivy.wallet.domain.data.TransactionHistoryItem
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrnsWithDateDivsAct @Inject constructor(
    private val exchangeAct: ExchangeAct,
    private val tagRepository: TagRepository,
    private val accountRepository: AccountRepository,
    private val timeConverter: TimeConverter,
) {
    suspend operator fun invoke(input: Input): List<TransactionHistoryItem> = withContext(Dispatchers.IO) {
        transactionsWithDateDividers(
            transactions = input.transactions,
            baseCurrencyCode = input.baseCurrency,
            timeConverter = timeConverter,
            getTags = { tagIds -> tagRepository.findByIds(tagIds) },
            getAccount = { accountId -> accountRepository.findById(AccountId(accountId)) },
            exchange = exchangeAct::convert
        )
    }

    data class Input(
        val baseCurrency: String,
        val transactions: List<Transaction>
    )
}
