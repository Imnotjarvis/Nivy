package com.nivy.main

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nivy.accounts.AccountsTab
import com.nivy.base.model.TransactionType
import com.nivy.home.HomeTab
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.LocalMainScreenState
import com.nivy.wallet.data.model.MainTab
import com.nivy.navigation.EditPlannedScreen
import com.nivy.navigation.EditTransactionScreen
import com.nivy.navigation.navigation
import com.nivy.wallet.domain.logic.model.CreateAccountData
import com.nivy.wallet.ui.theme.modal.edit.AccountModal
import com.nivy.wallet.ui.theme.modal.edit.AccountModalData
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.data.model.QuickAdd
import com.nivy.navigation.QuickAddsScreen
import com.nivy.ui.R
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.ui.component.quickadd.currency
import com.nivy.wallet.ui.component.quickadd.name
import com.nivy.wallet.ui.theme.components.BottomBarMessage
import com.nivy.wallet.ui.theme.modal.edit.AmountModal
import com.nivy.wallet.utils.amountAndCode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.MainScreen() {
    val viewModel: MainViewModel = viewModel()

    val currency by viewModel.currency.observeAsState("")

    LaunchedEffect(Unit) {
        viewModel.start()
    }

    val mainScreenState = LocalMainScreenState.current
    // Back from the accounts tab goes to home; from home it leaves the app.
    BackHandler(enabled = mainScreenState.tab == MainTab.ACCOUNTS) {
        mainScreenState.selectTab(MainTab.HOME)
    }
    UI(
        tab = mainScreenState.tab,
        baseCurrency = currency,
        selectTab = viewModel::selectTab,
        onCreateAccount = viewModel::createAccount,
        quickAdds = viewModel.quickAdds,
        baseData = viewModel.baseData,
        onQuickAdd = viewModel::quickAdd,
        onEditQuickAdd = { viewModel.edit(it) },
    )

    QuickAddAmount(
        quickAdd = viewModel.amountAsked,
        baseData = viewModel.baseData,
        onAmount = viewModel::enterAmount,
        dismiss = viewModel::dismissAmount,
    )

    QuickAddedMessage(added = viewModel.added, baseData = viewModel.baseData, onUndo = viewModel::undo)
}

/** The amount of [quickAdd], which has none of its own, entered on the keypad, under its name. */
@Composable
internal fun BoxWithConstraintsScope.QuickAddAmount(
    quickAdd: QuickAdd?,
    baseData: AppBaseData,
    onAmount: (Double) -> Unit,
    dismiss: () -> Unit,
) {
    // Always there, closed while nothing asks for an amount, so that it opens as dialogs do; it
    // reads as it did while it closes. Each time it opens, it starts empty.
    var shown by remember { mutableStateOf(quickAdd) }
    if (quickAdd != null) shown = quickAdd
    val id = remember(quickAdd) { UUID.randomUUID() }
    AmountModal(
        id = id,
        visible = quickAdd != null,
        currency = shown?.currency(baseData) ?: baseData.baseCurrency,
        initialAmount = null,
        header = {
            Spacer(Modifier.height(24.dp))
            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = shown?.name(baseData).orEmpty(),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
        },
        dismiss = dismiss,
        onAmountChanged = onAmount,
    )
}

/** What the last quick add saved, with Undo, above the bar while it can be taken back. */
@Composable
internal fun BoxWithConstraintsScope.QuickAddedMessage(
    added: QuickAdded?,
    baseData: AppBaseData,
    onUndo: () -> Unit,
) {
    // Always there, hidden while there's nothing to take back, so that it fades in; it reads as
    // it did while it fades out.
    var shown by remember { mutableStateOf(added) }
    if (added != null) shown = added
    BottomBarMessage(
        visible = added != null,
        text = shown?.let {
            stringResource(
                R.string.quick_add_added,
                it.quickAdd.name(baseData),
                amountAndCode(it.value.amount.value, it.value.asset.code),
            )
        }.orEmpty(),
        action = stringResource(R.string.undo),
        onAction = onUndo,
    )
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun BoxWithConstraintsScope.UI(
    tab: MainTab,

    baseCurrency: String,

    selectTab: (MainTab) -> Unit,
    onCreateAccount: (CreateAccountData) -> Unit,
    quickAdds: ImmutableList<QuickAdd>,
    baseData: AppBaseData,
    onQuickAdd: (QuickAdd) -> Unit,
    onEditQuickAdd: (QuickAdd) -> Unit,
) {
    when (tab) {
        MainTab.HOME -> HomeTab()
        MainTab.ACCOUNTS -> AccountsTab()
    }

    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }

    val nav = navigation()
    BottomBar(
        tab = tab,
        selectTab = selectTab,

        onAddIncome = {
            nav.navigateTo(
                EditTransactionScreen(
                    initialTransactionId = null,
                    type = TransactionType.INCOME
                )
            )
        },
        onAddExpense = {
            nav.navigateTo(
                EditTransactionScreen(
                    initialTransactionId = null,
                    type = TransactionType.EXPENSE
                )
            )
        },
        onAddTransfer = {
            nav.navigateTo(
                EditTransactionScreen(
                    initialTransactionId = null,
                    type = TransactionType.TRANSFER
                )
            )
        },
        onAddPlannedPayment = {
            nav.navigateTo(
                EditPlannedScreen(
                    type = TransactionType.EXPENSE,
                    plannedPaymentRuleId = null
                )
            )
        },

        quickAdds = QuickAddsMenu(
            quickAdds = quickAdds,
            baseData = baseData,
            onQuickAdd = onQuickAdd,
            onEditQuickAdd = onEditQuickAdd,
            onEdit = { nav.navigateTo(QuickAddsScreen) },
        ),

        showAddAccountModal = {
            accountModalData = AccountModalData(
                account = null,
                balance = 0.0,
                baseCurrency = baseCurrency
            )
        }
    )

    AccountModal(
        modal = accountModalData,
        onCreateAccount = onCreateAccount,
        onEditAccount = { _, _, _ -> },
        dismiss = {
            accountModalData = null
        }
    )
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun PreviewMainScreen() {
    NivyPreview {
        UI(
            tab = MainTab.HOME,
            baseCurrency = "BGN",
            selectTab = {},
            onCreateAccount = { },
            quickAdds = persistentListOf(),
            baseData = AppBaseData("BGN", persistentListOf(), persistentListOf()),
            onQuickAdd = {},
            onEditQuickAdd = {},
        )
    }
}
