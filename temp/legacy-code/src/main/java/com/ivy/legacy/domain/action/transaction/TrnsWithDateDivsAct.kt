package com.ivy.wallet.domain.action.transaction

import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.time.TimeConverter
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.TagRepository
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import com.ivy.legacy.domain.pure.transaction.LegacyTrnDateDividers
import com.ivy.legacy.domain.pure.transaction.transactionsWithDateDividers
import com.ivy.wallet.domain.action.exchange.ExchangeAct
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrnsWithDateDivsAct @Inject constructor(
    private val accountDao: AccountDao,
    private val exchangeAct: ExchangeAct,
    private val tagRepository: TagRepository,
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(input: Input): List<TransactionHistoryItem> = withContext(Dispatchers.IO) {
        transactionsWithDateDividers(
            transactions = input.transactions,
            baseCurrencyCode = input.baseCurrency,
            getTags = { tagIds -> tagRepository.findByIds(tagIds) },
            getAccount = { accountId -> accountDao.findById(accountId)?.toLegacyDomain() },
            accountRepository = accountRepository,
            exchange = exchangeAct::convert
        )
    }

    data class Input(
        val baseCurrency: String,
        val transactions: List<com.ivy.data.model.Transaction>
    )
}

@Deprecated("Uses legacy Transaction")
class LegacyTrnsWithDateDivsAct @Inject constructor(
    private val accountDao: AccountDao,
    private val exchangeAct: ExchangeAct,
    private val timeConverter: TimeConverter,
) {
    suspend operator fun invoke(input: Input): List<TransactionHistoryItem> = withContext(Dispatchers.IO) {
        LegacyTrnDateDividers.transactionsWithDateDividers(
            transactions = input.transactions,
            baseCurrencyCode = input.baseCurrency,
            getAccount = { accountId -> accountDao.findById(accountId)?.toLegacyDomain() },
            exchange = exchangeAct::convert,
            timeConverter = timeConverter,
        )
    }

    data class Input(
        val baseCurrency: String,
        val transactions: List<Transaction>
    )
}