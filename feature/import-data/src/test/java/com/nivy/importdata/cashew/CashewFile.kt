package com.nivy.importdata.cashew

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant

/**
 * A Cashew data file for tests: a real SQLite database with Cashew's tables as its schema
 * [version] has them (`resources/cashew/schema-*.sql`), and rows added the way Cashew writes them.
 * A value for a column the version doesn't have yet is left out, as that version would.
 */
internal class CashewFile(version: Int = 46) : AutoCloseable {
    val file: File = File.createTempFile("cashew", ".sql")
    private val connection: Connection = DriverManager.getConnection("jdbc:sqlite:${file.path}")

    init {
        val schema = checkNotNull(javaClass.classLoader?.getResource("cashew/schema-$version.sql")) {
            "No schema $version"
        }.readText()
        connection.createStatement().use { statement ->
            schema.lines().filter { it.isNotBlank() && !it.startsWith("--") }.forEach(statement::execute)
        }
    }

    /** The tables as the import reads them. */
    val tables: CashewTables = JdbcCashewTables(connection)

    fun wallet(pk: String, name: String, currency: String? = "eur", colour: String? = null, order: Int = 0) =
        insert(
            "wallets",
            "wallet_pk" to pk, "name" to name, "currency" to currency, "colour" to colour,
            "order" to order, "date_created" to Created,
        )

    fun category(
        pk: String,
        name: String,
        colour: String? = "0xff4caf50",
        icon: String? = "cutlery.png",
        order: Int = 0,
        main: String? = null,
    ) = insert(
        "categories",
        "category_pk" to pk, "name" to name, "colour" to colour, "icon_name" to icon, "order" to order,
        "main_category_pk" to main, "date_created" to Created,
    )

    @Suppress("LongParameterList") // one per column a test sets
    fun transaction(
        pk: String,
        amount: Double,
        wallet: String = "w1",
        category: String = "c1",
        date: String = "2026-10-05T13:00:00Z",
        name: String = "",
        note: String = "",
        paid: Boolean = true,
        type: CashewType? = null,
        subCategory: String? = null,
        paired: String? = null,
        repeat: CashewRepeat? = null,
        repeatEvery: Int? = null,
        dueDate: String? = null,
        endDate: String? = null,
        skipped: Boolean = false,
        goal: String? = null,
        loan: String? = null,
        modified: String? = null,
    ) = insert(
        "transactions",
        "transaction_pk" to pk, "amount" to amount, "wallet_fk" to wallet, "category_fk" to category,
        "date_created" to seconds(date), "name" to name, "note" to note, "income" to (amount > 0),
        "paid" to paid, "type" to type?.ordinal, "sub_category_fk" to subCategory,
        "paired_transaction_fk" to paired, "reoccurrence" to repeat?.ordinal, "period_length" to repeatEvery,
        "original_date_due" to dueDate?.let(::seconds), "end_date" to endDate?.let(::seconds),
        "skip_paid" to skipped, "objective_fk" to goal, "objective_loan_fk" to loan,
        "date_time_modified" to modified?.let(::seconds),
    )

    fun objective(
        pk: String,
        name: String,
        loan: Boolean = false,
        income: Boolean = false,
        wallet: String = "w1",
        date: String = "2026-09-01T10:00:00Z",
    ) = insert(
        "objectives",
        "objective_pk" to pk, "name" to name, "type" to if (loan) 1 else 0, "income" to income,
        "wallet_fk" to wallet, "amount" to 0.0, "order" to 0, "date_created" to seconds(date),
    )

    @Suppress("LongParameterList") // one per column a test sets
    fun budget(
        pk: String,
        name: String,
        amount: Double,
        repeat: CashewRepeat? = CashewRepeat.Monthly,
        repeatEvery: Int = 1,
        wallet: String = "w1",
        wallets: String? = null,
        categories: String? = null,
        excluded: String? = null,
        income: Boolean = false,
        archived: Boolean = false,
        addedOnly: Boolean = false,
    ) = insert(
        "budgets",
        "budget_pk" to pk, "name" to name, "amount" to amount, "reoccurrence" to repeat?.ordinal,
        "period_length" to repeatEvery, "wallet_fk" to wallet, "wallet_fks" to wallets,
        "category_fks" to categories, "category_fks_exclude" to excluded, "income" to income,
        "archived" to archived, "added_transactions_only" to addedOnly, "start_date" to Created,
        "end_date" to Created, "date_created" to Created, "order" to 0,
    )

    fun categoryLimit(pk: String, budget: String, category: String) = insert(
        "category_budget_limits",
        "category_limit_pk" to pk, "budget_fk" to budget, "category_fk" to category, "amount" to 10.0,
    )

    fun settings(json: String) = insert(
        "app_settings",
        "settings_pk" to 0, "settings_j_s_o_n" to json, "date_updated" to Created,
    )

    private fun insert(table: String, vararg values: Pair<String, Any?>) {
        val columns = columnsOf(table)
        val kept = values.filter { (column, _) -> column in columns }
        val names = kept.joinToString { "\"${it.first}\"" }
        val marks = kept.joinToString { "?" }
        connection.prepareStatement("INSERT INTO \"$table\" ($names) VALUES ($marks)").use { statement ->
            kept.forEachIndexed { index, (_, value) ->
                statement.setObject(index + 1, if (value is Boolean) (if (value) 1 else 0) else value)
            }
            statement.executeUpdate()
        }
    }

    private fun columnsOf(table: String): Set<String> =
        connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA table_info(\"$table\")").use { result ->
                buildSet { while (result.next()) add(result.getString("name")) }
            }
        }

    override fun close() {
        connection.close()
        file.delete()
    }

    companion object {
        private val Created = seconds("2026-01-10T09:00:00Z")

        fun seconds(date: String): Long = Instant.parse(date).epochSecond
    }
}

/** Cashew's tables read with JDBC, as the app reads them with Android's SQLite. */
internal class JdbcCashewTables(private val connection: Connection) : CashewTables {
    override fun rows(table: String): List<Map<String, Any?>> {
        val exists = connection.prepareStatement(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?"
        ).use { it.setString(1, table); it.executeQuery().use { result -> result.next() } }
        if (!exists) return emptyList()
        return connection.createStatement().use { statement ->
            statement.executeQuery("SELECT * FROM \"$table\"").use { result ->
                val meta = result.metaData
                buildList {
                    while (result.next()) {
                        add((1..meta.columnCount).associate { meta.getColumnName(it) to result.getObject(it) })
                    }
                }
            }
        }
    }
}
