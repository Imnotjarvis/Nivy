package com.nivy.categories

import android.database.sqlite.SQLiteFullException
import androidx.lifecycle.viewModelScope
import com.nivy.base.Toaster
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.CategoryRepository
import com.nivy.domain.UserWrites
import com.nivy.domain.usecase.category.CategoryIncomeExpenseUseCase
import com.nivy.ui.R
import com.nivy.ui.testing.runComposeTest
import com.nivy.wallet.PeriodState
import com.nivy.wallet.domain.data.SortOrder
import com.nivy.wallet.domain.pure.data.IncomeExpenseTransferPair
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class CategoriesViewModelTest {
    // The repository's order isn't the person's (Transport, Food, Salary), nor the alphabet's.
    private val food = category("Food", orderNum = 1.0)
    private val transport = category("Transport", orderNum = 0.0)
    private val salary = category("Salary", orderNum = 2.0)

    private val toaster = mockk<Toaster>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository>(relaxUnitFun = true) {
        coEvery { findAll() } returns listOf(food, transport, salary)
    }
    private val appPreferences = mockk<AppPreferences>(relaxed = true) {
        coEvery { categorySortOrder(any()) } returns SortOrder.DEFAULT.orderNum
    }
    private val categoryIncomeExpense = mockk<CategoryIncomeExpenseUseCase> {
        coEvery { incomeExpense(any(), any(), any(), any()) } returns IncomeExpenseTransferPair.zero()
    }

    private val viewModel = CategoriesViewModel(
        categoryCreator = mockk(),
        categoryRepository = categoryRepository,
        periodState = PeriodState(FixedTime),
        appPreferences = appPreferences,
        currencyRepository = mockk { coEvery { getBaseCurrency() } returns AssetCode.unsafe("EUR") },
        accountRepository = mockk { coEvery { findAll() } returns emptyList() },
        transactionsInRangeUseCase = mockk { coEvery { transactions(any(), any()) } returns emptyList() },
        categoryIncomeExpenseUseCase = categoryIncomeExpense,
        features = mockk(relaxed = true),
        timeProvider = FixedTime,
        timeConverter = TestTimeConverter(),
        userWrites = UserWrites(toaster),
    )

    @Test
    fun `the list opens in the person's order, each category with its month`() {
        coEvery { categoryIncomeExpense.incomeExpense(any(), any(), food, "EUR") } returns
            IncomeExpenseTransferPair.zero().copy(income = BigDecimal("10"), expense = BigDecimal("42.5"))

        stateAfter {
            allCategories.names() shouldBe listOf("Transport", "Food", "Salary")
            categories shouldBe allCategories
            allCategories.single { it.category == food } shouldBe
                CategoryData(food, monthlyBalance = -32.5, monthlyExpenses = 42.5, monthlyIncome = 10.0)
            baseCurrency shouldBe "EUR"
        }
    }

    @Test
    fun `the list opens in the sort order remembered`() {
        coEvery { appPreferences.categorySortOrder(any()) } returns SortOrder.ALPHABETICAL.orderNum

        stateAfter {
            allCategories.names() shouldBe listOf("Food", "Salary", "Transport")
            sortOrder shouldBe SortOrder.ALPHABETICAL
        }
    }

    @Test
    fun `a search lists the categories whose name holds it, out of all of them`() {
        stateAfter(CategoriesScreenEvent.OnSearchQueryUpdate("a")) {
            categories.names() shouldBe listOf("Transport", "Salary")
            allCategories.names() shouldBe listOf("Transport", "Food", "Salary")
        }
    }

    @Test
    fun `a new order is saved, each category at its place, and kept`() {
        stateAfter(CategoriesScreenEvent.OnReorder(salaryMovedToTheTop())) {
            allCategories.names() shouldBe listOf("Salary", "Transport", "Food")
            sortOrder shouldBe SortOrder.DEFAULT
        }

        coVerifyOrder {
            categoryRepository.save(salary.copy(orderNum = 0.0))
            categoryRepository.save(transport.copy(orderNum = 1.0))
            categoryRepository.save(food.copy(orderNum = 2.0))
            appPreferences.setCategorySortOrder(SortOrder.DEFAULT.orderNum)
        }
    }

    @Test
    fun `an order the database refuses says so, and the list stays as it was`() {
        coEvery { categoryRepository.save(any()) } throws SQLiteFullException("database or disk is full")

        stateAfter(CategoriesScreenEvent.OnReorder(salaryMovedToTheTop())) {
            allCategories.names() shouldBe listOf("Transport", "Food", "Salary")
        }

        coVerify(exactly = 1) { toaster.show(R.string.msg_change_not_saved, any()) }
        coVerify(exactly = 0) { appPreferences.setCategorySortOrder(any()) }
    }

    @Test
    fun `choosing a sort order sorts the list and remembers the choice, and saves no category`() {
        stateAfter(CategoriesScreenEvent.OnReorder(listOf(transport, food, salary).data(), SortOrder.ALPHABETICAL)) {
            allCategories.names() shouldBe listOf("Food", "Salary", "Transport")
            sortOrder shouldBe SortOrder.ALPHABETICAL
        }

        coVerify(exactly = 1) { appPreferences.setCategorySortOrder(SortOrder.ALPHABETICAL.orderNum) }
        coVerify(exactly = 0) { categoryRepository.save(any()) }
    }

    /** The state once the screen has loaded and each of [events] has run to its end, one after the other. */
    private fun stateAfter(vararg events: CategoriesScreenEvent, verify: CategoriesScreenState.() -> Unit) =
        runComposeTest(
            present = { viewModel.uiState() },
            actions = {
                settle()
                events.forEach {
                    viewModel.onEvent(it)
                    settle()
                }
            },
            verify = verify,
        )

    // The view model works on threads of its own, not on a dispatcher a test can pass: what it
    // started is waited for.
    private fun settle() = runBlocking {
        viewModel.viewModelScope.coroutineContext.job.children.toList().joinAll()
    }

    // What the reorder dialog sends: the list in its new order, the moved one numbered before the first.
    private fun salaryMovedToTheTop() = listOf(salary.copy(orderNum = -1.0), transport, food).data()

    private fun List<Category>.data() = map {
        CategoryData(category = it, monthlyBalance = 0.0, monthlyExpenses = 0.0, monthlyIncome = 0.0)
    }

    private fun List<CategoryData>.names() = map { it.category.name.value }

    private companion object {
        fun category(name: String, orderNum: Double) = Category(
            id = CategoryId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            color = ColorInt(0),
            icon = null,
            orderNum = orderNum,
        )

        /** November 15, 2026, noon, in UTC. */
        val FixedTime = object : TimeProvider {
            private val now = LocalDateTime.of(2026, 11, 15, 12, 0)
            override fun getZoneId(): ZoneId = ZoneId.of("UTC")
            override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
            override fun localNow(): LocalDateTime = now
            override fun localDateNow(): LocalDate = now.toLocalDate()
            override fun localTimeNow(): LocalTime = now.toLocalTime()
        }
    }
}
