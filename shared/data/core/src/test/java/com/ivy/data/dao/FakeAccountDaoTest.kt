package com.ivy.data.dao

import com.ivy.data.db.dao.fake.FakeAccountDao
import com.ivy.data.db.entity.AccountEntity
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.UUID

class FakeAccountDaoTest {
    private lateinit var dao: FakeAccountDao

    @Before
    fun setup() {
        dao = FakeAccountDao()
    }

    @Test
    fun `find by id - existing id`() = runTest {
        // given
        val id = UUID.randomUUID()
        val account = AccountEntity(
            id = id,
            name = "Bank",
            currency = "BGN",
            color = 1,
            icon = null,
            includeInBalance = true,
            orderNum = 1.0,
            isSynced = true
        )

        // when
        dao.save(account)
        val res = dao.findById(id)

        // then
        res shouldBe account
    }

    @Test
    fun `find by id - not existing id`() = runTest {
        // given
        val id = UUID.randomUUID()

        // when
        val res = dao.findById(id)

        // then
        res shouldBe null
    }

    @Test
    fun `find all`() = runTest {
        // given
        val accounts = listOf(
            AccountEntity(
                id = UUID.randomUUID(),
                name = "Bank",
                currency = "BGN",
                color = 1,
                icon = null,
                includeInBalance = true,
                orderNum = 1.0,
            ),
            AccountEntity(
                id = UUID.randomUUID(),
                name = "Cash",
                currency = "BGN",
                color = 1,
                icon = null,
                includeInBalance = true,
                orderNum = 2.0,
            ),
        )

        // when
        dao.saveMany(accounts)
        val res = dao.findAll()

        // then
        res shouldBe accounts
    }

    @Test
    fun `find all - empty list`() = runTest {
        // given
        val accounts = emptyList<AccountEntity>()

        // when
        dao.saveMany(accounts)
        val res = dao.findAll()

        // then
        res shouldBe emptyList()
    }

    @Test
    fun `find max order num of accounts`() = runTest {
        // given
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val accounts = listOf(
            AccountEntity(
                id = id1,
                name = "Bank",
                currency = "BGN",
                color = 1,
                icon = null,
                includeInBalance = true,
                orderNum = 0.0,
            ),
            AccountEntity(
                id = id2,
                name = "Cash",
                currency = "BGN",
                color = 1,
                icon = null,
                includeInBalance = true,
                orderNum = 1.0,
            )
        )

        // when
        dao.saveMany(accounts)
        val res = dao.findMaxOrderNum()

        // then
        res shouldBe 1.0
    }

    @Test
    fun `find max order num of empty list of accounts`() = runTest {
        // given
        val accounts = emptyList<AccountEntity>()

        // when
        dao.saveMany(accounts)
        val res = dao.findMaxOrderNum()

        // then
        res shouldBe null
    }

    @Test
    fun `save - create new`() = runTest {
        // given
        val id = UUID.randomUUID()
        val account = AccountEntity(
            id = id,
            name = "Bank",
            currency = "BGN",
            color = 1,
            icon = null,
            includeInBalance = true,
            orderNum = 0.0,
        )

        // when
        dao.save(account)
        val res = dao.findById(account.id)

        // then
        res shouldBe account
    }

    @Test
    fun `save - update existing`() = runTest {
        // given
        val id = UUID.randomUUID()
        val account = AccountEntity(
            id = id,
            name = "Bank",
            currency = "BGN",
            color = 1,
            icon = null,
            includeInBalance = true,
            orderNum = 0.0,
        )

        // when
        dao.save(account)
        dao.save(account.copy(name = "Cash"))
        val res = dao.findById(id)

        // then
        res shouldBe AccountEntity(
            id = id,
            name = "Cash",
            currency = "BGN",
            color = 1,
            icon = null,
            includeInBalance = true,
            orderNum = 0.0,
        )
    }

    @Test
    fun `save many`() = runTest {
        // given
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val accounts = listOf(
            AccountEntity(
                id = id1,
                name = "Bank",
                currency = "BGN",
                color = 1,
                icon = null,
                includeInBalance = true,
                orderNum = 1.0,
            ),
            AccountEntity(
                id = id2,
                name = "Cash",
                currency = "BGN",
                color = 1,
                icon = null,
                includeInBalance = true,
                orderNum = 2.0,
            )
        )

        // when
        dao.saveMany(accounts)
        val res = dao.findAll()

        // then
        res shouldBe accounts
    }

    @Test
    fun `delete by id`() = runTest {
        // given
        val id = UUID.randomUUID()
        val accounts = listOf(
            AccountEntity(
                id = id,
                name = "Bank",
                currency = "BGN",
                color = 1,
                icon = null,
                includeInBalance = true,
                orderNum = 1.0,
            )
        )

        // when
        dao.saveMany(accounts)
        dao.deleteById(id)
        val res = dao.findAll()

        // then
        res shouldBe emptyList()
    }

    @Test
    fun `delete all`() = runTest {
        // given
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()

        // when
        dao.saveMany(
            listOf(
                AccountEntity(
                    id = id1,
                    name = "Bank",
                    currency = "BGN",
                    color = 1,
                    icon = null,
                    includeInBalance = true,
                    orderNum = 1.0,
                ),
                AccountEntity(
                    id = id2,
                    name = "Cash",
                    currency = "BGN",
                    color = 1,
                    icon = null,
                    includeInBalance = true,
                    orderNum = 2.0,
                )
            )
        )
        dao.deleteAll()
        val res = dao.findAll()

        // then
        res shouldBe emptyList()
    }
}
