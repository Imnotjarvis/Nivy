package com.ivy.wallet.ui.component.transaction

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Account
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.Tag
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import com.ivy.data.model.getFromAccount
import com.ivy.data.model.getFromValue
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.temp.migration.getTransactionType
import com.ivy.design.system.colors.BlueLight
import com.ivy.wallet.IvyWalletPreview
import com.ivy.wallet.data.AppBaseData
import com.ivy.wallet.ui.previewAccount
import com.ivy.wallet.ui.previewTransaction
import com.ivy.wallet.utils.capitalizeLocal
import com.ivy.wallet.utils.format
import com.ivy.navigation.Navigation
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider
import com.ivy.ui.time.TimeFormatter
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.design.system.colors.Blue
import com.ivy.design.system.colors.Gradient
import com.ivy.design.system.colors.GradientGreen
import com.ivy.design.system.colors.GradientIvy
import com.ivy.design.system.colors.GradientOrangeRevert
import com.ivy.design.system.colors.GradientRed
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.Green
import com.ivy.design.system.colors.GreenDark
import com.ivy.design.system.colors.Ivy
import com.ivy.design.system.colors.IvyDark
import com.ivy.design.system.colors.Orange
import com.ivy.design.system.colors.Red
import com.ivy.design.system.colors.White
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.design.system.colors.findContrastTextColor
import com.ivy.design.system.colors.gradientExpenses
import com.ivy.design.system.colors.toComposeColor
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDateTime
import java.util.UUID
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.typography.numberBodyLarge
import com.ivy.design.system.typography.numberLabelMedium

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
                ).uppercase(),
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

        if (title != null) {
            Spacer(
                Modifier.height(
                    if (dueDate != null) 8.dp else 12.dp
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
                style = MaterialTheme.typography.numberLabelMedium.copy(
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
                    backgroundGradient = Gradient.solid(MaterialTheme.colorScheme.surface),
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
                    backgroundGradient = if (isExpense) gradientExpenses() else GradientGreen,
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
private fun ColumnScope.TransactionTags(tags: ImmutableList<Tag>) {
    Spacer(Modifier.height(12.dp))

    LazyRow(
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        item {
            // Tag Text
            Text(
                text = "Tags:",
                style = MaterialTheme.typography.numberLabelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            )

            Spacer(modifier = Modifier.width(8.dp))
        }

        items(tags, key = { it.id.value }) { tag ->
            Text(
                text = "#${tag.name.value}",
                style = MaterialTheme.typography.numberLabelMedium.copy(
                    color = BlueLight,
                    fontWeight = FontWeight.Normal
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransactionHeaderRow(
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
                account?.color?.value?.toComposeColor() ?: MaterialTheme.colorScheme.surface
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
        backgroundColor = category.color.value.toComposeColor(),
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
private fun getTransactionDescription(transaction: Transaction): String? {
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
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor, CircleShape)
            .clickable {
                onClick()
            }
            .padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(8.dp))

        val contrastColor = findContrastTextColor(backgroundColor)

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

private const val TransferHeaderGradientThreshold = 0.35f

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

    Row(
        modifier = Modifier
            .then(
                if (shouldShowAccountSpecificColorInTransactions && account != null && toAccount != null) {
                    Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                0f to account.color.value.toComposeColor(),
                                (TransferHeaderGradientThreshold) to account.color.value.toComposeColor(),
                                (1f - TransferHeaderGradientThreshold) to toAccount.color.value.toComposeColor(),
                                1f to toAccount.color.value.toComposeColor()
                            ),
                            shape = CircleShape
                        )
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(8.dp))

        val accountContrastColor =
            if (shouldShowAccountSpecificColorInTransactions && account != null) {
                findContrastTextColor(account.color.value.toComposeColor())
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

        IvyIcon(icon = R.drawable.ic_arrow_right, tint = accountContrastColor)

        Spacer(Modifier.width(12.dp))

        val toAccountContrastColor =
            if (shouldShowAccountSpecificColorInTransactions && toAccount != null) {
                findContrastTextColor(toAccount.color.value.toComposeColor())
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
                    gradient = GradientGreen,
                    iconTint = White,
                    textColor = Green
                )
            }

            TransactionType.EXPENSE -> {
                when {
                    dueDate != null && dueDate.isAfter(LocalTimeProvider.current.localNow()) -> {
                        // Upcoming Expense
                        AmountTypeStyle(
                            icon = R.drawable.ic_expense,
                            gradient = GradientOrangeRevert,
                            iconTint = White,
                            textColor = Orange
                        )
                    }

                    dueDate != null && dueDate.isBefore(LocalTimeProvider.current.localDateNow().atStartOfDay()) -> {
                        // Overdue Expense
                        AmountTypeStyle(
                            icon = R.drawable.ic_overdue,
                            gradient = GradientRed,
                            iconTint = White,
                            textColor = Red
                        )
                    }

                    else -> {
                        // Normal Expense
                        AmountTypeStyle(
                            icon = R.drawable.ic_expense,
                            gradient = Gradient.black(),
                            iconTint = White,
                            textColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            TransactionType.TRANSFER -> {
                // Transfer
                AmountTypeStyle(
                    icon = R.drawable.ic_transfer,
                    gradient = GradientIvy,
                    iconTint = White,
                    textColor = Ivy
                )
            }
        }

        IvyIcon(
            modifier = Modifier
                .background(style.gradient.asHorizontalBrush(), CircleShape),
            icon = style.icon,
            tint = style.iconTint
        )

        Spacer(Modifier.width(12.dp))

        AmountCurrencyB1(
            amount = amount,
            currency = currency,
            textColor = style.textColor
        )

        Spacer(Modifier.width(24.dp))
    }
}

private data class AmountTypeStyle(
    @DrawableRes val icon: Int,
    val gradient: Gradient,
    val iconTint: Color,
    val textColor: Color
)

@Preview
@Composable
private fun PreviewUpcomingExpense() {
    IvyWalletPreview {
        LazyColumn(Modifier.fillMaxSize()) {
            val cash = previewAccount("Cash", color = Green, currency = "BGN")
            val food = Category(
                name = NotBlankTrimmedString.unsafe("Food"),
                color = ColorInt(Blue.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            )

            item {
                TransactionCard(
                    baseData = AppBaseData(
                        baseCurrency = "BGN",
                        categories = persistentListOf(food),
                        accounts = persistentListOf(cash)
                    ),
                    transaction = previewTransaction(
                        type = TransactionType.EXPENSE,
                        account = cash,
                        amount = 250.75,
                        time = Instant.now().plus(5, ChronoUnit.DAYS),
                        settled = false,
                        title = "Lidl pazar",
                        category = food.id,
                    ),
                    shouldShowAccountSpecificColorInTransactions = false,
                    onPayOrGet = {},
                ) {
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewUpcomingExpenseBadgeSecondRow() {
    IvyWalletPreview {
        LazyColumn(Modifier.fillMaxSize()) {
            val cash = previewAccount("Cash", color = Green, currency = "BGN")
            val food = Category(
                name = NotBlankTrimmedString.unsafe("Food-Travel-Entertaiment-Food"),
                color = ColorInt(Blue.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            )

            item {
                TransactionCard(
                    baseData = AppBaseData(
                        baseCurrency = "BGN",
                        categories = persistentListOf(food),
                        accounts = persistentListOf(cash)
                    ),
                    transaction = previewTransaction(
                        type = TransactionType.EXPENSE,
                        account = cash,
                        amount = 250.75,
                        time = Instant.now().plus(5, ChronoUnit.DAYS),
                        settled = false,
                        title = "Lidl pazar",
                        category = food.id,
                    ),
                    shouldShowAccountSpecificColorInTransactions = false,
                    onPayOrGet = {},
                ) {
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewOverdueExpense() {
    IvyWalletPreview {
        LazyColumn(Modifier.fillMaxSize()) {
            val cash = previewAccount("Cash", color = Green, currency = "BGN")
            val food = Category(
                name = NotBlankTrimmedString.unsafe("Rent"),
                color = ColorInt(Green.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            )

            item {
                TransactionCard(
                    baseData = AppBaseData(
                        baseCurrency = "BGN",
                        categories = persistentListOf(food),
                        accounts = persistentListOf(cash)
                    ),
                    transaction = previewTransaction(
                        type = TransactionType.EXPENSE,
                        account = cash,
                        amount = 500.0,
                        time = Instant.now().minus(5, ChronoUnit.DAYS),
                        settled = false,
                        title = "Rent",
                        category = food.id,
                    ),
                    shouldShowAccountSpecificColorInTransactions = false,
                    onPayOrGet = {},
                ) {
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewNormalExpense() {
    IvyWalletPreview {
        LazyColumn(Modifier.fillMaxSize()) {
            val cash = previewAccount("Cash", color = Green, currency = "BGN")
            val food = Category(
                name = NotBlankTrimmedString.unsafe("Bitovi"),
                color = ColorInt(Orange.toArgb()),
                icon = IconAsset.unsafe("groceries"),
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            )

            item {
                TransactionCard(
                    baseData = AppBaseData(
                        baseCurrency = "BGN",
                        categories = persistentListOf(food),
                        accounts = persistentListOf(cash)
                    ),
                    transaction = previewTransaction(
                        type = TransactionType.EXPENSE,
                        account = cash,
                        amount = 32.51,
                        title = "Близкия магазин",
                        category = food.id,
                    ),
                    shouldShowAccountSpecificColorInTransactions = false,
                    onPayOrGet = {},
                ) {
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewIncome() {
    IvyWalletPreview {
        LazyColumn(Modifier.fillMaxSize()) {
            val cash = previewAccount("DSK Bank", color = Green, currency = "BGN")
            val category = Category(
                name = NotBlankTrimmedString.unsafe("Salary"),
                color = ColorInt(GreenDark.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            )

            item {
                TransactionCard(
                    baseData = AppBaseData(
                        baseCurrency = "BGN",
                        categories = persistentListOf(category),
                        accounts = persistentListOf(cash)
                    ),
                    transaction = previewTransaction(
                        type = TransactionType.INCOME,
                        account = cash,
                        amount = 8049.70,
                        title = "Qredo Salary May",
                        category = category.id,
                    ),
                    shouldShowAccountSpecificColorInTransactions = false,
                    onPayOrGet = {},
                ) {
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewTransfer() {
    IvyWalletPreview {
        LazyColumn(Modifier.fillMaxSize()) {
            val acc1 = previewAccount("DSK Bank", color = Green, icon = "bank", currency = "BGN")
            val acc2 = previewAccount("Revolut", color = IvyDark, icon = "revolut", currency = "BGN")

            item {
                TransactionCard(
                    baseData = AppBaseData(
                        baseCurrency = "BGN",
                        categories = persistentListOf(),
                        accounts = persistentListOf(acc1, acc2)
                    ),
                    transaction = previewTransaction(
                        type = TransactionType.TRANSFER,
                        account = acc1,
                        amount = 1000.0,
                        title = "Top-up revolut",
                        toAccount = acc2,
                    ),
                    shouldShowAccountSpecificColorInTransactions = false,
                    onPayOrGet = {},
                ) {
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewTransfer_differentCurrency() {
    IvyWalletPreview {
        LazyColumn(Modifier.fillMaxSize()) {
            val acc1 = previewAccount("DSK Bank", color = Green, icon = "bank", currency = "BGN")
            val acc2 = previewAccount("Revolut", color = IvyDark, icon = "revolut", currency = "EUR")

            item {
                TransactionCard(
                    baseData = AppBaseData(
                        baseCurrency = "BGN",
                        categories = persistentListOf(),
                        accounts = persistentListOf(acc1, acc2)
                    ),
                    transaction = previewTransaction(
                        type = TransactionType.TRANSFER,
                        account = acc1,
                        amount = 1000.0,
                        title = "Top-up revolut",
                        toAccount = acc2,
                        toAmount = 510.0,
                    ),
                    shouldShowAccountSpecificColorInTransactions = true,
                    onPayOrGet = {},
                ) {
                }
            }
        }
    }
}
