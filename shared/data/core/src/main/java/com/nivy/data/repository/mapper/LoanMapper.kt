package com.nivy.data.repository.mapper

import arrow.core.Either
import arrow.core.raise.either
import com.nivy.data.db.entity.LoanEntity
import com.nivy.data.db.entity.LoanRecordEntity
import com.nivy.data.model.AccountId
import com.nivy.data.model.Loan
import com.nivy.data.model.LoanId
import com.nivy.data.model.LoanRecord
import com.nivy.data.model.LoanRecordId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import javax.inject.Inject

class LoanMapper @Inject constructor() {
    fun LoanEntity.toDomain(): Either<String, Loan> = either {
        Loan(
            id = LoanId(id),
            name = NotBlankTrimmedString.from(name).bind(),
            amount = PositiveDouble.from(amount).bind(),
            type = type,
            color = ColorInt(color),
            icon = icon?.let(IconAsset::from)?.getOrNull(),
            account = accountId?.let(::AccountId),
            note = note?.let(NotBlankTrimmedString::from)?.getOrNull(),
            dateTime = dateTime,
            orderNum = orderNum,
        )
    }

    fun Loan.toEntity(): LoanEntity = LoanEntity(
        name = name.value,
        amount = amount.value,
        type = type,
        color = color.value,
        icon = icon?.id,
        orderNum = orderNum,
        accountId = account?.value,
        note = note?.value,
        dateTime = dateTime,
        id = id.value,
    )

    fun LoanRecordEntity.toDomain(): Either<String, LoanRecord> = either {
        LoanRecord(
            id = LoanRecordId(id),
            loan = LoanId(loanId),
            amount = PositiveDouble.from(amount).bind(),
            note = note?.let(NotBlankTrimmedString::from)?.getOrNull(),
            dateTime = dateTime,
            interest = interest,
            account = accountId?.let(::AccountId),
            convertedAmount = convertedAmount?.let(PositiveDouble::from)?.getOrNull(),
            type = loanRecordType,
        )
    }

    fun LoanRecord.toEntity(): LoanRecordEntity = LoanRecordEntity(
        loanId = loan.value,
        amount = amount.value,
        note = note?.value,
        dateTime = dateTime,
        interest = interest,
        accountId = account?.value,
        convertedAmount = convertedAmount?.value,
        loanRecordType = type,
        id = id.value,
    )
}
