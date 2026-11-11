package com.nivy.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.Transaction
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.ui.R
import com.nivy.wallet.ui.theme.modal.ChoosePeriodModalData
import com.nivy.wallet.ui.theme.modal.DeleteConfirmationModal
import com.nivy.wallet.ui.theme.modal.DeleteModal
import com.nivy.wallet.ui.theme.wallet.PeriodSelector
import com.nivy.wallet.ui.theme.wallet.backToThisMonth
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal fun LazyListScope.choosePeriodModal(
    period: TimePeriod,
    itemColor: Color,
    initWithTransactions: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onChoosePeriodModal: (ChoosePeriodModalData?) -> Unit,
    onSetPeriod: (TimePeriod) -> Unit,
) {
    item {
        Box {
            RoundedTopJoin(itemColor)

            PeriodSelector(
                modifier = Modifier.padding(top = 16.dp),
                period = period,
                onPreviousMonth = { if (!initWithTransactions) onPreviousMonth() },
                onNextMonth = { if (!initWithTransactions) onNextMonth() },
                onShowChoosePeriodModal = {
                    if (!initWithTransactions) {
                        onChoosePeriodModal(
                            ChoosePeriodModalData(
                                period = period
                            )
                        )
                    }
                },
                onThisMonth = if (initWithTransactions) null else backToThisMonth(period, onSetPeriod),
            )
        }
    }
}

@Composable
internal fun BoxWithConstraintsScope.DeleteModals(
    deleteModal1Visible: Boolean,
    setDeleteModal1Visible: (Boolean) -> Unit,
    account: Account?,
    category: Category?,
    updateAccountNameConfirmation: (String) -> Unit,
    enableDeletionButton: Boolean,
    onDelete: () -> Unit,
    skipAllModalVisible: Boolean,
    onSkipAllModalVisible: (Boolean) -> Unit,
    onSkipAllTransactions: (List<Transaction>) -> Unit,
    overdue: ImmutableList<Transaction> = persistentListOf(),
) {
    var deleteModal3Visible by remember { mutableStateOf(false) }

    DeleteModal(
        visible = deleteModal1Visible,
        title = stringResource(R.string.confirm_deletion),
        description = if (account != null) {
            stringResource(R.string.account_confirm_deletion_description)
        } else {
            stringResource(R.string.category_confirm_deletion_description)
        },
        dismiss = {
            setDeleteModal1Visible(false)
        }
    ) {
        deleteModal3Visible = true
    }

    DeleteConfirmationModal(
        visible = deleteModal3Visible,
        title = stringResource(id = R.string.confirm_deletion),
        description = if (account != null) {
            stringResource(
                id = R.string.account_confirm_deletion_type_account_name,
                account.name.value
            )
        } else {
            stringResource(R.string.please_type_category_name, category?.name?.value ?: "")
        },
        hint = if (account != null) stringResource(id = R.string.account_name) else "Category name",
        onAccountNameChange = updateAccountNameConfirmation,
        enableDeletionButton = enableDeletionButton,
        dismiss = {
            updateAccountNameConfirmation("")
            deleteModal3Visible = false
            setDeleteModal1Visible(false)
        }
    ) {
        onDelete()
        updateAccountNameConfirmation("")
        setDeleteModal1Visible(false)
    }

    DeleteModal(
        visible = skipAllModalVisible,
        title = stringResource(R.string.confirm_skip_all),
        description = stringResource(R.string.confirm_skip_all_description),
        dismiss = {
            onSkipAllModalVisible(false)
        }
    ) {
        onSkipAllTransactions(overdue)
        onSkipAllModalVisible(false)
    }
}
