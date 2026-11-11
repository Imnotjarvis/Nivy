package com.nivy.importdata.cashew

import java.time.Instant

/**
 * What the import reads from a Cashew data file: its rows as Cashew keeps them. Cashew is
 * written in Flutter and keeps everything in one SQLite database; its "data file" export is that
 * database. Amounts are signed (below zero spends, above zero earns), dates are moments, and a
 * wallet's currency is Cashew's lower-case key ("eur").
 */
internal data class CashewData(
    val wallets: List<CashewWallet>,
    val categories: List<CashewCategory>,
    val transactions: List<CashewTransaction>,
    val objectives: List<CashewObjective>,
    val budgets: List<CashewBudget>,
    /** The budgets that limit some of their categories' spending, which no Nivy budget does. */
    val limitedBudgets: Set<String>,
    val settings: CashewSettings,
)

internal data class CashewWallet(
    val pk: String,
    val name: String,
    /** "0xffRRGGBB", or none. */
    val colour: String?,
    val iconName: String?,
    val order: Int,
    val currency: String?,
)

internal data class CashewCategory(
    val pk: String,
    val name: String,
    val colour: String?,
    val iconName: String?,
    val order: Int,
    /** The main category of a subcategory; none for a main category. */
    val mainCategoryPk: String?,
)

internal data class CashewTransaction(
    val pk: String,
    /** A transfer's other half: on the half the money left, the pk of the half it arrived in. */
    val pairedPk: String?,
    val name: String,
    val amount: Double,
    val note: String,
    val categoryPk: String,
    val subCategoryPk: String?,
    val walletPk: String,
    val date: Instant,
    /** When it last changed: for a lent or borrowed amount marked as returned, about when that was. */
    val modified: Instant?,
    /** For a paid occurrence of something planned, the day it was due. */
    val dueDate: Instant?,
    val repeatEvery: Int?,
    val repeat: CashewRepeat?,
    val endDate: Instant?,
    val type: CashewType?,
    val paid: Boolean,
    val skipped: Boolean,
    /** The goal it counts towards. */
    val goalPk: String?,
    /** The long-term loan it belongs to. */
    val loanPk: String?,
)

/** A goal, or a long-term loan. */
internal data class CashewObjective(
    val pk: String,
    val loan: Boolean,
    val name: String,
    val colour: String?,
    val iconName: String?,
    val order: Int,
    val date: Instant,
    /** For a loan: the money was lent. Borrowed otherwise. */
    val income: Boolean,
    val walletPk: String?,
)

internal data class CashewBudget(
    val pk: String,
    val name: String,
    val amount: Double,
    /** The wallet whose currency the amount is in. */
    val walletPk: String?,
    val walletPks: List<String>?,
    val categoryPks: List<String>?,
    val excludedCategoryPks: List<String>?,
    val income: Boolean,
    val archived: Boolean,
    val addedTransactionsOnly: Boolean,
    val repeatEvery: Int,
    val repeat: CashewRepeat?,
    val order: Int,
)

/**
 * Cashew's settings that the import uses. Its rates are each currency's units for one US dollar,
 * by lower-case key: [customRates] the ones set by hand, [rates] the last downloaded.
 */
internal data class CashewSettings(
    val primaryWalletPk: String?,
    val rates: Map<String, Double>,
    val customRates: Map<String, Double>,
) {
    /** One US dollar in [currency], as Cashew converts it; none when Cashew has no rate. */
    fun rate(currency: String): Double? = customRates[currency] ?: rates[currency]
}

/** Cashew's kinds of planned and lent amounts, in its order. */
internal enum class CashewType { Upcoming, Subscription, Repetitive, Credit, Debt }

/** How often something repeats in Cashew, in its order. */
internal enum class CashewRepeat { Custom, Daily, Weekly, Monthly, Yearly }
