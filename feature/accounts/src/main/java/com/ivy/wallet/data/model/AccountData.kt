package com.ivy.wallet.data.model

import androidx.compose.runtime.Immutable
import com.ivy.data.model.Account
import com.ivy.data.model.CreditCard
import com.ivy.wallet.domain.data.Reorderable
import kotlinx.collections.immutable.ImmutableList

/**
 * @property creditCards the account's credit cards, with what each owes.
 */
@Immutable
data class AccountData(
    val account: Account,
    val balance: Double,
    val balanceBaseCurrency: Double?,
    val monthlyExpenses: Double,
    val monthlyIncome: Double,
    val creditCards: ImmutableList<CreditCardUse>,
) : Reorderable {
    override fun getItemOrderNum() = account.orderNum

    override fun withNewOrderNum(newOrderNum: Double) = this.copy(
        account = account.copy(
            orderNum = newOrderNum
        )
    )
}

/** What a credit [card] owes: the purchases it hasn't charged yet, in its account's currency. */
@Immutable
data class CreditCardUse(
    val card: CreditCard,
    val used: Double,
)
