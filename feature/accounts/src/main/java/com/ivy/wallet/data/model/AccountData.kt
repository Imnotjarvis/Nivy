package com.ivy.wallet.data.model

import androidx.compose.runtime.Immutable
import com.ivy.data.model.Account
import com.ivy.wallet.domain.data.Reorderable

/**
 * @property creditUsed for a credit card: what it owes, the purchases it hasn't charged yet
 * included; null for other accounts.
 */
@Immutable
data class AccountData(
    val account: Account,
    val balance: Double,
    val balanceBaseCurrency: Double?,
    val monthlyExpenses: Double,
    val monthlyIncome: Double,
    val creditUsed: Double?,
) : Reorderable {
    override fun getItemOrderNum() = account.orderNum

    override fun withNewOrderNum(newOrderNum: Double) = this.copy(
        account = account.copy(
            orderNum = newOrderNum
        )
    )
}
