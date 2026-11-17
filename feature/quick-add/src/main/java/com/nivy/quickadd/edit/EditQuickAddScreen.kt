package com.nivy.quickadd.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.nivy.navigation.EditQuickAddScreen
import com.nivy.navigation.screenScopedViewModel
import com.nivy.ui.R
import com.nivy.wallet.ui.edit.core.Category
import com.nivy.wallet.ui.edit.core.EditBottomSheet
import com.nivy.wallet.ui.edit.core.Title
import com.nivy.wallet.ui.edit.core.Toolbar
import com.nivy.wallet.ui.theme.components.CardChoice
import com.nivy.wallet.ui.theme.components.ChangeTransactionTypeModal
import com.nivy.wallet.ui.theme.modal.DeleteModal
import com.nivy.wallet.ui.theme.modal.ModalSet
import com.nivy.wallet.ui.theme.modal.edit.AccountModal
import com.nivy.wallet.ui.theme.modal.edit.AccountModalData
import com.nivy.wallet.ui.theme.modal.edit.CategoryModal
import com.nivy.wallet.ui.theme.modal.edit.CategoryModalData
import com.nivy.wallet.ui.theme.modal.edit.ChooseCategoryModal

@Composable
fun BoxWithConstraintsScope.EditQuickAddScreen(screen: EditQuickAddScreen) {
    val viewModel: EditQuickAddViewModel = screenScopedViewModel()
    val state = viewModel.uiState()
    LaunchedEffect(Unit) {
        viewModel.start(screen)
    }
    UI(screen = screen, state = state, onEvent = viewModel::onEvent)
}

/**
 * Its type and title, its category and card, and at the bottom its amount and account. A new one
 * with nothing filled in asks for its type, then its amount, then its category, then its title.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BoxWithConstraintsScope.UI(
    screen: EditQuickAddScreen,
    state: EditQuickAddState,
    onEvent: (EditQuickAddEvent) -> Unit,
) {
    var titleTextFieldValue by remember(state.initialTitle) {
        mutableStateOf(TextFieldValue(state.initialTitle.orEmpty()))
    }
    val titleFocus = remember { FocusRequester() }
    // How high the amount panel covers the screen: the card list stays clear of it.
    var bottomSheetHeight by remember { mutableStateOf(0.dp) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))

        Toolbar(
            type = state.type,
            initialTransactionId = screen.quickAddId,
            onDeleteTrnModal = { onEvent(EditQuickAddEvent.OnDeleteModalVisible(true)) },
            onChangeTransactionTypeModal = { onEvent(EditQuickAddEvent.OnTypeModalVisible(true)) },
            showDuplicateButton = false,
            onDuplicate = {},
        )

        Spacer(Modifier.height(32.dp))

        Title(
            type = state.type,
            titleFocus = titleFocus,
            titleTextFieldValue = titleTextFieldValue,
            setTitleTextFieldValue = { titleTextFieldValue = it },
            suggestions = emptySet(),
            onTitleChanged = { onEvent(EditQuickAddEvent.OnTitleChanged(it)) },
        )

        Spacer(Modifier.height(32.dp))

        Category(
            category = state.category,
            onChooseCategory = { onEvent(EditQuickAddEvent.OnCategoryModalVisible(true)) },
        )

        if (state.creditCards.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))

            CardChoice(
                cards = state.creditCards,
                selected = state.creditCard,
                onSelect = { onEvent(EditQuickAddEvent.OnCreditCardChanged(it)) },
                coveredBottom = { bottomSheetHeight },
            )
        }

        Spacer(Modifier.height(600.dp)) // scroll hack
    }

    val latestOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(Unit) {
        // A new one with nothing filled in, as from the quick adds' list: its type first.
        if (screen.quickAddId == null && screen.accountId == null) {
            latestOnEvent(EditQuickAddEvent.OnTypeModalVisible(true))
        }
    }

    EditBottomSheet(
        onHeightChanged = { bottomSheetHeight = it },
        initialTransactionId = screen.quickAddId,
        type = state.type,
        accounts = state.accounts,
        selectedAccount = state.account,
        toAccount = state.toAccount,
        amount = state.amount,
        currency = state.currency,
        emptyAmount = stringResource(R.string.any_amount),
        actionButton = {
            ModalSet(
                modifier = Modifier.testTag("editQuickAddScreen_set"),
                enabled = state.canSave,
            ) {
                onEvent(EditQuickAddEvent.OnSave)
            }
        },
        amountModalShown = state.amountModalVisible,
        setAmountModalShown = { onEvent(EditQuickAddEvent.OnAmountModalVisible(it)) },
        onAmountChanged = {
            onEvent(EditQuickAddEvent.OnAmountChanged(it))
            when {
                state.category == null -> onEvent(EditQuickAddEvent.OnCategoryModalVisible(true))
                titleTextFieldValue.text.isBlank() -> titleFocus.requestFocus()
            }
        },
        onSelectedAccountChanged = { onEvent(EditQuickAddEvent.OnAccountChanged(it)) },
        onToAccountChanged = { onEvent(EditQuickAddEvent.OnToAccountChanged(it)) },
        onAddNewAccount = {
            onEvent(
                EditQuickAddEvent.OnAccountModalDataChanged(
                    AccountModalData(account = null, baseCurrency = state.currency, balance = 0.0)
                )
            )
        },
    )

    ChooseCategoryModal(
        visible = state.categoryModalVisible,
        initialCategory = state.category,
        categories = state.categories,
        showCategoryModal = { onEvent(EditQuickAddEvent.OnCategoryModalDataChanged(CategoryModalData(it))) },
        onCategoryChanged = {
            onEvent(EditQuickAddEvent.OnCategoryChanged(it))
            if (titleTextFieldValue.text.isBlank()) titleFocus.requestFocus()
        },
        dismiss = { onEvent(EditQuickAddEvent.OnCategoryModalVisible(false)) },
    )

    CategoryModal(
        modal = state.categoryModalData,
        onCreateCategory = { onEvent(EditQuickAddEvent.OnCreateCategory(it)) },
        onEditCategory = { onEvent(EditQuickAddEvent.OnEditCategory(it)) },
        dismiss = { onEvent(EditQuickAddEvent.OnCategoryModalDataChanged(null)) },
    )

    AccountModal(
        modal = state.accountModalData,
        onCreateAccount = { onEvent(EditQuickAddEvent.OnCreateAccount(it)) },
        onEditAccount = { _, _, _ -> },
        dismiss = { onEvent(EditQuickAddEvent.OnAccountModalDataChanged(null)) },
    )

    DeleteModal(
        visible = state.deleteModalVisible,
        title = stringResource(R.string.confirm_deletion),
        description = stringResource(R.string.quick_add_confirm_deletion_description),
        dismiss = { onEvent(EditQuickAddEvent.OnDeleteModalVisible(false)) },
    ) {
        onEvent(EditQuickAddEvent.OnDelete)
    }

    ChangeTransactionTypeModal(
        title = stringResource(R.string.set_payment_type),
        visible = state.typeModalVisible,
        includeTransferType = true,
        initialType = state.type,
        dismiss = { onEvent(EditQuickAddEvent.OnTypeModalVisible(false)) },
    ) {
        onEvent(EditQuickAddEvent.OnTypeChanged(it))
        // A new one's amount comes next.
        if (screen.quickAddId == null && state.amount == 0.0) {
            onEvent(EditQuickAddEvent.OnAmountModalVisible(true))
        }
    }
}
