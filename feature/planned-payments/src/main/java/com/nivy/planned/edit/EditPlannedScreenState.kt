package com.nivy.planned.edit

import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.CreditCard
import com.nivy.base.model.IntervalType
import com.nivy.wallet.ui.theme.modal.RecurringRuleModalData
import com.nivy.wallet.ui.theme.modal.edit.AccountModalData
import com.nivy.wallet.ui.theme.modal.edit.CategoryModalData
import kotlinx.collections.immutable.ImmutableList
import java.time.LocalDateTime
import javax.annotation.concurrent.Immutable

@Immutable
data class EditPlannedScreenState(
    val currency: String,
    val transactionType: TransactionType,
    val startDate: LocalDateTime?,
    val intervalN: Int?,
    val intervalType: IntervalType?,
    val oneTime: Boolean,
    val initialTitle: String?,
    val description: String?,
    val categories: ImmutableList<Category>,
    val accounts: ImmutableList<Account>,
    val account: Account?,
    // The account's credit cards a planned expense can be paid with, and the one it's paid with.
    val creditCards: ImmutableList<CreditCard>,
    val creditCard: CreditCard?,
    val category: Category?,
    val amount: Double,
    val categoryModalVisible: Boolean,
    val descriptionModalVisible: Boolean,
    val deleteTransactionModalVisible: Boolean,
    val categoryModalData: CategoryModalData?,
    val accountModalData: AccountModalData?,
    val recurringRuleModalData: RecurringRuleModalData?,
    val transactionTypeModalVisible: Boolean,
    val amountModalVisible: Boolean,
    // Whether the form makes a rule: a start date, an interval or one time, and an amount.
    val canSave: Boolean = false,
)
