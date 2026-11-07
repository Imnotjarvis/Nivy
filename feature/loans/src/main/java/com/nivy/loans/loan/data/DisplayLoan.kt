package com.nivy.loans.loan.data

import com.nivy.data.model.Loan
import com.nivy.wallet.utils.getDefaultFIATCurrency
import com.nivy.wallet.domain.data.Reorderable

data class DisplayLoan(
    val loan: Loan,
    val loanTotalAmount: Double,
    val amountPaid: Double,
    val currencyCode: String? = getDefaultFIATCurrency().currencyCode,
    val formattedDisplayText: String = "",
    val percentPaid: Double = 0.0
) : Reorderable {
    override fun getItemOrderNum(): Double {
        return loan.orderNum
    }

    override fun withNewOrderNum(newOrderNum: Double): Reorderable {
        return this.copy(
            loan = loan.copy(
                orderNum = newOrderNum
            )
        )
    }
}
