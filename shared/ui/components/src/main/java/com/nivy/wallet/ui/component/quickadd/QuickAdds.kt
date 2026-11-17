package com.nivy.wallet.ui.component.quickadd

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nivy.base.model.TransactionType
import com.nivy.data.model.AccountId
import com.nivy.data.model.QuickAdd
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.categoryColor
import com.nivy.design.system.colors.expenseRed
import com.nivy.design.system.colors.incomeGreen
import com.nivy.design.system.colors.readableOn
import com.nivy.design.system.shapes.AppCornerRadius
import com.nivy.design.system.shapes.nestedShape
import com.nivy.design.system.typography.numberBodyLarge
import com.nivy.ui.R
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.ui.component.transaction.amountPrefix
import com.nivy.wallet.ui.component.transaction.label
import com.nivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.nivy.wallet.ui.theme.components.NivyIcon
import com.nivy.wallet.utils.amountAndCode

/**
 * A quick add in the add menu: its category's icon on its color, then its name over its amount.
 * The menu's panel holds it 16 dp in from its sides, so its corners are the panel's less that,
 * as Add planned payment's are.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickAddTile(
    quickAdd: QuickAdd,
    baseData: AppBaseData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = nestedShape(AppCornerRadius.ScreenMargin)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, shape)
            .combinedClickable(role = Role.Button, onLongClick = onLongClick, onClick = onClick)
            .padding(12.dp),
    ) {
        QuickAddIcon(quickAdd, baseData, size = 32.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = quickAdd.name(baseData),
            style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = quickAdd.amountText(baseData),
            style = MaterialTheme.typography.labelMedium.copy(color = Gray),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * A quick add in their list, as Home's compact rows show a transaction: its category's icon on
 * its color, its name over its account, and its signed amount, or that it's entered each time.
 */
@Composable
fun QuickAddRow(
    quickAdd: QuickAdd,
    baseData: AppBaseData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = MaterialTheme.colorScheme.background
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuickAddIcon(quickAdd, baseData, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = quickAdd.name(baseData),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = quickAdd.accounts(baseData),
                style = MaterialTheme.typography.labelMedium.copy(color = Gray),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        val amount = quickAdd.amount
        if (amount == null) {
            Text(
                text = stringResource(R.string.any_amount),
                style = MaterialTheme.typography.labelMedium.copy(color = Gray),
                maxLines = 1,
            )
        } else {
            Text(
                text = amountPrefix(quickAdd.type) + amountAndCode(amount.value, quickAdd.currency(baseData)),
                style = MaterialTheme.typography.numberBodyLarge.copy(
                    color = when (quickAdd.type) {
                        TransactionType.INCOME -> incomeGreen(page)
                        TransactionType.EXPENSE -> expenseRed(page)
                        TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
    }
}

/** What names a quick add: its title, else its category's name, else its type. */
@Composable
fun QuickAdd.name(baseData: AppBaseData): String =
    title?.value
        ?: baseData.categories.find { it.id == category }?.name?.value
        ?: stringResource(type.label())

/** Its amount in its account's currency, or that it's entered each time. */
@Composable
fun QuickAdd.amountText(baseData: AppBaseData): String =
    amount?.let { amountAndCode(it.value, currency(baseData)) } ?: stringResource(R.string.any_amount)

/** The currency of the account it's added to. */
fun QuickAdd.currency(baseData: AppBaseData): String =
    baseData.accounts.find { it.id == account }?.asset?.code ?: baseData.baseCurrency

// Its account, or a transfer's two.
@Composable
private fun QuickAdd.accounts(baseData: AppBaseData): String {
    fun name(id: AccountId) = baseData.accounts.find { it.id == id }?.name?.value
    val from = name(account) ?: stringResource(R.string.deleted)
    val to = toAccount ?: return from
    return "$from → ${name(to) ?: stringResource(R.string.deleted)}"
}

// The category's icon on its color, as Home's compact rows draw it; a transfer's arrows on grey.
@Composable
private fun QuickAddIcon(quickAdd: QuickAdd, baseData: AppBaseData, size: Dp) {
    val category = baseData.categories.find { it.id == quickAdd.category }
    val background = category?.let { categoryColor(it.color.value) } ?: MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (quickAdd.type == TransactionType.TRANSFER) {
            NivyIcon(icon = R.drawable.ic_transfer, tint = MaterialTheme.colorScheme.onSurface)
        } else {
            ItemIconSDefaultIcon(
                iconName = category?.icon?.id,
                defaultIcon = R.drawable.ic_custom_category_s,
                tint = readableOn(background),
            )
        }
    }
}
