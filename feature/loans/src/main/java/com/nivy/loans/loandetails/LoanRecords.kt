package com.nivy.loans.loandetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nivy.base.model.TransactionType
import com.nivy.base.model.processByType
import com.nivy.data.model.Account
import com.nivy.data.model.Loan
import com.nivy.data.model.LoanRecord
import com.nivy.base.model.LoanType
import com.nivy.wallet.ui.component.transaction.TypeAmountCurrency
import com.nivy.wallet.utils.format
import com.nivy.loans.loan.data.DisplayLoanRecord
import com.nivy.navigation.TransactionsScreen
import com.nivy.navigation.navigation
import com.nivy.ui.R
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.ui.time.TimeFormatter
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.MediumBlack
import com.nivy.design.system.colors.MediumWhite
import com.nivy.wallet.ui.theme.components.NivyButton
import com.nivy.wallet.ui.theme.components.NivyIcon
import com.nivy.wallet.ui.theme.components.getCustomIconIdS
import com.nivy.design.system.colors.isDarkColor
import com.nivy.design.system.colors.toComposeColor
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.typography.numberBodyLarge
import com.nivy.design.system.typography.numberLabelMedium

fun LazyListScope.loanRecords(
    loan: Loan,
    displayLoanRecords: List<DisplayLoanRecord> = emptyList(),
    onClick: (DisplayLoanRecord) -> Unit
) {
    items(items = displayLoanRecords, key = { it.loanRecord.id.value }) { displayLoanRecord ->
        LoanRecordItem(
            loan = loan,
            loanRecord = displayLoanRecord.loanRecord,
            baseCurrency = displayLoanRecord.loanRecordCurrencyCode,
            account = displayLoanRecord.account,
            loanBaseCurrency = displayLoanRecord.loanCurrencyCode
        ) {
            onClick(displayLoanRecord)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LoanRecordItem(
    loan: Loan,
    loanRecord: LoanRecord,
    baseCurrency: String,
    loanBaseCurrency: String = "",
    account: Account? = null,
    onClick: () -> Unit
) {
    val nav = navigation()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .clickable {
                onClick()
            }
            .testTag("loan_record_item")
    ) {
        if (account != null || loanRecord.interest) {
            Row(Modifier.padding(16.dp)) {
                if (account != null) {
                    NivyButton(
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        iconTint = MaterialTheme.colorScheme.onSurface,
                        text = account.name.value,
                        iconStart = getCustomIconIdS(
                            iconName = account.icon?.id,
                            defaultIcon = R.drawable.ic_custom_account_s
                        ),
                        textStyle = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        padding = 8.dp,
                        iconEdgePadding = 8.dp
                    ) {
                        nav.navigateTo(
                            TransactionsScreen(
                                accountId = account.id.value,
                                categoryId = null
                            )
                        )
                    }
                }

                if (loanRecord.interest) {
                    // Spacer(modifier = Modifier.width(8.dp))

                    val textIconColor = if (isDarkColor(loan.color.value)) MediumWhite else MediumBlack

                    NivyButton(
                        modifier = Modifier.padding(start = 8.dp),
                        backgroundColor = loan.color.value.toComposeColor(),
                        iconTint = textIconColor,
                        text = stringResource(R.string.interest),
                        iconStart = getCustomIconIdS(
                            iconName = "currency",
                            defaultIcon = R.drawable.ic_currency
                        ),
                        textStyle = MaterialTheme.typography.labelMedium.copy(
                            color = textIconColor,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        padding = 8.dp,
                        iconEdgePadding = 8.dp
                    ) {
                        // do Nothing
                    }
                }
            }
        } else {
            Spacer(Modifier.height(20.dp))
        }

        val timeFormatter = LocalTimeFormatter.current
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = with(timeFormatter) {
                loanRecord.dateTime.formatLocal(
                    TimeFormatter.Style.DateAndTime(includeWeekDay = true)
                )
            },
            style = MaterialTheme.typography.numberLabelMedium.copy(
                color = Gray,
                fontWeight = FontWeight.Bold
            )
        )

        loanRecord.note?.let { note ->
            Text(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                text = note.value,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        if (loanRecord.note == null) {
            Spacer(Modifier.height(16.dp))
        }
        val transactionType = when (loan.type) {
            LoanType.LEND -> {
                loanRecord.type.processByType(
                    increaseAction = { TransactionType.EXPENSE },
                    decreaseAction = { TransactionType.INCOME }
                )
            }

            LoanType.BORROW -> {
                loanRecord.type.processByType(
                    increaseAction = { TransactionType.INCOME },
                    decreaseAction = { TransactionType.EXPENSE }
                )
            }
        }
        TypeAmountCurrency(
            transactionType = transactionType,
            dueDate = null,
            currency = baseCurrency,
            amount = loanRecord.amount.value
        )

        loanRecord.convertedAmount?.let { convertedAmount ->
            Text(
                modifier = Modifier.padding(start = 68.dp),
                text = convertedAmount.value.format(baseCurrency) + " $loanBaseCurrency",
                style = MaterialTheme.typography.numberBodyLarge.copy(
                    color = Gray,
                    fontWeight = FontWeight.Normal
                )
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
internal fun InitialRecordItem(
    loan: Loan,
    amount: Double,
    baseCurrency: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .testTag("loan_record_item")
    ) {
        NivyButton(
            modifier = Modifier.padding(16.dp),
            backgroundColor = MaterialTheme.colorScheme.surface,
            text = stringResource(id = R.string.initial_loan_record),
            iconTint = MaterialTheme.colorScheme.onSurface,
            iconStart = getCustomIconIdS(
                iconName = loan.icon?.id,
                defaultIcon = R.drawable.ic_custom_loan_s
            ),
            textStyle = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            ),
            padding = 8.dp,
        ) {}

        val timeFormatter = LocalTimeFormatter.current

        loan.dateTime?.let { dateTime ->
            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = with(timeFormatter) {
                    dateTime.format(
                        TimeFormatter.Style.DateAndTime(includeWeekDay = true)
                    )
                },
                style = MaterialTheme.typography.numberLabelMedium.copy(
                    color = Gray,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        loan.note?.let { note ->
            Text(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                text = note.value,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        if (loan.note == null) {
            Spacer(Modifier.height(16.dp))
        }

        TypeAmountCurrency(
            transactionType = if (loan.type == LoanType.LEND) TransactionType.EXPENSE else TransactionType.INCOME,
            dueDate = null,
            currency = baseCurrency,
            amount = amount
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
internal fun NoLoanRecordsEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        NivyIcon(
            icon = R.drawable.ic_notransactions,
            tint = Gray
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.no_records),
            style = MaterialTheme.typography.titleLarge.copy(
                color = Gray,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.no_records_for_the_loan),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Gray,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )
    }
}
