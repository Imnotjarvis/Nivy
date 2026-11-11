package com.nivy.importdata.cashew

import com.nivy.data.model.Account
import com.nivy.data.model.Budget
import com.nivy.data.model.Category
import com.nivy.data.model.ExchangeRate
import com.nivy.data.model.Loan
import com.nivy.data.model.LoanRecord
import com.nivy.data.model.PlannedPaymentRule
import com.nivy.data.model.Tag
import com.nivy.data.model.TagAssociation
import com.nivy.data.model.Transaction
import com.nivy.data.model.primitive.AssetCode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What's in Nivy already: the import lands on its accounts, categories and tags of the same name. */
internal data class NivyWallet(
    val accounts: List<Account>,
    val categories: List<Category>,
    val tags: List<Tag>,
    /** The exchange rates entered by hand: the import keeps them. */
    val manualRates: List<ExchangeRate>,
    val baseCurrency: AssetCode,
    val zone: ZoneId,
    val now: Instant,
    val lastLoanOrder: Double,
    val lastBudgetOrder: Double,
)

/**
 * What the import writes, and what of Cashew's it leaves out. The planned payments' unpaid
 * occurrences aren't in it: Nivy plans them from each rule, as for a rule made in the app.
 */
internal data class CashewPlan(
    val accounts: List<Account>,
    val categories: List<Category>,
    val tags: List<Tag>,
    val transactions: List<Transaction>,
    /** Each transaction's tags: its Cashew subcategory's, its goal's. */
    val tagLinks: List<TagAssociation>,
    val plannedPayments: List<PlannedPaymentRule>,
    val loans: List<Loan>,
    val loanRecords: List<LoanRecord>,
    val budgets: List<Budget>,
    val rates: List<ExchangeRate>,
    /** Cashew's transactions that count in its balances, and how many of them come over. */
    val found: Int,
    val imported: Int,
    val leftOut: List<LeftOut>,
)

/** Something of Cashew's that doesn't come over, or not as it was, with what names it: the result page lists it. */
data class LeftOut(val reason: Reason, val items: List<Item>) {
    data class Item(val name: String, val date: LocalDate? = null)

    enum class Reason {
        /** Transactions of no amount: a Nivy transaction has one. */
        NoAmount,

        /** Transactions Cashew holds as not paid, outside anything planned. */
        NotPaid,

        /** Transactions of a wallet that isn't in the file. */
        NoAccount,

        /** Planned payments that end in Cashew: Nivy's repeat until they're deleted. */
        EndsInCashew,

        /** Planned transfers: Nivy plans incomes and expenses. */
        PlannedTransfer,

        /** Planned payments that repeat in a way Nivy's don't. */
        CantRepeat,

        /** Budgets that aren't monthly, or count income, or only what was added to them. */
        Budget,

        /** Budgets' spending limits per category. */
        CategoryLimits,

        /** Amounts in another currency without a rate: a budget's, or a loan payment's. */
        NoRate,
    }
}
