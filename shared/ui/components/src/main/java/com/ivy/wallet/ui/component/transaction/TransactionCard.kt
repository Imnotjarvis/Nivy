package com.ivy.wallet.ui.component.transaction

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.ivy.design.system.shapes.PillShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Tag
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import com.ivy.data.model.getFromAccount
import com.ivy.data.model.getFromValue
import com.ivy.data.temp.migration.getTransactionType
import com.ivy.wallet.data.AppBaseData
import com.ivy.wallet.utils.format
import com.ivy.ui.R
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider
import com.ivy.ui.time.TimeFormatter
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.design.system.colors.Green
import com.ivy.design.system.colors.Ivy
import com.ivy.design.system.colors.Orange
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.White
import com.ivy.design.system.colors.readableTint
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDateTime
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.typography.numberBodyLarge
import com.ivy.design.system.typography.numberLabelMedium
import com.ivy.design.system.colors.incomeGreen
import com.ivy.design.system.colors.expenseRed

@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
fun TransactionCard(
    baseData: AppBaseData,
    transaction: Transaction,
    shouldShowAccountSpecificColorInTransactions: Boolean,
    onPayOrGet: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    tags: ImmutableList<Tag> = persistentListOf(),
    onSkipTransaction: (Transaction) -> Unit = {},
    onClick: (Transaction) -> Unit,
) {
    val type = transaction.getTransactionType()
    val dueDate = transaction.time.takeUnless { transaction.settled }
    // A credit card expense is listed on its charge date.
    val purchaseDate = transaction.metadata.purchaseDateTime
    val title = transaction.title?.value
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .clickable {
                if (baseData.accounts.find { it.id == transaction.getFromAccount() } != null) {
                    onClick(transaction)
                }
            }
            .testTag("transaction_card")
    ) {
        val transactionCurrency = transaction.getFromValue().asset.code
        val toAccountCurrency = (transaction as? Transfer)?.toValue?.asset?.code ?: transactionCurrency

        Spacer(Modifier.height(20.dp))

        TransactionHeaderRow(
            transaction = transaction,
            categories = baseData.categories,
            accounts = baseData.accounts,
            shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions
        )

        if (dueDate != null) {
            Spacer(Modifier.height(12.dp))
            val timeFormatter = LocalTimeFormatter.current
            val timeProvider = LocalTimeProvider.current
            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = stringResource(
                    R.string.due_on,
                    with(timeFormatter) {
                        dueDate.formatLocal(
                            TimeFormatter.Style.DateOnly(
                                includeWeekDay = true
                            )
                        )
                    }
                ),
                style = MaterialTheme.typography.numberLabelMedium.copy(
                    color = if (dueDate.isAfter(timeProvider.utcNow())) {
                        Orange
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Bold
                )
            )
        }

        if (purchaseDate != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .testTag("transaction_purchase_date"),
                text = stringResource(
                    R.string.purchased_on,
                    with(LocalTimeFormatter.current) {
                        purchaseDate.formatLocal(TimeFormatter.Style.DateOnly(includeWeekDay = true))
                    }
                ),
                style = MaterialTheme.typography.numberLabelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        if (title != null) {
            Spacer(
                Modifier.height(
                    if (dueDate != null || purchaseDate != null) 8.dp else 12.dp
                )
            )
            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        val description = getTransactionDescription(transaction)
        if (!description.isNullOrBlank()) {
            Spacer(Modifier.height(if (title != null) 4.dp else 8.dp))
            Text(
                text = description,
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (dueDate != null) {
            Spacer(Modifier.height(12.dp))
        } else {
            Spacer(Modifier.height(16.dp))
        }

        TypeAmountCurrency(
            transactionType = type,
            dueDate = with(LocalTimeConverter.current) {
                dueDate?.toLocalDateTime()
            },
            currency = transactionCurrency,
            amount = transaction.getFromValue().amount.value
        )

        if (transaction is Transfer && toAccountCurrency != transactionCurrency) {
            Text(
                modifier = Modifier.padding(start = 68.dp),
                text = "${
                    transaction.toValue.amount.value
                        .format(IvyCurrency.getDecimalPlaces(toAccountCurrency))
                } $toAccountCurrency",
                style = MaterialTheme.typography.numberBodyLarge.copy(
                    color = Gray,
                    fontWeight = FontWeight.Normal
                )
            )
        }

        if (!transaction.settled) {
            // Pay/Get button
            Spacer(Modifier.height(16.dp))
            val isExpense = type == TransactionType.EXPENSE
            Row {
                IvyButton(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 24.dp),
                    text = stringResource(R.string.skip),
                    wrapContentMode = false,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    onSkipTransaction(transaction)
                }

                Spacer(Modifier.width(8.dp))

                IvyButton(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 24.dp),
                    text = if (isExpense) stringResource(R.string.pay) else stringResource(R.string.get),
                    wrapContentMode = false,
                    backgroundColor = if (isExpense) MaterialTheme.colorScheme.onSurface else Green,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = if (isExpense) MaterialTheme.colorScheme.surface else White,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    onPayOrGet(transaction)
                }
            }
        }

        if (tags.isNotEmpty()) {
            TransactionTags(tags)
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun TypeAmountCurrency(
    transactionType: TransactionType,
    dueDate: LocalDateTime?,
    currency: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.testTag("type_amount_currency"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        val style = when (transactionType) {
            TransactionType.INCOME -> {
                AmountTypeStyle(
                    icon = R.drawable.ic_income,
                    fill = incomeGreen(MaterialTheme.colorScheme.surfaceVariant),
                    iconTint = White,
                    textColor = incomeGreen(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            TransactionType.EXPENSE -> {
                when {
                    dueDate != null && dueDate.isAfter(LocalTimeProvider.current.localNow()) -> {
                        // Upcoming Expense
                        AmountTypeStyle(
                            icon = R.drawable.ic_expense,
                            fill = Orange,
                            iconTint = White,
                            textColor = Orange
                        )
                    }

                    dueDate != null && dueDate.isBefore(LocalTimeProvider.current.localDateNow().atStartOfDay()) -> {
                        // Overdue Expense: a warning, red also in minimal colors.
                        AmountTypeStyle(
                            icon = R.drawable.ic_overdue,
                            fill = expenseRed(MaterialTheme.colorScheme.surfaceVariant),
                            iconTint = White,
                            textColor = expenseRed(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }

                    else -> {
                        // Normal Expense
                        AmountTypeStyle(
                            icon = R.drawable.ic_expense,
                            fill = expenseRed(MaterialTheme.colorScheme.surfaceVariant),
                            iconTint = White,
                            textColor = expenseRed(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }

            TransactionType.TRANSFER -> {
                // Transfer
                AmountTypeStyle(
                    icon = R.drawable.ic_transfer,
                    fill = Ivy,
                    iconTint = White,
                    textColor = Ivy
                )
            }
        }

        IvyIcon(
            modifier = Modifier
                .background(style.fill, PillShape),
            icon = style.icon,
            tint = readableTint(style.fill, style.iconTint)
        )

        Spacer(Modifier.width(12.dp))

        AmountCurrencyB1(
            amount = amount,
            currency = currency,
            textColor = style.textColor,
            prefix = amountPrefix(transactionType)
        )

        Spacer(Modifier.width(24.dp))
    }
}

/**
 * The sign before a transaction's amount: "+" for income, "-" for expenses, none for transfers.
 * With the green and red, and the in and out icons, it tells them apart also without color.
 */
fun amountPrefix(type: TransactionType): String = when (type) {
    TransactionType.INCOME -> "+"
    TransactionType.EXPENSE -> "-"
    TransactionType.TRANSFER -> ""
}

private data class AmountTypeStyle(
    @DrawableRes val icon: Int,
    val fill: Color,
    val iconTint: Color,
    val textColor: Color
)
