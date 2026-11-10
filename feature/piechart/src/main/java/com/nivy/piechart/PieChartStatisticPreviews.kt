package com.nivy.piechart

import java.time.ZoneOffset
import java.time.Instant
import kotlinx.collections.immutable.toImmutableList
import com.nivy.wallet.domain.data.TransactionHistoryItem
import com.nivy.wallet.domain.data.TransactionHistoryEntry
import com.nivy.wallet.domain.data.TransactionHistoryDateDivider
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.AppBaseData
import com.nivy.navigation.BudgetPage
import com.nivy.data.model.Budget
import com.nivy.data.model.BudgetId
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.TransactionId
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Expense
import com.nivy.data.model.AccountId
import com.nivy.data.model.Account
import com.nivy.base.model.Theme
import com.nivy.base.model.AccountType
import com.nivy.wallet.ui.component.transaction.transactions
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.nivy.base.model.TransactionType
import com.nivy.base.time.impl.DeviceTimeProvider
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.NivyDark
import com.nivy.design.system.colors.NivyLight
import com.nivy.design.system.colors.Orange
import com.nivy.design.system.colors.Red
import com.nivy.design.system.colors.RedLight
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview_Expense() {
    com.nivy.wallet.NivyPreview {
        val state = PieChartStatisticState(
            transactionType = TransactionType.EXPENSE,
            period = com.nivy.wallet.data.model.TimePeriod.currentMonth(
                startDayOfMonth = 1,
                timeProvider = DeviceTimeProvider(),
            ), // preview
            baseCurrency = "BGN",
            totalAmount = 1828.0,
            categoryAmounts = persistentListOf(
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Bills"),
                        color = ColorInt(Green.toArgb()),
                        icon = IconAsset.unsafe("bills"),
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 791.0
                ),
                CategoryAmount(
                    category = null,
                    amount = 497.0,
                    isCategoryUnspecified = true
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Shisha"),
                        color = ColorInt(Orange.toArgb()),
                        icon = IconAsset.unsafe("trees"),
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 411.93
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Food & Drink"),
                        color = ColorInt(NivyDark.toArgb()),
                        icon = null,
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 260.03
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Gifts"),
                        color = ColorInt(RedLight.toArgb()),
                        icon = null,
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 160.0
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Clothes & Jewelery Fancy"),
                        color = ColorInt(Red.toArgb()),
                        icon = null,
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 2.0
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Finances, Burocracy & Governance"),
                        color = ColorInt(NivyLight.toArgb()),
                        icon = IconAsset.unsafe("work"),
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 2.0
                ),
            ),
            selectedCategory = null,
            accountIdFilterList = persistentListOf(),
            choosePeriodModal = null,
            showCloseButtonOnly = false,
            transactions = persistentListOf()
        )

        UI(state = state)
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview_Income() {
    com.nivy.wallet.NivyPreview {
        val state = PieChartStatisticState(
            transactionType = TransactionType.INCOME,
            period = com.nivy.wallet.data.model.TimePeriod.currentMonth(
                startDayOfMonth = 1,
                timeProvider = DeviceTimeProvider(),
            ), // preview
            baseCurrency = "BGN",
            totalAmount = 1828.0,
            categoryAmounts = persistentListOf(
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Bills"),
                        color = ColorInt(Green.toArgb()),
                        icon = IconAsset.unsafe("bills"),
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 791.0
                ),
                CategoryAmount(
                    category = null,
                    amount = 497.0,
                    isCategoryUnspecified = true
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Shisha"),
                        color = ColorInt(Orange.toArgb()),
                        icon = IconAsset.unsafe("trees"),
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 411.93
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Food & Drink"),
                        color = ColorInt(NivyDark.toArgb()),
                        icon = null,
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 260.03
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Gifts"),
                        color = ColorInt(RedLight.toArgb()),
                        icon = null,
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 160.0
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Clothes & Jewelery Fancy"),
                        color = ColorInt(Red.toArgb()),
                        icon = null,
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 2.0
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Finances, Burocracy & Governance"),
                        color = ColorInt(NivyLight.toArgb()),
                        icon = IconAsset.unsafe("work"),
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 2.0
                ),
            ),
            selectedCategory = null,
            accountIdFilterList = persistentListOf(),
            choosePeriodModal = null,
            showCloseButtonOnly = false,
            transactions = persistentListOf()
        )

        UI(state = state)
    }
}

