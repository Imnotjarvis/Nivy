package com.nivy.quickadd.edit

import com.nivy.base.model.AccountType
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.CreditCard
import com.nivy.data.model.CreditCardId
import com.nivy.data.model.QuickAdd
import com.nivy.data.model.QuickAddId
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.QuickAddRepository
import com.nivy.domain.UserWrites
import com.nivy.navigation.EditQuickAddScreen
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
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class EditQuickAddViewModelTest {

    private val bank = account("Bank", AccountType.BANK)
    private val savings = account("Savings", AccountType.SAVINGS)
    private val visaCard = CreditCard(
        id = CreditCardId(UUID.randomUUID()),
        account = bank.id,
        name = NotBlankTrimmedString.unsafe("Visa"),
        chargeDay = 5,
        statementClosingDay = null,
        creditLimit = null,
    )
    private val transport = Category(
        id = CategoryId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe("Transport"),
        color = ColorInt(0),
        icon = null,
        orderNum = 0.0,
    )
    private val coffee = QuickAdd(
        id = QuickAddId(UUID.randomUUID()),
        type = TransactionType.EXPENSE,
        account = bank.id,
        toAccount = null,
        amount = PositiveDouble.unsafe(1.3),
        category = null,
        title = NotBlankTrimmedString.unsafe("Coffee"),
        creditCard = null,
        orderNum = 4.0,
    )

    private val saved = mutableListOf<QuickAdd>()
    private val quickAddRepository = mockk<QuickAddRepository>(relaxUnitFun = true) {
        coEvery { findAll() } returns listOf(coffee)
        coEvery { findById(any()) } returns null
        coEvery { findById(coffee.id) } returns coffee
        coEvery { save(any()) } answers { saved += firstArg<QuickAdd>() }
    }
    private val nav = mockk<Navigation>(relaxed = true)

    private val viewModel = EditQuickAddViewModel(
        accountRepository = mockk<AccountRepository> {
            coEvery { findAll() } returns listOf(bank, savings)
            coEvery { findById(bank.id) } returns bank
        },
        categoryRepository = mockk<CategoryRepository> {
            coEvery { findAll() } returns listOf(transport)
            coEvery { findById(transport.id) } returns transport
        },
        creditCardRepository = mockk<CreditCardRepository> { coEvery { findAll() } returns listOf(visaCard) },
        quickAddRepository = quickAddRepository,
        categoryCreator = mockk(relaxed = true),
        accountCreator = mockk(relaxed = true),
        nav = nav,
        userWrites = UserWrites(mockk(relaxed = true)),
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

    private fun newQuickAdd(type: TransactionType = TransactionType.EXPENSE, amount: Double? = 2.2) =
        viewModel.start(
            EditQuickAddScreen(
                quickAddId = null,
                type = type,
                amount = amount,
                accountId = bank.id.value,
                categoryId = transport.id.value,
                title = "Metro",
            )
        )

    @Test
    fun `a new quick add is saved as filled in, after the others, and the editor closes`() {
        // given
        newQuickAdd()

        // when
        viewModel.runTest(events = listOf(EditQuickAddEvent.OnSave)) {}

        // then
        val quickAdd = saved.single()
        quickAdd.type shouldBe TransactionType.EXPENSE
        quickAdd.account shouldBe bank.id
        quickAdd.toAccount shouldBe null
        quickAdd.amount shouldBe PositiveDouble.unsafe(2.2)
        quickAdd.category shouldBe transport.id
        quickAdd.title shouldBe NotBlankTrimmedString.unsafe("Metro")
        quickAdd.orderNum shouldBe 5.0
        coVerifyOrder {
            quickAddRepository.save(quickAdd)
            nav.back()
        }
    }

    @Test
    fun `without an amount, it's entered each time`() {
        // given
        newQuickAdd(amount = null)

        // when
        viewModel.runTest(events = listOf(EditQuickAddEvent.OnSave)) {
            amount shouldBe 0.0
        }

        // then
        saved.single().amount shouldBe null
    }

    @Test
    fun `an amount set back to nothing is entered each time`() {
        // given
        newQuickAdd()

        // when
        viewModel.runTest(events = listOf(EditQuickAddEvent.OnAmountChanged(0.0), EditQuickAddEvent.OnSave)) {}

        // then
        saved.single().amount shouldBe null
    }

    @Test
    fun `a transfer goes to an account other than its own`() {
        // given
        newQuickAdd(type = TransactionType.TRANSFER)

        // then: none to go to, then its own, then another one
        viewModel.runTest { canSave shouldBe false }
        viewModel.runTest(events = listOf(EditQuickAddEvent.OnToAccountChanged(bank))) { canSave shouldBe false }
        viewModel.runTest(events = listOf(EditQuickAddEvent.OnToAccountChanged(savings), EditQuickAddEvent.OnSave)) {
            canSave shouldBe true
        }
        saved.single().toAccount shouldBe savings.id
    }

    @Test
    fun `filled in from a transaction paid with a card, it's paid with it too`() {
        // when
        viewModel.start(
            EditQuickAddScreen(
                quickAddId = null,
                type = TransactionType.EXPENSE,
                amount = 2.2,
                accountId = bank.id.value,
                creditCardId = visaCard.id.value,
            )
        )
        viewModel.runTest(events = listOf(EditQuickAddEvent.OnSave)) {
            creditCard shouldBe visaCard
        }

        // then
        saved.single().creditCard shouldBe visaCard.id
    }

    @Test
    fun `one already saved is saved on every change, and the editor stays open`() {
        // given
        viewModel.start(EditQuickAddScreen(quickAddId = coffee.id.value, type = TransactionType.EXPENSE))

        // when
        viewModel.runTest(events = listOf(EditQuickAddEvent.OnTitleChanged("Espresso"))) {
            initialTitle shouldBe "Coffee"
        }

        // then
        saved.single() shouldBe coffee.copy(title = NotBlankTrimmedString.unsafe("Espresso"))
        verify(exactly = 0) { nav.back() }
    }

    @Test
    fun `deleting it deletes it and closes the editor`() {
        // given
        viewModel.start(EditQuickAddScreen(quickAddId = coffee.id.value, type = TransactionType.EXPENSE))

        // when
        viewModel.runTest(events = listOf(EditQuickAddEvent.OnDelete)) {}

        // then
        coVerifyOrder {
            quickAddRepository.deleteById(coffee.id)
            nav.back()
        }
        coVerify(exactly = 0) { quickAddRepository.save(any()) }
    }

    private fun account(name: String, type: AccountType) = Account(
        id = AccountId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe(name),
        asset = AssetCode.unsafe("EUR"),
        color = ColorInt(0),
        icon = null,
        includeInBalance = true,
        type = type,
        archived = false,
        orderNum = 0.0,
    )
}
