package com.nivy.importdata.csvimport

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nivy.importdata.csvimport.flow.ImportFrom
import com.nivy.importdata.csvimport.flow.ImportProcessing
import com.nivy.importdata.csvimport.flow.ImportResultUI
import com.nivy.importdata.csvimport.flow.instructions.ImportInstructions
import com.nivy.navigation.ImportScreen
import com.nivy.onboarding.viewmodel.OnboardingViewModel
import com.nivy.data.backup.ImportResult
import com.nivy.importdata.usecase.CashewImportResult
import com.nivy.ui.R
import androidx.activity.compose.BackHandler

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.ImportCSVScreen(screen: ImportScreen) {
    val viewModel: ImportViewModel = viewModel()

    val importStep by viewModel.importStep.observeAsState(ImportStep.IMPORT_FROM)
    val importProgressPercent by viewModel.importProgressPercent.observeAsState(0)
    val importResult by viewModel.importResult.observeAsState()
    val source by viewModel.source.observeAsState(ImportSource.NIVY)
    val cashewResult by viewModel.cashewResult.observeAsState()

    val onboardingViewModel: OnboardingViewModel = viewModel()

    // Past the first step, back returns to it (and does nothing while importing).
    BackHandler(enabled = importStep != ImportStep.IMPORT_FROM) {
        viewModel.onBack()
    }
    val context = LocalContext.current

    UI(
        screen = screen,
        importStep = importStep,
        importProgressPercent = importProgressPercent,
        importResult = importResult,
        source = source,
        cashewResult = cashewResult,

        onChooseNivyBackup = viewModel::chooseNivyBackup,
        onChooseCashew = viewModel::chooseCashew,
        onUploadCSVFile = { viewModel.uploadFile(context) },
        onSkip = {
            viewModel.skip(
                screen = screen,
                onboardingViewModel = onboardingViewModel
            )
        },
        onFinish = {
            viewModel.finish(
                screen = screen,
                onboardingViewModel = onboardingViewModel
            )
        }
    )
}

@ExperimentalFoundationApi
@Composable
private fun BoxWithConstraintsScope.UI(
    screen: ImportScreen,

    importStep: ImportStep,
    importProgressPercent: Int,
    importResult: ImportResult?,
    source: ImportSource = ImportSource.NIVY,
    cashewResult: CashewImportResult? = null,

    onChooseNivyBackup: () -> Unit = {},
    onChooseCashew: () -> Unit = {},
    onUploadCSVFile: () -> Unit = {},
    onSkip: () -> Unit = {},
    onFinish: () -> Unit = {},
) {
    when (importStep) {
        ImportStep.IMPORT_FROM -> {
            ImportFrom(
                launchedFromOnboarding = screen.launchedFromOnboarding,
                onSkip = onSkip,
                onNivyBackup = onChooseNivyBackup,
                onCashew = onChooseCashew,
            )
        }

        ImportStep.INSTRUCTIONS -> {
            ImportInstructions(
                hasSkip = screen.launchedFromOnboarding,
                onSkip = onSkip,
                onUploadClick = onUploadCSVFile,
                source = source,
            )
        }

        ImportStep.LOADING -> {
            ImportProcessing(
                progressPercent = importProgressPercent,
                text = if (source == ImportSource.CASHEW) R.string.importing_the_cashew_file else R.string.importing_the_csv_file,
            )
        }

        ImportStep.RESULT -> {
            ImportResultUI(
                result = importResult!!,
                launchedFromOnboarding = screen.launchedFromOnboarding,
                cashew = cashewResult,
            ) {
                onFinish()
            }
        }
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview() {
    com.nivy.wallet.NivyPreview {
        UI(
            screen = ImportScreen(launchedFromOnboarding = true),
            importStep = ImportStep.IMPORT_FROM,
            importProgressPercent = 0,
            importResult = null
        )
    }
}
