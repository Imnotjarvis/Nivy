package com.ivy.importdata.csv

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivy.importdata.csv.domain.CSVImporterV2
import com.ivy.importdata.csv.domain.SAMPLE_SIZE
import com.ivy.importdata.csv.domain.parseAccount
import com.ivy.importdata.csv.domain.parseAccountCurrency
import com.ivy.importdata.csv.domain.parseAmount
import com.ivy.importdata.csv.domain.parseCategory
import com.ivy.importdata.csv.domain.parseDate
import com.ivy.importdata.csv.domain.parseDescription
import com.ivy.importdata.csv.domain.parseStatus
import com.ivy.importdata.csv.domain.parseTitle
import com.ivy.importdata.csv.domain.parseToAccount
import com.ivy.importdata.csv.domain.parseToAccountCurrency
import com.ivy.importdata.csv.domain.parseTransactionType
import com.ivy.navigation.Navigation
import com.ivy.data.file.FileSystem
import com.ivy.domain.usecase.csv.ReadCsvUseCase
import com.ivy.ui.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.nio.charset.Charset
import javax.inject.Inject
import kotlin.math.roundToInt

// A CSV that splits into fewer columns than this was read with the wrong separator.
private const val MinColumns = 3

private const val Percent = 100

@HiltViewModel
class CSVViewModel @Inject constructor(
    private val fileReader: FileSystem,
    private val readCsv: ReadCsvUseCase,
    private val csvImporter: CSVImporterV2,
    private val nav: Navigation,
) : ViewModel() {

    private var columns by mutableStateOf<CSVRow?>(null)
    private var csv by mutableStateOf<ImmutableList<CSVRow>?>(null)

    // region Important fields
    private var amount by mutableStateOf(
        ColumnMapping(
            ivyColumn = R.string.csv_column_amount,
            helpInfo = R.string.csv_column_amount_help,
            name = "",
            index = -1,
            metadata = 1,
            required = true,
        )
    )
    private var type by mutableStateOf(
        ColumnMapping(
            ivyColumn = R.string.csv_column_type,
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
    )
    private var date by mutableStateOf(
        ColumnMapping(
            ivyColumn = R.string.csv_column_date,
            helpInfo = R.string.csv_column_date_help,
            name = "",
            index = -1,
            required = true,
            metadata = DateMetadata.DateFirst
        )
    )
    private var account by mutableStateOf(
        ColumnMapping(
            ivyColumn = R.string.csv_column_account,
            helpInfo = R.string.csv_column_account_help,
            name = "",
            index = -1,
            required = true,
            metadata = Unit,
        )
    )
    private var accountCurrency by mutableStateOf(
        ColumnMapping(
            ivyColumn = R.string.csv_column_account_currency,
            helpInfo = R.string.csv_column_account_currency_help,
            name = "",
            index = -1,
            required = false,
            metadata = Unit,
        )
    )
    // endregion

    // region Transfer fields
    private var toAccount by mutableStateOf(
        ColumnMapping(
            ivyColumn = R.string.csv_column_to_account,
            helpInfo = R.string.csv_column_to_account_help,
            name = "",
            index = -1,
            required = false,
            metadata = Unit,
        )
    )
    private var toAccountCurrency by mutableStateOf(
        ColumnMapping(
            ivyColumn = R.string.csv_column_to_account_currency,
            helpInfo = R.string.csv_column_to_account_currency_help,
            name = "",
            index = -1,
            required = false,
            metadata = Unit,
        )
    )
    private var toAmount by mutableStateOf(
        ColumnMapping(
            ivyColumn = R.string.csv_column_to_amount,
            helpInfo = R.string.csv_column_to_amount_help,
            name = "",
            index = -1,
            required = false,
            metadata = 1,
        )
    )
    // endregion

    // region Optional fields
    private var category by mutableStateOf(
        ColumnMapping(
            ivyColumn = R.string.csv_column_category,
            helpInfo = R.string.csv_column_category_help,
            name = "",
            index = -1,
            required = false,
            metadata = Unit,
        )
    )
    private var title by mutableStateOf(
        ColumnMapping(
            ivyColumn = R.string.csv_column_title,
            helpInfo = R.string.csv_column_title_help,
            name = "",
            index = -1,
            required = false,
            metadata = Unit,
        )
    )
    private var description by mutableStateOf(
        ColumnMapping(
            ivyColumn = R.string.csv_column_description,
            helpInfo = R.string.csv_column_description_help,
            name = "",
            index = -1,
            required = false,
            metadata = Unit,
        )
    )
    // endregion

    private var uiState by mutableStateOf<UIState>(UIState.Idle)

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
            val result = withContext(Dispatchers.Default) {
                if (sampleCSV != null) {
                    ImportantFields(
                        amount = amount,
                        amountStatus = sampleCSV.parseStatus(amount, ::parseAmount),
                        type = type,
                        typeStatus = sampleCSV.parseStatus(type, ::parseTransactionType),
                        date = date,
                        dateStatus = sampleCSV.parseStatus(date, ::parseDate),
                        account = account,
                        accountStatus = sampleCSV.parseStatus(account, ::parseAccount),
                        accountCurrency = accountCurrency,
                        accountCurrencyStatus = sampleCSV.parseStatus(
                            accountCurrency,
                            ::parseAccountCurrency
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
            val result = withContext(Dispatchers.Default) {
                if (sampleCSV != null) {
                    TransferFields(
                        toAccount = toAccount,
                        toAccountStatus = sampleCSV.parseStatus(toAccount, ::parseToAccount),
                        toAccountCurrency = toAccountCurrency,
                        toAccountCurrencyStatus = sampleCSV.parseStatus(
                            toAccountCurrency,
                            ::parseToAccountCurrency
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
            val result = withContext(Dispatchers.Default) {
                if (sampleCSV != null) {
                    OptionalFields(
                        category = category,
                        categoryStatus = sampleCSV.parseStatus(category, ::parseCategory),
                        title = title,
                        titleStatus = sampleCSV.parseStatus(title, ::parseTitle),
                        description = description,
                        descriptionStatus = sampleCSV.parseStatus(description, ::parseDescription),
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

            CSVEvent.ResetState -> {
                uiState = UIState.Idle
            }

            is CSVEvent.FinishImport -> {
                handleFinishImport(event)
            }
        }
    }

    // region Import CSV
    private suspend fun handleFilePicked(event: CSVEvent.FilePicked): Unit =
        withContext(Dispatchers.IO) {
            importCSV(uri = event.uri, normalizeCSV = false)
            if (columns?.values?.size?.let { it < MinColumns } == true) {
                importCSV(uri = event.uri, normalizeCSV = true)
            }
            csv = csv?.map { row ->
                row.copy(values = row.values.map { it.trim() })
            }?.toImmutableList()
        }

    private fun importCSV(uri: Uri, normalizeCSV: Boolean): Unit = try {
        csv = processFile(uri, normalizeCSV = normalizeCSV)?.toImmutableList()
        columns = csv?.firstOrNull()
    } catch (e: Exception) {
        Timber.e(e)
    }

    private fun processFile(
        uri: Uri,
        normalizeCSV: Boolean = false,
        charset: Charset = Charsets.UTF_8
    ): List<CSVRow>? {
        return try {
            fileReader.read(uri, charset).getOrNull()
                ?.let { parseCSV(it, normalizeCSV) }
                ?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            // A file that doesn't parse as UTF-8 gets a second try as UTF-16.
            if (charset != Charsets.UTF_16) processFile(uri, normalizeCSV, charset = Charsets.UTF_16) else null
        }
    }

    private fun parseCSV(csv: String, normalizeCSV: Boolean): List<CSVRow> {
        val finalCSV = if (normalizeCSV) {
            csv.replace(",", " ")
                .replace(";", ",")
        } else {
            csv
        }
        return readCsv.readCsv(finalCSV).map(::CSVRow)
    }
    // endregion

    private suspend fun handleContinue() {
        val csv = this.csv ?: return
        val emptyStatus = MappingStatus(emptyList(), false)

        withContext(Dispatchers.IO) {
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
                    com.ivy.wallet.utils.uiThread {
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
            val importSuccess = (uiState as? UIState.Result)?.importResult
                ?.transactionsImported?.let { it > 0 } ?: false
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
