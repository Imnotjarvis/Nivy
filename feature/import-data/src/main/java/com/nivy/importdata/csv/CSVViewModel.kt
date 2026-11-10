package com.nivy.importdata.csv

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nivy.base.Toaster
import com.nivy.base.threading.DispatchersProvider
import com.nivy.importdata.csv.domain.CSVImporterV2
import com.nivy.importdata.csv.domain.SAMPLE_SIZE
import com.nivy.importdata.csv.domain.parseAmount
import com.nivy.importdata.csv.domain.parseDate
import com.nivy.importdata.csv.domain.parseStatus
import com.nivy.importdata.csv.domain.parseText
import com.nivy.importdata.csv.domain.parseTransactionType
import com.nivy.navigation.Navigation
import com.nivy.data.file.FileSystem
import com.nivy.domain.usecase.csv.ReadCsvUseCase
import com.nivy.ui.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.nio.charset.Charset
import javax.inject.Inject
import kotlin.math.roundToInt

// A CSV that splits into fewer columns than this was read with the wrong separator: it's read again
// with semicolons, which spreadsheets use where decimals are written with a comma ("12,50").
private const val MinColumns = 3

private const val Percent = 100

@HiltViewModel
class CSVViewModel @Inject constructor(
    private val fileReader: FileSystem,
    private val readCsv: ReadCsvUseCase,
    private val csvImporter: CSVImporterV2,
    private val nav: Navigation,
    private val dispatchers: DispatchersProvider,
    private val toaster: Toaster,
) : ViewModel() {

    private var columns by mutableStateOf<CSVRow?>(null)
    private var csv by mutableStateOf<ImmutableList<CSVRow>?>(null)

    // region Important fields
    private var amount by mutableStateOf(Initial.amount)
    private var type by mutableStateOf(Initial.type)
    private var date by mutableStateOf(Initial.date)
    private var account by mutableStateOf(Initial.account)
    private var accountCurrency by mutableStateOf(Initial.accountCurrency)
    // endregion

    // region Transfer fields
    private var toAccount by mutableStateOf(Initial.toAccount)
    private var toAccountCurrency by mutableStateOf(Initial.toAccountCurrency)
    private var toAmount by mutableStateOf(Initial.toAmount)
    // endregion

    // region Optional fields
    private var category by mutableStateOf(Initial.category)
    private var title by mutableStateOf(Initial.title)
    private var description by mutableStateOf(Initial.description)
    // endregion

    private var uiState by mutableStateOf<UIState>(UIState.Idle)

    // A new file starts with nothing mapped: the last one's columns may be elsewhere in it.
    private fun resetMappings() {
        amount = Initial.amount
        type = Initial.type
        date = Initial.date
        account = Initial.account
        accountCurrency = Initial.accountCurrency
        toAccount = Initial.toAccount
        toAccountCurrency = Initial.toAccountCurrency
        toAmount = Initial.toAmount
        category = Initial.category
        title = Initial.title
        description = Initial.description
    }

    /** The mappings a file starts with: no column chosen. */
    private object Initial {
        val amount = ColumnMapping(
            nivyColumn = R.string.csv_column_amount,
            helpInfo = R.string.csv_column_amount_help,
            name = "",
            index = -1,
            metadata = 1,
            required = true,
        )

        val type = ColumnMapping(
            nivyColumn = R.string.csv_column_type,
            helpInfo = R.string.csv_column_type_help,
            name = "",
            index = -1,
            required = true,
            metadata = TrnTypeMetadata(
                income = "",
                expense = "",
                transfer = null,
            )
        )

        val date = ColumnMapping(
            nivyColumn = R.string.csv_column_date,
            helpInfo = R.string.csv_column_date_help,
            name = "",
            index = -1,
            required = true,
            metadata = DateMetadata.DateFirst
        )

        val account = ColumnMapping(
            nivyColumn = R.string.csv_column_account,
            helpInfo = R.string.csv_column_account_help,
            name = "",
            index = -1,
            required = true,
            metadata = Unit,
        )

        val accountCurrency = ColumnMapping(
            nivyColumn = R.string.csv_column_account_currency,
            helpInfo = R.string.csv_column_account_currency_help,
            name = "",
            index = -1,
            required = false,
            metadata = Unit,
        )

        val toAccount = ColumnMapping(
            nivyColumn = R.string.csv_column_to_account,
            helpInfo = R.string.csv_column_to_account_help,
            name = "",
            index = -1,
            required = false,
            metadata = Unit,
        )

        val toAccountCurrency = ColumnMapping(
            nivyColumn = R.string.csv_column_to_account_currency,
            helpInfo = R.string.csv_column_to_account_currency_help,
            name = "",
            index = -1,
            required = false,
            metadata = Unit,
        )

        val toAmount = ColumnMapping(
            nivyColumn = R.string.csv_column_to_amount,
            helpInfo = R.string.csv_column_to_amount_help,
            name = "",
            index = -1,
            required = false,
            metadata = 1,
        )

        val category = ColumnMapping(
            nivyColumn = R.string.csv_column_category,
            helpInfo = R.string.csv_column_category_help,
            name = "",
            index = -1,
            required = false,
            metadata = Unit,
        )

        val title = ColumnMapping(
            nivyColumn = R.string.csv_column_title,
            helpInfo = R.string.csv_column_title_help,
            name = "",
            index = -1,
            required = false,
            metadata = Unit,
        )

        val description = ColumnMapping(
            nivyColumn = R.string.csv_column_description,
            helpInfo = R.string.csv_column_description_help,
            name = "",
            index = -1,
            required = false,
            metadata = Unit,
        )
    }

    @Composable
    fun uiState(): CSVState {
        val sampleCSV = remember(csv) {
            // drop the header
            csv?.drop(1)?.take(SAMPLE_SIZE)
        }

        val important = importantFields(sampleCSV)
        return CSVState(
            uiState = uiState,
            columns = columns,
            csv = csv,
            important = important,
            transfer = transferFields(sampleCSV),
            optional = optionalFields(sampleCSV),
            continueEnabled = continueEnabled(important = important)
        )
    }

    private fun continueEnabled(important: ImportantFields?): Boolean {
        return important != null && important.accountStatus.success &&
            important.amountStatus.success &&
            important.typeStatus.success &&
            important.dateStatus.success
    }

    @Composable
    private fun importantFields(sampleCSV: List<CSVRow>?): ImportantFields? {
        return produceState<ImportantFields?>(
            initialValue = null,
            sampleCSV,
            amount,
            type,
            date,
            account,
            accountCurrency,
        ) {
            val result = withContext(dispatchers.default) {
                if (sampleCSV != null) {
                    ImportantFields(
                        amount = amount,
                        amountStatus = sampleCSV.parseStatus(amount, ::parseAmount),
                        type = type,
                        typeStatus = sampleCSV.parseStatus(type, ::parseTransactionType),
                        date = date,
                        dateStatus = sampleCSV.parseStatus(date, ::parseDate),
                        account = account,
                        accountStatus = sampleCSV.parseStatus(account, ::parseText),
                        accountCurrency = accountCurrency,
                        accountCurrencyStatus = sampleCSV.parseStatus(
                            accountCurrency,
                            ::parseText
                        ),
                    )
                } else {
                    null
                }
            }
            value = result
        }.value
    }

    @Composable
    private fun transferFields(sampleCSV: List<CSVRow>?): TransferFields? {
        return produceState<TransferFields?>(
            initialValue = null,
            sampleCSV,
            toAccount,
            toAccountCurrency,
            toAmount,
        ) {
            val result = withContext(dispatchers.default) {
                if (sampleCSV != null) {
                    TransferFields(
                        toAccount = toAccount,
                        toAccountStatus = sampleCSV.parseStatus(toAccount, ::parseText),
                        toAccountCurrency = toAccountCurrency,
                        toAccountCurrencyStatus = sampleCSV.parseStatus(
                            toAccountCurrency,
                            ::parseText
                        ),
                        toAmount = toAmount,
                        toAmountStatus = sampleCSV.parseStatus(toAmount, ::parseAmount)
                    )
                } else {
                    null
                }
            }
            value = result
        }.value
    }

    @Composable
    private fun optionalFields(sampleCSV: List<CSVRow>?): OptionalFields? {
        return produceState<OptionalFields?>(
            initialValue = null,
            sampleCSV,
            category,
            title,
            description,
        ) {
            val result = withContext(dispatchers.default) {
                if (sampleCSV != null) {
                    OptionalFields(
                        category = category,
                        categoryStatus = sampleCSV.parseStatus(category, ::parseText),
                        title = title,
                        titleStatus = sampleCSV.parseStatus(title, ::parseText),
                        description = description,
                        descriptionStatus = sampleCSV.parseStatus(description, ::parseText),
                    )
                } else {
                    null
                }
            }
            value = result
        }.value
    }

    private suspend fun handleEvent(event: CSVEvent) {
        when (event) {
            is CSVEvent.FilePicked -> handleFilePicked(event)
            is CSVEvent.AmountMultiplier -> {
                amount = amount.copy(
                    metadata = event.multiplier,
                )
            }

            is CSVEvent.DataMetaChange -> {
                date = date.copy(
                    metadata = event.meta
                )
            }

            is CSVEvent.MapAccount -> {
                account = account.copy(
                    index = event.index,
                    name = event.name
                )
            }

            is CSVEvent.MapAccountCurrency -> {
                accountCurrency = accountCurrency.copy(
                    index = event.index,
                    name = event.name
                )
            }

            is CSVEvent.MapAmount -> {
                amount = amount.copy(
                    index = event.index,
                    name = event.name
                )
            }

            is CSVEvent.MapDate -> {
                date = date.copy(
                    index = event.index,
                    name = event.name
                )
            }

            is CSVEvent.MapType -> {
                type = type.copy(
                    index = event.index,
                    name = event.name
                )
            }

            is CSVEvent.TypeMetaChange -> {
                type = type.copy(
                    metadata = event.meta
                )
            }

            is CSVEvent.MapCategory -> {
                category = category.copy(
                    index = event.index,
                    name = event.name
                )
            }

            is CSVEvent.MapDescription -> {
                description = description.copy(
                    index = event.index,
                    name = event.name
                )
            }

            is CSVEvent.MapTitle -> {
                title = title.copy(
                    index = event.index,
                    name = event.name
                )
            }

            is CSVEvent.MapToAccount -> {
                toAccount = toAccount.copy(
                    index = event.index,
                    name = event.name
                )
            }

            is CSVEvent.MapToAccountCurrency -> {
                toAccountCurrency = toAccountCurrency.copy(
                    index = event.index,
                    name = event.name
                )
            }

            CSVEvent.Continue -> handleContinue()
            is CSVEvent.MapToAmount -> {
                toAmount = toAmount.copy(
                    index = event.index,
                    name = event.name
                )
            }

            is CSVEvent.ToAmountMetaChange -> {
                toAmount = toAmount.copy(
                    metadata = event.multiplier
                )
            }

            is CSVEvent.FinishImport -> {
                handleFinishImport(event)
            }
        }
    }

    // region Import CSV
    private suspend fun handleFilePicked(event: CSVEvent.FilePicked): Unit =
        withContext(dispatchers.io) {
            resetMappings()
            importCSV(uri = event.uri, semicolons = false)
            if (columns?.values?.size?.let { it < MinColumns } == true) {
                importCSV(uri = event.uri, semicolons = true)
            }
            csv = csv?.map { row ->
                row.copy(values = row.values.map { it.trim() })
            }?.toImmutableList()
            if (csv == null) toaster.show(R.string.csv_file_not_read)
        }

    private fun importCSV(uri: Uri, semicolons: Boolean) {
        csv = processFile(uri, semicolons = semicolons)?.toImmutableList()
        columns = csv?.firstOrNull()
    }

    private fun processFile(
        uri: Uri,
        semicolons: Boolean = false,
        charset: Charset = Charsets.UTF_8
    ): List<CSVRow>? {
        return try {
            fileReader.read(uri, charset).getOrNull()
                ?.let { parseCSV(it, semicolons) }
                ?.takeIf { it.isNotEmpty() }
        } catch (ignored: Exception) {
            // Whatever the user's file makes the reader throw, a UTF-8 read gets a second try as
            // UTF-16, and that one failing means there's no file to show.
            if (charset != Charsets.UTF_16) processFile(uri, semicolons, charset = Charsets.UTF_16) else null
        }
    }

    private fun parseCSV(csv: String, semicolons: Boolean): List<CSVRow> =
        readCsv.readCsv(csv, separator = if (semicolons) ';' else ',').map(::CSVRow)
    // endregion

    // An import that fails, like on a database error, imports nothing (it's one transaction): it
    // says so, and the columns show again to try once more.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun handleContinue() {
        try {
            import()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "CSV import failed")
            uiState = UIState.Idle
            toaster.show(R.string.msg_import_failed)
        }
    }

    private suspend fun import() {
        val csv = this.csv ?: return
        val emptyStatus = MappingStatus(emptyList(), false)

        withContext(dispatchers.io) {
            val result = csvImporter.import(
                csv = csv,
                importantFields = ImportantFields(
                    amount = amount,
                    date = date,
                    type = type,
                    account = account,
                    accountCurrency = accountCurrency,
                    amountStatus = emptyStatus,
                    dateStatus = emptyStatus,
                    accountStatus = emptyStatus,
                    typeStatus = emptyStatus,
                    accountCurrencyStatus = emptyStatus,
                ),
                transferFields = TransferFields(
                    toAccount = toAccount,
                    toAccountCurrency = toAccountCurrency,
                    toAmount = toAmount,
                    toAmountStatus = emptyStatus,
                    toAccountStatus = emptyStatus,
                    toAccountCurrencyStatus = emptyStatus,
                ),
                optionalFields = OptionalFields(
                    category = category,
                    title = title,
                    description = description,
                    categoryStatus = emptyStatus,
                    titleStatus = emptyStatus,
                    descriptionStatus = emptyStatus,
                ),
                onProgress = { progressPercent ->
                    withContext(dispatchers.main) {
                        uiState = UIState.Processing(
                            (progressPercent * Percent).roundToInt()
                        )
                    }
                }
            )
            uiState = UIState.Result(result)
        }
    }

    private fun handleFinishImport(event: CSVEvent.FinishImport) {
        if (event.launchedFromOnboarding) {
            val importSuccess = (uiState as? UIState.Result)?.importResult?.succeeded ?: false
            event.onboardingViewModel.importFinished(
                success = importSuccess
            )
            nav.back()
        }

        nav.back()
        resetState()
    }

    private fun resetState() {
        uiState = UIState.Idle
        csv = null
        columns = null
        resetMappings()
    }

    // region Boiler-plate
    private val events = MutableSharedFlow<CSVEvent>(replay = 0)

    init {
        viewModelScope.launch {
            events.collect {
                handleEvent(it)
            }
        }
    }

    fun onEvent(event: CSVEvent) {
        viewModelScope.launch {
            events.emit(event)
        }
    }
    // endregion
}
