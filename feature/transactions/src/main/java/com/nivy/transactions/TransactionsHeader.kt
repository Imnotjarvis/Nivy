package com.nivy.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.Transfer
import com.nivy.wallet.Constants
import com.nivy.wallet.ui.component.IncomeExpensesCards
import com.nivy.ui.time.TimeFormatter
import com.nivy.wallet.ui.component.InvestmentCards
import com.nivy.wallet.ui.component.ItemStatisticToolbar
import com.nivy.wallet.ui.component.transaction.transactions
import com.nivy.wallet.utils.balancePrefix
import com.nivy.design.utils.clickableNoIndication
import com.nivy.design.utils.rememberInteractionSource
import com.nivy.design.utils.StatusBarDarkText
import com.nivy.navigation.EditTransactionScreen
import com.nivy.navigation.PieChartStatisticScreen
import com.nivy.navigation.TransactionsScreen
import com.nivy.navigation.navigation
import com.nivy.ui.R
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.wallet.domain.data.TransactionHistoryItem
import com.nivy.wallet.ui.theme.components.BalanceRow
import com.nivy.wallet.ui.theme.components.BalanceRowMedium
import com.nivy.wallet.ui.theme.components.ItemIconMDefaultIcon
import com.nivy.design.system.colors.dynamicContrast
import com.nivy.design.system.colors.readableOn
import com.nivy.design.system.colors.isDarkColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.colors.accountColor

@Suppress("LongParameterList")
@Composable
internal fun Header(
    screen: TransactionsScreen,
    history: ImmutableList<TransactionHistoryItem>,
    currency: String,
    baseCurrency: String,
    itemColor: Color,
    account: Account?,
    category: Category?,
    balance: Double,
    balanceBaseCurrency: Double?,
    income: Double,
    expenses: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit,

    onBalanceClick: () -> Unit,
    showCategoryModal: () -> Unit,
    showAccountModal: () -> Unit,
    treatTransfersAsIncomeExpense: Boolean = false,
    investment: InvestmentData? = null,
    onUpdateValue: (() -> Unit)? = null,
) {
    val contrastColor = readableOn(itemColor)

    val darkColor = isDarkColor(itemColor)
    StatusBarDarkText(darkText = !darkColor)

    Column(
        modifier = Modifier.background(itemColor)
    ) {
        Spacer(Modifier.height(20.dp))

        // Only an account, or a category of the person's, can be edited or deleted: not the
        // Unspecified page, nor the account transfers Reports opens.
        val editable = (account != null || category != null) &&
            !screen.unspecifiedCategory && screen.transactions.none { it is Transfer }

        ItemStatisticToolbar(
            contrastColor = contrastColor,
            onEdit = onEdit,
            onDelete = onDelete,
            showEditButton = editable,
            showDeleteButton = editable,
        )

        Spacer(Modifier.height(24.dp))

        Item(
            contrastColor = contrastColor,
            account = account,
            category = category,
            showAccountModal = showAccountModal,
            showCategoryModal = showCategoryModal
        )

        BalanceRow(
            modifier = Modifier
                .padding(start = 32.dp)
                .testTag("balance")
                .clickableNoIndication(rememberInteractionSource()) {
                    onBalanceClick()
                },
            textColor = contrastColor,
            currency = currency,
            balance = balance,
            balanceAmountPrefix = if (category != null) {
                balancePrefix(
                    income = income,
                    expenses = expenses
                )
            } else {
                null
            }
        )

        if (currency != baseCurrency && balanceBaseCurrency != null) {
            BalanceRowMedium(
                modifier = Modifier
                    .padding(start = 32.dp)
                    .clickableNoIndication(rememberInteractionSource()) {
                        onBalanceClick()
                    },
                textColor = itemColor.dynamicContrast(),
                currency = baseCurrency,
                balance = balanceBaseCurrency,
                balanceAmountPrefix = if (category != null) {
                    balancePrefix(
                        income = income,
                        expenses = expenses
                    )
                } else {
                    null
                }
            )
        }

        investment?.values?.firstOrNull()?.let { latest ->
            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = stringResource(
                    R.string.last_value_update,
                    with(LocalTimeFormatter.current) {
                        latest.value.time.formatLocal(TimeFormatter.Style.DateOnly(includeWeekDay = true))
                    }
                ),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = itemColor.dynamicContrast(),
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(Modifier.height(20.dp))

        val nav = navigation()
        if (investment != null && account != null && onUpdateValue != null) {
            InvestmentCards(
                currency = currency,
                invested = investment.invested,
                gain = investment.gain,
                cash = investment.cash,
                transfersIn = investment.transfersIn,
                itemColor = itemColor,
                onAddTransfer = {
                    nav.navigateTo(
                        EditTransactionScreen(
                            initialTransactionId = null,
                            type = TransactionType.TRANSFER,
                            toAccountId = account.id.value,
                        )
                    )
                },
                onUpdateValue = onUpdateValue,
            )
        } else {
            IncomeExpensesCards(
                history = history,
                currency = currency,
                income = income,
                expenses = expenses,

                hasAddButtons = true,

                itemColor = itemColor,
                incomeHeaderCardClicked = {
                    if (account != null) {
                        nav.navigateTo(
                            PieChartStatisticScreen(
                                type = TransactionType.INCOME,
                                accountList = persistentListOf(account.id.value),
                                filterExcluded = false,
                                treatTransfersAsIncomeExpense = treatTransfersAsIncomeExpense
                            )
                        )
                    }
                },
                expenseHeaderCardClicked = {
                    if (account != null) {
                        nav.navigateTo(
                            PieChartStatisticScreen(
                                type = TransactionType.EXPENSE,
                                accountList = persistentListOf(account.id.value),
                                filterExcluded = false,
                                treatTransfersAsIncomeExpense = treatTransfersAsIncomeExpense
                            )
                        )
                    }
                }
            ) { trnType ->
                nav.navigateTo(
                    EditTransactionScreen(
                        initialTransactionId = null,
                        type = trnType,
                        accountId = account?.id?.value,
                        categoryId = category?.id?.value
                    )
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Item(
    contrastColor: Color,
    account: Account?,
    category: Category?,

    showCategoryModal: () -> Unit,
    showAccountModal: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(start = 22.dp)
            .clickableNoIndication(rememberInteractionSource()) {
                when {
                    account != null -> {
                        showAccountModal()
                    }

                    category != null -> {
                        showCategoryModal()
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            account != null -> {
                ItemIconMDefaultIcon(
                    iconName = account.icon?.id,
                    defaultIcon = R.drawable.ic_custom_account_m,
                    tint = contrastColor
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = account.name.value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = contrastColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                if (!account.includeInBalance) {
                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = stringResource(R.string.excluded),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = accountColor(account.color.value).dynamicContrast(),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            category != null -> {
                ItemIconMDefaultIcon(
                    iconName = category.icon?.id,
                    defaultIcon = R.drawable.ic_custom_category_m,
                    tint = contrastColor
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = category.name.value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = contrastColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }

            else -> {
                // Unspecified
                ItemIconMDefaultIcon(
                    iconName = null,
                    defaultIcon = R.drawable.ic_custom_category_m,
                    tint = contrastColor
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = Constants.CATEGORY_UNSPECIFIED_NAME,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = contrastColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }
    }
}
