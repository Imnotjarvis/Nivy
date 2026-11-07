package com.ivy.loans.loandetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.data.model.Account
import com.ivy.data.model.Loan
import com.ivy.wallet.humanReadableType
import com.ivy.wallet.ui.component.ItemStatisticToolbar
import com.ivy.design.utils.clickableNoIndication
import com.ivy.wallet.utils.format
import com.ivy.design.utils.rememberInteractionSource
import com.ivy.design.utils.StatusBarDarkText
import com.ivy.loans.LoanProgress
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.TimeFormatter
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.MediumBlack
import com.ivy.design.system.colors.MediumWhite
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.ItemIconMDefaultIcon
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.wallet.ui.theme.components.ProgressBar
import com.ivy.wallet.ui.theme.components.getCustomIconIdS
import com.ivy.design.system.colors.dynamicContrast
import com.ivy.design.system.colors.readableOn
import com.ivy.design.system.colors.isDarkColor
import com.ivy.design.system.colors.toComposeColor
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.typography.numberBodyLarge
import com.ivy.design.system.typography.numberLabelMedium
import com.ivy.design.system.typography.numberTitleLarge

@Composable
internal fun Header(
    loan: Loan,
    baseCurrency: String,
    progress: LoanProgress,
    itemColor: Color,
    onAmountClick: () -> Unit,
    onEditLoan: () -> Unit,
    onDeleteLoan: () -> Unit,
    selectedLoanAccount: Account? = null,
    onAddRecord: () -> Unit
) {
    val contrastColor = readableOn(itemColor)

    val darkColor = isDarkColor(itemColor)
    StatusBarDarkText(darkText = !darkColor)

    Column(
        modifier = Modifier.background(itemColor)
    ) {
        Spacer(Modifier.height(20.dp))

        ItemStatisticToolbar(
            contrastColor = contrastColor,
            onEdit = onEditLoan,
            onDelete = onDeleteLoan
        )

        Spacer(Modifier.height(24.dp))

        LoanItem(
            loan = loan,
            contrastColor = contrastColor,
        ) {
            onEditLoan()
        }

        BalanceRow(
            modifier = Modifier
                .padding(start = 32.dp)
                .testTag("loan_amount")
                .clickableNoIndication(rememberInteractionSource()) {
                    onAmountClick()
                },
            textColor = contrastColor,
            currency = baseCurrency,
            balance = progress.total,
        )

        Spacer(Modifier.height(20.dp))

        LoanInfoCard(
            loan = loan,
            baseCurrency = baseCurrency,
            progress = progress,
            selectedLoanAccount = selectedLoanAccount,
            onAddRecord = onAddRecord
        )

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun LoanItem(
    loan: Loan,
    contrastColor: Color,

    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(start = 22.dp)
            .clickableNoIndication(rememberInteractionSource()) {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemIconMDefaultIcon(
            iconName = loan.icon?.id,
            defaultIcon = R.drawable.ic_custom_loan_m,
            tint = contrastColor
        )

        Spacer(Modifier.width(8.dp))

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.testTag("loan_name"),
                    text = loan.name.value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = contrastColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = loan.humanReadableType(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = loan.color.value.toComposeColor().dynamicContrast(),
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            loan.dateTime?.let {
                Text(
                    text = with(LocalTimeFormatter.current) {
                        it.format(TimeFormatter.Style.DateOnly(includeWeekDay = true))
                    },
                    style = MaterialTheme.typography.numberLabelMedium.copy(
                        color = contrastColor,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun LoanInfoCard(
    loan: Loan,
    baseCurrency: String,
    progress: LoanProgress,
    selectedLoanAccount: Account? = null,

    onAddRecord: () -> Unit
) {
    val backgroundColor = if (isDarkColor(loan.color.value)) {
        MediumBlack.copy(alpha = 0.9f)
    } else {
        MediumWhite.copy(alpha = 0.9f)
    }

    val contrastColor = readableOn(backgroundColor)
    val nav = navigation()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(backgroundColor, MaterialTheme.shapes.extraLarge),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                modifier = Modifier.padding(top = 8.dp, start = 24.dp),
                text = stringResource(R.string.paid),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = contrastColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            if (selectedLoanAccount != null) {
                IvyButton(
                    modifier = Modifier.padding(end = 16.dp, top = 12.dp),
                    backgroundColor = loan.color.value.toComposeColor(),
                    iconTint = contrastColor,
                    text = selectedLoanAccount.name.value,
                    iconStart = getCustomIconIdS(
                        iconName = selectedLoanAccount.icon?.id,
                        defaultIcon = R.drawable.ic_custom_account_s
                    ),
                    textStyle = MaterialTheme.typography.labelMedium.copy(
                        color = contrastColor,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    padding = 8.dp,
                    iconEdgePadding = 10.dp
                ) {
                    nav.navigateTo(
                        TransactionsScreen(
                            accountId = selectedLoanAccount.id.value,
                            categoryId = null
                        )
                    )
                }
            }
        }

        // Support UI for Old Versions where
        if (selectedLoanAccount == null) {
            Spacer(Modifier.height(12.dp))
        }

        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .testTag("amount_paid"),
            text = "${progress.paid.format(baseCurrency)} / ${progress.total.format(baseCurrency)}",
            style = MaterialTheme.typography.numberTitleLarge.copy(
                color = contrastColor,
                fontWeight = FontWeight.ExtraBold
            )
        )
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = IvyCurrency.fromCode(baseCurrency)?.name ?: "",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = contrastColor,
                fontWeight = FontWeight.Normal
            )
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.testTag("percent_paid"),
                text = "${progress.percentPaid.times(100).format(2)}%",
                style = MaterialTheme.typography.numberTitleLarge.copy(
                    color = contrastColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.width(8.dp))

            Text(
                modifier = Modifier.testTag("left_to_pay"),
                text = stringResource(
                    R.string.left_to_pay,
                    progress.left.format(baseCurrency),
                    baseCurrency
                ),
                style = MaterialTheme.typography.numberBodyLarge.copy(
                    color = Gray,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        ProgressBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .padding(horizontal = 24.dp),
            notFilledColor = MaterialTheme.colorScheme.surface,
            percent = progress.percentPaid
        )

        if (progress.interestPaid != 0.0) {
            HorizontalDivider(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidth(),
                thickness = 1.dp,
                color = contrastColor
            )

            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = stringResource(R.string.loan_interest),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = contrastColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.testTag("loan_interest_percent_paid"),
                    text = "${progress.interestPercentPaid.times(100).format(2)}%",
                    style = MaterialTheme.typography.numberTitleLarge.copy(
                        color = contrastColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    modifier = Modifier.testTag("interest_paid"),
                    text = stringResource(
                        R.string.interest_paid,
                        progress.interestPaid.format(baseCurrency),
                        baseCurrency
                    ),
                    style = MaterialTheme.typography.numberBodyLarge.copy(
                        color = Gray,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            ProgressBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .padding(horizontal = 24.dp),
                notFilledColor = MaterialTheme.colorScheme.surface,
                percent = progress.interestPercentPaid
            )
        }

        Spacer(Modifier.height(24.dp))

        IvyButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterHorizontally),
            text = stringResource(R.string.add_record),
            backgroundColor = contrastColor,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = readableOn(contrastColor),
                fontWeight = FontWeight.Bold
            ),
            wrapContentMode = false
        ) {
            onAddRecord()
        }

        Spacer(Modifier.height(12.dp))
    }
}
