package com.nivy.wallet.ui.edit.core

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nivy.data.model.Account
import com.nivy.wallet.NivyPreview
import com.nivy.design.utils.isKeyboardVisible
import com.nivy.design.utils.keyboardHeight
import com.nivy.design.utils.uncoveredNavigationBar
import com.nivy.design.utils.consumeClicks
import com.nivy.design.utils.densityScope
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.theme.components.AccountsRow
import com.nivy.wallet.utils.format
import com.nivy.design.utils.hideKeyboard
import com.nivy.design.utils.lerp
import com.nivy.wallet.utils.springBounce
import com.nivy.wallet.utils.verticalSwipeListener
import com.nivy.base.model.TransactionType
import com.nivy.design.utils.rememberInteractionSource
import com.nivy.wallet.utils.rememberSwipeListenerState
import com.nivy.ui.R
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.GreenDark
import com.nivy.design.system.colors.GreenLight
import com.nivy.design.system.colors.NivyDark
import com.nivy.wallet.ui.theme.modal.ModalSave
import com.nivy.wallet.ui.theme.modal.ModalSet
import com.nivy.wallet.ui.theme.modal.edit.AmountModal
import java.util.UUID
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.shapes.ShapeTop

const val SWIPE_UP_EXPANDED_THRESHOLD = 200

@Suppress("LongMethod", "LongParameterList", "ParameterNaming")
@Composable
fun BoxWithConstraintsScope.EditBottomSheet(
    initialTransactionId: UUID?,
    type: TransactionType,
    accounts: List<Account>,
    selectedAccount: Account?,
    toAccount: Account?,
    amount: Double,
    currency: String,
    amountModalShown: Boolean,
    setAmountModalShown: (Boolean) -> Unit,
    actionButton: @Composable () -> Unit,
    onAmountChanged: (Double) -> Unit,
    onSelectedAccountChanged: (Account) -> Unit,
    onToAccountChanged: (Account) -> Unit,
    onAddNewAccount: () -> Unit,
    modifier: Modifier = Modifier,
    convertedAmount: Double? = null,
    convertedAmountCurrencyCode: String? = null,
    onHeightChanged: (Dp) -> Unit = {},
) {
    val rootView = LocalView.current
    val keyboardShown = isKeyboardVisible()
    // Both follow the keyboard as it opens and closes, and the bar above it with it.
    val keyboardShownInsetDp = keyboardHeight()
    val navBarPadding = uncoveredNavigationBar()

    var bottomBarHeight by remember { mutableIntStateOf(0) }

    var internalExpanded by remember { mutableStateOf(true) }
    val expanded = internalExpanded && !keyboardShown

    val percentExpanded by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = springBounce()
    )
    val percentCollapsed = 1f - percentExpanded

    val showConvertedAmountText by remember(type, convertedAmount, convertedAmountCurrencyCode) {
        if (type == TransactionType.TRANSFER && convertedAmount != null && convertedAmountCurrencyCode != null) {
            mutableStateOf(
                "${convertedAmount.format(convertedAmountCurrencyCode)} $convertedAmountCurrencyCode"
            )
        } else {
            mutableStateOf(null)
        }
    }

    // How high the sheet reaches above the navigation bar, for what should stay clear of it.
    val density = LocalDensity.current
    val navigationBar = WindowInsets.navigationBars.getBottom(density)
    Column(
        modifier = modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 24.dp)
            .onSizeChanged {
                onHeightChanged(with(density) { (it.height - navigationBar).coerceAtLeast(0).toDp() })
            }
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = ShapeTop
            )
            .background(MaterialTheme.colorScheme.surface, ShapeTop)
            .verticalSwipeListener(
                sensitivity = SWIPE_UP_EXPANDED_THRESHOLD,
                state = rememberSwipeListenerState(),
                onSwipeUp = {
                    hideKeyboard(rootView)
                    internalExpanded = true
                },
                onSwipeDown = {
                    internalExpanded = false
                }
            )
            .consumeClicks(rememberInteractionSource())
    ) {
        // Accounts label
        val label = when (type) {
            TransactionType.INCOME -> stringResource(R.string.add_money_to)
            TransactionType.EXPENSE -> stringResource(R.string.pay_with)
            TransactionType.TRANSFER -> stringResource(R.string.from)
        }

        SheetHeader(
            percentExpanded = percentExpanded,
            label = label,
            type = type,
            accounts = accounts,
            selectedAccount = selectedAccount,
            toAccount = toAccount,
            onSelectedAccountChanged = onSelectedAccountChanged,
            onToAccountChanged = onToAccountChanged,
            onAddNewAccount = onAddNewAccount
        )

        val spacerAboveAmount = lerp(40, 16, percentCollapsed)
        Spacer(Modifier.height(spacerAboveAmount.dp))

        if (type == TransactionType.TRANSFER && percentExpanded < 1f) {
            TransferRowMini(
                percentCollapsed = percentCollapsed,
                fromAccount = selectedAccount,
                toAccount = toAccount,
                onSetExpanded = {
                    internalExpanded = true
                }
            )
        }

        Amount(
            type = type,
            amount = amount,
            currency = currency,
            label = label,
            account = selectedAccount,
            showConvertedAmountText = showConvertedAmountText,
            percentExpanded = percentExpanded,
            onShowAmountModal = {
                setAmountModalShown(true)
            },
            onAccountMiniClick = {
                hideKeyboard(rootView)
                internalExpanded = true
            },
        )

        val lastSpacer = lerp(20f, 8f, percentCollapsed)
        if (lastSpacer > 0) {
            Spacer(Modifier.height(lastSpacer.dp))
        }
//
        // system stuff + keyboard padding
        Spacer(Modifier.height(densityScope { bottomBarHeight.toDp() }))
        Spacer(Modifier.height(keyboardShownInsetDp))
    }

    BottomBar(
        keyboardShown = keyboardShown,
        expanded = expanded,
        internalExpanded = internalExpanded,
        setInternalExpanded = {
            internalExpanded = it
        },
        setBottomBarHeight = {
            bottomBarHeight = it
        },

        keyboardShownInsetDp = keyboardShownInsetDp,
        navBarPadding = navBarPadding,

        actionButton = actionButton
    )

    val amountModalId = remember(initialTransactionId, amount) {
        UUID.randomUUID()
    }
    AmountModal(
        id = amountModalId,
        visible = amountModalShown,
        currency = currency,
        initialAmount = amount.takeIf { it > 0 },
        header = {
            Spacer(Modifier.height(24.dp))

            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = stringResource(R.string.account),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(16.dp))

            AccountsRow(
                accounts = accounts,
                selectedAccount = selectedAccount,
                onSelectedAccountChanged = onSelectedAccountChanged,
                onAddNewAccount = onAddNewAccount,
                childrenTestTag = "amount_modal_account"
            )
        },
        amountSpacerTop = 48.dp,
        dismiss = {
            setAmountModalShown(false)
        }
    ) {
        onAmountChanged(it)
    }
}

