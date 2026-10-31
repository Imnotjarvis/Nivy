package com.ivy.reports

import com.ivy.base.model.TransactionType
import com.ivy.data.model.Account
import com.ivy.data.model.Category
import com.ivy.data.model.TagId
import com.ivy.wallet.data.model.TimePeriod
import java.util.UUID

data class ReportFilter(
    val id: UUID = UUID.randomUUID(),
    val trnTypes: List<TransactionType>,
    val period: TimePeriod?,
    val accounts: List<Account>,
    val categories: List<Category>,
    val currency: String,
    val minAmount: Double?,
    val maxAmount: Double?,
    val includeKeywords: List<String>,
    val excludeKeywords: List<String>,
    val includedTags: List<TagId>,
    val excludedTags: List<TagId>,

) {
    companion object {
        fun emptyFilter(
            baseCurrency: String
        ) = ReportFilter(
            trnTypes = emptyList(),
            period = null,
            accounts = emptyList(),
            categories = emptyList(),
            currency = baseCurrency,
            includeKeywords = emptyList(),
            excludeKeywords = emptyList(),
            minAmount = null,
            maxAmount = null,
            includedTags = emptyList(),
            excludedTags = emptyList()
        )
    }

    fun validate(): Boolean {
        val everythingChosen = trnTypes.isNotEmpty() && accounts.isNotEmpty() && categories.isNotEmpty()
        val amountsInOrder = minAmount == null || maxAmount == null || minAmount <= maxAmount
        return everythingChosen && period != null && amountsInOrder
    }
}
