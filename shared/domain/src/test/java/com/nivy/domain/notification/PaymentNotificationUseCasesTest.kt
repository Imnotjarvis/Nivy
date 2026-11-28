package com.nivy.domain.notification

import arrow.core.None
import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.AccountType
import com.nivy.base.time.TimeProvider
import com.nivy.data.DataObserver
import com.nivy.data.db.dao.fake.FakePaymentNotificationDao
import com.nivy.data.db.dao.fake.FakePaymentSourceDao
import com.nivy.data.db.entity.PaymentSourceEntity
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.PaymentNotificationRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.creditcard.CreditCardChargeUseCase
import com.nivy.domain.usecase.exchange.BaseCurrencyRates
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.wallet.domain.logic.SmartTitleSuggestionsLogic
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID

class PaymentNotificationUseCasesTest {
    private val notificationDao = FakePaymentNotificationDao()
    private val sourceDao = FakePaymentSourceDao()
    private val repository = PaymentNotificationRepository(
        notificationDao = notificationDao,
        writeNotificationDao = notificationDao,
        sourceDao = sourceDao,
        writeSourceDao = sourceDao,
        dispatchersProvider = TestDispatchersProvider,
    )

    // The transactions saved, by id; a payment entered by hand, when there is one.
    private val saved = linkedMapOf<UUID, Transaction>()
    private var byHand = emptyList<Transaction>()
    private val transactionRepository = mockk<TransactionRepository> {
        coEvery { save(any()) } answers { firstArg<Transaction>().let { saved[it.id.value] = it } }
        // A value class: the mock gets its UUID.
        coEvery { deleteById(any()) } answers {
            saved.remove(firstArg<Any>().let { (it as? TransactionId)?.value ?: it as UUID })
        }
        coEvery { findByIds(any()) } answers { firstArg<List<TransactionId>>().mapNotNull { saved[it.value] } }
        coEvery { findAllByAccountAndBetween(any(), any(), any()) } answers { byHand }
    }
    private val accountRepository = mockk<AccountRepository> {
        coEvery { findById(Intesa.id) } returns Intesa
        coEvery { findById(Revolut.id) } returns Revolut
    }
    private val transactions = NotificationTransactions(
        accountRepository = accountRepository,
        transactionRepository = transactionRepository,
        creditCardChargeUseCase = mockk<CreditCardChargeUseCase> { coEvery { dated(any()) } answers { firstArg() } },
        titleSuggestions = mockk<SmartTitleSuggestionsLogic> {
            coEvery { categoryOf(any()) } returns null
            coEvery { categoryOf("CONAD SUPERSTORE") } returns Groceries
        },
        exchangeUseCase = mockk<ExchangeUseCase> { coEvery { convert(any(), any()) } returns None },
        dataObserver = DataObserver(),
    )
    private var adding = true
    private val capture = CapturePaymentNotificationUseCase(
        repository = repository,
        transactions = transactions,
        transactionRepository = transactionRepository,
        accountRepository = accountRepository,
        creditCardRepository = mockk<CreditCardRepository>(),
        appPreferences = mockk<AppPreferences> { coEvery { addFromNotifications() } answers { adding } },
        dataObserver = DataObserver(),
        timeProvider = mockk<TimeProvider> { every { utcNow() } returns Now },
    )
    private val actions = PaymentNotificationActions(repository, transactions, DataObserver())
    private val list = PaymentNotificationListUseCase(repository, transactionRepository)

    @Test
    fun `a payment from an app picked is added on its account, titled and in its shop's category`() = runTest {
        // given
        pick(IntesaApp, Intesa)

        // when
        capture.capture(posted(IntesaApp, "Intesa Sanpaolo", "Hai pagato 12,50 € presso CONAD SUPERSTORE"))

        // then
        val expense = saved.values.single().shouldBeInstanceOf<Expense>()
        expense.value shouldBe PositiveValue(PositiveDouble.unsafe(12.5), Eur)
        expense.account shouldBe Intesa.id
        expense.title shouldBe NotBlankTrimmedString.unsafe("CONAD SUPERSTORE")
        expense.category shouldBe Groceries.id
        expense.time shouldBe Now
        expense.settled shouldBe true
        val kept = notificationDao.findUnreviewed().single()
        kept.outcome shouldBe Outcome.Added.name
        kept.transactionId shouldBe expense.id.value
    }

