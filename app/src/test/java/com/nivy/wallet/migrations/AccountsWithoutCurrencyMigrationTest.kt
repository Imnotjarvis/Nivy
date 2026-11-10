package com.nivy.wallet.migrations

import com.nivy.data.DataObserver
import com.nivy.data.db.dao.fake.FakeAccountDao
import com.nivy.data.db.entity.AccountEntity
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.repository.CurrencyRepository
import com.nivy.wallet.migrations.impl.AccountsWithoutCurrencyMigration
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountsWithoutCurrencyMigrationTest {
    @Test
    fun `an account stored without a currency gets the base currency`() = runTest {
        // given
        val accounts = FakeAccountDao()
        accounts.save(AccountEntity(name = "Cash", color = 0, currency = null))
        val migration = AccountsWithoutCurrencyMigration(
            accountDao = accounts,
            writeAccountDao = accounts,
            currencyRepository = mockk<CurrencyRepository> {
                coEvery { getBaseCurrency() } returns AssetCode.unsafe("EUR")
            },
            dataObserver = DataObserver(),
        )

        // when
        migration.migrate()

        // then
        accounts.findAll().single().currency shouldBe "EUR"
    }
}
