package com.nivy.main

import com.nivy.base.model.TransactionType
import com.nivy.data.model.AccountId
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.QuickAdd
import com.nivy.data.model.QuickAddId
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.domain.UserWrites
import com.nivy.domain.usecase.quickadd.QuickAddUseCase
import com.nivy.navigation.EditTransactionScreen
import com.nivy.navigation.Navigation
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val quickAddUseCase = mockk<QuickAddUseCase>(relaxUnitFun = true)
    private val nav = mockk<Navigation>(relaxed = true)

    private val viewModel = MainViewModel(
        mainScreenState = mockk(relaxed = true),
        accountCreator = mockk(relaxed = true),
        currencyRepository = mockk(relaxed = true),
        accountRepository = mockk(relaxed = true),
        categoryRepository = mockk(relaxed = true),
        quickAddRepository = mockk(relaxed = true),
        quickAddUseCase = quickAddUseCase,
        userWrites = UserWrites(mockk(relaxed = true)),
        nav = nav,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a quick add with an amount is added at once, and can be taken back`() = runTest(dispatcher) {
        // given
        coEvery { quickAddUseCase.add(Metro, null) } returns MetroAdded

        // when
        viewModel.quickAdd(Metro)
        runCurrent()

        // then
        viewModel.amountAsked shouldBe null
        viewModel.added shouldBe QuickAdded(MetroAdded.id, Metro, MetroAdded.value)

        // when
        viewModel.undo()
        runCurrent()

        // then
        coVerify { quickAddUseCase.undo(MetroAdded.id) }
        viewModel.added shouldBe null
    }

    @Test
    fun `one without an amount asks for it, then is added with it`() = runTest(dispatcher) {
        // given
        val lunch = Metro.copy(title = NotBlankTrimmedString.unsafe("Lunch"), amount = null)
        coEvery { quickAddUseCase.add(lunch, PositiveDouble.unsafe(11.8)) } returns MetroAdded

        // when
        viewModel.quickAdd(lunch)
        runCurrent()

        // then
        viewModel.amountAsked shouldBe lunch
        coVerify(exactly = 0) { quickAddUseCase.add(lunch, null) }

        // when
        viewModel.enterAmount(11.8)
        runCurrent()

        // then
        viewModel.amountAsked shouldBe null
        coVerify { quickAddUseCase.add(lunch, PositiveDouble.unsafe(11.8)) }
    }

    @Test
    fun `one the use case can't add opens the editor filled in with it`() = runTest(dispatcher) {
        // given: a transfer between currencies without a rate
        coEvery { quickAddUseCase.add(Metro, null) } returns null

        // when
        viewModel.quickAdd(Metro)
        runCurrent()

        // then
        viewModel.added shouldBe null
        verify {
            nav.navigateTo(
                EditTransactionScreen(
                    initialTransactionId = null,
                    type = TransactionType.EXPENSE,
                    accountId = Metro.account.value,
                    categoryId = Metro.category?.value,
                    title = "Metro",
                    amount = 2.2,
                )
            )
        }
    }

    @Test
    fun `Undo goes away after five seconds`() = runTest(dispatcher) {
        // given
        coEvery { quickAddUseCase.add(Metro, null) } returns MetroAdded
        viewModel.quickAdd(Metro)
        runCurrent()

        // when
        advanceTimeBy(4_900)
        runCurrent()

        // then
        viewModel.added shouldBe QuickAdded(MetroAdded.id, Metro, MetroAdded.value)

        // when
        advanceTimeBy(200)
        runCurrent()

        // then
        viewModel.added shouldBe null
    }

    private companion object {
        val Bank = AccountId(UUID.randomUUID())

        val Metro = QuickAdd(
            id = QuickAddId(UUID.randomUUID()),
            type = TransactionType.EXPENSE,
            account = Bank,
            toAccount = null,
            amount = PositiveDouble.unsafe(2.2),
            category = CategoryId(UUID.randomUUID()),
            title = NotBlankTrimmedString.unsafe("Metro"),
            creditCard = null,
            orderNum = 0.0,
        )

        val MetroAdded = Expense(
            id = TransactionId(UUID.randomUUID()),
            title = Metro.title,
            description = null,
            category = Metro.category,
            time = Instant.parse("2026-11-17T08:30:00Z"),
            settled = true,
            metadata = TransactionMetadata(
                recurringRuleId = null,
                paidForDateTime = null,
                loanRecordId = null,
                purchaseDateTime = null,
            ),
            value = PositiveValue(PositiveDouble.unsafe(2.2), AssetCode.unsafe("EUR")),
            account = Bank,
        )
    }
}
