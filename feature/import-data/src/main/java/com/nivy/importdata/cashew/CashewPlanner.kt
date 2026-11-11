package com.nivy.importdata.cashew

import com.nivy.base.model.AccountType
import com.nivy.base.model.IntervalType
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Loan
import com.nivy.data.model.LoanRecord
import com.nivy.data.model.PlannedPaymentInterval
import com.nivy.data.model.PlannedPaymentRule
import com.nivy.data.model.PlannedPaymentRuleId
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Tag
import com.nivy.data.model.TagAssociation
import com.nivy.data.model.TagId
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.AssociationId
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.model.primitive.PositiveInt
import com.nivy.domain.usecase.account.AdjustAccountBalanceUseCase
import com.nivy.importdata.cashew.LeftOut.Reason
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.abs

/** What the import writes from Cashew's [data] into [wallet], and what it leaves out. */
internal fun planCashewImport(data: CashewData, wallet: NivyWallet): CashewPlan = CashewPlanner(data, wallet).plan()

/**
 * Turns Cashew's rows into Nivy's. Only what Cashew counts comes over as a transaction: paid
 * rows, not the ones skipped or still to pay. What's planned becomes Nivy's planned payments, what
 * was lent or borrowed its loans, a subcategory or a goal a tag; the rest is listed in [LeftOut].
 */
internal class CashewPlanner(internal val data: CashewData, internal val wallet: NivyWallet) {
    /** Each wallet's account, by Cashew's pk: a new one, or the wallet's of the same name and currency. */
    internal val accounts = mutableMapOf<String, Account>()

    /** Each category's, by Cashew's pk: a subcategory has its main category's. */
    internal val categories = mutableMapOf<String, Category>()
    private val subcategoryNames = mutableMapOf<String, String>()
    private val goals = data.objectives.filterNot { it.loan }.associateBy { it.pk }

    private val newAccounts = mutableListOf<Account>()
    private val newCategories = mutableListOf<Category>()
    private val newTags = mutableListOf<Tag>()
    private val tagsByName = wallet.tags.associateTo(mutableMapOf()) { nameKey(it.name.value) to it.id }
    private val transactions = mutableListOf<Transaction>()
    private val tagLinks = mutableListOf<TagAssociation>()
    private val rules = mutableListOf<PlannedPaymentRule>()
    internal val loans = mutableListOf<Loan>()
    internal val loanRecords = mutableListOf<LoanRecord>()
    private val leftOut = linkedMapOf<Reason, MutableList<LeftOut.Item>>()
    internal var found = 0
    private var imported = 0

    fun plan(): CashewPlan {
        planAccounts()
        planCategories()
        val counted = data.transactions.filterNot { it.skipped }.sortedBy { it.date }
        val (corrections, others) = counted.partition { it.categoryPk == BalanceCorrection }
        planCorrections(corrections)
        val longTermLoans = data.objectives.filter { it.loan }.associateBy { it.pk }
        val (inLoans, rest) = others.partition { it.loanPk in longTermLoans }
        inLoans.groupBy { it.loanPk }.forEach { (pk, rows) -> longTermLoans[pk]?.let { planLongTermLoan(it, rows) } }
        rest.filter { it.type == CashewType.Subscription || it.type == CashewType.Repetitive }
            .groupBy { it.pk.substringBefore(SeriesKeySeparator) }
            .values.forEach(::planSeries)
        rest.forEach { cashew ->
            when (cashew.type) {
                null -> if (cashew.paid) bringOver(cashew) else leave(Reason.NotPaid, cashew)
                CashewType.Upcoming -> planUpcoming(cashew)
                CashewType.Credit, CashewType.Debt -> planLentOrBorrowed(cashew)
                CashewType.Subscription, CashewType.Repetitive -> Unit // planned above, by series
            }
        }
        return CashewPlan(
            accounts = newAccounts,
            categories = newCategories,
            tags = newTags,
            transactions = transactions,
            tagLinks = tagLinks,
            plannedPayments = rules,
            loans = loans,
            loanRecords = loanRecords,
            budgets = planBudgets(),
            rates = planRates(),
            found = found,
            imported = imported,
            leftOut = leftOut.map { (reason, items) -> LeftOut(reason, items) },
        )
    }

