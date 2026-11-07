package com.nivy.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.base.time.impl.DeviceTimeProvider
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.previewTransaction
import com.nivy.navigation.NivyMaterial3Preview
import com.nivy.navigation.TransactionsScreen
import com.nivy.design.system.colors.GreenDark
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

@Preview
@Composable
private fun BoxWithConstraintsScope.Preview_empty() {
    NivyMaterial3Preview {
        UI(
            period = TimePeriod.currentMonth(
                startDayOfMonth = 1,
                timeProvider = DeviceTimeProvider(),
            ), // preview
            baseCurrency = "BGN",
            currency = "BGN",

            categories = persistentListOf(),
            accounts = persistentListOf(),

            balance = 1314.578,
            balanceBaseCurrency = null,
            income = 8000.0,
            expenses = 6000.0,

            history = persistentListOf(),
            category = null,
            account = previewAccount("DSK", color = GreenDark, icon = "pet"),
            onSetPeriod = { },
            onPreviousMonth = {},
            onNextMonth = {},
            onDelete = {},
            onEditAccount = { _, _, _ -> },
            onEditCategory = {},
            updateAccountNameConfirmation = {},
            enableDeletionButton = true,
            deleteModal1Visible = false,
            onDeleteModal1Visible = {},
            skipAllModalVisible = false,
            onSkipAllModalVisible = {},
            onChoosePeriodModal = {},
            choosePeriodModal = null,
            screen = TransactionsScreen(),
            shouldShowAccountSpecificColorInTransactions = false
        )
    }
}

@Preview
@Composable
private fun BoxWithConstraintsScope.Preview_crypto() {
    NivyMaterial3Preview {
        UI(
            period = TimePeriod.currentMonth(
                startDayOfMonth = 1,
                timeProvider = DeviceTimeProvider(),
            ), // preview
            baseCurrency = "BGN",
            currency = "ADA",

            categories = persistentListOf(),
            accounts = persistentListOf(),

            balance = 1314.578,
            balanceBaseCurrency = 2879.28,
            income = 8000.0,
            expenses = 6000.0,

            history = persistentListOf(),
            category = null,
            account = previewAccount("DSK", color = GreenDark, icon = "pet", includeInBalance = false),
            onSetPeriod = { },
            onPreviousMonth = {},
            onNextMonth = {},
            onDelete = {},
            onEditAccount = { _, _, _ -> },
            onEditCategory = {},
            updateAccountNameConfirmation = {},
            enableDeletionButton = true,
            deleteModal1Visible = false,
            onDeleteModal1Visible = {},
            skipAllModalVisible = false,
            onSkipAllModalVisible = {},
            onChoosePeriodModal = {},
            choosePeriodModal = null,
            screen = TransactionsScreen(),
            shouldShowAccountSpecificColorInTransactions = false
        )
    }
}

@Preview
@Composable
private fun BoxWithConstraintsScope.Preview_empty_upcoming(isDark: Boolean = false) {
    NivyMaterial3Preview(isDark) {
        UI(
            period = TimePeriod(month = Month.monthsList().first(), year = 2023),
            baseCurrency = "BGN",
            currency = "BGN",

            categories = persistentListOf(),
            accounts = persistentListOf(),

            balance = 1314.578,
            balanceBaseCurrency = null,
            income = 8000.0,
            expenses = 6000.0,

            history = persistentListOf(),
            category = null,
            account = previewAccount("DSK", color = GreenDark, icon = "pet"),
            onSetPeriod = { },
            onPreviousMonth = {},
            onNextMonth = {},
            onDelete = {},
            onEditAccount = { _, _, _ -> },
            onEditCategory = {},
            upcoming = persistentListOf(
                previewTransaction(
                    type = TransactionType.EXPENSE,
                    account = previewAccount("Cash", color = GreenDark, currency = "BGN", id = UUID(1L, 2L)),
                    amount = 10.0,
                )
            ),
            updateAccountNameConfirmation = {},
            enableDeletionButton = true,
            deleteModal1Visible = false,
            onDeleteModal1Visible = {},
            skipAllModalVisible = false,
            onSkipAllModalVisible = {},
            onChoosePeriodModal = {},
            choosePeriodModal = null,
            screen = TransactionsScreen(),
            shouldShowAccountSpecificColorInTransactions = false
        )
    }
}

/** For screenshot testing */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    NivyPreview(theme) {
        Preview_empty_upcoming(isDark)
    }
}
