package com.nivy.domain.usecase

import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.db.dao.fake.FakeAccountDao
import com.nivy.data.db.dao.fake.FakeAccountValueDao
import com.nivy.data.db.dao.fake.FakeExchangeRatesDao
import com.nivy.data.db.dao.fake.FakeTagAssociationDao
import com.nivy.data.db.dao.fake.FakeTagDao
import com.nivy.data.db.dao.fake.FakeTransactionDao
import com.nivy.data.db.entity.AccountEntity
import com.nivy.data.db.entity.ExchangeRateEntity
import com.nivy.data.db.entity.TransactionEntity
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Tag
import com.nivy.data.model.TagId
import com.nivy.data.model.primitive.AssociationId
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.AccountValueRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.ExchangeRatesRepository
import com.nivy.data.repository.TagRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.data.repository.fake.fakeRepositoryMemoFactory
import com.nivy.data.repository.mapper.AccountMapper
import com.nivy.data.repository.mapper.AccountValueMapper
import com.nivy.data.repository.mapper.ExchangeRateMapper
import com.nivy.data.repository.mapper.TagMapper
import com.nivy.data.repository.mapper.TransactionMapper
import com.nivy.domain.usecase.account.AccountBalanceUseCase
import com.nivy.domain.usecase.account.AccountTransactionsUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/**
 * The real repositories and use cases behind Home's numbers, over in-memory DAOs, with euros as the
 * base currency and November 15, 2026 as today. Tests add accounts, transactions and rates, then
 * ask the use case they test.
 */
internal class HomeNumbers {
    val accountDao = FakeAccountDao()
    val transactionDao = FakeTransactionDao(accountDao)
    private val ratesDao = FakeExchangeRatesDao()
    private val currencies = CurrencyRepository(
        appPreferences = mockk<AppPreferences> { coEvery { baseCurrency() } returns Euros },
        dispatchersProvider = TestDispatchersProvider,
    )
    val accounts = AccountRepository(
        mapper = AccountMapper(currencies),
        accountDao = accountDao,
        writeAccountDao = accountDao,
        dispatchersProvider = TestDispatchersProvider,
        memoFactory = fakeRepositoryMemoFactory(),
    )
    private val databaseTransaction = object : DatabaseTransaction(mockk(), mockk()) {
        override suspend fun <R> run(block: suspend () -> R): R = block()
    }
    val tagLinkDao = FakeTagAssociationDao(transactions = transactionDao)
    private val tagDao = FakeTagDao(associations = tagLinkDao)
    val tags = TagRepository(
        mapper = TagMapper(),
        tagDao = tagDao,
        tagAssociationDao = tagLinkDao,
        writeTagDao = tagDao,
        writeTagAssociationDao = tagLinkDao,
        dispatchersProvider = TestDispatchersProvider,
        memoFactory = fakeRepositoryMemoFactory(),
        databaseTransaction = databaseTransaction,
    )
    val transactions = TransactionRepository(
        mapper = TransactionMapper(accounts),
        transactionDao = transactionDao,
        writeTransactionDao = transactionDao,
        dispatchersProvider = TestDispatchersProvider,
        tagRepository = tags,
        databaseTransaction = databaseTransaction,
    )
    val exchange = ExchangeUseCase(
        exchangeRatesRepository = ExchangeRatesRepository(
            mapper = ExchangeRateMapper(),
            exchangeRatesDao = ratesDao,
            writeExchangeRatesDao = ratesDao,
            remoteExchangeRatesDataSource = mockk(),
            dispatchers = TestDispatchersProvider,
        ),
        currencyRepository = currencies,
    )
    private val accountValueDao = FakeAccountValueDao()
    val accountBalance = AccountBalanceUseCase(
        transactionRepository = transactions,
        accountValueRepository = AccountValueRepository(
            accountValueDao = accountValueDao,
            writeAccountValueDao = accountValueDao,
            mapper = AccountValueMapper(),
            dispatchersProvider = TestDispatchersProvider,
        ),
        timeProvider = Today,
        dispatchers = TestDispatchersProvider,
    )
    val accountTransactions = AccountTransactionsUseCase(
        transactionRepository = transactions,
        dispatchers = TestDispatchersProvider,
    )

    /** An account saved in [currency], in the balance unless [included] is false. */
    suspend fun account(name: String, currency: String = Euros, included: Boolean = true): Account {
        val entity = AccountEntity(name = name, currency = currency, color = 0, includeInBalance = included)
        accountDao.save(entity)
        return checkNotNull(accounts.findById(AccountId(entity.id)))
    }

    /** A tag named [name], on [transactions]. */
    suspend fun tag(name: String, vararg transactions: UUID): Tag {
        val tag = Tag(
            id = TagId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            creationTimestamp = Instant.EPOCH,
        )
        tags.save(tag)
        transactions.forEach { tags.associateTagToEntity(AssociationId(it), tag.id) }
        return tag
    }

    /** What one euro buys of [currency]. */
    suspend fun rate(currency: String, perEuro: Double) {
        ratesDao.save(ExchangeRateEntity(baseCurrency = Euros, currency = currency, rate = perEuro))
    }

    suspend fun income(account: Account, amount: Double, on: Instant = Nov3, category: UUID? = null) =
        save(account, TransactionType.INCOME, amount, on, category)

    suspend fun expense(account: Account, amount: Double, on: Instant = Nov3, category: UUID? = null) =
        save(account, TransactionType.EXPENSE, amount, on, category)

    suspend fun transfer(from: Account, to: Account, amount: Double, toAmount: Double = amount, on: Instant = Nov3) {
        transactionDao.save(
            TransactionEntity(
                accountId = from.id.value,
                type = TransactionType.TRANSFER,
                amount = amount,
                toAccountId = to.id.value,
                toAmount = toAmount,
                dateTime = on,
            )
        )
    }

    /** A planned income or expense, due on [due] and not paid. */
    suspend fun planned(account: Account, type: TransactionType, amount: Double, due: Instant) {
        transactionDao.save(
            TransactionEntity(accountId = account.id.value, type = type, amount = amount, dueDate = due)
        )
    }

    /** Saves the transaction, and returns its id. */
    private suspend fun save(
        account: Account,
        type: TransactionType,
        amount: Double,
        on: Instant,
        category: UUID?,
    ): UUID {
        val entity = TransactionEntity(
            accountId = account.id.value,
            type = type,
            amount = amount,
            dateTime = on,
            categoryId = category,
        )
        transactionDao.save(entity)
        return entity.id
    }

    companion object {
        const val Euros = "EUR"
        val Nov1: Instant = Instant.parse("2026-11-01T00:00:00Z")
        val Nov3: Instant = Instant.parse("2026-11-03T12:00:00Z")
        val Nov30: Instant = Instant.parse("2026-11-30T23:59:59Z")
        val Oct20: Instant = Instant.parse("2026-10-20T12:00:00Z")

        /** November 15, 2026, noon, in UTC. */
        val Today = object : TimeProvider {
            private val now = LocalDateTime.of(2026, 11, 15, 12, 0)
            override fun getZoneId(): ZoneId = ZoneId.of("UTC")
            override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
            override fun localNow(): LocalDateTime = now
            override fun localDateNow(): LocalDate = now.toLocalDate()
            override fun localTimeNow(): LocalTime = now.toLocalTime()
        }

        val timeConverter = TestTimeConverter()
    }
}