    // region Accounts, categories and tags
    private fun planAccounts() {
        val existing = wallet.accounts.uniqueBy { nameKey(it.name.value) }
        var order = (wallet.accounts.maxOfOrNull { it.orderNum } ?: -1.0) + 1
        data.wallets.sortedBy { it.order }.forEachIndexed { index, cashew ->
            val asset = currency(cashew.currency)
            val name = NotBlankTrimmedString.from(cashew.name).getOrNull() ?: NotBlankTrimmedString.unsafe(asset.code)
            accounts[cashew.pk] = existing[nameKey(name.value)]?.takeIf { it.asset == asset } ?: Account(
                id = AccountId(UUID.randomUUID()),
                name = name,
                asset = asset,
                color = cashewColor(cashew.colour) ?: pickerColor(index),
                icon = cashewIcon(cashew.iconName),
                includeInBalance = true,
                type = AccountType.OTHER,
                archived = false,
                orderNum = order++,
            ).also(newAccounts::add)
        }
    }

    /** A wallet's currency in Nivy: Cashew's key in capitals, or the base currency for none. */
    internal fun currency(key: String?): AssetCode = key?.let { AssetCode.from(it).getOrNull() } ?: wallet.baseCurrency

    /**
     * Main categories come over, one per name: Cashew can keep an income and an expense category
     * of the same name, Nivy keeps one. A subcategory's transactions go to its main category.
     */
    private fun planCategories() {
        val byName = wallet.categories.uniqueBy { nameKey(it.name.value) }.toMutableMap()
        var order = (wallet.categories.maxOfOrNull { it.orderNum } ?: -1.0) + 1
        val (mains, subs) = data.categories.filter { it.pk != BalanceCorrection }.partition { it.mainCategoryPk == null }
        mains.sortedBy { it.order }.forEachIndexed { index, cashew ->
            val name = NotBlankTrimmedString.from(cashew.name).getOrNull() ?: return@forEachIndexed
            categories[cashew.pk] = byName.getOrPut(nameKey(name.value)) {
                Category(
                    id = CategoryId(UUID.randomUUID()),
                    name = name,
                    color = cashewColor(cashew.colour) ?: pickerColor(index),
                    icon = cashewIcon(cashew.iconName),
                    orderNum = order++,
                ).also(newCategories::add)
            }
        }
        subs.forEach { sub ->
            sub.mainCategoryPk?.let(categories::get)?.let { categories[sub.pk] = it }
            sub.name.trim().takeIf { it.isNotEmpty() }?.let { subcategoryNames[sub.pk] = it }
        }
    }

    /** The tag named [name]: the wallet's, or a new one. */
    private fun tag(name: String): TagId? {
        val tagName = NotBlankTrimmedString.from(name).getOrNull() ?: return null
        return tagsByName.getOrPut(nameKey(tagName.value)) {
            Tag(id = TagId(UUID.randomUUID()), name = tagName, creationTimestamp = wallet.now)
                .also(newTags::add).id
        }
    }

    /** A Cashew transaction's tags: its subcategory's name, and its goal's. */
    private fun tagsOf(cashew: CashewTransaction): List<TagId> = listOfNotNull(
        cashew.subCategoryPk?.let(subcategoryNames::get)?.let(::tag),
        cashew.goalPk?.let(goals::get)?.let { tag(it.name) },
    ).distinct()
    // endregion

    // region Transactions
    /**
     * Brings a paid [cashew] transaction over as an income or an expense, with its tags, or lists
     * why it can't come: no amount, or no wallet. Returns what it brought over.
     */
    internal fun bringOver(
        cashew: CashewTransaction,
        metadata: TransactionMetadata = NoMetadata,
        title: String? = cashew.name,
        category: CategoryId? = categories[cashew.categoryPk]?.id,
    ): Transaction? {
        found++
        val account = accounts[cashew.walletPk]
        val amount = PositiveDouble.from(abs(cashew.amount)).getOrNull()
        return when {
            account == null -> null.also { leave(Reason.NoAccount, cashew) }
            amount == null -> null.also { leave(Reason.NoAmount, cashew) }
            else -> flow(cashew, account, amount, metadata, title, category).also { add(it, cashew) }
        }
    }

    private fun flow(
        cashew: CashewTransaction,
        account: Account,
        amount: PositiveDouble,
        metadata: TransactionMetadata,
        title: String?,
        category: CategoryId?,
    ): Transaction {
        val id = TransactionId(UUID.randomUUID())
        val value = PositiveValue(amount = amount, asset = account.asset)
        val name = title?.let { NotBlankTrimmedString.from(it).getOrNull() }
        val description = description(cashew.note)
        return if (cashew.amount > 0) {
            Income(
                id = id, title = name, description = description, category = category, time = cashew.date,
                settled = true, metadata = metadata, value = value, account = account.id,
            )
        } else {
            Expense(
                id = id, title = name, description = description, category = category, time = cashew.date,
                settled = true, metadata = metadata, value = value, account = account.id,
            )
        }
    }

