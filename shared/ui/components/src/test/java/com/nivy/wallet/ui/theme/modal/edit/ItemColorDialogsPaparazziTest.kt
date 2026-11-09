package com.nivy.wallet.ui.theme.modal.edit

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.AccountType
import com.nivy.base.model.Theme
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.colors.Palette
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * The account and category dialogs, where their colors are picked: the hues, or in minimal colors
 * the greys. The account's green, picked before, shows as the grey nearest its own; the
 * category's grey, picked in minimal colors, as itself.
 */
@RunWith(TestParameterInjector::class)
class ItemColorDialogsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot the account dialog`() = shot {
        AccountModal(
            modal = AccountModalData(account = Savings, baseCurrency = "EUR", balance = 2_500.0, autoFocusKeyboard = false),
            onCreateAccount = {},
            onEditAccount = { _, _, _ -> },
            dismiss = {},
        )
    }

    @Test
    fun `snapshot the category dialog`() = shot {
        CategoryModal(
            modal = CategoryModalData(category = Groceries, autoFocusKeyboard = false),
            onCreateCategory = {},
            onEditCategory = {},
            dismiss = {},
        )
    }

    private fun shot(content: @Composable BoxWithConstraintsScope.() -> Unit) =
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(theme = if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
                content()
            }
        }

    private companion object {
        val Savings = Account(
            id = AccountId(UUID(1L, 1L)),
            name = NotBlankTrimmedString.unsafe("Savings"),
            asset = AssetCode.unsafe("EUR"),
            color = ColorInt(Palette.Green.toArgb()),
            icon = IconAsset.unsafe("safe"),
            includeInBalance = true,
            type = AccountType.SAVINGS,
            archived = false,
            orderNum = 0.0,
        )

        val Groceries = Category(
            id = CategoryId(UUID(9L, 1L)),
            name = NotBlankTrimmedString.unsafe("Groceries"),
            // The third of the greys offered in minimal colors.
            color = ColorInt(0xFF4E4E4E.toInt()),
            icon = IconAsset.unsafe("groceries"),
            orderNum = 0.0,
        )
    }
}
