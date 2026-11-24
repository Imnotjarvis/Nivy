package com.nivy.quickadd

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.QuickAdd
import com.nivy.data.model.QuickAddId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.design.system.colors.Palette
import com.nivy.navigation.EditQuickAddScreen
import com.nivy.quickadd.edit.EditQuickAddState
import com.nivy.quickadd.list.QuickAddsState
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.ui.previewAccount
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import com.nivy.quickadd.edit.UI as QuickAddEditor
import com.nivy.quickadd.list.UI as QuickAddList

/** The quick adds' list, with some and with none yet, and a quick add's editor. */
@RunWith(TestParameterInjector::class)
class QuickAddScreensPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot the quick adds`() = shot {
        QuickAddList(
            state = QuickAddsState(QuickAdds.toImmutableList(), BaseData, reorderVisible = false),
            onEvent = {},
        )
    }

    @Test
    fun `snapshot no quick adds yet`() = shot {
        QuickAddList(state = QuickAddsState(persistentListOf(), BaseData, reorderVisible = false), onEvent = {})
    }

    @Test
    fun `snapshot a quick add of any amount in its editor`() = shot {
        QuickAddEditor(
            screen = EditQuickAddScreen(quickAddId = UUID(0, 1), type = TransactionType.EXPENSE),
            state = EditQuickAddState(
                type = TransactionType.EXPENSE,
                initialTitle = "Lunch",
                currency = "EUR",
                categories = persistentListOf(EatingOut),
                mostUsedCategories = persistentListOf(),
                accounts = persistentListOf(Bank, Savings),
                account = Bank,
                toAccount = null,
                creditCards = persistentListOf(),
                creditCard = null,
                category = EatingOut,
                amount = 0.0,
                categoryModalVisible = false,
                categoryModalData = null,
                accountModalData = null,
                deleteModalVisible = false,
                typeModalVisible = false,
                amountModalVisible = false,
                canSave = true,
            ),
            onEvent = {},
        )
    }

    private fun shot(content: @Composable BoxWithConstraintsScope.() -> Unit) =
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(theme = if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
                content()
            }
        }

    private companion object {
        val Bank = previewAccount("Main Bank", Palette.Blue3, icon = "bank", currency = "EUR", id = UUID(0, 10))
        val Cash = previewAccount("Cash", Palette.Green, icon = "cash", currency = "EUR", id = UUID(0, 11))
        val Savings = previewAccount("Savings", Palette.Purple1, icon = "safe", currency = "EUR", id = UUID(0, 12))
        val Transport = category("Transport", Palette.Blue, "transport", 20)
        val Bar = category("Bar", Palette.Orange3, "coffee", 21)
        val EatingOut = category("Eating out", Palette.Red3, "restaurant", 22)

        val BaseData = AppBaseData(
            baseCurrency = "EUR",
            accounts = persistentListOf(Bank, Cash, Savings),
            categories = persistentListOf(Transport, Bar, EatingOut),
        )

        val QuickAdds = listOf(
            quickAdd("Metro", TransactionType.EXPENSE, Bank, 2.2, Transport),
            quickAdd("Coffee", TransactionType.EXPENSE, Cash, 1.3, Bar),
            quickAdd("Lunch", TransactionType.EXPENSE, Bank, null, EatingOut),
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

        fun category(name: String, color: Color, icon: String, id: Long) = Category(
            id = CategoryId(UUID(0, id)),
            name = NotBlankTrimmedString.unsafe(name),
            color = ColorInt(color.toArgb()),
            icon = IconAsset.unsafe(icon),
            orderNum = 0.0,
        )
    }
}
