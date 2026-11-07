package com.nivy.domain.usecase.csv

import com.nivy.base.model.TransactionType
import com.nivy.data.model.AccountId
import com.nivy.data.model.CategoryId
import com.nivy.data.model.TransactionId
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.NonNegativeDouble
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import java.time.Instant

data class NivyCsvRow(
    val date: Instant?,
    val title: NotBlankTrimmedString?,
    val category: CategoryId?,
    val account: AccountId,
    val amount: NonNegativeDouble,
    val currency: AssetCode,
    val type: TransactionType,
    val transferAmount: PositiveDouble?,
    val transferCurrency: AssetCode?,
    val toAccountId: AccountId?,
    val receiveAmount: PositiveDouble?,
    val receiveCurrency: AssetCode?,
    val description: NotBlankTrimmedString?,
    val dueData: Instant?,
    val id: TransactionId
) {
    companion object {
        val Columns = listOf(
            "Date",
            "Title",
            "Category",
            "Account",
            "Amount",
            "Currency",
            "Type",
            "Transfer Amount",
            "Transfer Currency",
            "To Account",
            "Receive Amount",
            "Receive Currency",
            "Description",
            "Due Date",
            "ID",
        )
    }
}