@Preview
@Composable
private fun Preview() {
    NivyPreview {
        val acc1 = previewAccount("Cash", color = Green)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
        ) {
            EditBottomSheet(
                amountModalShown = false,
                setAmountModalShown = {},
                initialTransactionId = null,
                type = TransactionType.INCOME,
                actionButton = {
                    ModalSet {
                    }
                },
                accounts = listOf(
                    acc1,
                    previewAccount("DSK", color = GreenDark),
                    previewAccount("phyre", color = GreenLight),
                    previewAccount("Revolut", color = NivyDark),
                ),
                selectedAccount = acc1,
                toAccount = null,
                amount = 12350.0,
                currency = "BGN",
                onAmountChanged = {},
                onSelectedAccountChanged = {},
                onToAccountChanged = {},
                onAddNewAccount = {}
            )
        }
    }
}

@Preview
@Composable
private fun Preview_Transfer() {
    NivyPreview {
        val acc1 = previewAccount("Cash", color = Green)
        val acc2 = previewAccount("DSK", color = GreenDark)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
        ) {
            EditBottomSheet(
                amountModalShown = false,
                setAmountModalShown = {},
                initialTransactionId = UUID.randomUUID(),
                actionButton = {
                    ModalSave {
                    }
                },
                type = TransactionType.TRANSFER,
                accounts = listOf(
                    acc1,
                    acc2,
                    previewAccount("phyre", color = GreenLight, icon = "cash"),
                    previewAccount("Revolut", color = NivyDark),
                ),
                selectedAccount = acc1,
                toAccount = acc2,
                amount = 12350.0,
                currency = "BGN",
                onAmountChanged = {},
                onSelectedAccountChanged = {},
                onToAccountChanged = {},
                onAddNewAccount = {}
            )
        }
    }
}
