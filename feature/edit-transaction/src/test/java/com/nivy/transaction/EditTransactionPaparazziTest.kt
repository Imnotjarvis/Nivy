package com.nivy.transaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Tag
import com.nivy.data.model.TagId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.colors.Palette
import com.nivy.navigation.EditTransactionScreen
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.data.EditTransactionDisplayLoan
import com.nivy.wallet.domain.data.CustomExchangeRateState
import com.nivy.wallet.ui.component.tags.ShowTagModal
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.theme.modal.edit.ChooseCategoryModal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/** The transaction editor as it opens an existing transaction, and with its category and tag dialogs. */
@OptIn(ExperimentalFoundationApi::class)
@RunWith(TestParameterInjector::class)
class EditTransactionPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot an expense`() = shot {
        Editor(
            type = TransactionType.EXPENSE,
            title = "Groceries at Lidl",
            amount = 42.5,
            category = Groceries,
            description = "Weekly shop",
            associatedTags = persistentListOf(Holiday.id),
        )
    }

    @Test
    fun `snapshot an income`() = shot {
        Editor(TransactionType.INCOME, "Salary", 2_100.0, Salary)
    }

    @Test
    fun `snapshot a transfer with an exchange rate`() = shot {
        Editor(
            type = TransactionType.TRANSFER,
            title = "Savings",
            amount = 100.0,
            category = null,
            toAccount = Revolut,
            exchangeRate = CustomExchangeRateState(
                showCard = true,
                fromCurrencyCode = "EUR",
                toCurrencyCode = "USD",
                exchangeRate = 1.08,
                convertedAmount = 108.0,
            ),
        )
    }

    @Test
    fun `snapshot a large transfer with an exchange rate`() = shot {
        Editor(
            type = TransactionType.TRANSFER,
            title = "Savings",
            amount = 1_250.0,
            category = null,
            toAccount = Revolut,
            exchangeRate = CustomExchangeRateState(
                showCard = true,
                fromCurrencyCode = "EUR",
                toCurrencyCode = "USD",
                exchangeRate = 1.08,
                convertedAmount = 1_350.0,
            ),
        )
    }

    @Test
    fun `snapshot the category dialog`() = shot {
        Editor(TransactionType.EXPENSE, "Groceries at Lidl", 42.5, Groceries)
        ChooseCategoryModal(
            visible = true,
            initialCategory = Groceries,
            categories = Categories,
            showCategoryModal = {},
            onCategoryChanged = {},
            dismiss = {},
        )
    }

    @Test
    fun `snapshot the tag dialog`() = shot {
        Editor(
            type = TransactionType.EXPENSE,
            title = "Groceries at Lidl",
            amount = 42.5,
            category = Groceries,
            associatedTags = persistentListOf(Holiday.id),
        )
        ShowTagModal(
            visible = true,
            allTagList = Tags,
            selectedTagList = persistentListOf(Holiday.id),
            onDismiss = {},
            onTagAdd = {},
            onTagEdit = { _, _ -> },
            onTagDelete = {},
            onTagSelected = {},
            onTagDeSelected = {},
            // Focusing asks for the keyboard, and the screenshot tests' renderer can crash
            // starting it (the input method's latency tracker, on a thread it starts).
            focusSearch = false,
            onTagSearch = {},
        )
    }

    // The app's theme on a clock stopped at FixedTime, so dates read the same on every run.
    private fun shot(content: @Composable BoxWithConstraintsScope.() -> Unit) =
        snapshot(theme, minimal = colors == Colors.Minimal) {
            val dark = theme == PaparazziTheme.Dark
            NivyPreview(theme = if (dark) Theme.DARK else Theme.LIGHT, timeProvider = FixedTime) {
                content()
            }
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoxWithConstraintsScope.Editor(
    type: TransactionType,
    title: String,
    amount: Double,
    category: Category?,
    description: String? = null,
    associatedTags: ImmutableList<TagId> = persistentListOf(),
    toAccount: Account? = null,
    exchangeRate: CustomExchangeRateState = CustomExchangeRateState(),
) {
    UI(
        // An existing transaction: a new one opens the amount panel at once.
        screen = EditTransactionScreen(initialTransactionId = UUID(0, 1), type = type),
        state = EditTransactionViewState(
            transactionType = type,
            initialTitle = title,
            titleSuggestions = persistentSetOf(),
            currency = "EUR",
            description = description,
            dateTime = Instant.parse("2026-11-03T17:30:00Z"),
            dueDate = null,
            creditCards = persistentListOf(),
            creditCard = null,
            chargeDate = null,
            canMoveChargeEarlier = false,
            accounts = persistentListOf(Bank, Revolut),
            categories = Categories,
            account = Bank,
            toAccount = toAccount,
            category = category,
            amount = amount,
            canSave = amount > 0.0,
            hasChanges = false,
            displayLoanHelper = EditTransactionDisplayLoan(),
            backgroundProcessingStarted = false,
            customExchangeRateState = exchangeRate,
            tags = Tags,
            transactionAssociatedTags = associatedTags,
        ),
        onEvent = {},
    )
}

/** November 7, 2026, noon, in UTC. */
private object FixedTime : TimeProvider {
    private val now = LocalDateTime.of(2026, 11, 7, 12, 0)
    override fun getZoneId(): ZoneId = ZoneId.of("UTC")
    override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
    override fun localNow(): LocalDateTime = now
    override fun localDateNow(): LocalDate = now.toLocalDate()
    override fun localTimeNow(): LocalTime = now.toLocalTime()
}

// Stored colors are the palette's own: minimal colors turn them grey where they're drawn.
private val Bank = previewAccount("Bank", color = Palette.Blue, icon = "bank", currency = "EUR")
private val Revolut = previewAccount("Revolut", color = Palette.Purple, icon = "revolut", currency = "USD")

private val Groceries = category("Groceries", Palette.Green, "groceries")
private val Salary = category("Salary", Palette.Yellow, "work")
private val Rent = category("Rent", Palette.Orange, "house")
private val Categories = persistentListOf(Groceries, Salary, Rent)

private val Holiday = tag("Holiday")
private val Tags = persistentListOf(Holiday, tag("Work"), tag("Family"))

private fun category(name: String, color: Color, icon: String) = Category(
    id = CategoryId(UUID.nameUUIDFromBytes(name.toByteArray())),
    name = NotBlankTrimmedString.unsafe(name),
    color = ColorInt(color.toArgb()),
    icon = IconAsset.unsafe(icon),
    orderNum = 0.0,
)

private fun tag(name: String) = Tag(
    id = TagId(UUID.nameUUIDFromBytes(name.toByteArray())),
    name = NotBlankTrimmedString.unsafe(name),
    creationTimestamp = Instant.parse("2026-01-01T00:00:00Z"),
)