    private fun add(transaction: Transaction, vararg rows: CashewTransaction) {
        transactions += transaction
        imported += rows.size
        tagsOf(rows.first()).forEach { tagLinks += TagAssociation(id = it, associatedId = AssociationId(transaction.id.value)) }
    }

    /** A note as a description; none for the ones Cashew writes itself on a transfer or a correction. */
    private fun description(note: String): NotBlankTrimmedString? =
        note.takeUnless { isCashewsOwnNote(it) }?.let { NotBlankTrimmedString.from(it).getOrNull() }

    // "Transferred Balance\nBank → Savings", "Updated Total Balance\nBank: 1,000.00 €", in the user's language.
    private fun isCashewsOwnNote(note: String): Boolean {
        val lines = note.lines()
        return lines.size == 2 && (
            TransferArrows.any { it in lines[1] } || data.wallets.any { lines[1].startsWith("${it.name}: ") }
            )
    }

    internal fun leave(reason: Reason, cashew: CashewTransaction, date: Instant = cashew.date) =
        leave(reason, LeftOut.Item(name = label(cashew), date = local(date).toLocalDate()))

    internal fun leave(reason: Reason, item: LeftOut.Item) {
        leftOut.getOrPut(reason) { mutableListOf() } += item
    }

    /** How Cashew names a transaction: its title, or its category's name without one. */
    internal fun label(cashew: CashewTransaction): String =
        cashew.name.trim().ifEmpty { data.categories.find { it.pk == cashew.categoryPk }?.name?.trim().orEmpty() }

    internal fun local(instant: Instant): LocalDateTime = instant.atZone(wallet.zone).toLocalDateTime()
    // endregion

    // region Transfers and balance corrections
    /**
     * Cashew's balance corrections: a transfer is two of them, the half the money left pointing at
     * the half it arrived in. A paid pair is a transfer; a correction alone adjusts a balance.
     */
    private fun planCorrections(rows: List<CashewTransaction>) {
        val byPk = rows.associateBy { it.pk }
        val paired = mutableSetOf<String>()
        rows.forEach { half ->
            val other = half.pairedPk?.let(byPk::get)
                ?.takeIf { it.pk != half.pk && it.paid == half.paid && it.pk !in paired && half.pk !in paired }
                ?: return@forEach
            paired += half.pk
            paired += other.pk
            when {
                half.paid -> planTransfer(half, other)
                // A transfer that's still to come: Nivy plans incomes and expenses only.
                half.type in Planned -> leave(Reason.PlannedTransfer, half)
            }
        }
        rows.filter { it.pk !in paired }.forEach { correction ->
            when {
                correction.paid -> bringOver(
                    correction,
                    title = correction.name.ifBlank { AdjustAccountBalanceUseCase.AdjustTitle },
                    category = null,
                )
                correction.type in Planned -> leave(Reason.PlannedTransfer, correction)
            }
        }
    }

    private fun planTransfer(first: CashewTransaction, second: CashewTransaction) {
        // The money left the half below zero.
        val (from, to) = if (first.amount <= second.amount) first to second else second to first
        val fromAccount = accounts[from.walletPk]
        val toAccount = accounts[to.walletPk]
        val fromAmount = PositiveDouble.from(abs(from.amount)).getOrNull()
        val toAmount = PositiveDouble.from(abs(to.amount)).getOrNull()
        found += 2
        when {
            fromAccount == null || toAccount == null -> leave(Reason.NoAccount, from)
            fromAmount == null || toAmount == null -> leave(Reason.NoAmount, from)
            else -> add(
                Transfer(
                    id = TransactionId(UUID.randomUUID()),
                    // Cashew titles each half after its wallet unless one title was given to both.
                    title = from.name.takeIf { it == to.name }?.let { NotBlankTrimmedString.from(it).getOrNull() },
                    description = description(from.note),
                    category = null,
                    time = minOf(from.date, to.date),
                    settled = true,
                    metadata = NoMetadata,
                    fromAccount = fromAccount.id,
                    fromValue = PositiveValue(fromAmount, fromAccount.asset),
                    toAccount = toAccount.id,
                    toValue = PositiveValue(toAmount, toAccount.asset),
                ),
                from,
                to,
            )
        }
    }
    // endregion

