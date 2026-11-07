package com.ivy.settings

import com.ivy.design.system.colors.deleteRed
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.model.DateDisplayFormat
import com.ivy.base.model.Theme
import com.ivy.design.system.shapes.AppCornerRadius
import com.ivy.design.system.colors.ColorMode
import com.ivy.wallet.IvyWalletPreview
import com.ivy.wallet.rootScreen
import com.ivy.navigation.ExchangeRatesScreen
import com.ivy.navigation.FeaturesScreen
import com.ivy.navigation.ImportScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.design.system.colors.Green
import com.ivy.wallet.ui.theme.components.IvyToolbar
import com.ivy.wallet.ui.theme.modal.ChooseStartDateOfMonthModal
import com.ivy.wallet.ui.theme.modal.CurrencyModal
import com.ivy.wallet.ui.theme.modal.DeleteModal
import com.ivy.wallet.ui.theme.modal.NameModal
import com.ivy.wallet.ui.theme.modal.ProgressModal
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.typography.numberLabelMedium
import com.ivy.navigation.rememberOnBack
import kotlin.math.roundToInt

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.SettingsScreen() {
    val viewModel: SettingsViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()
    val rootScreen = rootScreen()

    UI(
        currencyCode = uiState.currencyCode,
        theme = uiState.currentTheme,
        onSwitchTheme = {
            viewModel.onEvent(SettingsEvent.SwitchTheme)
        },
        lockApp = uiState.lockApp,
        showNotifications = uiState.showNotifications,
        hideCurrentBalance = uiState.hideCurrentBalance,
        hideIncome = uiState.hideIncome,
        progressState = uiState.progressState,
        treatTransfersAsIncomeExpense = uiState.treatTransfersAsIncomeExpense,
        nameLocalAccount = uiState.name,
        startDateOfMonth = uiState.startDateOfMonth.toInt(),
        dateFormat = uiState.dateFormat,
        onSetCurrency = {
            viewModel.onEvent(SettingsEvent.SetCurrency(it))
        },
        onSetName = {
            viewModel.onEvent(SettingsEvent.SetName(it))
        },
        onBackupData = {
            viewModel.onEvent(SettingsEvent.BackupData(rootScreen))
        },
        onExportToCSV = {
            viewModel.onEvent(SettingsEvent.ExportToCsv(rootScreen))
        },
        onSetLockApp = {
            viewModel.onEvent(SettingsEvent.SetLockApp(it))
        },
        onSetShowNotifications = {
            viewModel.onEvent(SettingsEvent.SetShowNotifications(it))
        },
        onSetHideCurrentBalance = {
            viewModel.onEvent(SettingsEvent.SetHideCurrentBalance(it))
        },
        onSetHideIncome = {
            viewModel.onEvent(SettingsEvent.SetHideIncome(it))
        },
        onSetStartDateOfMonth = {
            viewModel.onEvent(SettingsEvent.SetStartDateOfMonth(it))
        },
        onSetTreatTransfersAsIncExp = {
            viewModel.onEvent(SettingsEvent.SetTransfersAsIncomeExpense(it))
        },
        onDeleteAllUserData = {
            viewModel.onEvent(SettingsEvent.DeleteAllUserData)
        },
        onSetDateFormat = {
            viewModel.onEvent(SettingsEvent.SetDateFormat(it))
        },
        onSetCornerRadius = {
            viewModel.onEvent(SettingsEvent.SetCornerRadius(it))
        },
        onSetMinimalColors = {
            viewModel.onEvent(SettingsEvent.SetMinimalColors(it))
        }
    )
}

