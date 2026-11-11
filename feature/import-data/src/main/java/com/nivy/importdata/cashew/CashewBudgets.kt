package com.nivy.importdata.cashew

import com.nivy.data.model.Budget
import com.nivy.data.model.BudgetId
import com.nivy.data.model.CategoryId
import com.nivy.data.model.ExchangeRate
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.importdata.cashew.LeftOut.Reason
import java.util.UUID

/**
 * Cashew's budgets that a Nivy budget can be: monthly, of what's spent in their categories and
 * wallets. Cashew keeps a budget's amount in its wallet's currency, Nivy in the base currency.
 */
internal fun CashewPlanner.planBudgets(): List<Budget> =
    data.budgets.sortedBy { it.order }.mapNotNull { budget(it, order = wallet.lastBudgetOrder + 1 + it.order) }

private fun CashewPlanner.budget(cashew: CashewBudget, order: Double): Budget? {
    val name = cashew.name.trim().ifEmpty { DefaultBudgetName }
    val item = LeftOut.Item(name)
    val monthly = cashew.repeat == CashewRepeat.Monthly && cashew.repeatEvery == 1
    // Not an income budget, one counting only what was added to it, nor one put away.
    val ofWhatsSpent = !cashew.income && !cashew.addedTransactionsOnly && !cashew.archived
    val categories = budgetCategories(cashew)
    val amount = budgetAmount(cashew)
    val kept = monthly && ofWhatsSpent && categories != null && cashew.amount > 0
    return when {
        !kept -> null.also { leave(Reason.Budget, item) }
        amount == null -> null.also { leave(Reason.BudgetWithoutRate, item) }
        else -> Budget(
            id = BudgetId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            amount = amount,
            categories = categories.orEmpty(),
            accounts = cashew.walletPks?.mapNotNull { accounts[it]?.id }?.distinct().orEmpty(),
            orderNum = order,
        ).also { if (cashew.pk in data.limitedBudgets) leave(Reason.CategoryLimits, item) }
    }
}

/**
 * A budget's categories: the ones it names, or all but the ones it leaves out; none for all of
 * them. Null when none of its categories came over.
 */
private fun CashewPlanner.budgetCategories(cashew: CashewBudget): List<CategoryId>? {
    val included = cashew.categoryPks
    val excluded = cashew.excludedCategoryPks
    return when {
        included != null -> included.mapNotNull { categories[it]?.id }.distinct().takeIf { it.isNotEmpty() }
        excluded != null -> {
            val out = excluded.mapNotNullTo(HashSet()) { categories[it]?.id }
            categories.values.map { it.id }.distinct().filterNot { it in out }.takeIf { it.isNotEmpty() }
        }
        else -> emptyList()
    }
}

/** A budget's amount in the base currency; none without a rate for its wallet's currency. */
private fun CashewPlanner.budgetAmount(cashew: CashewBudget): PositiveDouble? {
    val asset = cashew.walletPk?.let(accounts::get)?.asset ?: wallet.baseCurrency
    val base = wallet.baseCurrency
    val amount = if (asset == base) {
        cashew.amount
    } else {
        cashewRate(base)?.let { baseRate -> cashewRate(asset)?.let { cashew.amount * baseRate / it } }
    }
    return amount?.let { PositiveDouble.from(it).getOrNull() }
}

/**
 * The rates set by hand in Cashew for the currencies of the accounts it brings, as Nivy's rates
 * entered by hand. A rate the wallet has entered already stays as it is.
 */
internal fun CashewPlanner.planRates(): List<ExchangeRate> {
    val base = wallet.baseCurrency
    val baseRate = cashewRate(base) ?: return emptyList()
    return accounts.values.map { it.asset }.distinct()
        .filter { asset -> asset != base && !wallet.hasManualRate(base, asset) }
        .mapNotNull { asset ->
            cashewRate(asset, setByHand = true)
                ?.let { PositiveDouble.from(it / baseRate).getOrNull() }
                ?.let { ExchangeRate(baseCurrency = base, currency = asset, rate = it, manualOverride = true) }
        }
}

/**
 * One US dollar in [asset] at Cashew's rates, [setByHand] or not, under the key of a wallet in
 * that currency or the code in small letters. None without a rate, except for the dollar itself.
 */
internal fun CashewPlanner.cashewRate(asset: AssetCode, setByHand: Boolean = false): Double? {
    val keys = data.wallets.mapNotNull { it.currency }.filter { currency(it) == asset } + asset.code.lowercase()
    // The ones set by hand win, as in Cashew.
    val rates = if (setByHand) data.settings.customRates else data.settings.rates + data.settings.customRates
    return keys.firstNotNullOfOrNull { rates[it] } ?: if (asset == AssetCode.USD && !setByHand) 1.0 else null
}

private fun NivyWallet.hasManualRate(base: AssetCode, currency: AssetCode): Boolean =
    manualRates.any { it.baseCurrency == base && it.currency == currency }

private const val DefaultBudgetName = "Budget"
