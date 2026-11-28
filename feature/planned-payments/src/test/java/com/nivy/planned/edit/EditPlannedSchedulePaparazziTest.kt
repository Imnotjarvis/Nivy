package com.nivy.planned.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.IntervalType
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/** The planned payment editor of a monthly plan that ends, and of a planned transfer. */
@OptIn(ExperimentalFoundationApi::class)
@RunWith(TestParameterInjector::class)
class EditPlannedSchedulePaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot a monthly plan with an end date`() = shot {
        Editor(state(endDate = November30))
    }

    @Test
    fun `snapshot a planned transfer`() = shot {
        Editor(
            state(
                type = TransactionType.TRANSFER,
                title = "Savings",
                category = null,
                amount = 200.0,
                toAccount = Savings,
            )
        )
    }

    // The app's theme on a clock stopped at FixedTime, so dates read the same on every run.
    private fun shot(content: @Composable BoxWithConstraintsScope.() -> Unit) =
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(
                theme = if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT,
                timeProvider = FixedTime,
            ) {
                content()
            }
        }

    @Composable
    private fun BoxWithConstraintsScope.Editor(state: EditPlannedScreenState) {
        UI(
            screen = EditPlannedScreen(plannedPaymentRuleId = UUID(0, 1), type = state.transactionType),
            state = state,
            onEvent = {},
        )
    }

    private fun state(
        type: TransactionType = TransactionType.EXPENSE,
        title: String = "Rent",
        category: Category? = Housing,
        amount: Double = 750.0,
        startDate: LocalDateTime = December1,
        endDate: LocalDateTime? = null,
        toAccount: Account? = null,
    ) = EditPlannedScreenState(
        oneTime = false,
        endDate = endDate,
        startDate = startDate,
        intervalN = 1,
        intervalType = IntervalType.MONTH,
        initialTitle = title,
        currency = "EUR",
        description = null,
        category = category,
        account = Bank,
        toAccount = toAccount,
        creditCards = persistentListOf(),
        creditCard = null,
        amount = amount,
        transactionType = type,
        categories = persistentListOf(Housing),
        mostUsedCategories = persistentListOf(),
        accounts = persistentListOf(Bank, Savings),
        canSave = true,
    )

    private companion object {
        val December1: LocalDateTime = LocalDateTime.of(2026, 12, 1, 12, 0)
        val November30: LocalDateTime = LocalDateTime.of(2027, 11, 30, 12, 0)
        val Bank = previewAccount("Bank", color = Palette.Blue, icon = "bank", currency = "EUR", id = UUID(0, 10))
        val Savings =
            previewAccount("Savings", color = Palette.Green, icon = "safe", currency = "EUR", id = UUID(0, 11))
        val Housing = Category(
            id = CategoryId(UUID(0, 20)),
            name = NotBlankTrimmedString.unsafe("Housing"),
            color = ColorInt(Palette.Orange.toArgb()),
            icon = null,
            orderNum = 0.0,
        )

        /** November 16, 2026, noon, in UTC. */
        val FixedTime = object : TimeProvider {
            private val now = LocalDateTime.of(2026, 11, 16, 12, 0)
            override fun getZoneId(): ZoneId = ZoneId.of("UTC")
            override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
            override fun localNow(): LocalDateTime = now
            override fun localDateNow(): LocalDate = now.toLocalDate()
            override fun localTimeNow(): LocalTime = now.toLocalTime()
        }
    }
}