@ExperimentalFoundationApi
@Composable
@Suppress("LongMethod")
private fun BoxWithConstraintsScope.UI(
    currencyCode: String,
    theme: Theme,
    onSwitchTheme: () -> Unit,
    lockApp: Boolean,
    nameLocalAccount: String?,
    onSetCurrency: (String) -> Unit,
    startDateOfMonth: Int = 1,
    dateFormat: DateDisplayFormat = DateDisplayFormat.Default,
    showNotifications: Boolean = true,
    hideCurrentBalance: Boolean = false,
    hideIncome: Boolean = false,
    progressState: Boolean = false,
    treatTransfersAsIncomeExpense: Boolean = false,
    onSetName: (String) -> Unit = {},
    onBackupData: () -> Unit = {},
    onExportToCSV: () -> Unit = {},
    onSetLockApp: (Boolean) -> Unit = {},
    onSetShowNotifications: (Boolean) -> Unit = {},
    onSetTreatTransfersAsIncExp: (Boolean) -> Unit = {},
    onSetHideCurrentBalance: (Boolean) -> Unit = {},
    onSetHideIncome: (Boolean) -> Unit = {},
    onSetStartDateOfMonth: (Int) -> Unit = {},
    onDeleteAllUserData: () -> Unit = {},
    onSetDateFormat: (DateDisplayFormat) -> Unit = {},
    onSetCornerRadius: (Int?) -> Unit = {},
    onSetMinimalColors: (Boolean) -> Unit = {},
) {
    var currencyModalVisible by remember { mutableStateOf(false) }
    var nameModalVisible by remember { mutableStateOf(false) }
    var chooseStartDateOfMonthVisible by remember { mutableStateOf(false) }
    var chooseDateFormatVisible by remember { mutableStateOf(false) }
    var chooseCornerRadiusVisible by remember { mutableStateOf(false) }
    var deleteAllDataModalVisible by remember { mutableStateOf(false) }
    var deleteAllDataModalFinalVisible by remember { mutableStateOf(false) }
    val nav = navigation()
    val onBack = rememberOnBack()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("settings_lazy_column")
    ) {
        stickyHeader {
            IvyToolbar(
                onBack = { onBack() },
            ) {
                Spacer(Modifier.weight(1f))

                val rootScreen = rootScreen()
                Text(
                    text = "${rootScreen.buildVersionName} (${rootScreen.buildVersionCode})",
                    style = MaterialTheme.typography.numberLabelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(Modifier.width(32.dp))
            }
            // onboarding toolbar include paddingBottom 16.dp
        }

        item {
            Spacer(Modifier.height(8.dp))

            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black
                )
            )

            Spacer(Modifier.height(24.dp))

            CurrencyButton(currency = currencyCode) {
                currencyModalVisible = true
            }

            Spacer(Modifier.height(12.dp))

            AccountCard(
                nameLocalAccount = nameLocalAccount,
            ) {
                nameModalVisible = true
            }
        }

        item {
            SettingsSectionDivider(text = stringResource(R.string.import_export))

            Spacer(Modifier.height(16.dp))

            val nav = navigation()
            ExportCSV {
                onExportToCSV()
            }

            Spacer(Modifier.height(12.dp))

            SettingsDefaultButton(
                icon = R.drawable.ic_vue_security_shield,
                text = stringResource(R.string.backup_data),
                iconPadding = 8.dp
            ) {
                onBackupData()
            }

            Spacer(Modifier.height(12.dp))

            SettingsPrimaryButton(
                icon = R.drawable.ic_export_csv,
                text = stringResource(R.string.import_data),
                backgroundColor = Green
            ) {
                nav.navigateTo(
                    ImportScreen(
                        launchedFromOnboarding = false
                    )
                )
            }
        }

        item {
            SettingsSectionDivider(text = stringResource(R.string.app_settings))

            Spacer(Modifier.height(16.dp))

            AppThemeButton(
                icon = when (theme) {
                    Theme.LIGHT -> R.drawable.home_more_menu_light_mode
                    Theme.DARK -> R.drawable.home_more_menu_dark_mode
                    Theme.AMOLED_DARK -> R.drawable.home_more_menu_amoled_dark_mode
                    Theme.AUTO -> R.drawable.home_more_menu_auto_mode
                },
                label = when (theme) {
                    Theme.LIGHT -> stringResource(R.string.light_mode)
                    Theme.DARK -> stringResource(R.string.dark_mode)
                    Theme.AMOLED_DARK -> stringResource(R.string.amoled_mode)
                    Theme.AUTO -> stringResource(R.string.auto_mode)
                }
            ) {
                onSwitchTheme()
            }

            Spacer(Modifier.height(12.dp))

            val nav = navigation()

            SettingsDefaultButton(
                icon = R.drawable.ic_currency,
                text = stringResource(R.string.exchange_rates),
            ) {
                nav.navigateTo(ExchangeRatesScreen)
            }

            Spacer(Modifier.height(12.dp))

            AppSwitch(
                lockApp = lockApp,
                onSetLockApp = onSetLockApp,
                text = stringResource(R.string.lock_app),
                icon = R.drawable.ic_custom_fingerprint_m
            )

            Spacer(Modifier.height(12.dp))

            AppSwitch(
                lockApp = showNotifications,
                onSetLockApp = onSetShowNotifications,
                text = stringResource(R.string.show_notifications),
                icon = R.drawable.ic_notification_m
            )

            Spacer(Modifier.height(12.dp))

            AppSwitch(
                lockApp = hideCurrentBalance,
                onSetLockApp = onSetHideCurrentBalance,
                text = stringResource(R.string.hide_balance),
                description = stringResource(R.string.hide_balance_description),
                icon = R.drawable.ic_hide_m
            )

            Spacer(Modifier.height(12.dp))

            AppSwitch(
                lockApp = hideIncome,
                onSetLockApp = onSetHideIncome,
                text = stringResource(R.string.hide_income),
                description = stringResource(R.string.hide_income_description),
                icon = R.drawable.ic_hide_m
            )

            Spacer(Modifier.height(12.dp))

            AppSwitch(
                lockApp = treatTransfersAsIncomeExpense,
                onSetLockApp = onSetTreatTransfersAsIncExp,
                text = stringResource(R.string.transfers_as_income_expense),
                description = stringResource(R.string.transfers_as_income_expense_description),
                icon = R.drawable.ic_custom_transfer_m
            )

            Spacer(Modifier.height(12.dp))

            StartDateOfMonth(
                startDateOfMonth = startDateOfMonth
            ) {
                chooseStartDateOfMonthVisible = true
            }

            Spacer(Modifier.height(12.dp))

            SettingsValueRow(
                icon = R.drawable.ic_vue_main_calendar,
                label = stringResource(R.string.date_format),
                value = dateFormat.example()
            ) {
                chooseDateFormatVisible = true
            }

            Spacer(Modifier.height(12.dp))

            SettingsValueRow(
                icon = R.drawable.ic_vue_design_bezier,
                label = stringResource(R.string.corner_radius),
                value = stringResource(R.string.corner_radius_value, AppCornerRadius.value.value.roundToInt())
            ) {
                chooseCornerRadiusVisible = true
            }

            Spacer(Modifier.height(12.dp))

            AppSwitch(
                lockApp = ColorMode.minimal,
                onSetLockApp = onSetMinimalColors,
                text = stringResource(R.string.minimal_colors),
                description = stringResource(R.string.minimal_colors_description),
                icon = R.drawable.ic_custom_palette_m
            )

            Spacer(Modifier.height(12.dp))

            CustomFeatures(
                onClick = { nav.navigateTo(FeaturesScreen) }
            )
        }

        item {
            SettingsSectionDivider(
                text = stringResource(R.string.danger_zone),
                color = deleteRed()
            )

            Spacer(Modifier.height(16.dp))

            SettingsPrimaryButton(
                icon = R.drawable.ic_delete,
                text = stringResource(R.string.delete_all_user_data),
                backgroundColor = deleteRed()
            ) {
                deleteAllDataModalVisible = true
            }
        }

        item {
            Spacer(modifier = Modifier.height(120.dp)) // last item spacer
        }
    }

    CurrencyModal(
        title = stringResource(R.string.set_currency),
        initialCurrency = IvyCurrency.fromCode(currencyCode),
        visible = currencyModalVisible,
        dismiss = { currencyModalVisible = false }
    ) {
        onSetCurrency(it)
    }

    NameModal(
        visible = nameModalVisible,
        name = nameLocalAccount ?: "",
        dismiss = { nameModalVisible = false }
    ) {
        onSetName(it)
    }

    ChooseStartDateOfMonthModal(
        visible = chooseStartDateOfMonthVisible,
        selectedStartDateOfMonth = startDateOfMonth,
        dismiss = { chooseStartDateOfMonthVisible = false }
    ) {
        onSetStartDateOfMonth(it)
    }

    ChooseDateFormatModal(
        visible = chooseDateFormatVisible,
        selected = dateFormat,
        dismiss = { chooseDateFormatVisible = false },
        onSelect = onSetDateFormat,
    )

    ChooseCornerRadiusModal(
        visible = chooseCornerRadiusVisible,
        dismiss = { chooseCornerRadiusVisible = false },
        onSet = onSetCornerRadius,
    )

    DeleteModal(
        title = stringResource(R.string.delete_all_user_data_question),
        description = stringResource(
            R.string.delete_all_user_data_warning,
            stringResource(R.string.your_account)
        ),
        visible = deleteAllDataModalVisible,
        dismiss = { deleteAllDataModalVisible = false },
        onDelete = {
            deleteAllDataModalVisible = false
            deleteAllDataModalFinalVisible = true
        }
    )

    DeleteModal(
        title = stringResource(
            R.string.confirm_all_userd_data_deletion,
            stringResource(R.string.all_of_your_data)
        ),
        description = stringResource(R.string.final_deletion_warning),
        visible = deleteAllDataModalFinalVisible,
        dismiss = { deleteAllDataModalFinalVisible = false },
        onDelete = {
            onDeleteAllUserData()
        }
    )

    ProgressModal(
        title = stringResource(R.string.exporting_data),
        description = stringResource(R.string.exporting_data_description),
        visible = progressState
    )
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme) {
        UI(
            nameLocalAccount = null,
            theme = Theme.AUTO,
            onSwitchTheme = {},
            lockApp = false,
            currencyCode = "BGN",
            onSetCurrency = {}
        )
    }
}
