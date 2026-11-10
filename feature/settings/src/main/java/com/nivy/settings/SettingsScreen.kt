package com.nivy.settings

import com.nivy.design.system.colors.deleteRed
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
import com.nivy.base.model.AppLockDelay
import com.nivy.base.model.DateDisplayFormat
import com.nivy.base.model.Theme
import com.nivy.design.system.shapes.AppCornerRadius
import com.nivy.design.system.colors.ColorMode
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.rootScreen
import com.nivy.navigation.ExchangeRatesScreen
import com.nivy.navigation.FeaturesScreen
import com.nivy.navigation.ImportScreen
import com.nivy.navigation.navigation
import com.nivy.navigation.screenScopedViewModel
import com.nivy.ui.R
import com.nivy.wallet.domain.data.NivyCurrency
import com.nivy.design.system.colors.Green
import com.nivy.wallet.ui.theme.components.NivyToolbar
import com.nivy.wallet.ui.theme.modal.ChooseStartDateOfMonthModal
import com.nivy.wallet.ui.theme.modal.CurrencyModal
import com.nivy.wallet.ui.theme.modal.DeleteModal
import com.nivy.wallet.ui.theme.modal.ProgressModal
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.typography.numberLabelMedium
import com.nivy.navigation.rememberOnBack
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
        lockDelay = uiState.lockDelay,
        showNotifications = uiState.showNotifications,
        hideCurrentBalance = uiState.hideCurrentBalance,
        hideIncome = uiState.hideIncome,
        progressState = uiState.progressState,
        treatTransfersAsIncomeExpense = uiState.treatTransfersAsIncomeExpense,
        startDateOfMonth = uiState.startDateOfMonth.toInt(),
        dateFormat = uiState.dateFormat,
        onSetCurrency = {
            viewModel.onEvent(SettingsEvent.SetCurrency(it))
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
        onSetLockDelay = {
            viewModel.onEvent(SettingsEvent.SetLockDelay(it))
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
internal fun BoxWithConstraintsScope.UI(
    currencyCode: String,
    theme: Theme,
    onSwitchTheme: () -> Unit,
    lockApp: Boolean,
    onSetCurrency: (String) -> Unit,
    lockDelay: AppLockDelay = AppLockDelay.Default,
    startDateOfMonth: Int = 1,
    dateFormat: DateDisplayFormat = DateDisplayFormat.Default,
    showNotifications: Boolean = true,
    hideCurrentBalance: Boolean = false,
    hideIncome: Boolean = false,
    progressState: Boolean = false,
    treatTransfersAsIncomeExpense: Boolean = false,
    onBackupData: () -> Unit = {},
    onExportToCSV: () -> Unit = {},
    onSetLockApp: (Boolean) -> Unit = {},
    onSetLockDelay: (AppLockDelay) -> Unit = {},
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
    var chooseStartDateOfMonthVisible by remember { mutableStateOf(false) }
    var chooseDateFormatVisible by remember { mutableStateOf(false) }
    var chooseLockDelayVisible by remember { mutableStateOf(false) }
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
            NivyToolbar(
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

            if (lockApp) {
                Spacer(Modifier.height(12.dp))

                SettingsValueRow(
                    icon = R.drawable.ic_vue_main_timer,
                    label = stringResource(R.string.lock_after_leaving),
                    value = lockDelay.label()
                ) {
                    chooseLockDelayVisible = true
                }
            }

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
        initialCurrency = NivyCurrency.fromCode(currencyCode),
        visible = currencyModalVisible,
        dismiss = { currencyModalVisible = false }
    ) {
        onSetCurrency(it)
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

    ChooseLockDelayModal(
        visible = chooseLockDelayVisible,
        selected = lockDelay,
        dismiss = { chooseLockDelayVisible = false },
        onSelect = onSetLockDelay,
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
    NivyPreview(theme) {
        UI(
            theme = Theme.AUTO,
            onSwitchTheme = {},
            lockApp = false,
            currencyCode = "BGN",
            onSetCurrency = {}
        )
    }
}
