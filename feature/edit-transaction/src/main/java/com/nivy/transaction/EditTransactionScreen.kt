package com.nivy.transaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Category
import com.nivy.design.utils.hideKeyboard
import com.nivy.domain.RootScreen
import com.nivy.navigation.EditPlannedScreen
import com.nivy.navigation.EditTransactionScreen
import com.nivy.navigation.navigation
import com.nivy.navigation.screenScopedViewModel
import com.nivy.ui.R
import com.nivy.ui.time.LocalTimeConverter
import com.nivy.wallet.ui.component.edit.core.Description
import com.nivy.wallet.ui.component.tags.AddTagButton
import com.nivy.wallet.ui.edit.core.Category
import com.nivy.wallet.ui.edit.core.DueDate
import com.nivy.wallet.ui.edit.core.EditBottomSheet
import com.nivy.wallet.ui.edit.core.Title
import com.nivy.wallet.ui.edit.core.Toolbar
import com.nivy.wallet.ui.theme.components.AddPrimaryAttributeButton
import com.nivy.wallet.ui.theme.components.CardChoice
import com.nivy.wallet.ui.theme.components.CustomExchangeRateCard
import com.nivy.wallet.ui.theme.modal.ModalAdd
import com.nivy.wallet.ui.theme.modal.ModalCheck
import com.nivy.wallet.ui.theme.modal.ModalSave
import com.nivy.wallet.ui.theme.modal.edit.AccountModalData
import java.time.Instant
import java.time.LocalDateTime
import kotlin.math.roundToInt

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.EditTransactionScreen(screen: EditTransactionScreen) {
    val viewModel: EditTransactionViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    LaunchedEffect(Unit) {
        viewModel.start(screen)
    }

    val view = LocalView.current

    UI(screen = screen, state = uiState) { event ->
        if (event is EditTransactionViewEvent.Save) view.hideKeyboard()
        viewModel.onEvent(event)
    }
}