    // region Planned payments
    /**
     * A subscription or a repeating payment: its paid occurrences, and the next one to pay. The
     * next one starts a planned payment, which Nivy repeats from there; the paid ones stay linked
     * to it, as a planned payment's paid occurrences are. A series with nothing left to pay keeps
     * its occurrences linked, as after deleting a planned payment in Nivy.
     */
    private fun planSeries(series: List<CashewTransaction>) {
        val ruleId = UUID.randomUUID()
        series.filterNot { it.paid }.minByOrNull { it.date }?.let { next -> planRule(next, ruleId) }
        series.filter { it.paid }.forEach { bringOver(it, metadata = planned(ruleId, it)) }
    }

    private fun planRule(next: CashewTransaction, ruleId: UUID) {
        val account = accounts[next.walletPk]
        val amount = PositiveDouble.from(abs(next.amount)).getOrNull()
        val interval = interval(next)
        val ended = next.endDate?.let { next.date > it } == true
        when {
            ended -> Unit
            account == null -> leave(Reason.NoAccount, next)
            amount == null -> leave(Reason.NoAmount, next)
            interval == null -> leave(Reason.CantRepeat, next)
            else -> {
                rules += rule(next, ruleId, account, amount, interval)
                next.endDate?.let { leave(Reason.EndsInCashew, next, date = it) }
            }
        }
    }

    /** One upcoming payment: a one-time planned payment until paid, then an occurrence of one. */
    private fun planUpcoming(cashew: CashewTransaction) {
        if (cashew.paid) {
            bringOver(cashew, metadata = planned(UUID.randomUUID(), cashew))
            return
        }
        val account = accounts[cashew.walletPk]
        val amount = PositiveDouble.from(abs(cashew.amount)).getOrNull()
        when {
            account == null -> leave(Reason.NoAccount, cashew)
            amount == null -> leave(Reason.NoAmount, cashew)
            else -> rules += rule(cashew, UUID.randomUUID(), account, amount, interval = null)
        }
    }

    private fun rule(
        cashew: CashewTransaction,
        id: UUID,
        account: Account,
        amount: PositiveDouble,
        interval: PlannedPaymentInterval?,
    ) = PlannedPaymentRule(
        id = PlannedPaymentRuleId(id),
        type = if (cashew.amount > 0) TransactionType.INCOME else TransactionType.EXPENSE,
        account = account.id,
        amount = amount,
        category = categories[cashew.categoryPk]?.id,
        title = NotBlankTrimmedString.from(cashew.name).getOrNull(),
        description = description(cashew.note),
        startDate = cashew.date,
        interval = interval,
        creditCard = null,
    )

    private fun interval(cashew: CashewTransaction): PlannedPaymentInterval? {
        val type = when (cashew.repeat) {
            CashewRepeat.Daily -> IntervalType.DAY
            CashewRepeat.Weekly -> IntervalType.WEEK
            CashewRepeat.Monthly -> IntervalType.MONTH
            CashewRepeat.Yearly -> IntervalType.YEAR
            CashewRepeat.Custom, null -> null
        }
        val every = PositiveInt.from(cashew.repeatEvery ?: 1).getOrNull()
        return if (type != null && every != null) PlannedPaymentInterval(every, type) else null
    }

    /** A paid occurrence of a planned payment: when it was due, and the rule it's of. */
    private fun planned(ruleId: UUID, cashew: CashewTransaction) =
        NoMetadata.copy(recurringRuleId = ruleId, paidForDateTime = cashew.dueDate ?: cashew.date)
    // endregion

    internal companion object {
        /** Cashew's own category for balance corrections, transfers' halves among them. */
        const val BalanceCorrection = "0"

        // A series' occurrences after the first have its key followed by this and their number.
        const val SeriesKeySeparator = "::predict::"

        val Planned = setOf(CashewType.Upcoming, CashewType.Subscription, CashewType.Repetitive)
        val TransferArrows = listOf(" → ", " ← ")

        val NoMetadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanId = null,
            loanRecordId = null,
            purchaseDateTime = null,
        )

        /** How names are matched: whatever the case and the spaces around them. */
        fun nameKey(name: String): String = name.trim().lowercase()

        /** The items whose key no other item shares, by that key. */
        fun <T> List<T>.uniqueBy(key: (T) -> String): Map<String, T> =
            groupBy(key).filterValues { it.size == 1 }.mapValues { it.value.single() }
    }
}
