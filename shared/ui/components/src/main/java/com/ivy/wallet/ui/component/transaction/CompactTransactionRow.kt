package com.ivy.wallet.ui.component.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import com.ivy.data.model.getFromAccount
import com.ivy.data.model.getFromValue
import com.ivy.data.temp.migration.getTransactionType
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.categoryColor
import com.ivy.design.system.colors.expenseRed
import com.ivy.design.system.colors.readableOn
import com.ivy.design.system.colors.incomeGreen
import com.ivy.design.system.shapes.PillShape
import com.ivy.design.system.typography.numberBodyLarge
import com.ivy.ui.R
import com.ivy.wallet.data.AppBaseData
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.utils.format

/**
 * A transaction in one row, like the compact account and category cards: its category's icon,
 * its title over its account, and the signed amount in income's green or expenses' red. Its tags
 * and description stay on the full card.
 */
@Composable
fun CompactTransactionRow(
    baseData: AppBaseData,
    transaction: Transaction,
    onClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = transaction.getTransactionType()
    val category = category(transaction.category?.value, baseData.categories)
    val account = account(transaction.getFromAccount().value, baseData.accounts)
    val toAccount = (transaction as? Transfer)?.let { account(it.toAccount.value, baseData.accounts) }
    val page = MaterialTheme.colorScheme.background
    val outline = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
            .clip(MaterialTheme.shapes.medium)
            .border(2.dp, outline, MaterialTheme.shapes.medium)
            .clickable {
                // Like the full card: a transaction of a deleted account doesn't open.
                if (account != null) onClick(transaction)
            }
            .padding(10.dp)
            .testTag("compact_transaction_row"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconBackground = category?.let { categoryColor(it.color.value) } ?: outline
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(PillShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            if (type == TransactionType.TRANSFER) {
                IvyIcon(icon = R.drawable.ic_transfer, tint = MaterialTheme.colorScheme.onSurface)
            } else {
                ItemIconSDefaultIcon(
                    iconName = category?.icon?.id,
                    defaultIcon = R.drawable.ic_custom_category_s,
                    tint = readableOn(iconBackground),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title?.value ?: category?.name?.value ?: stringResource(type.label()),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val accountName = account?.name?.value ?: stringResource(R.string.deleted)
            Text(
                text = if (transaction is Transfer) {
                    "$accountName → ${toAccount?.name?.value ?: stringResource(R.string.deleted)}"
                } else {
                    accountName
                },
                style = MaterialTheme.typography.labelMedium.copy(color = Gray),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        val value = transaction.getFromValue()
        Text(
            text = "${amountPrefix(type)}${value.amount.value.format(value.asset.code)} ${value.asset.code}",
            style = MaterialTheme.typography.numberBodyLarge.copy(
                color = when (type) {
                    TransactionType.INCOME -> incomeGreen(page)
                    TransactionType.EXPENSE -> expenseRed(page)
                    TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }
}

private fun TransactionType.label(): Int = when (this) {
    TransactionType.INCOME -> R.string.income
    TransactionType.EXPENSE -> R.string.expense
    TransactionType.TRANSFER -> R.string.transfer
}