@ExperimentalFoundationApi
@Composable
internal fun BoxWithConstraintsScope.UI(
    screen: EditTransactionScreen,
    state: EditTransactionViewState,
    onEvent: (EditTransactionViewEvent) -> Unit,
) {
    val open = remember { OpenModals() }
    // How high the amount panel covers the screen: the card list stays clear of it.
    var bottomSheetHeight by remember { mutableStateOf(0.dp) }

    var titleTextFieldValue by remember(state.initialTitle) {
        mutableStateOf(TextFieldValue(state.initialTitle ?: ""))
    }
    val titleFocus = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    // This is to scroll the column to the customExchangeCard composable when it is shown
    var customExchangeRatePosition by remember { mutableFloatStateOf(0F) }
    LaunchedEffect(key1 = state.customExchangeRateState.showCard) {
        val scrollInt =
            if (state.customExchangeRateState.showCard) customExchangeRatePosition.roundToInt() else 0
        scrollState.animateScrollTo(scrollInt)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
    ) {
        Spacer(Modifier.height(16.dp))

        Toolbar(
            // Setting the transaction type to TransactionType.TRANSFER for transactions associated
            // with loan record to hide the ChangeTransactionType Button
            type = if (state.displayLoanHelper.isLoanRecord) TransactionType.TRANSFER else state.transactionType,
            initialTransactionId = screen.initialTransactionId,
            onDeleteTrnModal = { open.delete = true },
            onChangeTransactionTypeModal = { open.changeType = true },
            showDuplicateButton = true,
            onDuplicate = { onEvent(EditTransactionViewEvent.Duplicate) }
        )

        Spacer(Modifier.height(32.dp))

        Title(
            type = state.transactionType,
            titleFocus = titleFocus,
            titleTextFieldValue = titleTextFieldValue,
            setTitleTextFieldValue = { titleTextFieldValue = it },
            suggestions = state.titleSuggestions,
            scrollState = scrollState,
            onTitleChanged = { onEvent(EditTransactionViewEvent.OnTitleChanged(it)) },
        )

        state.displayLoanHelper.loanCaption?.let { LoanCaption(it) }

        Spacer(Modifier.height(32.dp))

        Category(category = state.category, onChooseCategory = { open.chooseCategory = true })

        Spacer(Modifier.height(16.dp))

        AddTagButton(transactionAssociatedTags = state.transactionAssociatedTags, onClick = { open.tags = true })

        Spacer(Modifier.height(32.dp))

        if (state.dueDate != null) {
            PlannedDueDate(dueDate = state.dueDate) { onEvent(EditTransactionViewEvent.OnDueDateChanged(it)) }

            Spacer(Modifier.height(12.dp))
        }

        Description(
            description = state.description,
            onAddDescription = { open.description = true },
            onEditDescription = { open.description = true }
        )

        TransactionDateTime(
            dateTime = state.dateTime,
            dueDateTime = state.dueDate,
            onEditDate = { onEvent(EditTransactionViewEvent.OnChangeDate) },
            onEditTime = { onEvent(EditTransactionViewEvent.OnChangeTime) },
        )

        if (state.creditCards.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))

            CardChoice(
                cards = state.creditCards,
                selected = state.creditCard,
                onSelect = { onEvent(EditTransactionViewEvent.OnCreditCardChanged(it)) },
                coveredBottom = { bottomSheetHeight },
            )
        }

        if (state.chargeDate != null) {
            Spacer(Modifier.height(8.dp))

            ChargeDate(
                chargeDate = state.chargeDate,
                canMoveEarlier = state.canMoveChargeEarlier,
                onMove = { onEvent(EditTransactionViewEvent.OnMoveCharge(it)) },
            )
        }

        if (state.showsRateCard) {
            Spacer(Modifier.height(12.dp))

            TransferRateCard(
                state = state,
                onEvent = onEvent,
                onEditRate = { open.exchangeRate = true },
                onPositionChange = { customExchangeRatePosition = it },
            )
        }

        if (state.canBecomePlanned) {
            Spacer(Modifier.height(12.dp))

            PlanItButton(state = state, title = { titleTextFieldValue.text })
        }

        Spacer(Modifier.height(600.dp)) // scroll hack
    }

    LaunchedEffect(Unit) {
        if (screen.initialTransactionId == null) {
            open.amount = true
        }
    }

    EditBottomSheet(
        onHeightChanged = { bottomSheetHeight = it },
        initialTransactionId = screen.initialTransactionId,
        type = state.transactionType,
        accounts = state.accounts,
        selectedAccount = state.account,
        toAccount = state.toAccount,
        amount = state.amount,
        currency = state.currency,
        convertedAmount = state.customExchangeRateState.convertedAmount,
        convertedAmountCurrencyCode = state.customExchangeRateState.toCurrencyCode,
        actionButton = { ActionButton(screen = screen, state = state, onEvent = onEvent) },
        amountModalShown = open.amount,
        setAmountModalShown = { open.amount = it },
        onAmountChanged = {
            onEvent(EditTransactionViewEvent.OnAmountChanged(it))
            if (shouldFocusCategory(state.category)) {
                open.chooseCategory = true
            } else if (shouldFocusTitle(titleTextFieldValue, state.transactionType)) {
                titleFocus.requestFocus()
            }
        },
        onSelectedAccountChanged = {
            if (state.displayLoanHelper.isLoan && state.account?.asset != it.asset) {
                open.loanAccount = it
            } else {
                onEvent(EditTransactionViewEvent.OnAccountChanged(it))
            }
        },
        onToAccountChanged = { onEvent(EditTransactionViewEvent.OnToAccountChanged(it)) },
        onAddNewAccount = {
            open.newAccount = AccountModalData(account = null, baseCurrency = state.currency, balance = 0.0)
        }
    )

    EditTransactionModals(open = open, screen = screen, state = state, onEvent = onEvent) {
        if (shouldFocusTitle(titleTextFieldValue, state.transactionType)) {
            titleFocus.requestFocus()
        } else if (shouldFocusAmount(amount = state.amount)) {
            open.amount = true
        }
    }
}

@Composable
private fun LoanCaption(caption: String) {
    Spacer(modifier = Modifier.height(8.dp))

    Text(
        modifier = Modifier.padding(horizontal = 24.dp),
        text = caption,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.inverseSurface,
            fontWeight = FontWeight.Normal
        )
    )
}

