package com.nivy.transaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.nivy.base.model.TransactionType
import com.nivy.design.system.colors.Orange
import com.nivy.wallet.ui.previewAccount
import com.nivy.navigation.EditTransactionScreen
import com.nivy.navigation.NivyMaterial3Preview
import com.nivy.wallet.data.EditTransactionDisplayLoan
import com.nivy.wallet.domain.data.CustomExchangeRateState
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
    NivyMaterial3Preview(isDark) {
        UI(
            screen = EditTransactionScreen(null, TransactionType.EXPENSE),
            state = EditTransactionViewState(
                transactionType = TransactionType.INCOME,
                initialTitle = "",
                titleSuggestions = persistentSetOf(),
                currency = "BGN",
                description = null,
                dateTime = testDateTime,
                dueDate = null,
                creditCards = persistentListOf(),
                creditCard = null,
                chargeDate = null,
                canMoveChargeEarlier = false,
                accounts = persistentListOf(),
                categories = persistentListOf(),
                account = previewAccount("phyre", color = Orange, currency = "BGN"),
                toAccount = null,
                category = null,
                amount = 0.0,
                hasChanges = false,
                displayLoanHelper = EditTransactionDisplayLoan(),
                backgroundProcessingStarted = false,
                customExchangeRateState = CustomExchangeRateState(),
                tags = persistentListOf(),
                transactionAssociatedTags = persistentListOf(),
            ),
            onEvent = {},
        )
    }
}
