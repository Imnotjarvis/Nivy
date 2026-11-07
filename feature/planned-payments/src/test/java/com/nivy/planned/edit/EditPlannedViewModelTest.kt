package com.nivy.planned.edit

import com.nivy.base.model.AccountType
import com.nivy.base.model.IntervalType
import com.nivy.base.model.TransactionType
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.CreditCard
import com.nivy.data.model.CreditCardId
import com.nivy.data.model.PlannedPaymentInterval
import com.nivy.data.model.PlannedPaymentRule
import com.nivy.data.model.PlannedPaymentRuleId
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.model.primitive.PositiveInt
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.PlannedPaymentRuleRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.planned.PlannedPaymentsGenerator
import com.nivy.navigation.EditPlannedScreen
import com.nivy.navigation.Navigation
import com.nivy.ui.testing.runTest
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class EditPlannedViewModelTest {

    private val bank = account("Bank", AccountType.BANK)
    private val visa = account("Visa", AccountType.CREDIT_CARD)
    private val closed = account("Closed", AccountType.CASH, archived = true)
    private val visaCard = CreditCard(
        id = CreditCardId(UUID.randomUUID()),
        account = visa.id,
        name = NotBlankTrimmedString.unsafe("Visa"),
        chargeDay = 5,
        statementClosingDay = null,
        creditLimit = null,
    )
    private val rent = Category(
        id = CategoryId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe("Rent"),
        color = ColorInt(0),
        icon = null,
        orderNum = 0.0,
    )
    private val gym = PlannedPaymentRule(
        id = PlannedPaymentRuleId(UUID.randomUUID()),
        type = TransactionType.EXPENSE,
        account = visa.id,
        amount = PositiveDouble.unsafe(30.0),
        category = null,
        title = NotBlankTrimmedString.unsafe("Gym"),
        description = null,
        startDate = Instant.parse("2026-11-02T08:00:00Z"),
        interval = PlannedPaymentInterval(PositiveInt.unsafe(2), IntervalType.WEEK),
        creditCard = visaCard.id,
    )

    private var accounts = listOf(bank, visa, closed)
    private val saved = mutableListOf<PlannedPaymentRule>()
    private val ruleRepository = mockk<PlannedPaymentRuleRepository>(relaxUnitFun = true) {
        coEvery { findById(any()) } returns null
        coEvery { findById(gym.id) } returns gym
        coEvery { save(capture(saved)) } returns Unit
    }
    private val generator = mockk<PlannedPaymentsGenerator>(relaxUnitFun = true)
    private val transactionRepository = mockk<TransactionRepository>(relaxUnitFun = true)
    private val nav = mockk<Navigation>(relaxed = true)

    private val viewModel = EditPlannedViewModel(
        accountRepository = mockk<AccountRepository> {
            coEvery { findAll() } answers { accounts }
            coEvery { findById(visa.id) } returns visa
        },
        categoryRepository = mockk<CategoryRepository> {
            coEvery { findAll() } returns listOf(rent)
            coEvery { findById(rent.id) } returns rent
        },
        nav = nav,
        plannedPaymentRuleRepository = ruleRepository,
        plannedPaymentsGenerator = generator,
        categoryCreator = mockk(relaxed = true),
        accountCreator = mockk(relaxed = true),
        transactionRepository = transactionRepository,
        timeConverter = TestTimeConverter(),
        creditCardRepository = mockk<CreditCardRepository> { coEvery { findAll() } returns listOf(visaCard) },
    )

    private val firstOfDecember = LocalDateTime.of(2026, 12, 1, 9, 0)
    private val monthly = EditPlannedScreenEvent.OnRuleChanged(
        startDate = firstOfDecember,
        oneTime = false,
        intervalN = 1,
        intervalType = IntervalType.MONTH,
    )

    @Before
    fun setUp() {
        // start() runs in viewModelScope before runTest() sets its own main dispatcher.
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newPlan(type: TransactionType = TransactionType.EXPENSE, account: Account = bank) =
        viewModel.start(
            EditPlannedScreen(
                plannedPaymentRuleId = null,
                type = type,
                amount = 50.0,
                accountId = account.id.value,
                title = "Rent",
            )
        )

    @Test
    fun `a new plan starts on its account, in its currency, without archived accounts to pick`() {
        // when
        newPlan()

        // then
        viewModel.runTest {
            account shouldBe bank
            currency shouldBe "EUR"
            amount shouldBe 50.0
            initialTitle shouldBe "Rent"
            startDate shouldBe null
            accounts shouldBe listOf(bank, visa)
        }
    }

    @Test
    fun `saving a monthly plan saves its rule, generates its payments and closes the editor`() {
        // given
        newPlan()

        // when
        viewModel.runTest(
            events = listOf(monthly, EditPlannedScreenEvent.OnCategoryChanged(rent), EditPlannedScreenEvent.OnSave())
        ) {}

        // then
        val rule = saved.single()
        rule.type shouldBe TransactionType.EXPENSE
        rule.account shouldBe bank.id
        rule.amount shouldBe PositiveDouble.unsafe(50.0)
        rule.category shouldBe rent.id
        rule.title shouldBe NotBlankTrimmedString.unsafe("Rent")
        rule.startDate shouldBe Instant.parse("2026-12-01T09:00:00Z")
        rule.interval shouldBe PlannedPaymentInterval(PositiveInt.unsafe(1), IntervalType.MONTH)
        coVerifyOrder {
            ruleRepository.save(rule)
            generator.generate(rule, previous = null)
            nav.back()
        }
    }

    @Test
    fun `a one-time plan has no interval`() {
        // given
        newPlan()
        val once = EditPlannedScreenEvent.OnRuleChanged(firstOfDecember, oneTime = true, null, null)

        // when
        viewModel.runTest(events = listOf(once, EditPlannedScreenEvent.OnSave())) {}

        // then
        saved.single().interval shouldBe null
    }

    @Test
    fun `an incomplete plan isn't saved, and the editor stays open`() {
        // given
        newPlan()
        val noInterval = EditPlannedScreenEvent.OnRuleChanged(firstOfDecember, oneTime = false, null, null)

        // when
        viewModel.runTest(events = listOf(EditPlannedScreenEvent.OnSave())) {}
        viewModel.runTest(events = listOf(noInterval, EditPlannedScreenEvent.OnSave())) {}
        viewModel.runTest(
            events = listOf(monthly, EditPlannedScreenEvent.OnAmountChanged(0.0), EditPlannedScreenEvent.OnSave())
        ) {}

        // then
        saved shouldBe emptyList()
        verify(exactly = 0) { nav.back() }
    }

    @Test
    fun `a transfer can't be planned`() {
        // given
        newPlan(type = TransactionType.TRANSFER)

        // when
        viewModel.runTest(events = listOf(monthly, EditPlannedScreenEvent.OnSave())) {}

        // then
        saved shouldBe emptyList()
    }

    @Test
    fun `an expense on a credit card account is paid with its card, an income with none`() {
        // given
        newPlan(account = visa)

        viewModel.runTest {
            creditCard shouldBe visaCard
            creditCards shouldBe listOf(visaCard)
        }
        // when
        viewModel.runTest(events = listOf(EditPlannedScreenEvent.OnSetTransactionType(TransactionType.INCOME))) {
            // then
            creditCard shouldBe null
            creditCards shouldBe emptyList()
        }
    }

    @Test
    fun `moving a plan to another account takes that account's card`() {
        // given
        newPlan(account = bank)

        // when
        viewModel.runTest(events = listOf(EditPlannedScreenEvent.OnAccountChanged(visa))) {
            // then
            creditCard shouldBe visaCard
        }
    }

    @Test
    fun `an existing plan shows its rule, and each change saves it without closing the editor`() {
        // given
        viewModel.start(EditPlannedScreen(plannedPaymentRuleId = gym.id.value, type = TransactionType.EXPENSE))

        viewModel.runTest {
            account shouldBe visa
            creditCard shouldBe visaCard
            amount shouldBe 30.0
            oneTime shouldBe false
            intervalN shouldBe 2
            intervalType shouldBe IntervalType.WEEK
            startDate shouldBe LocalDateTime.of(2026, 11, 2, 8, 0)
        }

        // when
        viewModel.runTest(events = listOf(EditPlannedScreenEvent.OnAmountChanged(45.0))) {}

        // then
        val rule = saved.single()
        rule.id shouldBe gym.id
        rule.amount shouldBe PositiveDouble.unsafe(45.0)
        rule.interval shouldBe gym.interval
        // The rule as it was tells which of its dates were skipped.
        coVerify(exactly = 1) { generator.generate(rule, previous = gym) }
        verify(exactly = 0) { nav.back() }
    }

    @Test
    fun `deleting a plan deletes its unpaid payments too, and closes the editor`() {
        // given
        viewModel.start(EditPlannedScreen(plannedPaymentRuleId = gym.id.value, type = TransactionType.EXPENSE))

        // when
        viewModel.runTest(events = listOf(EditPlannedScreenEvent.OnDelete)) {}

        // then
        coVerifyOrder {
            ruleRepository.deleteById(gym.id)
            transactionRepository.deletedByRecurringRuleIdAndNoDateTime(gym.id.value)
            nav.back()
        }
    }

    @Test
    fun `without an account there's nothing to plan, so the editor closes`() {
        // given
        accounts = emptyList()

        // when
        newPlan()

        // then
        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        fun account(name: String, type: AccountType, archived: Boolean = false) = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            asset = AssetCode.unsafe("EUR"),
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            type = type,
            archived = archived,
            orderNum = 0.0,
        )
    }
}
