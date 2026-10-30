package com.ivy.data.model

import com.ivy.base.model.LoanRecordType
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.sync.Identifiable
import com.ivy.data.model.sync.UniqueId
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@JvmInline
value class LoanId(override val value: UUID) : UniqueId

/**
 * @property account the account the loan's money moved through, if any.
 * @property dateTime when the loan was made, in local time.
 */
data class Loan(
    override val id: LoanId,
    val name: NotBlankTrimmedString,
    val amount: PositiveDouble,
    val type: LoanType,
    val color: ColorInt,
    val icon: IconAsset?,
    val account: AccountId?,
    val note: NotBlankTrimmedString?,
    val dateTime: LocalDateTime?,
    override val orderNum: Double,
) : Identifiable<LoanId>, Reorderable

@JvmInline
value class LoanRecordId(override val value: UUID) : UniqueId

/**
 * A payment that increases or decreases a loan.
 *
 * @property convertedAmount the amount in the loan's currency, when [account] has another one.
 */
data class LoanRecord(
    override val id: LoanRecordId,
    val loan: LoanId,
    val amount: PositiveDouble,
    val note: NotBlankTrimmedString?,
    val dateTime: Instant,
    val interest: Boolean,
    val account: AccountId?,
    val convertedAmount: PositiveDouble?,
    val type: LoanRecordType,
) : Identifiable<LoanRecordId>
