package com.ivy.importdata.csvimport

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivy.domain.RootScreen
import com.ivy.wallet.domain.logic.csv.CSVImporter
import com.ivy.wallet.domain.logic.csv.model.ImportType
import com.ivy.data.backup.BackupDataUseCase
import com.ivy.wallet.utils.asLiveData
import com.ivy.base.file.getFileName
import com.ivy.navigation.ImportScreen
import com.ivy.navigation.Navigation
import com.ivy.onboarding.viewmodel.OnboardingViewModel
import com.ivy.wallet.domain.logic.csv.CSVMapper
import com.ivy.wallet.domain.logic.csv.CSVNormalizer
import com.ivy.data.file.FileSystem
import com.ivy.data.backup.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val nav: Navigation,
    private val fileReader: FileSystem,
    private val csvNormalizer: CSVNormalizer,
    private val csvMapper: CSVMapper,
    private val csvImporter: CSVImporter,
    private val backupDataUseCase: BackupDataUseCase
) : ViewModel() {
    private val _importStep = MutableLiveData<ImportStep>()
    val importStep = _importStep.asLiveData()

    private val _importType = MutableLiveData<ImportType>()
    val importType = _importType.asLiveData()

    private val _importProgressPercent = MutableLiveData<Int>()
    val importProgressPercent = _importProgressPercent.asLiveData()

    private val _importResult = MutableLiveData<ImportResult>()
    val importResult = _importResult.asLiveData()

    fun onBack() {
        when (importStep.value) {
            ImportStep.INSTRUCTIONS, ImportStep.RESULT -> _importStep.value = ImportStep.IMPORT_FROM
            ImportStep.IMPORT_FROM, ImportStep.LOADING, null -> {
                // the first step leaves the screen; back is disabled while importing
            }
        }
    }

    @ExperimentalStdlibApi
    fun uploadFile(context: Context) {
        val importType = importType.value ?: return

        (context as RootScreen).openFile { fileUri ->
            viewModelScope.launch {
                _importStep.value = ImportStep.LOADING

                _importResult.value = if (hasCSVExtension(context, fileUri)) {
                    restoreCSVFile(fileUri = fileUri, importType = importType)
                } else {
                    backupDataUseCase.importBackupFile(
                        backupFileUri = fileUri
                    ) { progressPercent ->
                        com.ivy.wallet.utils.uiThread {
                            _importProgressPercent.value =
                                (progressPercent * 100).roundToInt()
                        }
                    }
                }

                _importStep.value = ImportStep.RESULT
            }
        }
    }

    @ExperimentalStdlibApi
    private suspend fun restoreCSVFile(fileUri: Uri, importType: ImportType): ImportResult {
        return com.ivy.wallet.utils.ioThread {
            val rawCSV = fileReader.read(
                uri = fileUri,
                charset = when (importType) {
                    ImportType.IVY -> Charsets.UTF_16
                    else -> Charsets.UTF_8
                }
            ).getOrNull()
            if (rawCSV.isNullOrBlank()) {
                return@ioThread ImportResult(
                    rowsFound = 0,
                    transactionsImported = 0,
                    accountsImported = 0,
                    categoriesImported = 0,
                    failedRows = persistentListOf()
                )
            }

            val normalizedCSV = csvNormalizer.normalize(
                rawCSV = rawCSV,
                importType = importType
            )

            val mapping = csvMapper.mapping(
                type = importType,
                headerRow = normalizedCSV.split("\n").getOrNull(0)
            )

            return@ioThread try {
                val result = csvImporter.import(
                    csv = normalizedCSV,
                    rowMapping = mapping,
                    onProgress = { progressPercent ->
                        com.ivy.wallet.utils.uiThread {
                            _importProgressPercent.value =
                                (progressPercent * 100).roundToInt()
                        }
                    }
                )

                if (result.failedRows.isNotEmpty()) {
                    Timber.e("Import failed rows: ${result.failedRows}")
                }

                result
            } catch (e: Exception) {
                Timber.e(e)
                ImportResult(
                    rowsFound = 0,
                    transactionsImported = 0,
                    accountsImported = 0,
                    categoriesImported = 0,
                    failedRows = persistentListOf()
                )
            }
        }
    }

    fun setImportType(importType: ImportType) {
        _importType.value = importType
        _importStep.value = ImportStep.INSTRUCTIONS
    }

    fun skip(
        screen: ImportScreen,
        onboardingViewModel: OnboardingViewModel
    ) {
        if (screen.launchedFromOnboarding) {
            onboardingViewModel.importSkip()
        }

        nav.back()
        resetState()
    }

    fun finish(
        screen: ImportScreen,
        onboardingViewModel: OnboardingViewModel
    ) {
        if (screen.launchedFromOnboarding) {
            val importSuccess = importResult.value?.transactionsImported?.let { it > 0 } ?: false
            onboardingViewModel.importFinished(
                success = importSuccess
            )
        }

        nav.back()
        resetState()
    }

    private fun resetState() {
        _importStep.value = ImportStep.IMPORT_FROM
    }

    private suspend fun hasCSVExtension(
        context: Context,
        fileUri: Uri
    ): Boolean = com.ivy.wallet.utils.ioThread {
        val fileName = context.getFileName(fileUri)
        fileName?.endsWith(suffix = ".csv", ignoreCase = true) ?: false
    }
}
