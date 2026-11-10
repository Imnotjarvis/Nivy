package com.nivy.loans

import com.nivy.base.model.LoanRecordType
import com.nivy.base.model.LoanType
import com.nivy.data.model.Loan
import com.nivy.data.model.LoanId
import com.nivy.data.model.LoanRecord
import com.nivy.data.model.LoanRecordId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.Instant
import java.util.UUID

class LoanProgressTest {
    private val loan = Loan(
        id = LoanId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe("Car"),
        amount = PositiveDouble.unsafe(1_000.0),
        type = LoanType.BORROW,
        color = ColorInt(0),
        icon = null,
        account = null,
        note = null,
        dateTime = null,
        orderNum = 0.0,
    )

    @Test
    fun `without records nothing is paid yet`() {
        // when
        val progress = loan.progress(emptyList())

        // then
        progress shouldBe LoanProgress(total = 1_000.0, paid = 0.0, interestPaid = 0.0)
        progress.percentPaid shouldBe 0.0
        progress.left shouldBe 1_000.0
    }

    @Test
    fun `decreases pay the loan back and increases add to it`() {
        // when
        val progress = loan.progress(
            listOf(increase(500.0), decrease(300.0), decrease(200.0))
        )

        // then
        progress shouldBe LoanProgress(total = 1_500.0, paid = 500.0, interestPaid = 0.0)
        progress.percentPaid shouldBe (1.0 / 3).plusOrMinus(Tolerance)
        progress.left shouldBe 1_000.0
    }

    @Test
    fun `interest is paid apart and doesn't pay the loan back`() {
        // when
        val progress = loan.progress(listOf(decrease(100.0, interest = true), decrease(400.0)))

        // then
        progress shouldBe LoanProgress(total = 1_000.0, paid = 400.0, interestPaid = 100.0)
        progress.percentPaid shouldBe 0.4
        progress.interestPercentPaid shouldBe 0.1
    }

    @Test
    fun `an interest record that increased the loan counts for nothing`() {
        // when: older data, the record dialog no longer offers it
        val progress = loan.progress(listOf(increase(50.0, interest = true), decrease(1_000.0)))

        // then: paid off, on the loans list and on the loan's page alike
        progress shouldBe LoanProgress(total = 1_000.0, paid = 1_000.0, interestPaid = 0.0)
        progress.percentPaid shouldBe 1.0
    }

    @Test
    fun `a record in another currency counts its converted amount`() {
        // when
        val progress = loan.progress(listOf(decrease(100.0, converted = 90.0)))

        // then
        progress.paid shouldBe 90.0
    }

    @Test
    fun `a loan paid back in equal payments with cents is paid off`() {
        // twelve payments of 50.02 add up, as doubles, to 600.2399999999999
        val progress = loan.copy(amount = PositiveDouble.unsafe(600.24))
            .progress(List(12) { record(LoanRecordType.DECREASE, 50.02, interest = false) })

        progress.paidOff shouldBe true
    }

    @Test
    fun `a loan with a cent left isn't paid off`() {
        val progress = loan.copy(amount = PositiveDouble.unsafe(100.0))
            .progress(listOf(record(LoanRecordType.DECREASE, 99.99, interest = false)))

        progress.paidOff shouldBe false
    }

    @Test
    fun `without a total the shares are 0, not a division by zero`() {
        // when
        val progress = LoanProgress.None

        // then
        progress.percentPaid shouldBe 0.0
        progress.interestPercentPaid shouldBe 0.0
    }

    private fun increase(amount: Double, interest: Boolean = false) =
        record(LoanRecordType.INCREASE, amount, interest = interest)

    private fun decrease(amount: Double, interest: Boolean = false, converted: Double? = null) =
        record(LoanRecordType.DECREASE, amount, interest = interest, converted = converted)

    private fun record(
        type: LoanRecordType,
        amount: Double,
        interest: Boolean,
        converted: Double? = null,
    ) = LoanRecord(
        id = LoanRecordId(UUID.randomUUID()),
        loan = loan.id,
        amount = PositiveDouble.unsafe(amount),
        note = null,
        dateTime = Instant.parse("2026-11-01T09:00:00Z"),
        interest = interest,
        account = null,
        convertedAmount = converted?.let { PositiveDouble.unsafe(it) },
        type = type,
    )

    private companion object {
        const val Tolerance = 1e-9
    }
}
