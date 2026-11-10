package com.nivy.wallet.domain.pure.transaction

import com.nivy.base.TestDispatchersProvider
import com.nivy.data.db.dao.BalanceCases
import com.nivy.data.db.dao.fake.FakeAccountDao
import com.nivy.data.db.dao.fake.FakeTransactionDao
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.data.repository.fake.fakeRepositoryMemoFactory
import com.nivy.data.repository.mapper.AccountMapper
import com.nivy.data.repository.mapper.TransactionMapper
import com.nivy.domain.usecase.account.AccountTransactionsUseCase
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * An account's balance added up in memory, as its page does for each value entered (its
 * transactions as read, then [AccountValueFunctions.balance] one by one), passes [BalanceCases]
 * as the database's query does.
 */
class AccountValueFunctionsBalanceTest {
    @Test
    fun `an account's transactions, read and added up one by one, give the database's balance`() = runTest {
        // given
        val accountDao = FakeAccountDao().apply { saveMany(BalanceCases.accounts) }
        val transactionDao = FakeTransactionDao(accountDao).apply { saveMany(BalanceCases.transactions) }
        val currencies = CurrencyRepository(
            appPreferences = mockk<AppPreferences> { coEvery { baseCurrency() } returns null },
            dispatchersProvider = TestDispatchersProvider,
        )
        val accounts = AccountRepository(
            mapper = AccountMapper(currencies),
            accountDao = accountDao,
            writeAccountDao = accountDao,
            dispatchersProvider = TestDispatchersProvider,
            memoFactory = fakeRepositoryMemoFactory(),
        )
        val accountTransactions = AccountTransactionsUseCase(
            transactionRepository = TransactionRepository(
                mapper = TransactionMapper(accounts),
                transactionDao = transactionDao,
                writeTransactionDao = transactionDao,
                dispatchersProvider = TestDispatchersProvider,
                tagRepository = mockk(relaxed = true),
                databaseTransaction = mockk(),
            ),
            dispatchers = TestDispatchersProvider,
        )

        // then
        BalanceCases.cases.forEach { case ->
            withClue(case.description) {
                accountTransactions.transactions(case.account, ClosedTimeRange(from = case.start, to = case.end))
                    .sumOf { AccountValueFunctions.balance(it, case.account) }
                    .toDouble() shouldBe case.balance.plusOrMinus(BalanceCases.Cents)
            }
        }
    }
}
