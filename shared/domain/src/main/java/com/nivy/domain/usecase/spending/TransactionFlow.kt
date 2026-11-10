package com.nivy.domain.usecase.spending

import com.nivy.base.time.TimeConverter
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Income
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.model.getFromAccount
import com.nivy.data.model.getFromValue
import com.nivy.domain.usecase.exchange.BaseCurrencyRates
import com.nivy.wallet.domain.pure.spending.Flow

/**
 * This transaction as a flow, in the base currency; null for a transfer, which stays within the
 * wallet, and for an amount without a rate to the base currency, which is left out rather than
 * counted as one of nothing.
 */
suspend fun Transaction.toFlow(
    rates: BaseCurrencyRates,
    timeConverter: TimeConverter,
    categories: Map<CategoryId, Category>,
    accounts: Map<AccountId, Account> = emptyMap(),
): Flow? {
    if (this is Transfer) return null
    val income = this is Income
    return rates.inBaseOrNull(getFromValue())?.let { amount ->
        Flow(
            amount = amount,
            income = income,
            day = with(timeConverter) { time.toLocalDate() },
            made = with(timeConverter) { (metadata.purchaseDateTime ?: time).toLocalDateTime() },
            title = title?.value,
            category = category?.let(categories::get),
            account = accounts[getFromAccount()],
            recurring = metadata.recurringRuleId != null,
            // A loan's money isn't a purchase.
            purchase = !income && metadata.loanId == null && metadata.loanRecordId == null,
        )
    }
}