/** For screenshot testing: a budget's page, its two categories and its transactions of the month. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BudgetPageUiTest(isDark: Boolean) {
    val groceries = sampleCategory("Groceries", Green, "groceries")
    val eatingOut = sampleCategory("Eating out", Orange, "fooddrink")
    val card = Account(
        id = AccountId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe("Card"),
        asset = AssetCode.unsafe("EUR"),
        color = ColorInt(NivyDark.toArgb()),
        icon = null,
        includeInBalance = true,
        type = AccountType.BANK,
        archived = false,
        orderNum = 0.0,
    )
    val expenses = listOf(
        sampleExpense(card, groceries, 64.2, "Supermarket", "2023-11-12T18:20:00Z"),
        sampleExpense(card, eatingOut, 38.0, "Pizza night", "2023-11-12T20:30:00Z"),
        sampleExpense(card, groceries, 150.2, "Weekly shop", "2023-11-07T11:00:00Z"),
        sampleExpense(card, eatingOut, 66.0, "Sushi", "2023-11-03T13:15:00Z"),
    )
    val history = expenses.groupBy { it.time.atZone(ZoneOffset.UTC).toLocalDate() }.flatMap { (date, ofDay) ->
        listOf<TransactionHistoryItem>(
            TransactionHistoryDateDivider(date = date, income = 0.0, expenses = ofDay.sumOf { it.value.amount.value }),
        ) + ofDay.map { TransactionHistoryEntry(it) }
    }
    com.nivy.wallet.NivyPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        UI(
            state = PieChartStatisticState(
                transactionType = TransactionType.EXPENSE,
                period = com.nivy.wallet.data.model.TimePeriod(month = Month.fromMonthValue(11), year = 2023),
                baseCurrency = "EUR",
                totalAmount = 318.4,
                categoryAmounts = persistentListOf(
                    CategoryAmount(category = groceries, amount = 214.4),
                    CategoryAmount(category = eatingOut, amount = 104.0),
                ),
                selectedCategory = null,
                accountIdFilterList = persistentListOf(),
                showCloseButtonOnly = true,
                transactions = expenses.toImmutableList(),
                choosePeriodModal = null,
                budgetPage = BudgetPage(
                    budget = Budget(
                        id = BudgetId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Food"),
                        amount = PositiveDouble.unsafe(400.0),
                        categories = listOf(groceries.id, eatingOut.id),
                        accounts = emptyList(),
                        orderNum = 0.0,
                    ),
                    month = "November 2023",
                    range = ClosedTimeRange(
                        from = Instant.parse("2023-11-01T00:00:00Z"),
                        to = Instant.parse("2023-11-30T23:59:59Z"),
                    ),
                ),
                history = history.toImmutableList(),
                baseData = AppBaseData(
                    baseCurrency = "EUR",
                    accounts = persistentListOf(card),
                    categories = persistentListOf(groceries, eatingOut),
                ),
            )
        )
    }
}

private fun sampleCategory(name: String, color: Color, icon: String) = Category(
    name = NotBlankTrimmedString.unsafe(name),
    color = ColorInt(color.toArgb()),
    icon = IconAsset.unsafe(icon),
    id = CategoryId(UUID.randomUUID()),
    orderNum = 0.0,
)

private fun sampleExpense(account: Account, category: Category, amount: Double, title: String, time: String) =
    Expense(
        id = TransactionId(UUID.randomUUID()),
        title = NotBlankTrimmedString.unsafe(title),
        description = null,
        category = category.id,
        time = Instant.parse(time),
        settled = true,
        metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
        ),
        value = PositiveValue(PositiveDouble.unsafe(amount), account.asset),
        account = account.id,
    )
