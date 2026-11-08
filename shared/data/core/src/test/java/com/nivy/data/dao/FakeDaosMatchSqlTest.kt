package com.nivy.data.dao

import com.nivy.base.model.LoanType
import com.nivy.base.model.TransactionType
import com.nivy.data.db.dao.fake.FakeBudgetDao
import com.nivy.data.db.dao.fake.FakeLoanDao
import com.nivy.data.db.dao.fake.FakeLoanRecordDao
import com.nivy.data.db.dao.fake.FakePlannedPaymentDao
import com.nivy.data.db.dao.fake.FakeTagAssociationDao
import com.nivy.data.db.dao.fake.FakeTagDao
import com.nivy.data.db.dao.fake.FakeTransactionDao
import com.nivy.data.db.entity.BudgetEntity
import com.nivy.data.db.entity.LoanEntity
import com.nivy.data.db.entity.LoanRecordEntity
import com.nivy.data.db.entity.PlannedPaymentRuleEntity
import com.nivy.data.db.entity.TagAssociationEntity
import com.nivy.data.db.entity.TagEntity
import com.nivy.data.db.entity.TransactionEntity
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.util.UUID

/**
 * The fakes answer as the DAOs' SQL does, so that a test passing on a fake would pass on Room:
 * Room's saves are upserts, and the queries' conditions are all there.
 */
class FakeDaosMatchSqlTest {

    @Test
    fun `saving again what has the same key replaces it, as Room's upsert does`() = runTest {
        val budgets = FakeBudgetDao()
        val budget = BudgetEntity("Food", 400.0, null, null, orderId = 0.0)
        budgets.save(budget)
        budgets.save(budget.copy(amount = 500.0))
        budgets.findAll() shouldBe listOf(budget.copy(amount = 500.0))

        val loans = FakeLoanDao()
        val loan = LoanEntity("Car", 1_000.0, LoanType.BORROW)
        loans.save(loan)
        loans.save(loan.copy(amount = 900.0))
        loans.findAll() shouldBe listOf(loan.copy(amount = 900.0))

        val records = FakeLoanRecordDao()
        val record = LoanRecordEntity(loanId = loan.id, amount = 100.0, dateTime = Now)
        records.save(record)
        records.save(record.copy(amount = 150.0))
        records.findAll() shouldBe listOf(record.copy(amount = 150.0))

        val rules = FakePlannedPaymentDao()
        val rule = PlannedPaymentRuleEntity(Now, 1, null, oneTime = false, TransactionType.EXPENSE, UUID.randomUUID())
        rules.save(rule)
        rules.save(rule.copy(amount = 30.0))
        rules.findAll() shouldBe listOf(rule.copy(amount = 30.0))

        val tags = FakeTagDao()
        val tag = TagEntity(UUID.randomUUID(), "Trip", null, 0, null, 0.0, Now, false, Now)
        tags.save(tag)
        tags.save(listOf(tag.copy(name = "Holiday")))
        tags.findAll() shouldBe listOf(tag.copy(name = "Holiday"))

        val associations = FakeTagAssociationDao()
        val association = TagAssociationEntity(tag.id, UUID.randomUUID(), Now, isDeleted = false)
        associations.save(association)
        associations.save(listOf(association.copy(lastSyncedTime = Later)))
        associations.findAll() shouldBe listOf(association.copy(lastSyncedTime = Later))
    }

    @Test
    fun `a title pattern matches as SQL's LIKE does, a percent sign for any text and letters in any case`() =
        runTest {
            val dao = FakeTransactionDao()
            listOf("Lunch", "lunch at work", "Brunch").forEach { dao.save(transaction(title = it)) }

            dao.findAllByTitleMatchingPattern("LUN%").map { it.title } shouldBe listOf("Lunch", "lunch at work")
        }

    @Test
    fun `a loan's own transaction isn't one of its records'`() = runTest {
        val dao = FakeTransactionDao()
        val loanId = UUID.randomUUID()
        val recordPayment = transaction(loanId = loanId, loanRecordId = UUID.randomUUID())
        val loanPayment = transaction(loanId = loanId)
        dao.save(recordPayment)
        dao.save(loanPayment)

        dao.findLoanTransaction(loanId) shouldBe loanPayment
    }

    @Test
    fun `deleting a planned payment rule's unpaid payments keeps the paid ones`() = runTest {
        val dao = FakeTransactionDao()
        val ruleId = UUID.randomUUID()
        val paid = transaction(recurringRuleId = ruleId, dateTime = Now)
        dao.save(transaction(recurringRuleId = ruleId, dueDate = Later))
        dao.save(paid)

        dao.deletedByRecurringRuleIdAndNoDateTime(ruleId)

        dao.findAll() shouldBe listOf(paid)
    }

    private fun transaction(
        title: String? = null,
        loanId: UUID? = null,
        loanRecordId: UUID? = null,
        recurringRuleId: UUID? = null,
        dateTime: Instant? = null,
        dueDate: Instant? = null,
    ) = TransactionEntity(
        accountId = Account,
        type = TransactionType.EXPENSE,
        amount = 10.0,
        title = title,
        dateTime = dateTime,
        dueDate = dueDate,
        recurringRuleId = recurringRuleId,
        loanId = loanId,
        loanRecordId = loanRecordId,
    )

    private companion object {
        val Account: UUID = UUID.randomUUID()
        val Now: Instant = Instant.parse("2026-11-08T10:00:00Z")
        val Later: Instant = Instant.parse("2026-11-20T10:00:00Z")
    }
}
