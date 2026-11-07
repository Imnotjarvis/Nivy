package com.nivy.domain.usecase.csv

import android.net.Uri
import arrow.core.Either
import com.nivy.base.model.TransactionType
import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeConverter
import com.nivy.data.file.FileSystem
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.NonNegativeDouble
import com.nivy.data.model.primitive.toNonNegative
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.wallet.domain.data.NivyCurrency
import kotlinx.coroutines.withContext
import org.apache.commons.text.StringEscapeUtils
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class ExportCsvUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val dispatchers: DispatchersProvider,
    private val fileSystem: FileSystem,
    private val timeConverter: TimeConverter
) {

    suspend fun exportToFile(
        outputFile: Uri,
        exportScope: suspend TransactionRepository.() -> List<Transaction> = {
            transactionRepository.findAll()
        }
    ): Either<FileSystem.Failure, Unit> = withContext(dispatchers.io) {
        val csv = exportCsv(exportScope)
        fileSystem.writeToFile(outputFile, csv)
    }

    suspend fun exportCsv(
        exportScope: suspend TransactionRepository.() -> List<Transaction>
    ): String = withContext(dispatchers.io) {
        val transactions = transactionRepository.exportScope()
        val accountsMap = accountRepository.findAll().associateBy(Account::id)
        val categoriesMap = categoryRepository.findAll().associateBy(Category::id)

        buildString {
            append(NivyCsvRow.Columns.joinToString(separator = CSV_SEPARATOR))
            append(NEWLINE)
            for (trn in transactions) {
                append(
                    trn.toNivyCsvRow().toCsvString(
                        accountsMap = accountsMap,
                        categoriesMap = categoriesMap
                    )
                )
                append(NEWLINE)
            }
        }
    }

    private fun NivyCsvRow.toCsvString(
        accountsMap: Map<AccountId, Account>,
        categoriesMap: Map<CategoryId, Category>,
    ): String = csvRow {
        // Date
        csvAppend(date?.csvFormat(timeConverter))
        // Title
        csvAppend(title?.value)
        // Category
        csvAppend(categoriesMap[category]?.name?.value)
        // Account
        csvAppend(accountsMap[account]?.name?.value)
        // Amount
        csvAppend(amount.value.csvFormat(currency))
        // Currency
        csvAppend(currency.code)
        // Type
        csvAppend(type.name)
        // Transfer Amount
        csvAppend(transferCurrency?.let { transferAmount?.value?.csvFormat(it) })
        // Transfer Currency
        csvAppend(transferCurrency?.code)
        // To Account
        csvAppend(accountsMap[toAccountId]?.name?.value)
        // Receive Amount
        csvAppend(receiveCurrency?.let { receiveAmount?.value?.csvFormat(it) })
        // Receive Currency
        csvAppend(receiveCurrency?.code)
        // Description
        csvAppend(description?.value)
        // Due Date
        csvAppend(dueData?.csvFormat(timeConverter))
        // ID
        csvAppend(id.value.toString())
    }

    private fun csvRow(build: CsvRowScope.() -> Unit): String {
        val columns = mutableListOf<String>()
        val rowScope = object : CsvRowScope {
            override fun csvAppend(value: String?) {
                columns.add(value?.let(::escapeCsvCell) ?: "")
            }
        }
        rowScope.build()
        return columns.joinToString(separator = CSV_SEPARATOR)
    }

    /**
     * Quotes the cell per RFC 4180 and keeps it from being run as a formula; see [CsvFormulaGuard].
     * [ReadCsvUseCase] reverses both, so the data comes back exactly as it went out.
     */
    private fun escapeCsvCell(value: String): String =
        StringEscapeUtils.escapeCsv(CsvFormulaGuard.escape(value))

    private fun Transaction.toNivyCsvRow(): NivyCsvRow = when (this) {
        is Expense -> expenseCsvRow()
        is Income -> incomeCsvRow()
        is Transfer -> transferCsvRow()
    }

    private fun Expense.expenseCsvRow(): NivyCsvRow = NivyCsvRow(
        date = time.takeIf
        { settled },
        title = title,
        category = category,
        account = account,
        amount = value.amount.toNonNegative(),
        currency = value.asset,
        type = TransactionType.EXPENSE,
        transferAmount = null,
        transferCurrency = null,
        toAccountId = null,
        receiveAmount = null,
        receiveCurrency = null,
        description = description,
        dueData = time.takeIf
        { !settled },
        id = id
    )

    private fun Income.incomeCsvRow(): NivyCsvRow = NivyCsvRow(
        date = time.takeIf { settled },
        title = title,
        category = category,
        account = account,
        amount = value.amount.toNonNegative(),
        currency = value.asset,
        type = TransactionType.INCOME,
        transferAmount = null,
        transferCurrency = null,
        toAccountId = null,
        receiveAmount = null,
        receiveCurrency = null,
        description = description,
        dueData = time.takeIf { !settled },
        id = id
    )

    private fun Transfer.transferCsvRow(): NivyCsvRow = NivyCsvRow(
        date = time.takeIf { settled },
        title = title,
        category = category,
        account = fromAccount,
        amount = NonNegativeDouble.unsafe(0.0),
        currency = fromValue.asset,
        type = TransactionType.TRANSFER,
        transferAmount = fromValue.amount,
        transferCurrency = fromValue.asset,
        toAccountId = toAccount,
        receiveAmount = toValue.amount,
        receiveCurrency = toValue.asset,
        description = description,
        dueData = time.takeIf { !settled },
        id = id
    )

    private fun Instant.csvFormat(timeConverter: TimeConverter): String {
        return with(timeConverter) {
            this@csvFormat.toLocalDateTime()
        }.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    // Fiat keeps two decimals; crypto keeps as many as the app stores, or 0.00042 BTC is lost.
    private fun Double.csvFormat(asset: AssetCode): String = DecimalFormat(NUMBER_FORMAT).apply {
        decimalFormatSymbols = DecimalFormatSymbols.getInstance(Locale.ENGLISH)
        maximumFractionDigits = maxOf(minimumFractionDigits, NivyCurrency.getDecimalPlaces(asset.code))
    }.format(this)

    interface CsvRowScope {
        fun csvAppend(value: String?)
    }

    companion object {
        private const val CSV_SEPARATOR = ","
        private const val NEWLINE = "\n"
        private const val NUMBER_FORMAT = "#,##0.00"
    }
}
