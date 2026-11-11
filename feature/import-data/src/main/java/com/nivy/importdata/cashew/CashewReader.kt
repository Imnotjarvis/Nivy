package com.nivy.importdata.cashew

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import java.time.Instant

/** Cashew's database, a table at a time. */
internal interface CashewTables {
    /**
     * Each row of [table] as [read] makes it from the row's values by column: a number, a text or
     * null. None when the database has no such table. A row is read and let go: a table of many
     * thousand transactions is never in memory twice.
     */
    fun <T> rows(table: String, read: (Map<String, Any?>) -> T): List<T>
}

/** The file isn't Cashew's: it has none of the tables Cashew's data file has. */
internal class NotCashewData : IllegalArgumentException("Not a Cashew data file")

/**
 * Reads what the import needs from Cashew's [tables]. A column an older version of Cashew didn't
 * have yet reads as its default: no pair for a transfer, no long-term loan.
 *
 * @throws NotCashewData when the database has no wallets.
 */
internal fun readCashew(tables: CashewTables): CashewData {
    val wallets = tables.rows("wallets") { Row(it).wallet() }
    if (wallets.isEmpty()) throw NotCashewData()
    return CashewData(
        wallets = wallets,
        categories = tables.rows("categories") { Row(it).category() },
        transactions = tables.rows("transactions") { Row(it).transaction() },
        objectives = tables.rows("objectives") { Row(it).objective() },
        budgets = tables.rows("budgets") { Row(it).budget() },
        limitedBudgets = tables.rows("category_budget_limits") { Row(it).text("budget_fk") }.toSet(),
        // Cashew writes its settings in the database's first row when it exports the file.
        settings = tables.rows("app_settings") { Row(it).long("settings_pk") to Row(it).text("settings_j_s_o_n") }
            .minByOrNull { (pk, _) -> pk }
            ?.let { (_, json) -> settings(json) }
            ?: NoSettings,
    )
}

private val NoSettings = CashewSettings(primaryWalletPk = null, rates = emptyMap(), customRates = emptyMap())

private fun Row.wallet() = CashewWallet(
    pk = text("wallet_pk"),
    name = text("name"),
    colour = textOrNull("colour"),
    iconName = textOrNull("icon_name"),
    order = long("order").toInt(),
    currency = textOrNull("currency"),
)

private fun Row.category() = CashewCategory(
    pk = text("category_pk"),
    name = text("name"),
    colour = textOrNull("colour"),
    iconName = textOrNull("icon_name"),
    order = long("order").toInt(),
    mainCategoryPk = textOrNull("main_category_pk"),
)

private fun Row.transaction() = CashewTransaction(
    pk = text("transaction_pk"),
    pairedPk = textOrNull("paired_transaction_fk"),
    name = text("name"),
    amount = double("amount"),
    note = text("note"),
    categoryPk = text("category_fk"),
    subCategoryPk = textOrNull("sub_category_fk"),
    walletPk = text("wallet_fk"),
    date = instant("date_created"),
    modified = instantOrNull("date_time_modified"),
    dueDate = instantOrNull("original_date_due"),
    repeatEvery = longOrNull("period_length")?.toInt(),
    repeat = longOrNull("reoccurrence")?.let { CashewRepeat.entries.getOrNull(it.toInt()) },
    endDate = instantOrNull("end_date"),
    type = longOrNull("type")?.let { CashewType.entries.getOrNull(it.toInt()) },
    paid = bool("paid"),
    // Cashew sets skip_paid on every ordinary transaction it makes. Only a planned one that isn't
    // paid is skipped by it, as Cashew's own lists read it.
    skipped = bool("skip_paid") && longOrNull("type") != null && !bool("paid"),
    goalPk = textOrNull("objective_fk"),
    loanPk = textOrNull("objective_loan_fk"),
)

private fun Row.objective() = CashewObjective(
    pk = text("objective_pk"),
    // Goals were Cashew's only objectives before long-term loans came.
    loan = longOrNull("type") == ObjectiveLoan,
    name = text("name"),
    colour = textOrNull("colour"),
    iconName = textOrNull("icon_name"),
    order = long("order").toInt(),
    date = instant("date_created"),
    income = bool("income"),
    walletPk = textOrNull("wallet_fk"),
)

private fun Row.budget() = CashewBudget(
    pk = text("budget_pk"),
    name = text("name"),
    amount = double("amount"),
    walletPk = textOrNull("wallet_fk"),
    walletPks = list("wallet_fks"),
    categoryPks = list("category_fks"),
    excludedCategoryPks = list("category_fks_exclude"),
    income = bool("income"),
    archived = bool("archived"),
    addedTransactionsOnly = bool("added_transactions_only"),
    repeatEvery = long("period_length").toInt(),
    repeat = longOrNull("reoccurrence")?.let { CashewRepeat.entries.getOrNull(it.toInt()) },
    order = long("order").toInt(),
)

private const val ObjectiveLoan = 1L

/**
 * Cashew's settings, a JSON object: the primary wallet, whose currency Cashew totals in, and its
 * exchange rates. Settings it can't read leave the import without rates.
 */
private fun settings(json: String): CashewSettings = try {
    val settings = Json.parseToJsonElement(json).jsonObject
    CashewSettings(
        primaryWalletPk = (settings["selectedWalletPk"] as? JsonPrimitive)?.content,
        rates = settings.rates("cachedCurrencyExchange"),
        customRates = settings.rates("customCurrencyAmounts"),
    )
} catch (ignored: IllegalArgumentException) {
    // Not JSON (SerializationException is one), or not an object at its top.
    NoSettings
}

private fun JsonObject.rates(key: String): Map<String, Double> =
    (this[key] as? JsonObject).orEmpty()
        .mapNotNull { (currency, rate) -> (rate as? JsonPrimitive)?.doubleOrNull?.let { currency to it } }
        .toMap()

/** A row's values by column, read as Cashew's database writes them. */
private class Row(private val values: Map<String, Any?>) {
    fun textOrNull(column: String): String? = values[column]?.toString()
    fun text(column: String): String = textOrNull(column).orEmpty()
    fun longOrNull(column: String): Long? = (values[column] as? Number)?.toLong()
    fun long(column: String): Long = longOrNull(column) ?: 0
    fun double(column: String): Double = (values[column] as? Number)?.toDouble() ?: 0.0
    fun bool(column: String): Boolean = long(column) != 0L

    // Dates are seconds since 1970.
    fun instantOrNull(column: String): Instant? = longOrNull(column)?.let(Instant::ofEpochSecond)
    fun instant(column: String): Instant = instantOrNull(column) ?: Instant.EPOCH

    /** A list Cashew keeps as a JSON array of texts; none when empty or unreadable. */
    fun list(column: String): List<String>? = textOrNull(column)?.let { json ->
        try {
            (Json.parseToJsonElement(json) as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.content }
                ?.takeIf { it.isNotEmpty() }
        } catch (ignored: SerializationException) {
            null
        }
    }
}
