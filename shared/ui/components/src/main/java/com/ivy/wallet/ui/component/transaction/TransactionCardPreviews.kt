package com.ivy.wallet.ui.component.transaction

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.wallet.IvyWalletPreview
import com.ivy.wallet.data.AppBaseData
import com.ivy.wallet.ui.previewAccount
import com.ivy.wallet.ui.previewTransaction
import com.ivy.design.system.colors.Blue
import com.ivy.design.system.colors.Green
import com.ivy.design.system.colors.Orange
import com.ivy.design.system.colors.GreenDark
import com.ivy.design.system.colors.IvyDark
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

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
