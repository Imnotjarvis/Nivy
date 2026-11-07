package com.nivy.data.backup

import androidx.annotation.Keep
import com.nivy.data.db.entity.AccountEntity
import com.nivy.data.db.entity.BudgetEntity
import com.nivy.data.db.entity.CategoryEntity
import com.nivy.data.db.entity.AccountValueEntity
import com.nivy.data.db.entity.CreditCardEntity
import com.nivy.data.db.entity.ExchangeRateEntity
import com.nivy.data.db.entity.LoanEntity
import com.nivy.data.db.entity.LoanRecordEntity
import com.nivy.data.db.entity.PlannedPaymentRuleEntity
import com.nivy.data.db.entity.TagAssociationEntity
import com.nivy.data.db.entity.TagEntity
import com.nivy.data.db.entity.TransactionEntity
import com.nivy.base.model.Theme
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Suppress("DataClassDefaultValues")
data class NivyCompleteData(
    @SerialName("accounts")
    val accounts: List<AccountEntity> = emptyList(),
    @SerialName("budgets")
    val budgets: List<BudgetEntity> = emptyList(),
    @SerialName("categories")
    val categories: List<CategoryEntity> = emptyList(),
    @SerialName("loanRecords")
    val loanRecords: List<LoanRecordEntity> = emptyList(),
    @SerialName("loans")
    val loans: List<LoanEntity> = emptyList(),
    @SerialName("plannedPaymentRules")
    val plannedPaymentRules: List<PlannedPaymentRuleEntity> = emptyList(),
    @SerialName("settings")
    val settings: List<SettingsBackup> = emptyList(),
    @SerialName("transactions")
    val transactions: List<TransactionEntity> = emptyList(),
    @SerialName("sharedPrefs")
    val sharedPrefs: HashMap<String, String> = HashMap(),
    @SerialName("tags")
    val tags: List<TagEntity> = emptyList(),
    @SerialName("tagAssociations")
    val tagAssociations: List<TagAssociationEntity> = emptyList(),
    @SerialName("creditCards")
    val creditCards: List<CreditCardEntity> = emptyList(),
    @SerialName("accountValues")
    val accountValues: List<AccountValueEntity> = emptyList(),
    /** The exchange rates entered by hand. The others are downloaded again. */
    @SerialName("exchangeRates")
    val exchangeRates: List<ExchangeRateEntity> = emptyList(),
)

/**
 * The settings in a backup. They have the fields of the old settings table, so backups from older
 * versions import, and older versions can import new backups.
 */
@Keep
@Serializable
data class SettingsBackup(
    @SerialName("theme")
    val theme: Theme,
    @SerialName("currency")
    val currency: String,
    @SerialName("bufferAmount")
    val bufferAmount: Double,
    @SerialName("name")
    val name: String,
)
