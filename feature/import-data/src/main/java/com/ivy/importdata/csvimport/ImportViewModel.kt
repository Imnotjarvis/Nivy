package com.ivy.importdata.csvimport

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivy.data.backup.BackupDataUseCase
import com.ivy.data.backup.ImportResult
import com.ivy.domain.RootScreen
import com.ivy.navigation.ImportScreen
import com.ivy.navigation.Navigation
import com.ivy.onboarding.viewmodel.OnboardingViewModel
import com.ivy.wallet.utils.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

private const val Percent = 100

/** Imports a Nivy backup; any CSV file goes through the manual CSV import instead. */
@HiltViewModel
class ImportViewModel @Inject constructor(
    private val nav: Navigation,
    private val backupDataUseCase: BackupDataUseCase
) : ViewModel() {
    private val _importStep = MutableLiveData<ImportStep>()
    val importStep = _importStep.asLiveData()

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

    fun uploadFile(context: Context) {
        (context as RootScreen).openFile { fileUri ->
            viewModelScope.launch {
                _importStep.value = ImportStep.LOADING

                _importResult.value = backupDataUseCase.importBackupFile(
                    backupFileUri = fileUri
                ) { progressPercent ->
                    com.ivy.wallet.utils.uiThread {
                        _importProgressPercent.value = (progressPercent * Percent).roundToInt()
                    }
                }

                _importStep.value = ImportStep.RESULT
            }
        }
    }

    fun chooseNivyBackup() {
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
}