/** A planned payment's due date; a tap picks another day, at noon. */
@Composable
private fun PlannedDueDate(dueDate: Instant, onChange: (LocalDateTime) -> Unit) {
    // looked up on tap, so previews without the activity still draw
    val context = LocalContext.current
    val timeConverter = LocalTimeConverter.current
    DueDate(dueDate = dueDate) {
        (context as RootScreen).pickDate(
            initialDate = with(timeConverter) {
                dueDate.toLocalDate()
            }
        ) {
            onChange(it.atTime(12, 0))
        }
    }
}

/** A transfer's rate between its accounts' currencies; [onPositionChange] gets how far down the card is. */
@Composable
private fun TransferRateCard(
    state: EditTransactionViewState,
    onEvent: (EditTransactionViewEvent) -> Unit,
    onEditRate: () -> Unit,
    onPositionChange: (Float) -> Unit,
) {
    CustomExchangeRateCard(
        fromCurrencyCode = state.currency,
        toCurrencyCode = state.customExchangeRateState.toCurrencyCode ?: state.currency,
        exchangeRate = state.customExchangeRateState.exchangeRate,
        onRefresh = {
            // Set exchangeRate to null to reset
            onEvent(EditTransactionViewEvent.UpdateExchangeRate(null))
        },
        modifier = Modifier.onGloballyPositioned { coordinates ->
            onPositionChange(coordinates.positionInParent().y * 0.3f)
        },
        onClick = onEditRate,
    )
}

/** Turns the new transaction into a planned payment, with what's entered so far. */
@Composable
private fun PlanItButton(state: EditTransactionViewState, title: () -> String) {
    val nav = navigation()
    AddPrimaryAttributeButton(
        icon = R.drawable.ic_planned_payments,
        text = stringResource(R.string.add_planned_date_payment),
        onClick = {
            nav.back()
            nav.navigateTo(
                EditPlannedScreen(
                    plannedPaymentRuleId = null,
                    type = state.transactionType,
                    amount = state.amount,
                    accountId = state.account?.id?.value,
                    categoryId = state.category?.id?.value,
                    title = title(),
                    description = state.description,
                )
            )
        }
    )
}

/** Add for a new transaction; Save for an edited one, or Pay (Get) for a planned one without changes. */
@Composable
private fun ActionButton(
    screen: EditTransactionScreen,
    state: EditTransactionViewState,
    onEvent: (EditTransactionViewEvent) -> Unit,
) {
    when {
        // create new mode
        // Without an amount there's nothing to save.
        screen.initialTransactionId == null -> ModalAdd(enabled = state.amount > 0) {
            onEvent(EditTransactionViewEvent.Save(closeScreen = true))
        }
        // normal transaction
        state.dueDate == null -> ModalSave(enabled = state.amount > 0) {
            onEvent(EditTransactionViewEvent.Save(closeScreen = true))
        }
        // a planned one with changes
        state.hasChanges -> ModalSave {
            onEvent(EditTransactionViewEvent.Save(closeScreen = false))
            onEvent(EditTransactionViewEvent.SetHasChanges(false))
        }
        // no changes, pay
        else -> ModalCheck(
            label = if (state.transactionType == TransactionType.EXPENSE) {
                stringResource(R.string.pay)
            } else {
                stringResource(R.string.get)
            }
        ) {
            onEvent(EditTransactionViewEvent.OnPayPlannedPayment)
        }
    }
}

/** A transfer between two currencies shows the rate it's made at. */
private val EditTransactionViewState.showsRateCard: Boolean
    get() = transactionType == TransactionType.TRANSFER && customExchangeRateState.showCard

/** A transaction without a date yet, and not planned nor a transfer, can be planned instead. */
private val EditTransactionViewState.canBecomePlanned: Boolean
    get() = dueDate == null && transactionType != TransactionType.TRANSFER && dateTime == null

private fun shouldFocusCategory(
    category: Category?,
): Boolean = category == null

private fun shouldFocusTitle(
    titleTextFieldValue: TextFieldValue,
    type: TransactionType
): Boolean = titleTextFieldValue.text.isBlank() && type != TransactionType.TRANSFER

private fun shouldFocusAmount(amount: Double) = amount == 0.0
