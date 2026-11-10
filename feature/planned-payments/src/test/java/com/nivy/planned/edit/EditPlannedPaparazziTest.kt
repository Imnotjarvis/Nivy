package com.nivy.planned.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.toArgb
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.colors.Palette
import com.nivy.navigation.EditPlannedScreen
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.ui.previewAccount
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** The planned payment editor, its rule not set yet: there's nothing to save. */
@OptIn(ExperimentalFoundationApi::class)
@RunWith(TestParameterInjector::class)
class EditPlannedPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot a plan without its date and interval yet`() = snapshot(theme, minimal = colors == Colors.Minimal) {
        NivyPreview(theme = if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
            UI(
                screen = EditPlannedScreen(plannedPaymentRuleId = UUID(0, 1), type = TransactionType.EXPENSE),
                state = EditPlannedScreenState(
                    oneTime = false,
                    startDate = null,
                    intervalN = null,
                    intervalType = null,
                    initialTitle = "Rent",
                    currency = "EUR",
                    description = null,
                    category = Housing,
                    account = Bank,
                    creditCards = persistentListOf(),
                    creditCard = null,
                    amount = 750.0,
                    transactionType = TransactionType.EXPENSE,
                    categories = persistentListOf(Housing),
                    accounts = persistentListOf(Bank),
                    categoryModalVisible = false,
                    categoryModalData = null,
                    accountModalData = null,
                    descriptionModalVisible = false,
                    deleteTransactionModalVisible = false,
                    recurringRuleModalData = null,
                    transactionTypeModalVisible = false,
                    amountModalVisible = false,
                ),
                onEvent = {},
            )
        }
    }

    private companion object {
        val Bank = previewAccount("Bank", color = Palette.Blue, icon = "bank", currency = "EUR", id = UUID(0, 10))
        val Housing = Category(
            id = CategoryId(UUID(0, 20)),
            name = NotBlankTrimmedString.unsafe("Housing"),
            color = ColorInt(Palette.Orange.toArgb()),
            icon = null,
            orderNum = 0.0,
        )
    }
}
