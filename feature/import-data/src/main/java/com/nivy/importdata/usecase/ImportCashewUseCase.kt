package com.nivy.importdata.usecase

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeProvider
import com.nivy.data.DataObserver
import com.nivy.data.DataWriteEvent
import com.nivy.data.backup.ImportResult
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.file.FileSystem
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.BudgetRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.ExchangeRatesRepository
import com.nivy.data.repository.LoanRepository
import com.nivy.data.repository.PlannedPaymentRuleRepository
import com.nivy.data.repository.TagRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.planned.PlannedPaymentsGenerator
import com.nivy.importdata.cashew.CashewData
import com.nivy.importdata.cashew.CashewFileReader
import com.nivy.importdata.cashew.CashewPlan
import com.nivy.importdata.cashew.LeftOut
import com.nivy.importdata.cashew.NivyWallet
import com.nivy.importdata.cashew.NotCashewData
import com.nivy.importdata.cashew.planCashewImport
import com.nivy.ui.R
import com.nivy.wallet.domain.data.NivyCurrency
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * An import's counts, as for any import, those of what only Cashew's import brings, and what of
 * Cashew's it left out.
 */
data class CashewImportResult(
    val result: ImportResult,
    val plannedPayments: Int = 0,
    val loans: Int = 0,
    val budgets: Int = 0,
    val tags: Int = 0,
    val leftOut: List<LeftOut> = emptyList(),
)

// How far the import is (0 to 1) after each of its stages.
private object Progress {
    const val COPIED = 0.1
    const val READ = 0.3
    const val TRANSACTIONS_SAVED = 0.6
    const val SAVED = 0.9
}

/**
 * Imports Cashew's data file, what its "Export Cashew Data File" writes: its whole database. What
 * comes over and how is [planCashewImport]'s; it's written in one database transaction, so an
 * import that fails leaves the wallet as it was.
 */
internal class ImportCashewUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileSystem: FileSystem,
    private val cashewFile: CashewFileReader,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val transactionRepository: TransactionRepository,
    private val plannedPaymentRuleRepository: PlannedPaymentRuleRepository,
    private val plannedPaymentsGenerator: PlannedPaymentsGenerator,
    private val loanRepository: LoanRepository,
    private val budgetRepository: BudgetRepository,
    private val exchangeRatesRepository: ExchangeRatesRepository,
    private val currencyRepository: CurrencyRepository,
    private val appPreferences: AppPreferences,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatchersProvider,
    private val databaseTransaction: DatabaseTransaction,
    private val dataObserver: DataObserver,
) {
    // Whatever fails is the result's error, shown to the user.
    @Suppress("TooGenericExceptionCaught")
    suspend fun import(uri: Uri, onProgress: suspend (Double) -> Unit): CashewImportResult =
        withContext(dispatchers.io) {
            val file = File(context.cacheDir, "cashew${System.currentTimeMillis()}.db")
            try {
                fileSystem.copy(uri, file)
                onProgress(Progress.COPIED)
                val data = cashewFile.read(file)
                onProgress(Progress.READ)
                val wallet = nivyWallet(data)
                val plan = planCashewImport(data, wallet)
                write(plan, onProgress)
                if (wallet.baseCurrency != currencyRepository.getBaseCurrency()) {
                    appPreferences.setBaseCurrency(wallet.baseCurrency.code)
                }
                onProgress(1.0)
                CashewImportResult(
                    result = plan.result(),
                    plannedPayments = plan.plannedPayments.size,
                    loans = plan.loans.size,
                    budgets = plan.budgets.size,
                    tags = plan.tags.size,
                    leftOut = plan.leftOut,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: NotCashewData) {
                notCashew(e)
            } catch (e: SQLiteException) {
                // Not a database at all.
                notCashew(e)
            } catch (e: Exception) {
                // The database is as it was: the import's writes are one transaction.
                Timber.e(e, "Cashew import failed")
                failed(e.message ?: e.javaClass.simpleName)
            } finally {
                // The copy, and the files SQLite may have made beside it.
                listOf("", "-journal", "-wal", "-shm").forEach { File(file.path + it).delete() }
                // Whatever happened, what's in memory may not match the database any more.
                dataObserver.post(DataWriteEvent.AllDataChange)
            }
        }

    /**
     * The wallet the import lands in. One without accounts takes Cashew's main currency, as a
     * restored backup takes its own: what Cashew totalled in, and what onboarding offers next.
     */
    private suspend fun nivyWallet(data: CashewData): NivyWallet {
        val accounts = accountRepository.findAll()
        val base = data.primaryCurrency()?.takeIf { accounts.isEmpty() } ?: currencyRepository.getBaseCurrency()
        return NivyWallet(
            accounts = accounts,
            categories = categoryRepository.findAll(),
            tags = tagRepository.findAll(),
            manualRates = exchangeRatesRepository.findAllManuallyOverridden(),
            baseCurrency = base,
            zone = timeProvider.getZoneId(),
            now = timeProvider.utcNow(),
            lastLoanOrder = loanRepository.findMaxOrderNum(),
            lastBudgetOrder = budgetRepository.findMaxOrderNum(),
        )
    }

    /** The currency of Cashew's primary wallet, when Nivy knows it. */
    private fun CashewData.primaryCurrency(): AssetCode? =
        wallets.find { it.pk == settings.primaryWalletPk }?.currency
            ?.let { AssetCode.from(it).getOrNull() }
            ?.takeIf { NivyCurrency.fromCode(it.code) != null }

    private suspend fun write(plan: CashewPlan, onProgress: suspend (Double) -> Unit) {
        databaseTransaction.run {
            accountRepository.saveMany(plan.accounts)
            plan.categories.forEach { categoryRepository.save(it) }
            plan.tags.forEach { tagRepository.save(it) }
            transactionRepository.saveMany(plan.transactions)
            tagRepository.associateMany(plan.tagLinks)
            onProgress(Progress.TRANSACTIONS_SAVED)
            loanRepository.saveMany(plan.loans)
            plan.loanRecords.forEach { loanRepository.saveRecord(it) }
            // Each planned payment plans its unpaid occurrences from its first, as one made in the app.
            plan.plannedPayments.forEach {
                plannedPaymentRuleRepository.save(it)
                plannedPaymentsGenerator.generate(it, previous = null)
            }
            budgetRepository.saveMany(plan.budgets)
            plan.rates.forEach { exchangeRatesRepository.save(it) }
            onProgress(Progress.SAVED)
        }
    }

    private fun CashewPlan.result() = ImportResult(
        rowsFound = found,
        transactionsImported = imported,
        accountsImported = accounts.size,
        categoriesImported = categories.size,
        failedRows = persistentListOf(),
    )

    private fun notCashew(e: Exception): CashewImportResult {
        Timber.w(e, "Not a Cashew data file")
        return failed(context.getString(R.string.cashew_not_data_file))
    }

    private fun failed(error: String) = CashewImportResult(
        result = ImportResult(
            rowsFound = 0,
            transactionsImported = 0,
            accountsImported = 0,
            categoriesImported = 0,
            failedRows = persistentListOf(),
            error = error,
        ),
    )
}
