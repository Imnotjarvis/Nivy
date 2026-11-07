package com.nivy.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.colors.Palette
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.ui.component.transaction.CompactTransactionRow
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.previewTransaction
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(TestParameterInjector::class)
class CompactTransactionsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot compact transaction rows`() {
        snapshot(theme) {
            CompactRows(if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT)
        }
    }
}

// An expense, an income, a transfer and an expense with neither title nor category.
@Composable
private fun CompactRows(theme: Theme) {
    val food = category("Groceries", Palette.Orange, "groceries", 1)
    val salary = category("Salary", Palette.Green, "work", 2)
    val cash = previewAccount("Cash", color = Palette.GreenDark, icon = "cash", currency = "EUR", id = UUID(1L, 1L))
    val bank = previewAccount("Bank", color = Palette.Blue, icon = "bank", currency = "EUR", id = UUID(1L, 2L))
    val baseData = AppBaseData(
        baseCurrency = "EUR",
        accounts = persistentListOf(cash, bank),
        categories = persistentListOf(food, salary),
    )
    val transactions = listOf(
        previewTransaction(TransactionType.EXPENSE, cash, 25.0, title = "Weekly shopping", category = food.id),
        previewTransaction(TransactionType.INCOME, bank, 2000.0, title = "November", category = salary.id),
        previewTransaction(TransactionType.TRANSFER, bank, 300.0, title = "Savings", toAccount = cash),
        previewTransaction(TransactionType.EXPENSE, cash, 4.5),
    )
    NivyPreview(theme) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            transactions.forEach { CompactTransactionRow(baseData = baseData, transaction = it, onClick = {}) }
        }
    }
}

private fun category(name: String, color: Color, icon: String, n: Long) = Category(
    id = CategoryId(UUID(9L, n)),
    name = NotBlankTrimmedString.unsafe(name),
    color = ColorInt(color.toArgb()),
    icon = IconAsset.unsafe(icon),
    orderNum = n.toDouble(),
)
