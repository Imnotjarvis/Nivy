package com.nivy.importdata.csvimport

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nivy.data.backup.BackupDataUseCase
import com.nivy.data.backup.ImportResult
import com.nivy.domain.RootScreen
import com.nivy.importdata.usecase.CashewImportResult
import com.nivy.importdata.usecase.ImportCashewUseCase
import com.nivy.navigation.ImportScreen
import com.nivy.navigation.Navigation
import com.nivy.onboarding.viewmodel.OnboardingViewModel
import com.nivy.wallet.utils.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

private const val Percent = 100

/** Where the data comes from: the instructions, the import and its result follow it. */
enum class ImportSource { NIVY, CASHEW }

/**
 * Imports a Nivy backup or Cashew's data file; any CSV file goes through the manual CSV import
 * instead.
 */
@HiltViewModel
class ImportViewModel @Inject internal constructor(
    private val nav: Navigation,
    private val backupDataUseCase: BackupDataUseCase,
    private val importCashew: ImportCashewUseCase,
) : ViewModel() {
    private val _source = MutableLiveData(ImportSource.NIVY)
    val source = _source.asLiveData()

    private val _importStep = MutableLiveData<ImportStep>()
    val importStep = _importStep.asLiveData()

    private val _importProgressPercent = MutableLiveData<Int>()
    val importProgressPercent = _importProgressPercent.asLiveData()

    private val _importResult = MutableLiveData<ImportResult>()
    val importResult = _importResult.asLiveData()

    /** What Cashew's import brought besides the counts every import has, and what it left out. */
    private val _cashewResult = MutableLiveData<CashewImportResult?>()
    val cashewResult = _cashewResult.asLiveData()

    fun onBack() {
        when (importStep.value) {
            ImportStep.INSTRUCTIONS, ImportStep.RESULT -> _importStep.value = ImportStep.IMPORT_FROM
            ImportStep.IMPORT_FROM, ImportStep.LOADING, null -> {
                // the first step leaves the screen; back is disabled while importing
            }
        }
    }

    fun uploadFile(context: Context) {
        (context as RootScreen).openFile { fileUri ->
            viewModelScope.launch {
                _importStep.value = ImportStep.LOADING
                val onProgress: suspend (Double) -> Unit = { progressPercent ->
                    com.nivy.wallet.utils.uiThread {
                        _importProgressPercent.value = (progressPercent * Percent).roundToInt()
                    }
                }
                if (_source.value == ImportSource.CASHEW) {
                    val imported = importCashew.import(fileUri, onProgress)
                    _cashewResult.value = imported
                    _importResult.value = imported.result
                } else {
                    _cashewResult.value = null
                    _importResult.value = backupDataUseCase.importBackupFile(backupFileUri = fileUri, onProgress = onProgress)
                }
                _importStep.value = ImportStep.RESULT
            }
        }
    }

    fun chooseNivyBackup() = choose(ImportSource.NIVY)

    fun chooseCashew() = choose(ImportSource.CASHEW)

    private fun choose(source: ImportSource) {
        _source.value = source
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
            val importSuccess = importResult.value?.succeeded ?: false
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
}
