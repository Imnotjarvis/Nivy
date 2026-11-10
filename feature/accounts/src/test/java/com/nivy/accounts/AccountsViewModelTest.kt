package com.nivy.accounts

import com.nivy.accounts.usecase.AccountDataUseCase
import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.AccountType
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.DataObserver
import com.nivy.data.DataWriteEvent
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.repository.AccountRepository
import com.nivy.domain.UserWrites
import com.nivy.ui.testing.runComposeTest
import com.nivy.ui.testing.runTest
import com.nivy.wallet.PeriodState
import com.nivy.wallet.data.model.AccountData
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
class AccountsViewModelTest {

    private val bank = account("Bank", orderNum = 0.0)
    private val cash = account("Cash", orderNum = 1.0)
    private val savings = account("Savings", orderNum = 2.0)

    private val writeEvents = MutableSharedFlow<DataWriteEvent>(extraBufferCapacity = 1)
    private val accountRepository = mockk<AccountRepository>(relaxUnitFun = true) {
        coEvery { findAll() } returns listOf(bank, cash, savings)
    }
    private val accountDataUseCase = mockk<AccountDataUseCase>()

    private lateinit var viewModel: AccountsViewModel

    @Before
    fun setUp() {
        // The view model collects the data changes from where it's made.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = AccountsViewModel(
            periodState = PeriodState(FixedTime),
            appPreferences = mockk(relaxed = true),
            currencyRepository = mockk { coEvery { getBaseCurrency() } returns AssetCode.unsafe("EUR") },
            accountDataUseCase = accountDataUseCase,
            accountRepository = accountRepository,
            dataObserver = mockk<DataObserver> { every { writeEvents } returns this@AccountsViewModelTest.writeEvents },
            features = mockk(relaxed = true),
            timeProvider = FixedTime,
            timeConverter = TestTimeConverter(),
            dispatchers = TestDispatchersProvider,
            userWrites = UserWrites(mockk(relaxed = true)),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a new order is saved in one write`() {
        coEvery { accountDataUseCase.accountsData(any(), any(), any(), any()) } answers {
            firstArg<List<Account>>().data()
        }

        viewModel.runTest(
            events = listOf(AccountsEvent.OnReorder(listOf(savings, bank, cash).data()))
        ) {}

        coVerify(exactly = 1) {
            accountRepository.saveMany(
                listOf(savings.copy(orderNum = 0.0), bank.copy(orderNum = 1.0), cash.copy(orderNum = 2.0))
            )
        }
        coVerify(exactly = 0) { accountRepository.save(any()) }
    }

    @Test
    fun `a load that a newer one overtakes doesn't put back what it read`() {
        // The first load is slow: a change comes in, and its load finishes before the first one.
        val firstLoad = CompletableDeferred<Unit>()
        var loads = 0
        coEvery { accountDataUseCase.accountsData(any(), any(), any(), any()) } coAnswers {
            val accounts = firstArg<List<Account>>()
            if (loads++ == 0) {
                firstLoad.await()
                accounts.take(1).data()
            } else {
                accounts.data()
            }
        }

        runComposeTest(
            present = { viewModel.uiState() },
            actions = {
                writeEvents.tryEmit(DataWriteEvent.AllDataChange)
                firstLoad.complete(Unit)
            },
        ) {
            accountsData.map { it.account } shouldBe listOf(bank, cash, savings)
        }
    }

    private fun List<Account>.data(): ImmutableList<AccountData> = map {
        AccountData(
            account = it,
            balance = 0.0,
            balanceBaseCurrency = null,
            monthlyExpenses = 0.0,
            monthlyIncome = 0.0,
            creditCards = persistentListOf(),
        )
    }.toImmutableList()

    private companion object {
        fun account(name: String, orderNum: Double) = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            asset = AssetCode.unsafe("EUR"),
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            type = AccountType.BANK,
            archived = false,
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
