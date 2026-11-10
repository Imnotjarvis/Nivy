package com.nivy.planned.edit

import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.CreditCard
import com.nivy.base.model.IntervalType
import com.nivy.wallet.domain.logic.model.CreateAccountData
import com.nivy.wallet.domain.logic.model.CreateCategoryData
import com.nivy.wallet.ui.theme.modal.RecurringRuleModalData
import com.nivy.wallet.ui.theme.modal.edit.AccountModalData
import com.nivy.wallet.ui.theme.modal.edit.CategoryModalData
import java.time.LocalDateTime

sealed interface EditPlannedScreenEvent {
    data class OnRuleChanged(
        val startDate: LocalDateTime,
        val oneTime: Boolean,
        val intervalN: Int?,
        val intervalType: IntervalType?
    ) : EditPlannedScreenEvent

    data class OnAmountChanged(val newAmount: Double) : EditPlannedScreenEvent
    data class OnTitleChanged(val newTitle: String?) : EditPlannedScreenEvent
    data class OnDescriptionChanged(val newDescription: String?) : EditPlannedScreenEvent
    data class OnCategoryChanged(val newCategory: Category?) : EditPlannedScreenEvent
    data class OnAccountChanged(val newAccount: Account) : EditPlannedScreenEvent
    data class OnCreditCardChanged(val creditCard: CreditCard?) : EditPlannedScreenEvent
    data class OnSetTransactionType(val newTransactionType: TransactionType) :
        EditPlannedScreenEvent

    data object OnSave : EditPlannedScreenEvent
    data object OnDelete : EditPlannedScreenEvent
    data class OnEditCategory(val updatedCategory: Category) : EditPlannedScreenEvent
    data class OnCreateCategory(val data: CreateCategoryData) : EditPlannedScreenEvent
    data class OnCreateAccount(val data: CreateAccountData) : EditPlannedScreenEvent
    data class OnCategoryModalVisible(val visible: Boolean) : EditPlannedScreenEvent
    data class OnDescriptionModalVisible(val visible: Boolean) : EditPlannedScreenEvent
    data class OnDeleteTransactionModalVisible(val visible: Boolean) : EditPlannedScreenEvent
    data class OnAmountModalVisible(val visible: Boolean) : EditPlannedScreenEvent
    data class OnTransactionTypeModalVisible(val visible: Boolean) : EditPlannedScreenEvent
    data class OnCategoryModalDataChanged(val categoryModalData: CategoryModalData?) :
        EditPlannedScreenEvent

    data class OnRecurringRuleModalDataChanged(val recurringRuleModalData: RecurringRuleModalData?) :
        EditPlannedScreenEvent

    data class OnAccountModalDataChanged(val accountModalData: AccountModalData?) :
        EditPlannedScreenEvent
}
