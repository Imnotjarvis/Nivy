package com.ivy.transaction

import android.content.Context
import arrow.core.Some
import com.ivy.base.Toaster
import com.ivy.base.model.AccountType
import com.ivy.base.model.TransactionType
import com.ivy.base.refreshWidget
import com.ivy.base.time.TimeProvider
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Expense
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.Transaction
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.AssociationId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.TagRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.domain.usecase.creditcard.CreditCardChargeUseCase
import com.ivy.domain.usecase.exchange.ExchangeUseCase
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.Navigation
import com.ivy.ui.R
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.logic.SmartTitleSuggestionsLogic
import com.ivy.wallet.utils.computationThread
import com.ivy.wallet.utils.ioThread
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class EditTransactionViewModelTest {

    private val bank = account("Bank", Eur)
    private val wallet = account("Wallet", Eur)
    private val dollars = account("Dollars", Usd)
    private val food = Category(
        id = CategoryId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe("Food"),
        color = ColorInt(0),
        icon = null,
        orderNum = 0.0,
    )

    private val accountRepository = mockk<AccountRepository> {
        listOf(bank, wallet, dollars).forEach { coEvery { findById(it.id) } returns it }
    }
    private val transactionRepo = mockk<TransactionRepository>(relaxUnitFun = true)
    private val nav = mockk<Navigation>(relaxed = true)
    private val toaster = mockk<Toaster>(relaxed = true)

    private lateinit var viewModel: EditTransactionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // The view model's background work runs where it's started, so a test sees it done.
        mockkStatic("com.ivy.wallet.utils.MVVMExtKt")
        coEvery { ioThread<Any?>(any()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
        coEvery { computationThread<Any?>(any()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
        // No home screen widgets to refresh.
        mockkStatic("com.ivy.base.WidgetRefreshKt")
        every { any<Context>().refreshWidget(any()) } just Runs

        val accountsAct = mockk<AccountsAct>()
        coEvery { accountsAct() } returns persistentListOf(bank, wallet, dollars)
        val timeConverter = TestTimeConverter()
        viewModel = EditTransactionViewModel(
            context = mockk(relaxed = true),
            toaster = toaster,
            loanDao = mockk(relaxed = true),
            currencyRepository = mockk<CurrencyRepository> { coEvery { getBaseCurrency() } returns Eur },
            nav = nav,
            appPreferences = mockk<AppPreferences>(relaxed = true) {
                coEvery { lastSelectedAccountId() } returns null
            },
            exchangeUseCase = mockk<ExchangeUseCase> {
                // 1 EUR = 2 USD
                coEvery { convert(any<PositiveValue>(), Usd) } answers { firstArg<PositiveValue>().to(Usd, 2.0) }
                coEvery { convert(any<PositiveValue>(), Eur) } answers { firstArg<PositiveValue>().to(Eur, 0.5) }
            },
            categoryCreator = mockk(relaxed = true),
            accountCreator = mockk(relaxed = true),
            dueTransactionsUseCase = mockk(relaxed = true),
            smartTitleSuggestionsLogic = mockk<SmartTitleSuggestionsLogic> {
                coEvery { suggest(any(), any(), any()) } returns emptySet()
            },
            loanTransactionsUseCase = mockk(relaxed = true),
            accountsAct = accountsAct,
            categoryRepository = mockk<CategoryRepository> {
                coEvery { findAll() } returns listOf(food)
                coEvery { findById(food.id) } returns food
            },
            accountRepository = accountRepository,
            transactionRepo = transactionRepo,
            transactionMapper = TransactionMapper(accountRepository),
            tagRepository = mockk<TagRepository> {
                coEvery { findAll() } returns emptyList()
                coEvery { findByAssociatedId(any<AssociationId>()) } returns emptyList()
            },
            tagMapper = mockk(relaxed = true),
            features = mockk(relaxed = true),
            timeConverter = timeConverter,
            timeProvider = FixedTime,
            dateTimePicker = mockk(relaxed = true),
            creditCardChargeUseCase = CreditCardChargeUseCase(accountRepository, timeConverter),
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `a new expense saves what was entered, then closes`() {
        viewModel.start(newTransaction(TransactionType.EXPENSE, wallet))
        viewModel.onEvent(EditTransactionViewEvent.OnAmountChanged(25.0))
        viewModel.onEvent(EditTransactionViewEvent.OnCategoryChanged(food))
        viewModel.onEvent(EditTransactionViewEvent.OnTitleChanged("Lunch "))
        viewModel.onEvent(EditTransactionViewEvent.Save(closeScreen = true))

        val expense = saved().shouldBeInstanceOf<Expense>()
        expense.account shouldBe wallet.id
        expense.value shouldBe PositiveValue(PositiveDouble.unsafe(25.0), Eur)
        expense.category shouldBe food.id
        expense.title?.value shouldBe "Lunch"
        expense.time shouldBe FixedTime.utcNow()
        expense.settled shouldBe true
        verify { nav.back() }
    }

    @Test
    fun `a transfer to another currency receives the converted amount`() {
        viewModel.start(newTransaction(TransactionType.TRANSFER, bank))
        viewModel.onEvent(EditTransactionViewEvent.OnToAccountChanged(dollars))
        viewModel.onEvent(EditTransactionViewEvent.OnAmountChanged(100.0))
        viewModel.onEvent(EditTransactionViewEvent.Save(closeScreen = true))

        val transfer = saved().shouldBeInstanceOf<Transfer>()
        transfer.fromAccount shouldBe bank.id
        transfer.fromValue shouldBe PositiveValue(PositiveDouble.unsafe(100.0), Eur)
        transfer.toAccount shouldBe dollars.id
        transfer.toValue shouldBe PositiveValue(PositiveDouble.unsafe(200.0), Usd)
    }

    @Test
    fun `a transfer to the same account isn't saved, and says why`() {
        viewModel.start(newTransaction(TransactionType.TRANSFER, bank))
        viewModel.onEvent(EditTransactionViewEvent.OnToAccountChanged(bank))
        viewModel.onEvent(EditTransactionViewEvent.OnAmountChanged(100.0))
        viewModel.onEvent(EditTransactionViewEvent.Save(closeScreen = true))

        coVerify { toaster.show(R.string.msg_source_account_destination_account_same_for_transfer, any()) }
        coVerify(exactly = 0) { transactionRepo.save(any()) }
    }

    @Test
    fun `a transfer without a destination isn't saved, and says why`() {
        viewModel.start(newTransaction(TransactionType.TRANSFER, bank))
        viewModel.onEvent(EditTransactionViewEvent.OnAmountChanged(100.0))
        viewModel.onEvent(EditTransactionViewEvent.Save(closeScreen = true))

        coVerify { toaster.show(R.string.msg_select_account_to_transfer, any()) }
        coVerify(exactly = 0) { transactionRepo.save(any()) }
    }

    @Test
    fun `nothing is saved without an amount`() {
        viewModel.start(newTransaction(TransactionType.EXPENSE, wallet))
        viewModel.onEvent(EditTransactionViewEvent.Save(closeScreen = true))

        coVerify(exactly = 0) { transactionRepo.save(any()) }
        verify(exactly = 0) { nav.back() }
    }

    @Test
    fun `an edited transaction is saved over the original as it changes`() {
        val original = Expense(
            id = TransactionId(UUID.randomUUID()),
            title = NotBlankTrimmedString.unsafe("Lunch"),
            description = null,
            category = food.id,
            time = Instant.parse("2026-11-10T12:00:00Z"),
            settled = true,
            metadata = TransactionMetadata(
                recurringRuleId = null,
                paidForDateTime = null,
                loanRecordId = null,
                purchaseDateTime = null,
            ),
            tags = emptyList(),
            value = PositiveValue(PositiveDouble.unsafe(25.0), Eur),
            account = wallet.id,
        )
        coEvery { transactionRepo.findById(original.id) } returns original

        viewModel.start(EditTransactionScreen(initialTransactionId = original.id.value, type = TransactionType.EXPENSE))
        viewModel.onEvent(EditTransactionViewEvent.OnAmountChanged(30.0))

        saved() shouldBe original.copy(value = PositiveValue(PositiveDouble.unsafe(30.0), Eur))
        // Still open: each change is saved as it's made.
        verify(exactly = 0) { nav.back() }
    }

    private fun newTransaction(type: TransactionType, account: Account) = EditTransactionScreen(
        initialTransactionId = null,
        type = type,
        accountId = account.id.value,
    )

    private fun PositiveValue.to(currency: AssetCode, rate: Double) = Some(
        PositiveValue(PositiveDouble.unsafe(amount.value * if (asset == currency) 1.0 else rate), currency)
    )

    private fun saved(): Transaction {
        val transaction = slot<Transaction>()
        coVerify { transactionRepo.save(capture(transaction)) }
        return transaction.captured
    }

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
        val Usd = AssetCode.unsafe("USD")

        /** November 15, 2026, noon, in UTC. */
        val FixedTime = object : TimeProvider {
            private val now = LocalDateTime.of(2026, 11, 15, 12, 0)
            override fun getZoneId(): ZoneId = ZoneId.of("UTC")
            override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
            override fun localNow(): LocalDateTime = now
            override fun localDateNow(): LocalDate = now.toLocalDate()
            override fun localTimeNow(): LocalTime = now.toLocalTime()
        }

        fun account(name: String, asset: AssetCode) = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            asset = asset,
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            type = AccountType.BANK,
            archived = false,
            creditLimit = null,
            chargeDay = null,
            statementClosingDay = null,
            orderNum = 0.0,
        )
    }
}
