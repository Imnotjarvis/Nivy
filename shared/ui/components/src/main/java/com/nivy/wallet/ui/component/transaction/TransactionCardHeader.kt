package com.nivy.wallet.ui.component.transaction

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.Tag
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.model.getFromAccount
import com.nivy.design.system.colors.tagBlue
import com.nivy.wallet.utils.capitalizeLocal
import com.nivy.navigation.Navigation
import com.nivy.navigation.TransactionsScreen
import com.nivy.navigation.navigation
import com.nivy.ui.R
import com.nivy.ui.time.LocalTimeConverter
import com.nivy.design.system.colors.ColorMode
import com.nivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.nivy.wallet.ui.theme.components.NivyIcon
import com.nivy.design.system.colors.readableOn
import com.nivy.design.system.colors.categoryColor
import kotlinx.collections.immutable.ImmutableList
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.colors.accountColor

@Composable
internal fun ColumnScope.TransactionTags(tags: ImmutableList<Tag>) {
    Spacer(Modifier.height(12.dp))

    LazyRow(
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        item {
            // Tag Text
            Text(
                text = stringResource(R.string.tags_colon),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            )

            Spacer(modifier = Modifier.width(8.dp))
        }

        items(tags, key = { it.id.value }) { tag ->
            Text(
                text = "#${tag.name.value}",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = tagBlue(MaterialTheme.colorScheme.surfaceVariant),
                    fontWeight = FontWeight.Normal
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TransactionHeaderRow(
    transaction: Transaction,
    categories: List<Category>,
    accounts: List<Account>,
    shouldShowAccountSpecificColorInTransactions: Boolean,
) {
    val nav = navigation()

    val category = category(
        categoryId = transaction.category?.value,
        categories = categories
    )

    if (transaction is Transfer) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            if (category != null) {
                CategoryBadgeDisplay(category, nav)
                Spacer(modifier = Modifier.height(8.dp))
            }
            TransferHeader(
                accounts = accounts,
                transaction = transaction,
                shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions
            )
        }
    } else {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (category != null) {
                CategoryBadgeDisplay(category, nav)
            }

            val account = account(
                accountId = transaction.getFromAccount().value,
                accounts = accounts
            )

            val accountBackgroundColor = if (shouldShowAccountSpecificColorInTransactions) {
                account?.let { accountColor(it.color.value) } ?: MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surface
            }

            TransactionBadge(
                text = account?.name?.value ?: stringResource(R.string.deleted),
                backgroundColor = accountBackgroundColor,
                icon = account?.icon?.id,
                defaultIcon = R.drawable.ic_custom_account_s
            ) {
                account?.let {
                    nav.navigateTo(
                        TransactionsScreen(
                            accountId = account.id.value,
                            categoryId = null
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryBadgeDisplay(
    category: Category,
    nav: Navigation,
) {
    TransactionBadge(
        text = category.name.value,
        backgroundColor = categoryColor(category.color.value),
        icon = category.icon?.id,
        defaultIcon = R.drawable.ic_custom_category_s
    ) {
        // Navigation logic
        nav.navigateTo(
            TransactionsScreen(
                accountId = null,
                categoryId = category.id.value
            )
        )
    }
}

@Composable
@ReadOnlyComposable
internal fun getTransactionDescription(transaction: Transaction): String? {
    val paidFor = with(LocalTimeConverter.current) {
        transaction.metadata.paidForDateTime?.toLocalDateTime()
    }
    val description = transaction.description?.value
    return when {
        description != null -> description
        transaction.metadata.recurringRuleId != null &&
                transaction.settled &&
                paidFor != null -> {
            stringResource(
                R.string.bill_paid,
                paidFor.month.name.lowercase().capitalizeLocal(),
                paidFor.year.toString()
            )
        }

        else -> null
    }
}

@Composable
private fun TransactionBadge(
    text: String,
    backgroundColor: Color,
    icon: String?,
    @DrawableRes
    defaultIcon: Int,

    onClick: () -> Unit
) {
    // In minimal colors the items' grey is the card's own: the tags take the page's color instead.
    val pillColor = if (ColorMode.minimal) MaterialTheme.colorScheme.surface else backgroundColor
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(pillColor, PillShape)
            .clickable {
                onClick()
            }
            .padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(8.dp))

        val contrastColor = readableOn(pillColor)

        ItemIconSDefaultIcon(
            iconName = icon,
            defaultIcon = defaultIcon,
            tint = contrastColor
        )

        Spacer(Modifier.width(4.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                color = contrastColor,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.width(20.dp))
    }
}

@Composable
private fun TransferHeader(
    accounts: List<Account>,
    transaction: Transfer,
    shouldShowAccountSpecificColorInTransactions: Boolean
) {
    val account = remember(accounts, transaction) {
        accounts.find { it.id == transaction.fromAccount }
    }
    val toAccount = remember(accounts, transaction) {
        accounts.find { it.id == transaction.toAccount }
    }
    // In minimal colors the accounts' grey is the card's own: the pill takes the page's color, as tags do.
    val accountColors = shouldShowAccountSpecificColorInTransactions && !ColorMode.minimal

    Row(
        modifier = Modifier
            .then(
                if (accountColors && account != null && toAccount != null) {
                    Modifier
                        .background(
                            // Two solid halves: the source account's color, then the destination's.
                            brush = Brush.horizontalGradient(
                                0f to accountColor(account.color.value),
                                0.5f to accountColor(account.color.value),
                                0.5f to accountColor(toAccount.color.value),
                                1f to accountColor(toAccount.color.value)
                            ),
                            shape = PillShape
                        )
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surface, PillShape)
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(8.dp))

        val accountContrastColor =
            if (accountColors && account != null) {
                readableOn(accountColor(account.color.value))
            } else {
                MaterialTheme.colorScheme.onSurface
            }

        ItemIconSDefaultIcon(
            iconName = account?.icon?.id,
            defaultIcon = R.drawable.ic_custom_account_s,
            tint = accountContrastColor
        )

        Spacer(Modifier.width(4.dp))

        Text(
            modifier = Modifier
                .padding(vertical = 8.dp),
            // used toString() in case of null
            text = account?.name?.value.toString(),
            style = MaterialTheme.typography.labelMedium.copy(
                color = accountContrastColor,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.width(12.dp))

        NivyIcon(icon = R.drawable.ic_arrow_right, tint = accountContrastColor)

        Spacer(Modifier.width(12.dp))

        val toAccountContrastColor =
            if (accountColors && toAccount != null) {
                readableOn(accountColor(toAccount.color.value))
            } else {
                MaterialTheme.colorScheme.onSurface
            }

        ItemIconSDefaultIcon(
            iconName = toAccount?.icon?.id,
            defaultIcon = R.drawable.ic_custom_account_s,
            tint = toAccountContrastColor
        )

        Spacer(Modifier.width(4.dp))

        Text(
            modifier = Modifier
                .padding(vertical = 8.dp),
            // used toString() in case of null
            text = toAccount?.name?.value.toString(),
            style = MaterialTheme.typography.labelMedium.copy(
                color = toAccountContrastColor,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.width(20.dp))
    }
}