    @Test
    fun `many banks' payment notifications each add their expense, with its amount and shop`() = runTest {
        // given: the shapes the reader reads, each from an app of its own
        val payments = listOf(
            BankPayment(
                "Revolut",
                "Hai pagato 10 € presso Vega Carburanti ⛽\n⚠️ Saldo: 24,53 €",
                10.0,
                "Vega Carburanti",
            ),
            BankPayment("isybank", "Pagamento di 12,50 € con carta *1234 presso CONAD", 12.5, "CONAD"),
            BankPayment(
                "UniCredit",
                "Pagamento con carta di EUR 12,50 presso CONAD SUPERSTORE il 27/11",
                12.5,
                "CONAD SUPERSTORE",
            ),
            BankPayment("Fineco", "Addebito carta: 12,50 EUR - CONAD SUPERSTORE", 12.5, "CONAD SUPERSTORE"),
            BankPayment("HYPE", "Pagamento di 7,90 € effettuato presso Netflix", 7.9, "Netflix"),
            BankPayment(
                "Nexi Pay",
                "Autorizzato pagamento di 1.234,56 EUR presso Mediaworld con carta ****1234",
                1234.56,
                "Mediaworld",
            ),
            BankPayment("N26", "You spent €23.49 at Lidl", 23.49, "Lidl"),
            BankPayment("Satispay", "Hai pagato 3,50 € a Bar Roma", 3.5, "Bar Roma"),
        )
        payments.forEach { pick(it.packageName, Intesa) }

        // when: two hours apart, so that none is taken for another's repeat
        payments.forEachIndexed { i, payment ->
            capture.capture(
                posted(payment.packageName, payment.app, payment.text, at = Now.plus(Duration.ofHours(2L * i)))
            )
        }

        // then
        saved.values.map { it as Expense }.map { Triple(it.value.amount.value, it.title?.value, it.account) } shouldBe
            payments.map { Triple(it.amount, it.shop, Intesa.id) }
    }

    @Test
    fun `while adding is off, a payment is logged as it would be added, and nothing is`() = runTest {
        // given
        pick(IntesaApp, Intesa)
        adding = false

        // when
        capture.capture(posted(IntesaApp, "Intesa Sanpaolo", "Hai pagato 12,50 € presso CONAD SUPERSTORE"))

        // then
        saved.values.shouldBeEmpty()
        notificationDao.findSince(Instant.EPOCH).single().outcome shouldBe Outcome.WouldAdd.name
    }

    @Test
    fun `while adding is off, nothing goes to Home's list, not even what wasn't added, and the log has it all`() =
        runTest {
            // given
            pick(IntesaApp, Intesa)
            adding = false

            // when
            capture.capture(posted(IntesaApp, "Intesa Sanpaolo", "Hai pagato 12,50 € presso CONAD SUPERSTORE"))
            capture.capture(
                posted(IntesaApp, "Intesa Sanpaolo", "Hai ricevuto 57 € da Marco", at = Now.plusSeconds(60))
            )

            // then
            list.load().size shouldBe 0
            notificationDao.findSince(Instant.EPOCH).map { it.outcome }.toSet() shouldBe
                setOf(Outcome.WouldAdd.name, Outcome.NotAdded.name)
        }

    @Test
    fun `an app that isn't picked isn't read`() = runTest {
        // when
        capture.capture(posted(IntesaApp, "Intesa Sanpaolo", "Hai pagato 12,50 € presso CONAD SUPERSTORE"))

        // then
        saved.values.shouldBeEmpty()
        notificationDao.findSince(Instant.EPOCH).shouldBeEmpty()
    }

