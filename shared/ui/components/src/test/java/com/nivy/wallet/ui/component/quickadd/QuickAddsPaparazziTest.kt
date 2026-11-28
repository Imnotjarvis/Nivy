package com.nivy.wallet.ui.component.quickadd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.QuickAdd
import com.nivy.data.model.QuickAddId
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.design.system.colors.Palette
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.previewCategory
import com.nivy.wallet.ui.theme.components.BottomBarMessage
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Quick adds as the add menu's tiles and as their list's rows, and the message a quick add leaves
 * above the bar, in light and dark, in both color modes.
 */
@RunWith(TestParameterInjector::class)
class QuickAddsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot tiles, rows and the message after a quick add`() {
        val dark = theme == PaparazziTheme.Dark
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(theme = if (dark) Theme.DARK else Theme.LIGHT) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickAdds.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach {
                                QuickAddTile(it, BaseData, onClick = {}, onLongClick = {}, Modifier.weight(1f))
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Spacer(Modifier.padding(8.dp))
                    QuickAdds.forEach { QuickAddRow(it, BaseData, onClick = {}) }
                }
                BottomBarMessage(visible = true, text = "Added Metro, 2.20 EUR", action = "Undo", onAction = {})
            }
        }
    }

    private companion object {
        val Bank = previewAccount("Main Bank", Palette.Blue3, icon = "bank", currency = "EUR", id = UUID(0, 1))
        val Cash = previewAccount("Cash", Palette.Green, icon = "cash", currency = "EUR", id = UUID(0, 2))
        val Savings = previewAccount("Savings", Palette.Purple1, icon = "safe", currency = "EUR", id = UUID(0, 3))
        val Transport = category("Transport", Palette.Blue, "transport", 11)
        val Bar = category("Bar", Palette.Orange3, "coffee", 12)
        val EatingOut = category("Eating out", Palette.Red3, "restaurant", 13)
        val Salary = category("Salary", Palette.Green2, "work", 14)

        val BaseData = AppBaseData(
            baseCurrency = "EUR",
            accounts = persistentListOf(Bank, Cash, Savings),
            categories = persistentListOf(Transport, Bar, EatingOut, Salary),
        )

        val QuickAdds = listOf(
            quickAdd("Metro", TransactionType.EXPENSE, Bank, 2.2, Transport),
            quickAdd("Coffee", TransactionType.EXPENSE, Cash, 1.3, Bar),
            quickAdd("Lunch", TransactionType.EXPENSE, Bank, null, EatingOut),
            quickAdd("Tips", TransactionType.INCOME, Cash, 20.0, Salary),
            quickAdd("To savings", TransactionType.TRANSFER, Bank, 50.0, null, toAccount = Savings),
        )

        fun quickAdd(
            title: String,
            type: TransactionType,
            account: Account,
            amount: Double?,
            category: Category?,
            toAccount: Account? = null,
        ) = QuickAdd(
            id = QuickAddId(UUID.nameUUIDFromBytes(title.toByteArray())),
            type = type,
            account = account.id,
            toAccount = toAccount?.id,
            amount = amount?.let(PositiveDouble::unsafe),
            category = category?.id,
            title = NotBlankTrimmedString.unsafe(title),
            creditCard = null,
            orderNum = 0.0,
        )

        fun category(name: String, color: Color, icon: String, id: Long) =
            previewCategory(name, color, icon, id = UUID(0, id))
    }
}
