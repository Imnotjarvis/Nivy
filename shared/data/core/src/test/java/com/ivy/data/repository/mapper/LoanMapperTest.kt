package com.ivy.data.repository.mapper

import com.ivy.base.model.LoanRecordType
import com.ivy.data.db.entity.LoanEntity
import com.ivy.data.db.entity.LoanRecordEntity
import com.ivy.data.model.AccountId
import com.ivy.data.model.Loan
import com.ivy.data.model.LoanId
import com.ivy.data.model.LoanRecord
import com.ivy.data.model.LoanRecordId
import com.ivy.base.model.LoanType
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class LoanMapperTest {
    private val mapper = LoanMapper()

    @Test
    fun `maps loan entity to domain and back`() {
        // when
        val loan = with(mapper) { ValidLoanEntity.toDomain() }.shouldBeRight()

        // then
        loan shouldBe ValidLoan
        with(mapper) { loan.toEntity() } shouldBe ValidLoanEntity
    }

    @Test
    fun `blank loan note is dropped`() {
        // when
        val loan = with(mapper) { ValidLoanEntity.copy(note = "  ").toDomain() }

        // then
        loan.shouldBeRight().note shouldBe null
    }

    @Test
    fun `loan without a name or an amount is not valid`() {
        with(mapper) { ValidLoanEntity.copy(name = "").toDomain() }.shouldBeLeft()
        with(mapper) { ValidLoanEntity.copy(amount = 0.0).toDomain() }.shouldBeLeft()
    }

    @Test
    fun `maps loan record entity to domain and back`() {
        // when
        val record = with(mapper) { ValidRecordEntity.toDomain() }.shouldBeRight()

        // then
        record shouldBe ValidRecord
        with(mapper) { record.toEntity() } shouldBe ValidRecordEntity
    }

    @Test
    fun `loan record without an amount is not valid`() {
        with(mapper) { ValidRecordEntity.copy(amount = -1.0).toDomain() }.shouldBeLeft()
    }

    companion object {
        val LoanUuid: UUID = UUID.randomUUID()
        val RecordUuid: UUID = UUID.randomUUID()
        val AccountUuid: UUID = UUID.randomUUID()
        val LoanDate: LocalDateTime = LocalDateTime.of(2024, 3, 1, 10, 30)
        val RecordDate: Instant = Instant.parse("2024-03-02T08:00:00Z")

        val ValidLoanEntity = LoanEntity(
            name = "Car",
            amount = 5000.0,
            type = LoanType.BORROW,
            color = 42,
            icon = "car",
            orderNum = 1.0,
            accountId = AccountUuid,
            note = "From the bank",
            dateTime = LoanDate,
            id = LoanUuid,
        )

        val ValidLoan = Loan(
            id = LoanId(LoanUuid),
            name = NotBlankTrimmedString.unsafe("Car"),
            amount = PositiveDouble.unsafe(5000.0),
            type = LoanType.BORROW,
            color = ColorInt(42),
            icon = IconAsset.unsafe("car"),
            account = AccountId(AccountUuid),
            note = NotBlankTrimmedString.unsafe("From the bank"),
            dateTime = LoanDate,
            orderNum = 1.0,
        )

        val ValidRecordEntity = LoanRecordEntity(
            loanId = LoanUuid,
            amount = 100.0,
            note = "March",
            dateTime = RecordDate,
            interest = true,
            accountId = AccountUuid,
            convertedAmount = 110.0,
            loanRecordType = LoanRecordType.INCREASE,
            id = RecordUuid,
        )

        val ValidRecord = LoanRecord(
            id = LoanRecordId(RecordUuid),
            loan = LoanId(LoanUuid),
            amount = PositiveDouble.unsafe(100.0),
            note = NotBlankTrimmedString.unsafe("March"),
            dateTime = RecordDate,
            interest = true,
            account = AccountId(AccountUuid),
            convertedAmount = PositiveDouble.unsafe(110.0),
            type = LoanRecordType.INCREASE,
        )
    }
}