    @Test
    fun `a bank's notification and Google Wallet's of one payment add one expense`() = runTest {
        // given
        pick(IntesaApp, Intesa)
        pick(Wallet, Intesa)

        // when
        capture.capture(posted(IntesaApp, "Intesa Sanpaolo", "Hai pagato 12,50 € con la carta *1501 da CONAD"))
        capture.capture(
            posted(Wallet, "CONAD", "12,50 € con Carta Visa ••1501", at = Now.plusSeconds(570))
        )

        // then
        saved.values.size shouldBe 1
        val listed = list.load()
        listed.added.single().alsoIn.map { it.app } shouldBe listOf("Wallet")
    }

    @Test
    fun `the list's totals count the payments still added, not the ones undone`() = runTest {
        // given
        pick(IntesaApp, Intesa)
        capture.capture(posted(IntesaApp, "Intesa Sanpaolo", "Hai pagato 12,50 € presso CONAD SUPERSTORE"))
        capture.capture(
            posted(IntesaApp, "Intesa Sanpaolo", "Hai pagato 4,20 € presso Bar Roma", at = Now.plusSeconds(60))
        )
        actions.undo(list.load().added.first { it.notification.merchant == "Bar Roma" }.notification.id)

        // when
        val totals = list.load().totals(BaseCurrencyRates(mockk(), Eur))

        // then
        totals shouldBe Totals(income = 0.0, expenses = 12.5)
    }

    @Test
    fun `Separate adds the other app's notification of a payment as a payment of its own`() = runTest {
        // given
        pick(IntesaApp, Intesa)
        pick(Wallet, Intesa)
        capture.capture(posted(IntesaApp, "Intesa Sanpaolo", "Hai pagato 12,50 € con la carta *1501 da CONAD"))
        capture.capture(
            posted(Wallet, "CONAD", "12,50 € con Carta Visa ••1501", at = Now.plusSeconds(570))
        )

        // when
        list.load().added.single().alsoIn.forEach { actions.add(it.id) }

        // then
        saved.values.size shouldBe 2
        val listed = list.load()
        listed.added.map { it.outcome } shouldBe listOf(Outcome.Added, Outcome.Added)
        listed.added.flatMap { it.alsoIn }.shouldBeEmpty()
    }

    @Test
    fun `Wallet's known card pays from its account, and one not known yet from Wallet's, as a guess`() = runTest {
        // given
        pick(Wallet, Intesa)
        sourceDao.save(PaymentSourceEntity(PaymentSourceEntity.Card, "1501", name = null, accountId = Revolut.id.value))

        // when
        capture.capture(posted(Wallet, "CONAD", "12,50 € con Carta Visa ••1501"))
        capture.capture(
            posted(Wallet, "Bar Roma", "4,20 € con Mastercard ••7733", at = Now.plusSeconds(60))
        )

        // then
        val (known, unknown) = saved.values.map { it as Expense }
        known.account shouldBe Revolut.id
        unknown.account shouldBe Intesa.id
        notificationDao.findUnreviewed().map { it.card to it.guessedAccount }.toSet() shouldBe
            setOf("1501" to false, "7733" to true)
    }

    @Test
    fun `a payment entered by hand in the hour before isn't added again`() = runTest {
        // given
        pick(IntesaApp, Intesa)
        byHand = listOf(expense(Intesa, 12.5, Now.minusSeconds(300)))

        // when
        capture.capture(posted(IntesaApp, "Intesa Sanpaolo", "Hai pagato 12,50 € presso CONAD SUPERSTORE"))

        // then
        saved.values.shouldBeEmpty()
        val kept = notificationDao.findUnreviewed().single()
        kept.outcome shouldBe Outcome.NotAdded.name
        kept.why shouldBe Why.AddedByHand.name
    }

