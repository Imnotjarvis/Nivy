package com.nivy.importdata.csvimport

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nivy.data.backup.BackupDataUseCase
import com.nivy.data.backup.ImportResult
import com.nivy.domain.RootScreen
import com.nivy.navigation.ImportScreen
import com.nivy.navigation.Navigation
import com.nivy.onboarding.viewmodel.OnboardingViewModel
import com.nivy.wallet.utils.asLiveData
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
                    com.nivy.wallet.utils.uiThread {
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
