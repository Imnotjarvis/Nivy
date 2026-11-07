package com.nivy.transaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.nivy.data.model.Account
import com.nivy.navigation.EditTransactionScreen
import com.nivy.transaction.EditTransactionViewEvent.TagEvent
import com.nivy.ui.R
import com.nivy.wallet.ui.component.tags.ShowTagModal
import com.nivy.wallet.ui.theme.components.ChangeTransactionTypeModal
import com.nivy.wallet.ui.theme.modal.DeleteModal
import com.nivy.wallet.ui.theme.modal.ProgressModal
import com.nivy.wallet.ui.theme.modal.edit.AccountModal
import com.nivy.wallet.ui.theme.modal.edit.AccountModalData
import com.nivy.wallet.ui.theme.modal.edit.AmountModal
import com.nivy.wallet.ui.theme.modal.edit.CategoryModal
import com.nivy.wallet.ui.theme.modal.edit.CategoryModalData
import com.nivy.wallet.ui.theme.modal.edit.ChooseCategoryModal
import com.nivy.wallet.ui.theme.modal.edit.DescriptionModal
import java.util.UUID

private const val ExchangeRateDecimals = 12

/** Which of the editor's modals is open, and what the open one was opened with. */
@Stable
internal class OpenModals {
    /** The amount panel, in the bottom sheet. */
    var amount by mutableStateOf(false)
    var chooseCategory by mutableStateOf(false)
    var editCategory by mutableStateOf<CategoryModalData?>(null)
    var newAccount by mutableStateOf<AccountModalData?>(null)
    var description by mutableStateOf(false)
    var delete by mutableStateOf(false)
    var changeType by mutableStateOf(false)

    /** The account a loan's transaction moves to once the user confirms it, since its currency differs. */
    var loanAccount by mutableStateOf<Account?>(null)
    var exchangeRate by mutableStateOf(false)
    var tags by mutableStateOf(false)
}

/** The editor's modals, each shown while [open] says so; [afterCategoryChoice] runs once one is chosen. */
@ExperimentalFoundationApi
@Composable
internal fun BoxWithConstraintsScope.EditTransactionModals(
    open: OpenModals,
    screen: EditTransactionScreen,
    state: EditTransactionViewState,
    onEvent: (EditTransactionViewEvent) -> Unit,
    afterCategoryChoice: () -> Unit,
) {
    ChooseCategoryModal(
        visible = open.chooseCategory,
        initialCategory = state.category,
        categories = state.categories,
        showCategoryModal = { open.editCategory = CategoryModalData(it) },
        onCategoryChanged = {
            onEvent(EditTransactionViewEvent.OnCategoryChanged(it))
            afterCategoryChoice()
        },
        dismiss = {
            open.chooseCategory = false
        }
    )

    CategoryModal(modal = open.editCategory, onCreateCategory = { createData ->
        onEvent(EditTransactionViewEvent.CreateCategory(createData))
        open.chooseCategory = false
    }, onEditCategory = { onEvent(EditTransactionViewEvent.EditCategory(it)) }, dismiss = {
        open.editCategory = null
    })

    AccountModal(
        modal = open.newAccount,
        onCreateAccount = { onEvent(EditTransactionViewEvent.CreateAccount(it)) },
        onEditAccount = { _, _, _ -> },
        dismiss = {
            open.newAccount = null
        }
    )

    DescriptionModal(
        visible = open.description,
        description = state.description,
        onDescriptionChanged = { onEvent(EditTransactionViewEvent.OnDescriptionChanged(it)) },
        dismiss = {
            open.description = false
        }
    )

    DeleteModal(
        visible = open.delete,
        title = stringResource(R.string.confirm_deletion),
        description = stringResource(R.string.transaction_confirm_deletion_description),
        dismiss = { open.delete = false }
    ) {
        onEvent(EditTransactionViewEvent.Delete)
    }

    ChangeTransactionTypeModal(
        visible = open.changeType,
        includeTransferType = true,
        initialType = state.transactionType,
        dismiss = {
            open.changeType = false
        }
    ) {
        onEvent(EditTransactionViewEvent.OnSetTransactionType(it))
    }

    DeleteModal(
        visible = open.loanAccount != null,
        title = stringResource(R.string.confirm_account_change),
        description = stringResource(R.string.confirm_account_change_description),
        buttonText = stringResource(R.string.confirm),
        iconStart = R.drawable.ic_agreed,
        dismiss = {
            open.loanAccount = null
        }
    ) {
        open.loanAccount?.let { onEvent(EditTransactionViewEvent.OnAccountChanged(it)) }
        open.loanAccount = null
    }

    ProgressModal(
        title = stringResource(R.string.confirm_account_change),
        description = stringResource(R.string.confirm_account_loan_change),
        visible = state.backgroundProcessingStarted
    )

    AmountModal(
        id = remember(screen.initialTransactionId, state.customExchangeRateState.exchangeRate) {
            UUID.randomUUID()
        },
        visible = open.exchangeRate,
        currency = "",
        initialAmount = state.customExchangeRateState.exchangeRate,
        dismiss = { open.exchangeRate = false },
        // A rate needs more decimals than an amount (1.0853, 0.0067), as on the exchange-rates screen.
        decimalCountMax = ExchangeRateDecimals,
        onAmountChanged = {
            onEvent(EditTransactionViewEvent.UpdateExchangeRate(it))
        }
    )

    ShowTagModal(
        visible = open.tags,
        onDismiss = {
            open.tags = false
            // Reset TagList, avoids showing incorrect tag list when user has searched for a tag
            onEvent(TagEvent.OnTagSearch(""))
        },
        allTagList = state.tags,
        selectedTagList = state.transactionAssociatedTags,
        onTagAdd = { onEvent(TagEvent.SaveTag(name = it)) },
        onTagEdit = { oldTag, newTag -> onEvent(TagEvent.OnTagEdit(oldTag, newTag)) },
        onTagDelete = { onEvent(TagEvent.OnTagDelete(it)) },
        onTagSelected = { onEvent(TagEvent.OnTagSelect(it)) },
        onTagDeSelected = { onEvent(TagEvent.OnTagDeSelect(it)) },
        onTagSearch = { onEvent(TagEvent.OnTagSearch(it)) }
    )
}