    @Test
    fun `Undo deletes the expense and keeps its repeat out, and Add back adds it again`() = runTest {
        // given
        pick(IntesaApp, Intesa)
        capture.capture(posted(IntesaApp, "Intesa Sanpaolo", "Hai pagato 12,50 € presso CONAD SUPERSTORE"))
        val added = notificationDao.findUnreviewed().single()

        // when
        actions.undo(added.id)
        capture.capture(
            posted(
                IntesaApp,
                "Intesa Sanpaolo",
                "Hai pagato 12,50 € presso CONAD SUPERSTORE",
                key = "again",
                at = Now.plusSeconds(600),
            )
        )

        // then
        saved.values.shouldBeEmpty()
        val (repeat, undone) = notificationDao.findUnreviewed()
        undone.outcome shouldBe Outcome.Undone.name
        repeat.why shouldBe Why.UndoneBefore.name

        // when
        actions.addBack(undone.id)

        // then
        saved.values.single().shouldBeInstanceOf<Expense>()
        notificationDao.findById(undone.id)?.outcome shouldBe Outcome.Added.name
    }

    @Test
    fun `money received is added as an income, and Dismiss and All good empty the list`() = runTest {
        // given
        pick(RevolutApp, Revolut)
        capture.capture(posted(RevolutApp, "Hai ricevuto 57 €", "Pagamento ricevuto da Marco. Premi per i dettagli"))
        capture.capture(posted(RevolutApp, "Denaro inviato", "Hai inviato 28 € a Giulia", at = Now.plusSeconds(60)))
        val (sent, received) = list.load().notAdded

        // when
        actions.add(received.notification.id)
        actions.dismiss(sent.notification.id)

        // then
        val income = saved.values.single().shouldBeInstanceOf<Income>()
        income.value shouldBe PositiveValue(PositiveDouble.unsafe(57.0), Eur)
        income.title shouldBe NotBlankTrimmedString.unsafe("Marco")
        list.load().added.size shouldBe 1
        list.load().notAdded.shouldBeEmpty()

        // when
        actions.allGood()

        // then
        list.load().size shouldBe 0
    }

    @Test
    fun `a card's account, once given, takes the expenses guessed for it`() = runTest {
        // given
        pick(Wallet, Intesa)
        capture.capture(posted(Wallet, "Bar Roma", "4,20 € con Mastercard ••7733"))

        // when
        actions.setCardAccount("7733", Revolut.id, creditCard = null)

        // then
        (saved.values.single() as Expense).account shouldBe Revolut.id
        notificationDao.findUnreviewed().single().guessedAccount shouldBe false
    }

    private suspend fun pick(packageName: String, account: Account) {
        sourceDao.save(
            PaymentSourceEntity(PaymentSourceEntity.App, packageName, name = "App", accountId = account.id.value)
        )
    }

    private fun posted(packageName: String, title: String, text: String, key: String = "k$title", at: Instant = Now) =
        PostedNotification(
            packageName = packageName,
            appName = if (packageName == Wallet) "Wallet" else "Bank",
            key = key,
            postTime = at,
            title = title,
            text = text,
        )

    private fun expense(account: Account, amount: Double, time: Instant) = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = time,
        settled = true,
        metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
        ),
        value = PositiveValue(PositiveDouble.unsafe(amount), account.asset),
        account = account.id,
    )

    private companion object {
        val Now: Instant = Instant.parse("2026-11-27T12:04:00Z")
        const val IntesaApp = "test.bank.intesa"
        const val RevolutApp = "test.bank.revolut"
        const val Wallet = PaymentNotificationReader.GoogleWallet
        val Eur = AssetCode.unsafe("EUR")
        val Intesa = account("Intesa")
        val Revolut = account("Revolut")
        val Groceries = Category(
            name = NotBlankTrimmedString.unsafe("Groceries"),
            color = ColorInt(0),
            icon = null,
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        )

        fun account(name: String) = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            asset = Eur,
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            type = AccountType.OTHER,
            archived = false,
            orderNum = 0.0,
        )
    }
}

// A bank's payment notification, and the expense it should add.
private data class BankPayment(val app: String, val text: String, val amount: Double, val shop: String) {
    val packageName = "test.bank." + app.lowercase().filter(Char::isLetter)
}
