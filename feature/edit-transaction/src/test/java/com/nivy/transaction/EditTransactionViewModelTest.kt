package com.nivy.transaction

import android.content.Context
import arrow.core.Some
import com.nivy.base.Toaster
import com.nivy.base.model.AccountType
import com.nivy.base.model.TransactionType
import com.nivy.base.refreshWidget
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.CreditCard
import com.nivy.data.model.CreditCardId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.AssociationId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.TagRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.data.repository.mapper.TransactionMapper
import com.nivy.domain.features.Features
import com.nivy.domain.usecase.creditcard.CreditCardChargeUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.navigation.EditTransactionScreen
import com.nivy.navigation.Navigation
import com.nivy.ui.R
import com.nivy.wallet.domain.logic.SmartTitleSuggestionsLogic
import com.nivy.wallet.utils.computationThread
import com.nivy.wallet.utils.ioThread
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
    private val amexAccount = account("Amex", Eur).copy(type = AccountType.CREDIT_CARD)
    private val visa = card("Visa", bank)
    private val amex = card("Amex", amexAccount)
    private val food = Category(
        id = CategoryId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe("Food"),
        color = ColorInt(0),
        icon = null,
        orderNum = 0.0,
    )

    private val accountRepository = mockk<AccountRepository> {
        listOf(bank, wallet, dollars, amexAccount).forEach { coEvery { findById(it.id) } returns it }
    }
    private val creditCardRepository = mockk<CreditCardRepository> {
        coEvery { findAll() } returns listOf(amex, visa)
        listOf(visa, amex).forEach { coEvery { findById(it.id) } returns it }
    }
    private val transactionRepo = mockk<TransactionRepository>(relaxUnitFun = true)
    private val nav = mockk<Navigation>(relaxed = true)
    private val toaster = mockk<Toaster>(relaxed = true)

    private lateinit var viewModel: EditTransactionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // The view model's background work runs where it's started, so a test sees it done.
        mockkStatic("com.nivy.wallet.utils.MVVMExtKt")
        coEvery { ioThread<Any?>(any()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
        coEvery { computationThread<Any?>(any()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
        // No home screen widgets to refresh.
        mockkStatic("com.nivy.base.WidgetRefreshKt")
        every { any<Context>().refreshWidget(any()) } just Runs

        coEvery { accountRepository.findAll() } returns persistentListOf(bank, wallet, dollars, amexAccount)
        val timeConverter = TestTimeConverter()
        val context = mockk<Context>(relaxed = true)
        val categoryRepository = mockk<CategoryRepository> {
            coEvery { findAll() } returns listOf(food)
            coEvery { findById(food.id) } returns food
        }
        val features = mockk<Features>(relaxed = true)
        viewModel = EditTransactionViewModel(
            transactionLoanDisplay = TransactionLoanDisplay(
                context = context,
                loanDao = mockk(relaxed = true),
            ),
            transferExchangeRate = TransferExchangeRate(
                currencyRepository = mockk<CurrencyRepository> { coEvery { getBaseCurrency() } returns Eur },
                exchangeUseCase = mockk<ExchangeUseCase> {
                    // 1 EUR = 2 USD
                    coEvery { convert(any<PositiveValue>(), Usd) } answers { firstArg<PositiveValue>().to(Usd, 2.0) }
                    coEvery { convert(any<PositiveValue>(), Eur) } answers { firstArg<PositiveValue>().to(Eur, 0.5) }
                },
            ),
            transactionSaving = TransactionSaving(
                context = context,
                toaster = toaster,
                transactionRepo = transactionRepo,
                transactionMapper = TransactionMapper(accountRepository),
                loanTransactionsUseCase = mockk(relaxed = true),
                creditCardChargeUseCase = CreditCardChargeUseCase(creditCardRepository, timeConverter),
                dueTransactionsUseCase = mockk(relaxed = true),
                timeProvider = FixedTime,
            ),
            nav = nav,
            appPreferences = mockk<AppPreferences>(relaxed = true) {
                coEvery { lastSelectedAccountId() } returns null
            },
            categoryCreator = mockk(relaxed = true),
            accountCreator = mockk(relaxed = true),
            smartTitleSuggestionsLogic = mockk<SmartTitleSuggestionsLogic> {
                coEvery { suggest(any(), any(), any()) } returns emptySet()
            },
            sortedCategories = SortedCategories(categoryRepository, features, context),
            categoryRepository = categoryRepository,
            accountRepository = accountRepository,
            transactionRepo = transactionRepo,
            transactionTags = TransactionTags(
                tagRepository = mockk<TagRepository> {
                    coEvery { findAll() } returns emptyList()
                    coEvery { findByAssociatedId(any<AssociationId>()) } returns emptyList()
                },
                tagMapper = mockk(relaxed = true),
            ),
            features = features,
            timeConverter = timeConverter,
            timeProvider = FixedTime,
            transactionDateTimePicker = TransactionDateTimePicker(
                dateTimePicker = mockk(relaxed = true),
                timeConverter = timeConverter,
                timeProvider = FixedTime,
            ),
            creditCardRepository = creditCardRepository,
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
        val original = lunch()
        coEvery { transactionRepo.findById(original.id) } returns original

        viewModel.start(EditTransactionScreen(initialTransactionId = original.id.value, type = TransactionType.EXPENSE))
        viewModel.onEvent(EditTransactionViewEvent.OnAmountChanged(30.0))

        saved() shouldBe original.copy(value = PositiveValue(PositiveDouble.unsafe(30.0), Eur))
        // Still open: each change is saved as it's made.
        verify(exactly = 0) { nav.back() }
    }

    @Test
    fun `a save that fails says so, and the screen stays open with what was entered`() {
        coEvery { transactionRepo.save(any()) } throws IllegalStateException("disk full")

        viewModel.start(newTransaction(TransactionType.EXPENSE, wallet))
        viewModel.onEvent(EditTransactionViewEvent.OnAmountChanged(25.0))
        viewModel.onEvent(EditTransactionViewEvent.Save(closeScreen = true))

        coVerify { toaster.show(R.string.msg_transaction_not_saved, any()) }
        verify(exactly = 0) { nav.back() }
    }

    @Test
    fun `a transaction that isn't a valid one any more isn't closed as if saved`() {
        viewModel.start(newTransaction(TransactionType.EXPENSE, wallet))
        viewModel.onEvent(EditTransactionViewEvent.OnAmountChanged(25.0))
        // Its account is gone, deleted meanwhile.
        coEvery { accountRepository.findById(wallet.id) } returns null
        viewModel.onEvent(EditTransactionViewEvent.Save(closeScreen = true))

        coVerify(exactly = 0) { transactionRepo.save(any()) }
        coVerify { toaster.show(R.string.msg_transaction_not_saved, any()) }
        verify(exactly = 0) { nav.back() }
    }

    @Test
    fun `a duplicate that fails says so, and the screen stays on the original`() {
        val original = lunch()
        coEvery { transactionRepo.findById(original.id) } returns original
        coEvery { transactionRepo.save(any()) } throws IllegalStateException("disk full")

        viewModel.start(EditTransactionScreen(initialTransactionId = original.id.value, type = TransactionType.EXPENSE))
        viewModel.onEvent(EditTransactionViewEvent.Duplicate)

        coVerify { toaster.show(R.string.msg_transaction_not_saved, any()) }
        verify(exactly = 0) { nav.back() }
    }

    @Test
    fun `an expense paid with a card counts on the day the card charges it`() {
        viewModel.start(newTransaction(TransactionType.EXPENSE, bank))
        viewModel.onEvent(EditTransactionViewEvent.OnCreditCardChanged(visa))
        viewModel.onEvent(EditTransactionViewEvent.OnAmountChanged(25.0))
        viewModel.onEvent(EditTransactionViewEvent.Save(closeScreen = true))

        val expense = saved().shouldBeInstanceOf<Expense>()
        expense.account shouldBe bank.id
        expense.metadata.creditCard shouldBe visa.id
        expense.metadata.purchaseDateTime shouldBe FixedTime.utcNow()
        // Bought on November 15th, in the statement that closes on the 30th, charged on December 15th.
        expense.time shouldBe Instant.parse("2026-12-15T12:00:00Z")
    }

    @Test
    fun `an expense on an account with a card is paid from the account unless the card is chosen`() {
        viewModel.start(newTransaction(TransactionType.EXPENSE, bank))
        viewModel.onEvent(EditTransactionViewEvent.OnAmountChanged(25.0))
        viewModel.onEvent(EditTransactionViewEvent.Save(closeScreen = true))

        val expense = saved().shouldBeInstanceOf<Expense>()
        expense.metadata.creditCard shouldBe null
        expense.time shouldBe FixedTime.utcNow()
    }

    @Test
    fun `a new expense on a credit card account is paid with its card`() {
        viewModel.start(newTransaction(TransactionType.EXPENSE, amexAccount))
        viewModel.onEvent(EditTransactionViewEvent.OnAmountChanged(25.0))
        viewModel.onEvent(EditTransactionViewEvent.Save(closeScreen = true))

        saved().metadata.creditCard shouldBe amex.id
    }

    @Test
    fun `another account doesn't take the card of the one it replaces`() {
        viewModel.start(newTransaction(TransactionType.EXPENSE, bank))
        viewModel.onEvent(EditTransactionViewEvent.OnCreditCardChanged(visa))
        viewModel.onEvent(EditTransactionViewEvent.OnAccountChanged(wallet))
        viewModel.onEvent(EditTransactionViewEvent.OnAmountChanged(25.0))
        viewModel.onEvent(EditTransactionViewEvent.Save(closeScreen = true))

        val expense = saved().shouldBeInstanceOf<Expense>()
        expense.account shouldBe wallet.id
        expense.metadata.creditCard shouldBe null
        expense.time shouldBe FixedTime.utcNow()
    }

    @Test
    fun `choosing the same account or type again keeps the card`() {
        viewModel.start(newTransaction(TransactionType.EXPENSE, bank))
        viewModel.onEvent(EditTransactionViewEvent.OnCreditCardChanged(visa))
        viewModel.onEvent(EditTransactionViewEvent.OnAccountChanged(bank))
        viewModel.onEvent(EditTransactionViewEvent.OnSetTransactionType(TransactionType.EXPENSE))
        viewModel.onEvent(EditTransactionViewEvent.OnAmountChanged(25.0))
        viewModel.onEvent(EditTransactionViewEvent.Save(closeScreen = true))

        saved().metadata.creditCard shouldBe visa.id
    }

    @Test
    fun `income isn't paid with a card`() {
        viewModel.start(newTransaction(TransactionType.EXPENSE, amexAccount))
        viewModel.onEvent(EditTransactionViewEvent.OnSetTransactionType(TransactionType.INCOME))
        viewModel.onEvent(EditTransactionViewEvent.OnAmountChanged(25.0))
        viewModel.onEvent(EditTransactionViewEvent.Save(closeScreen = true))

        val income = saved().shouldBeInstanceOf<Income>()
        income.metadata.creditCard shouldBe null
        income.time shouldBe FixedTime.utcNow()
    }

    private fun newTransaction(type: TransactionType, account: Account) = EditTransactionScreen(
        initialTransactionId = null,
        type = type,
        accountId = account.id.value,
    )

    private fun PositiveValue.to(currency: AssetCode, rate: Double) = Some(
        PositiveValue(PositiveDouble.unsafe(amount.value * if (asset == currency) 1.0 else rate), currency)
    )

    private fun lunch() = Expense(
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

        fun card(name: String, account: Account) = CreditCard(
            id = CreditCardId(UUID.randomUUID()),
            account = account.id,
            name = NotBlankTrimmedString.unsafe(name),
            chargeDay = 15,
            statementClosingDay = null,
            creditLimit = null,
        )

        fun account(name: String, asset: AssetCode) = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            asset = asset,
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            type = AccountType.BANK,
            archived = false,
            orderNum = 0.0,
        )
    }
}
