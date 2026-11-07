package com.ivy.transaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ivy.base.model.TransactionType
import com.ivy.design.system.colors.Orange
import com.ivy.wallet.ui.previewAccount
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.IvyPreview
import com.ivy.wallet.domain.data.CustomExchangeRateState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import java.time.LocalDateTime
import java.time.ZoneOffset

/** For Preview purpose **/
private val testDateTime = LocalDateTime.of(2023, 4, 27, 0, 35)
    .toInstant(ZoneOffset.UTC)

@ExperimentalFoundationApi
@Preview
@Composable
private fun BoxWithConstraintsScope.Preview(isDark: Boolean = false) {
    IvyPreview(isDark) {
        UI(
            screen = EditTransactionScreen(null, TransactionType.EXPENSE),
            initialTitle = "",
            titleSuggestions = persistentSetOf(),
            tags = persistentListOf(),
            transactionAssociatedTags = persistentListOf(),
            baseCurrency = "BGN",
            dateTime = testDateTime,
            description = null,
            category = null,
            account = previewAccount("phyre", color = Orange, currency = "BGN"),
            toAccount = null,
            amount = 0.0,
            dueDate = null,
            transactionType = TransactionType.INCOME,
            customExchangeRateState = CustomExchangeRateState(),

            categories = persistentListOf(),
            accounts = persistentListOf(),

            onDueDateChange = {},
            onCategoryChange = {},
            onAccountChange = {},
            onToAccountChange = {},
            onDescriptionChange = {},
            onTitleChange = {},
            onAmountChange = {},

            onCreateCategory = { },
            onEditCategory = {},
            onPayPlannedPayment = {},
            onSave = {},
            onSetHasChanges = {},
            onDelete = {},
            onDuplicate = {},
            onCreateAccount = { },
            onSetDate = {},
            onSetTime = {},
            onSetTransactionType = {}
        )
    }
}
